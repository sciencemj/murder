package murder;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.*;

public class Main extends JavaPlugin implements Listener {
    Player murderer;
    Player gunner;
    ItemStack gun;
    ItemStack gunp;
    boolean game = false;
    Location loc1;
    Location loc2;
    int survivors;
    @Override
    public void onEnable() {
        super.onEnable();
        this.getCommand("murder").setExecutor(this);
        this.getServer().getPluginManager().registerEvents(this,this);
        gunp = new ItemStack(Material.BLAZE_POWDER, 1);
        ItemMeta meta = gunp.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "총 조각");
        gunp.setItemMeta(meta);
        gun = new ItemStack(Material.BLAZE_ROD, 1);
        meta = gun.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "총");
        gun.setItemMeta(meta);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                if (game){
                    Location l = randomLocation(loc1, loc2);
                    murderer.getWorld().dropItem(l, gunp);
                }
            }
        }, 0L,45L);
    }


    Location randomLocation(Location min, Location max)
    {
        Location range = new Location(min.getWorld(), Math.abs(max.getX() - min.getX()), Math.abs(max.getY() - min.getY()), Math.abs(max.getZ() - min.getZ()));
        return new Location(min.getWorld(), (Math.random() * range.getX()) + (min.getX() <= max.getX() ? min.getX() : max.getX()), (Math.random() * range.getY()) + (min.getY() <= max.getY() ? min.getY() : max.getY()), (Math.random() * range.getZ()) + (min.getZ() <= max.getZ() ? min.getZ() : max.getZ()));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equals("murder")) {
            if (args.length > 0) {
                if (sender instanceof Player) {
                    Player s = (Player) sender;
                    if (args[0].equals("set1")) {
                        s.sendMessage("pos1 set");
                        loc1 = s.getLocation();
                    } else if (args[0].equals("set2")) {
                        s.sendMessage("pos2 set");
                        loc2 = s.getLocation();
                    } else if (args[0].equals("stop")) {
                        survivors = 0;
                        gameSet();
                    }
                }else {
                    if (args[0].equals("set1")) {
                        loc1 = new Location(Bukkit.getOnlinePlayers().toArray(new Player[0])[0].getWorld(), Double.parseDouble(args[1]),Double.parseDouble(args[2]),Double.parseDouble(args[3]));
                    } else if (args[0].equals("set2")) {
                        loc2 = new Location(Bukkit.getOnlinePlayers().toArray(new Player[0])[0].getWorld(), Double.parseDouble(args[1]),Double.parseDouble(args[2]),Double.parseDouble(args[3]));
                    }
                }
            } else {
                Player[] ps = ((Collection<Player>) Bukkit.getOnlinePlayers()).toArray(new Player[0]);
                Random r = new Random();
                game = true;
                survivors = 1;
                murderer = ps[r.nextInt(ps.length)];
                murderer.sendTitle(ChatColor.RED + "머더입니다", "", 20, 20, 0);
                gunner = ps[r.nextInt(ps.length)];
                while (murderer == gunner && ps.length != 1)
                    gunner = ps[r.nextInt(ps.length)];
                gunner.sendTitle(ChatColor.GREEN + "총잡이입니다", "", 20, 20, 0);
                gunner.getInventory().addItem(gun);
                murderer.getInventory().addItem(new ItemStack(Material.IRON_SWORD, 1));
                for (Player p : ps) {
                    if (p != murderer && p != gunner) {
                        survivors++;
                        p.sendTitle(ChatColor.GREEN + "시민입니다", "", 20, 20, 0);
                    }
                }
            }
        }
        return false;
    }
    @EventHandler
    public void playerDamaged(EntityDamageByEntityEvent e){
        if (e.getDamager() instanceof Player){
            Player p = (Player) e.getDamager();
            if (e.getEntity() instanceof Player) {
                Player target = (Player) e.getEntity();
                if (p.equals(murderer) && p.getInventory().getItemInMainHand().getType().equals(Material.IRON_SWORD)) {
                    Block head = target.getLocation().getBlock();
                    head.setType(Material.PLAYER_HEAD);
                    BlockState state = head.getState();
                    Skull skull = (Skull) state;
                    UUID uuid = target.getUniqueId();
                    skull.setOwningPlayer(Bukkit.getServer().getOfflinePlayer(uuid));
                    skull.update();
                    /*ArmorStand stand = target.getWorld().spawn(target.getLocation(), ArmorStand.class);
                    stand.setHelmet(getHead(target));
                    stand.setArms(true);
                    stand.setCollidable(false);
                    stand.setLeftArmPose(new EulerAngle(Math.toRadians(-90),Math.toRadians(0),Math.toRadians(45)));
                    //stand.setBasePlate(false);*/
                    //target.setHealth(0D);
                    target.setGameMode(GameMode.SPECTATOR);
                    survivors--;
                    gameSet();
                }
            }
        }

    }

    @EventHandler
    public void playerInteract(PlayerInteractEvent e){
        Player p = e.getPlayer();
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK){
            if (p.getInventory().getItemInMainHand().getItemMeta() != null) {
                if (p.getInventory().getItemInMainHand().getItemMeta().getDisplayName().equals(gun.getItemMeta().getDisplayName())) {
                    Vector loc = p.getLocation().getDirection().multiply(0.5f);
                    Location block = p.getEyeLocation();
                    //block.add(0, 1.5, 0);
                    for (int i = 0; i < 40; i++) { // 20block range
                        block = new Vector(block.getX() + loc.getX(), block.getY() + loc.getY(), block.getZ() + loc.getZ()).toLocation(p.getWorld());
                        if (!block.getBlock().getType().equals(Material.AIR)) {
                            //block.getWorld().createExplosion(block,0.1f);
                            break;
                        }
                        Collection<Entity> entities = block.getWorld().getNearbyEntities(block, 0.5D, 0.5D, 0.5D);
                        for (Entity entity : entities) {
                            if (entity != p) {
                                if (entity instanceof Player) {
                                    Player t = (Player) entity;
                                    if (t.getGameMode() != GameMode.SPECTATOR) {
                                        Block head = t.getLocation().getBlock();
                                        head.setType(Material.PLAYER_HEAD);
                                        BlockState state = head.getState();
                                        Skull skull = (Skull) state;
                                        UUID uuid = t.getUniqueId();
                                        skull.setOwningPlayer(Bukkit.getServer().getOfflinePlayer(uuid));
                                        skull.update();
                                        t.setGameMode(GameMode.SPECTATOR);
                                        if (t == murderer) {
                                            for (Player player : Bukkit.getOnlinePlayers()) {
                                                player.sendTitle(ChatColor.GREEN + "시민 승리!!", "", 20, 20, 0);
                                            }
                                            game = false;
                                            break;
                                        } else {
                                            survivors--;
                                            gameSet();
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        p.getWorld().spawnParticle(Particle.COMPOSTER, block, 1, 0, 0, 0, 0);
                    }
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ANVIL_HIT, 1, 1);
                    p.getInventory().removeItem(gun);
                } else if (p.getInventory().getItemInMainHand().getType().equals(gunp.getType())) {
                    if (p.getInventory().containsAtLeast(gunp, 10)) {
                        gunp.setAmount(10);
                        p.getInventory().removeItem(gunp);
                        gunp.setAmount(1);
                        p.getInventory().addItem(gun);
                    }
                }
            }
        }
    }

    /*@EventHandler
    public void playerPickup(PlayerPickupItemEvent e){
        Player p = e.getPlayer();
        if (p.getInventory().containsAtLeast(gunp, 9) && e.getItem().getItemStack().equals(gunp)){
            gunp.setAmount(10);
            p.getInventory().removeItem(gunp);
            gunp.setAmount(1);
            e.getItem().setItemStack(new ItemStack(Material.AIR));
            p.getInventory().addItem(gun);
        }
    }*/


    public void gameSet(){
        if (survivors == 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle(ChatColor.RED + "머더 승리!", "", 20, 20, 0);
            }
            World world = murderer.getWorld();//get the world
            List<Entity> entList = world.getEntities();//get all entities in the world

            for(Entity current : entList) {//loop through the list
                if (current instanceof Item) {
                    if (((Item) current).getItemStack().getType().equals(gunp.getType()))
                        current.remove();
                }
            }
            game = false;
        }
    }


    @Override
    public void onDisable() {
        super.onDisable();
    }
}
