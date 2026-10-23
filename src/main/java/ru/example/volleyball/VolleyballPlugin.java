package ru.example.volleyball;

import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public final class VolleyballPlugin extends JavaPlugin implements Listener {
    private NamespacedKey ballKey;

    @Override
    public void onEnable() {
        ballKey = new NamespacedKey(this, "volleyball");
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("volleyball") != null) {
            getCommand("volleyball").setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Эту команду может использовать только игрок.");
                    return true;
                }
                if (!player.hasPermission("volleyball.spawn")) {
                    player.sendMessage(ChatColor.RED + "У тебя нет прав на эту команду.");
                    return true;
                }
                spawnBall(player);
                player.sendMessage(ChatColor.GREEN + "Волейбольный мяч создан! Ударь его рукой.");
                return true;
            });
        }
        getLogger().info("VolleyballPlugin включён.");
    }

    private void spawnBall(Player player) {
        Location loc = player.getLocation().add(player.getLocation().getDirection().multiply(2));
        loc.setY(player.getLocation().getY() + 1.0);
        ArmorStand stand = player.getWorld().spawn(loc, ArmorStand.class, entity -> {
            entity.setInvisible(true);
            entity.setMarker(false);
            entity.setSmall(true);
            entity.setGravity(true);
            entity.setBasePlate(false);
            entity.setArms(false);
            entity.setInvulnerable(false);
            entity.setSilent(true);
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(ballKey, PersistentDataType.BYTE, (byte) 1);
            entity.setCustomName(ChatColor.YELLOW + "Волейбольный мяч");
            entity.setCustomNameVisible(false);
            entity.getEquipment().setHelmet(makeBallHead());
        });
    }

    private ItemStack makeBallHead() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Волейбольный мяч");
            meta.setLore(java.util.List.of(ChatColor.GRAY + "Ударь рукой, чтобы отбить мяч."));
            head.setItemMeta(meta);
        }
        return head;
    }

    private boolean isBall(Entity entity) {
        return entity instanceof ArmorStand stand
            && stand.getPersistentDataContainer().has(ballKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!isBall(event.getEntity())) return;
        event.setCancelled(true);

        if (!(event.getDamager() instanceof Player player)) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR
                && player.getInventory().getItemInOffHand().getType() != Material.AIR) {
            // Удар разрешён и с предметом в руке, если игрок атаковал сущность.
        }

        ArmorStand ball = (ArmorStand) event.getEntity();
        Vector direction = player.getLocation().getDirection().normalize();
        Vector velocity = direction.multiply(1.15);
        velocity.setY(Math.max(0.35, direction.getY() * 0.8 + 0.48));
        ball.setVelocity(velocity);
        ball.getWorld().playSound(ball.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.9f, 1.25f);
        ball.getWorld().spawnParticle(Particle.CRIT, ball.getLocation().add(0, 0.35, 0), 5, 0.15, 0.15, 0.15, 0.03);
    }

    @EventHandler
    public void onRightClickBall(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isBall(event.getRightClicked())) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        ArmorStand ball = (ArmorStand) event.getRightClicked();
        Vector direction = player.getLocation().getDirection().normalize();
        ball.setVelocity(direction.multiply(1.0).setY(Math.max(0.4, direction.getY() * 0.8 + 0.45)));
        ball.getWorld().playSound(ball.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.9f, 1.2f);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (isBall(event.getEntity())) event.setCancelled(true);
    }
}
