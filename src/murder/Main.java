package murder;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.*;

public class Main extends JavaPlugin implements Listener {
    Player murderer = null;
    String murderName;
    Player gunner = null;
    int timer = 0;
    ItemStack gun;
    ItemStack gunp;
    boolean game = false;
    Location loc1;
    Location loc2;
    BossBar bar = getServer().createBossBar("", BarColor.BLUE, BarStyle.SOLID, BarFlag.PLAY_BOSS_MUSIC);
    int gametimer = 420;
    float swordyaw;
    float swordpitch;
    ArrayList<ArmorStand> namehides = new ArrayList<ArmorStand>();
    ArrayList<Block> heads = new ArrayList<Block>();
    Queue<Location> swordpath = new LinkedList<Location>();
    ArmorStand stand;
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
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                if (stand != null) {
                    if (!swordpath.isEmpty()) {
                        //emurderer.sendMessage(swordpath.size() + "");
                        stand.teleport(swordpath.poll());
                        stand.setRotation(swordyaw, swordpitch);
                        Collection<Entity> entities = stand.getWorld().getNearbyEntities(stand.getLocation(), 1D, 1D, 1D);
                        for (Entity e : entities) {
                            if (e instanceof Player) {
                                Player ep = (Player) e;
                                if (ep != murderer) {
                                    headSet(ep);
                                    stand.remove();
                                    swordpath.clear();
                                    survivors--;
                                    ep.setGameMode(GameMode.SPECTATOR);
                                    gameSet();
                                }
                            }
                        }
                        armsetCenter(stand);
                    } else {
                        stand.remove();
                    }
                }
            }
        }, 0L,1L);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                if (game == true){
                    gametimer--;
                    if (gametimer >= 60) {
                        bar.setTitle(gametimer / 60 + "분 " + gametimer % 60 + "초");
                        bar.setProgress(gametimer/420D);
                    }else {
                        bar.setTitle(ChatColor.RED + "" + gametimer % 60 + "초");
                        bar.setProgress(gametimer/420D);
                    }
                    if (gametimer == 0){
                        murderer.setGameMode(GameMode.SPECTATOR);
                        gameSet();
                    }
                }
                if (murderer != null) {
                    if (timer > 0) {
                        timer--;
                        if (timer > 0) {
                            murderer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.GREEN + "쿨타임:" + timer + "초"));
                        } else {
                            murderer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.GREEN + "준비완료"));
                        }
                    } else {
                        murderer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.GREEN + "준비완료"));
                    }
                }
            }
        }, 0L,20L);
    }

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent e){
        Player p = e.getPlayer();
        for (Player s : Bukkit.getOnlinePlayers())
            s.sendMessage("join");
        if (game == true) {
            ArmorStand armor = p.getWorld().spawn(p.getLocation(), ArmorStand.class);
            armor.setVisible(false);
            armor.setInvulnerable(true);
            armor.setMarker(true);
            armor.setCustomNameVisible(false);
            namehides.add(armor);
            p.addPassenger(armor);
            if (p.getPlayer().getName().equals(murderName)) {
                murderer = p;
            }
            bar.addPlayer(p);
        }
    }

    public Location randomLocation(Location min, Location max)
    {
        Location range = new Location(min.getWorld(), Math.abs(max.getX() - min.getX()), Math.abs(max.getY() - min.getY()), Math.abs(max.getZ() - min.getZ()));
        return new Location(min.getWorld(), (Math.random() * range.getX()) + (min.getX() <= max.getX() ? min.getX() : max.getX()), (Math.random() * range.getY()) + (min.getY() <= max.getY() ? min.getY() : max.getY()), (Math.random() * range.getZ()) + (min.getZ() <= max.getZ() ? min.getZ() : max.getZ()));
    }

    public void armsetCenter(ArmorStand as){
        Location center = as.getLocation();
        Location arm = Rotate.getArmTip(as);
        Double x = center.getX() - arm.getX();
        Double y = center.getY() - arm.getY();
        Double z = center.getZ() - arm.getZ();
        as.teleport(center.add(x,y,z));
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
                    }else if (args[0].equals("set")){
                        murderer = s;
                    }
                }else {
                    if (args[0].equals("set1")) {
                        loc1 = new Location(Bukkit.getOnlinePlayers().toArray(new Player[0])[0].getWorld(), Double.parseDouble(args[1]),Double.parseDouble(args[2]),Double.parseDouble(args[3]));
                    } else if (args[0].equals("set2")) {
                        loc2 = new Location(Bukkit.getOnlinePlayers().toArray(new Player[0])[0].getWorld(), Double.parseDouble(args[1]),Double.parseDouble(args[2]),Double.parseDouble(args[3]));
                    }
                }
            } else {
                gameStart();
            }
        }
        return false;
    }

    public void gameStart(){
        Player[] ps = ((Collection<Player>) Bukkit.getOnlinePlayers()).toArray(new Player[0]);
        Random r = new Random();
        game = true;
        survivors = 1;
        murderer = ps[r.nextInt(ps.length)];
        murderer.sendTitle(ChatColor.RED + "머더입니다", "", 20, 20, 0);
        murderName = murderer.getName();
        gunner = ps[r.nextInt(ps.length)];
        while (murderer == gunner && ps.length != 1)
            gunner = ps[r.nextInt(ps.length)];
        gunner.sendTitle(ChatColor.GREEN + "총잡이입니다", "", 20, 20, 0);
        for (Player p : ps) {
            bar.addPlayer(p);
            Location rloc = randomLocation(loc1,loc2);
            while (rloc.getBlock().getType() != Material.AIR)
                rloc = randomLocation(loc1,loc2);
            p.teleport(rloc);
            p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 40, 254));
            p.getInventory().clear();
            ArmorStand armor = p.getWorld().spawn(p.getLocation(), ArmorStand.class);
            armor.setVisible(false);
            armor.setInvulnerable(true);
            armor.setMarker(true);
            armor.setCustomNameVisible(false);
            namehides.add(armor);
            p.addPassenger(armor);
            if (p != murderer && p != gunner) {
                survivors++;
                p.sendTitle(ChatColor.GREEN + "시민입니다", "", 20, 20, 0);
            }
        }
        gunner.getInventory().addItem(gun);
        murderer.getInventory().addItem(new ItemStack(Material.IRON_SWORD, 1));

        bar.setProgress(1);
        bar.setVisible(true);
    }
    @EventHandler
    public void playerDamaged(EntityDamageByEntityEvent e){
        if (e.getDamager() instanceof Player){
            Player p = (Player) e.getDamager();
            if (e.getEntity() instanceof Player) {
                Player target = (Player) e.getEntity();
                if (p.equals(murderer) && p.getInventory().getItemInMainHand().getType().equals(Material.IRON_SWORD)) {
                    headSet(target);
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
                                        headSet(t);
                                        t.setGameMode(GameMode.SPECTATOR);
                                        if (t == murderer) {
                                            gameSet();
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
                }else if (p.getInventory().getItemInMainHand().getType().equals(Material.IRON_SWORD)){
                    if (p == murderer){
                        if (timer == 0) {
                            stand = p.getWorld().spawn(p.getLocation().add(0,-1.5,0), ArmorStand.class);
                            stand.setItemInHand(new ItemStack(Material.IRON_SWORD));
                            stand.setRightArmPose(new EulerAngle(Math.toRadians(-10), Math.toRadians(0), Math.toRadians(0)));
                            //stand.setVelocitye(p.getLocation().getDirection().multiply(0.1D));
                            Vector loc = p.getLocation().getDirection().multiply(2);
                            Location block = p.getEyeLocation();
                            swordpath.clear();
                            swordyaw = p.getEyeLocation().getYaw();
                            swordpitch = p.getEyeLocation().getPitch();
                            stand.setCollidable(false);
                            stand.setVisible(false);
                            for (int i = 0; i < 8; i++) { // 10block range
                                block = new Vector(block.getX() + loc.getX(), block.getY() + loc.getY(), block.getZ() + loc.getZ()).toLocation(p.getWorld());
                                if (!block.getBlock().getType().equals(Material.AIR)) {
                                    //block.getWorld().createExplosion(block,0.1f);
                                    break;
                                }
                                swordpath.offer(block);
                            }
                            timer = 10;
                        }
                    }
                }
            }
        }
    }

    public void headSet(Player t){
        Block head = t.getLocation().getBlock();
        head.setType(Material.PLAYER_HEAD);
        BlockState state = head.getState();
        Skull skull = (Skull) state;
        UUID uuid = t.getUniqueId();
        skull.setOwningPlayer(Bukkit.getServer().getOfflinePlayer(uuid));
        skull.update();
        heads.add(head);
        for (Entity e : t.getPassengers())
            t.removePassenger(e);
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
            initialize();
        }else if (murderer.getGameMode() == GameMode.SPECTATOR){
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle(ChatColor.RED + "시민 승리!", "", 20, 20, 0);
            }
            World world = murderer.getWorld();//get the world
            List<Entity> entList = world.getEntities();//get all entities in the world

            for(Entity current : entList) {//loop through the list
                if (current instanceof Item) {
                    if (((Item) current).getItemStack().getType().equals(gunp.getType()))
                        current.remove();
                }
            }
            initialize();
        }
    }
    public void initialize(){
        murderer = null;
        murderName = null;
        gunner = null;
        game = false;
        gametimer = 420;
        bar.setVisible(false);
        for (Player p : Bukkit.getOnlinePlayers()){
            p.getInventory().clear();
        }
        for (Block b : heads){
            b.setType(Material.AIR);
        }
        for (ArmorStand armor : namehides){
            armor.remove();
        }
        namehides.clear();
    }



    @Override
    public void onDisable() {
        super.onDisable();
        initialize();
    }
}
