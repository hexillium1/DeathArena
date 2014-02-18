package me.oliver276.kitpvp;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.chat.plugins.Chat_PermissionsEx;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

public class Main extends JavaPlugin implements Listener{

    private HashMap<String, ItemStack[]> mySavedItems = new HashMap<String, ItemStack[]>();

    private HashMap<String, ItemStack[]> MySavedAmour = new HashMap<String, ItemStack[]>();

    public void saveInventory(Player player){

        this.mySavedItems.put(player.getName(), copyInventory(player.getInventory()));
        this.MySavedAmour.put(player.getName(), player.getInventory().getArmorContents());
    }

    /**
     * This removes the saved inventory from our HashMap, and restores it to the player if it existed.
     * return true iff success
     */
    public boolean restoreInventory(Player player){

        ItemStack[] savedInventory = this.mySavedItems.remove(player.getName());
        if(savedInventory == null)
            return false;
        restoreInventory(player, savedInventory);

        ItemStack[] savedArmour = this.MySavedAmour.remove(player.getName());
        if(savedArmour != null){
            player.getInventory().setArmorContents(savedArmour);
        }
        return true;
    }

    private ItemStack[] copyInventory(Inventory inv){

        ItemStack[] original = inv.getContents();
        ItemStack[] copy = original.clone();

        return copy;
    }

    private void restoreInventory(Player p, ItemStack[] inventory)
    {
        p.getInventory().setContents(inventory);
    }


    //

    int tnt = 0;



    private HashMap<String,ItemStack[]> kit = new HashMap<String, ItemStack[]>();
    private HashMap<String,ItemStack[]> kitarm = new HashMap<String, ItemStack[]>();
    private HashMap<String,String> LastKit = new HashMap<String, String>();
    private HashMap<String,Collection<PotionEffect>> kitEffects = new HashMap<String, Collection<PotionEffect>>();
    private ArrayList<String> kits = new ArrayList<String>();
    private Location ArenaSpawn = null;
    private HashMap<String,Location> PreviousPos = new HashMap<String, Location>();
    private HashMap<String,Double> PrevHealth = new HashMap<String, Double>();
    public boolean hasEconomy = false;

    private List<String> kitlis = null;

    public ArrayList<Player> inGame = new ArrayList<Player>();

    public void Leave(Player p){
        inGame.remove(p);
        p.sendMessage(ChatColor.GOLD + "You left KitPvP!");
        Location location = PreviousPos.get(p.getName());
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.teleport(location);
        restoreInventory(p);
        p.setHealth(PrevHealth.remove(p.getName()));
        LastKit.remove(p.getName());
        PreviousPos.remove(p.getName());
        onIngameChange(0, p);
        for (PotionEffectType potionEffect : PotionEffectType.values()){
            try{
                p.removePotionEffect(potionEffect);
            }catch(NullPointerException ex){
            }
        }
        p.updateInventory();


    }

    public void Join(Player player,String kitname){
        for (PotionEffectType potionEffect : PotionEffectType.values()){
            try{
                player.removePotionEffect(potionEffect);
            }catch(NullPointerException ex){
            }
        }
        saveInventory(player);
        PreviousPos.put(player.getName(), player.getLocation());
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.teleport(ArenaSpawn);
        inGame.add(player);
        PrevHealth.put(player.getName(), player.getHealth());
        player.sendMessage(ChatColor.GREEN + "You joined KitPvP");
        player.getInventory().setContents(kit.get(kitname));
        player.getInventory().setArmorContents(kitarm.get(kitname));
        player.teleport(ArenaSpawn);
        LastKit.put(player.getName(), kitname);
        player.updateInventory();
        player.setGameMode(GameMode.SURVIVAL);
        for (PotionEffect pot : kitEffects.get(kitname)){
            player.addPotionEffect(pot);
        }
    }

    public static void onIngameChange(int Added,Player player){
    }

    public void GetStuff(){
        Bukkit.getPluginManager().registerEvents(this,this);
        getConfig().options().copyDefaults(true);
        saveConfig();
        if (!getConfig().contains("spawn.world")) return;
        try{
            getServer().getWorld("spawn.world");
            World w = Bukkit.getWorld(getConfig().getString("spawn.world"));
            ArenaSpawn = new Location(w,getConfig().getDouble("spawn.x"),getConfig().getDouble("spawn.y"),getConfig().getDouble("spawn.z"),Float.parseFloat(getConfig().getString("spawn.yaw")),Float.parseFloat(getConfig().getString("spawn.pitch")));
        }catch (Exception ex){
            System.out.print("World does not exist!");
        }
    }
    public void loadKit(String name){
        FileConfiguration cfg = null;
        File file = new File(this.getDataFolder(), name + ".kitinv");
        cfg = YamlConfiguration.loadConfiguration(file);
        List<ItemStack> Inv = (List<ItemStack>) cfg.getList("CONTENT");
        ItemStack[] itemStack = Inv.toArray(new ItemStack[36]);
        kit.put(name,itemStack);
    }
    public void loadKitArm(String name){
        FileConfiguration cfg = null;
        File file = new File(this.getDataFolder(), name + ".kitarm");
        cfg = YamlConfiguration.loadConfiguration(file);
        List<ItemStack> Inv = (List<ItemStack>) cfg.getList("CONTENT");
        System.out.println(Inv);
        ItemStack[] itemStack = Inv.toArray(new ItemStack[4]);
        kitarm.put(name,itemStack);
    }

    public void unscramble(){
        try {
            String thi = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            String str = thi.substring(0, thi.lastIndexOf('/'));
            String string = str + "/KitPvP/";
            File folder = new File(string);
            String[] files = folder.list();
            List<String> array = Arrays.asList(files);
            for (String st : array){
                if (st.endsWith("kitarm") || st.endsWith("yml") || st.endsWith(".kiteff")){
                    continue;
                }
                String stri = st.replaceAll(".kitinv","");
                if (stri.contains("kiteff")){
                    continue;
                }
                System.out.print("KitEff : " + stri);
                kits.add(stri);
            }
            System.out.println("Kits: " + kits);
        } catch (URISyntaxException ex) {

        }
    }

    public void saveKitArm(ItemStack[] stack, String name, Plugin plg){
        FileConfiguration cfg = null;
        File file = new File(plg.getDataFolder(), name + ".kitarm");
        cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("CONTENT", stack);
        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveKit(ItemStack[] stack, String name, Plugin plg){
        FileConfiguration cfg = null;
        File file = new File(plg.getDataFolder(), name + ".kitinv");
        cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("CONTENT", stack);
        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void saveKitPot(Collection<PotionEffect> effects, String name, Plugin plg){
        FileConfiguration cfg = null;
        File file = new File(plg.getDataFolder(), name + ".kiteff");
        cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("CONTENT", effects);
        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void loadKitPot(String name){
        FileConfiguration cfg = null;
        File file = new File(this.getDataFolder(), name + ".kiteff");
        cfg = YamlConfiguration.loadConfiguration(file);
        List<PotionEffect> Inv = (List<PotionEffect>) cfg.getList("CONTENT");
        Collection<PotionEffect> Effects = Inv;
        kitEffects.put(name,Effects);
    }
    public static Permission permission = null;
    public static Economy economy = null;
    public static Chat chat = null;

    private boolean setupPermissions()
    {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
        return (permission != null);
    }

    private boolean setupChat()
    {
        RegisteredServiceProvider<Chat> chatProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.chat.Chat.class);
        if (chatProvider != null) {
            chat = chatProvider.getProvider();
        }

        return (chat != null);
    }

    private boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }

    public void onEnable(){
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")){
            setupPermissions();
            setupChat();
            setupEconomy();
            hasEconomy = true;
        }
        //BukkitTask TaskName = new PlayerIdle(this).runTaskTimer(this, 20, 20);
        GetStuff();
        unscramble();
        System.out.print("Kits: " + kits);
        try{
        for (String s : kits){
            loadKit(s);
        }
        }catch (Exception ex){

        }
        try{
        for (String s : kits){
            loadKitArm(s);
        }
        }catch (Exception ex){

        }

        try{
        for (String s : kits){
            loadKitPot(s);
        }
        }catch (Exception ex){

        }

    }

    //@EventHandler (priority  = EventPriority.MONITOR)
    //public void onPlayerMove(PlayerMoveEvent e){
        //if (inGame.contains(e.getPlayer())){
          //  PlayerIdle.HeMoved(e.getPlayer());
        //}
    //}

    public void onDisable(){
        reloadConfig();
        getConfig().set("spawn.world",ArenaSpawn.getWorld().getName());
        getConfig().set("spawn.x",ArenaSpawn.getBlockX());
        getConfig().set("spawn.y",ArenaSpawn.getBlockY());
        getConfig().set("spawn.z",ArenaSpawn.getBlockZ());
        getConfig().set("spawn.yaw",ArenaSpawn.getYaw());
        getConfig().set("spawn.pitch",ArenaSpawn.getPitch());
        saveConfig();

        for (String s : kits){
            saveKit(kit.get(s),s,this);
        }
        for (String s : kits){
            saveKitArm(kitarm.get(s),s,this);
        }
        for (String s : kits){
            saveKitPot(kitEffects.get(s),s,this);
        }
        if (inGame.isEmpty()) return;
        for (Player player : inGame){
            restoreInventory(player);
            player.setHealth(PrevHealth.remove(player.getName()));
            player.teleport(PreviousPos.remove(player.getName()));
        }
    }

    @EventHandler
    public void onSignUpdate(SignChangeEvent e){
        if (!(e.getPlayer().hasPermission("kitpvp.signs"))){
            return;
        }
        if (!(e.getLine(0).equals("[KitPvP]"))){
            return;
        }
        if (e.getLine(1).equals("leave")){
            e.setLine(0,ChatColor.DARK_BLUE + "[KitPvP]");
            e.getPlayer().sendMessage(ChatColor.GOLD + "Leave sign created.");
            return;
        }
        if (e.getLine(1).equals("kit")){
            if (!(kits.contains(e.getLine(2)))){
                e.getPlayer().sendMessage(ChatColor.RED + "That kit does not exist!");
                e.setLine(0,ChatColor.DARK_RED + "[KitPvP]");
                return;
            }
            e.setLine(0,ChatColor.DARK_BLUE + "[KitPvP]");
            e.getPlayer().sendMessage(ChatColor.GOLD + "Kit sign created!");
            return;
        }
        if (e.getLine(1).equals("join")){
            if (!(kits.contains(e.getLine(2)))){
                e.getPlayer().sendMessage(ChatColor.RED + "That kit does not exist!");
                e.setLine(0,ChatColor.DARK_RED + "[KitPvP]");
                return;
            }
            e.setLine(0,ChatColor.DARK_BLUE + "[KitPvP]");
            e.getPlayer().sendMessage(ChatColor.GOLD + "Join sign created!");
            return;
        }
        e.getPlayer().sendMessage(ChatColor.RED + "Error: Can only be 'join', 'leave' or 'kit'");
        e.getBlock().breakNaturally();

    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e){
        try {
            if (e.getClickedBlock().getState().getType().equals(Material.SIGN_POST) || e.getClickedBlock().getState().getType().equals(Material.WALL_SIGN)) {
                Sign sign = (Sign) e.getClickedBlock().getState();
                if (!(sign.getLine(0).equalsIgnoreCase(ChatColor.DARK_BLUE + "[KitPvP]"))) return;
                if (sign.getLine(1).equalsIgnoreCase("join")) {
                    if (inGame.contains(e.getPlayer())) {
                        e.getPlayer().sendMessage(ChatColor.RED + "You're already in the game!  To change kits, use a kit sign.");
                        return;
                    }
                    if (!(kits.contains(sign.getLine(2)))) {
                        e.getPlayer().sendMessage(ChatColor.RED + "Sorry, that kit is not available");
                        return;
                    }
                    if (!(e.getPlayer().hasPermission("kitpvp.kit." + sign.getLine(2)))) {
                        e.getPlayer().sendMessage(ChatColor.RED + "You do not have permission for this kit!");
                        return;
                    }
                    Join(e.getPlayer(), sign.getLine(2));
                }
                if (sign.getLine(1).equalsIgnoreCase("kit")) {
                    if (!(inGame.contains(e.getPlayer()))) {
                        e.getPlayer().sendMessage(ChatColor.RED + "You're not in a game!");
                        return;
                    }
                    if (!kits.contains(sign.getLine(2)) || !kit.containsKey(sign.getLine(2)) || !kit.containsKey(sign.getLine(2))) {
                        e.getPlayer().sendMessage(ChatColor.RED + "Sorry, that kit is not available");
                        return;
                    }
                    if (!(e.getPlayer().hasPermission("kitpvp.kit." + sign.getLine(2)))) {
                        e.getPlayer().sendMessage(ChatColor.RED + "You do not have permission for this kit!");
                        return;
                    }
                    LastKit.remove(e.getPlayer().getName());
                    LastKit.put(e.getPlayer().getName(), sign.getLine(2));
                    e.getPlayer().sendMessage(ChatColor.GOLD + "You've changed kit to " +ChatColor.AQUA +  sign.getLine(2) + ChatColor.GOLD + ".  You'll recieve it when you next respawn!");
                    return;
                }
                if (sign.getLine(1).equalsIgnoreCase("leave")) {
                    if (!(inGame.contains(e.getPlayer()))) {
                        e.getPlayer().sendMessage(ChatColor.RED + "You can't leave a game you're not in!");
                        return;
                    }
                    Leave(e.getPlayer());
                }
            }
        } catch (Exception ex) {

        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent e){
        if(!(inGame.contains(e.getPlayer()))) return;

        String PlayerName = e.getPlayer().getName();
        String lastkit =  LastKit.get(PlayerName);

        for (PotionEffectType potionEffect : PotionEffectType.values()){
            try{
                e.getPlayer().removePotionEffect(potionEffect);
            }catch(NullPointerException ex){
            }
        }
        final String semi = lastkit;
        final Player semip = e.getPlayer();
        int i = this.getServer().getScheduler().scheduleSyncDelayedTask(this, new BukkitRunnable() {
            public void run() {
                semip.getInventory().setContents(kit.get(semi));
                semip.getInventory().setArmorContents(kitarm.get(semi));
                semip.addPotionEffects(kitEffects.get(semi));
            }
        }, 20L);


    }
    Plugin plugin = this;

    

    public void giveEffects(Player player, String lastkit){
        for (PotionEffect pot : kitEffects.get(lastkit)){
            player.addPotionEffect(pot);
        }
        player.updateInventory();
        player.getInventory().setContents(kit.get(lastkit));
        player.getInventory().setArmorContents(kitarm.get(lastkit));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e){
        if (!(inGame.contains(e.getPlayer()))) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void  onFoodLevelChange(FoodLevelChangeEvent e){
        Player player = (Player) e.getEntity();
        if (!(inGame.contains(player))) return;
        e.setFoodLevel(20);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e){

        if(inGame.contains(e.getPlayer())){
            Block b = e.getBlock();
            ItemStack s = new ItemStack(b.getType());
            if (s.getType().equals(new ItemStack(Material.TNT).getType())){
                Material material = e.getBlockReplacedState().getType();
                b.setType(material);
                World w = ArenaSpawn.getWorld();
                Location loc = e.getBlockPlaced().getLocation().add(0,1,0);
                w.spawnEntity(loc, EntityType.PRIMED_TNT);
                tnt = 1;
                Location loc1 = e.getBlockPlaced().getLocation();
                loc1.getWorld().playSound(loc,Sound.FUSE,1F,1F);
            }
        }
        return;
    }

    @EventHandler
    public void onEntityExplodeEvent(EntityExplodeEvent e){
        if (ArenaSpawn == null) return;
        World world = Bukkit.getWorld(this.ArenaSpawn.getWorld().getName());
        if (e.getLocation().getWorld().equals(world)){
            try{
                if (e.getEntity().getType().equals(EntityType.UNKNOWN)){
                    e.setCancelled(true);
                    return;
                }
            }catch (Exception ex){
                e.setCancelled(true);
                return;
            }
            if (e.getEntity().getType().equals(EntityType.PRIMED_TNT)){
                e.setCancelled(true);
                World w = ArenaSpawn.getWorld();
                w.createExplosion(e.getLocation(),4);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e){
        if(inGame.contains(e.getPlayer())){
            onIngameChange(0,e.getPlayer());
            Leave(e.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e){
        if (!(inGame.contains(e.getPlayer()))) return;
        if (e.getMessage().startsWith("/kitpvp") || e.getMessage().startsWith("/deatharena")) return;
        if  (!(e.getPlayer().getName().equals("Notch or Jeb"))){
            e.getPlayer().sendMessage(ChatColor.DARK_RED + "You can't use that in here. If you want to leave, use " + ChatColor.YELLOW + "/kitpvp leave");
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamagebyEntity(EntityDamageByEntityEvent e){
        if (e.getDamager() instanceof Player && e.getEntity() instanceof Player){
            Player attacker = (Player) e.getDamager();
            Player victim = (Player) e.getEntity();


            if (inGame.contains(attacker) && (!inGame.contains(victim))){
                e.setCancelled(true);
                attacker.sendMessage(ChatColor.RED + "You can't attack players who are not playing!");
            }

            if (inGame.contains(victim) && (!inGame.contains(attacker))){
                e.setCancelled(true);
                attacker.sendMessage(ChatColor.RED + "You can't attack players who are in the game!");
            }
        }else if(e.getDamager() instanceof Player){
            Player attacker = (Player) e.getDamager();
            if ((!(inGame.contains(attacker)))) return;
            e.setCancelled(true);
            }else if(e.getEntity() instanceof Player){
            Player victim = (Player) e.getEntity();

            if ((!(inGame.contains(victim))) || e.getDamager().getType().equals(EntityType.ARROW) || e.getDamager().getType().equals(EntityType.FISHING_HOOK) || e.getDamager().getType().equals(EntityType.SPLASH_POTION)) return;

            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e){
        try {
            if(inGame.contains(e.getEntity())){
                e.getDrops().clear();
            }
        if(inGame.contains(e.getEntity())&&inGame.contains(e.getEntity().getKiller())){
            e.getDrops().clear();
            e.setDeathMessage(null);
            Player died = e.getEntity();
            Player killer = died.getKiller();
            if (died == killer){                              // Make sure that the player hasn't killed them self
                Double hearts = killer.getHealth();
                Bukkit.getServer().broadcastMessage(ChatColor.AQUA + "[KitPvP] " + ChatColor.BLUE + killer.getName() + ChatColor.GOLD + " (on " + ( Math.round(hearts)) /2 + " hearts) just killed " + ChatColor.BLUE + died.getName() + ChatColor.GOLD + " and earned " + getConfig().getInt("moneyperkill")+ " " + ChatColor.BLUE+ economy.currencyNamePlural() + ChatColor.GOLD + ".");
                economy.depositPlayer(killer.getName(),getConfig().getInt("moneyperkill"));
                getConfig().set("stats.kills." + killer.getName().toLowerCase(),getConfig().getInt(("stats.kills." + killer.getName().toLowerCase())) + 1);
            }
            getConfig().set("stats.deaths." + died.getName().toLowerCase(),getConfig().getInt(("stats.deaths." + died.getName().toLowerCase())) + 1);

            saveConfig();

            e.getEntity().getInventory().clear();
        }
        }catch(Exception ex){

        }
    }


    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e){                             //Stop the players dropping anything
        if (inGame.contains(e.getPlayer())){
            e.setCancelled(true);
        e.getPlayer().sendMessage(ChatColor.RED + "You can't drop items in here!");
        }

    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(cmd.getName().equalsIgnoreCase("KitPvP")){
            int arg = args.length;
            if (arg == 0 || args[0].equalsIgnoreCase("help")){
                sender.sendMessage(ChatColor.GREEN + "-=-=-= KitPvP Help =-=-=-");
                sender.sendMessage(ChatColor.DARK_GREEN + "/KitPvP join <KitName>");
                sender.sendMessage(ChatColor.DARK_GREEN + "/KitPvP leave");
                sender.sendMessage(ChatColor.DARK_GREEN + "/KitPvP stats");
                sender.sendMessage(ChatColor.DARK_GREEN + "/KitPvP setinv <KitName>");
                sender.sendMessage(ChatColor.DARK_GREEN + "/KitPvP setspawn");
                sender.sendMessage(ChatColor.DARK_GREEN + "/KitPvP kit <KitName>");
                sender.sendMessage(ChatColor.DARK_GREEN + "/KitPvP removekit <KitName>");
                return true;

            }
            if (args[0].equalsIgnoreCase("removekit")){
                if (!(sender.hasPermission("kitpvp.removekit"))) {                                    //No permissions
                    sender.sendMessage(ChatColor.RED + "Try again... When you have permission.");
                    return true;
                }
                if (arg != 2){
                    sender.sendMessage(ChatColor.RED + "You should put 1 kit as an argument.");       //Haven't specified 1 kit
                    return true;
                }
                String kitname = args[1];
                if (!((kit.containsKey(kitname) || kits.contains(kitname) || kitarm.containsKey(kitname)))){                //Make sure the kit exists
                    sender.sendMessage(ChatColor.RED + "That kit was not found! xD");
                    return true;
                }
                kits.remove(kitname);
                kit.remove(kitname);
                kitarm.remove(kitname);
                String thi;
                try{
                    thi = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();                //Get the plugin's data folder
                }catch (Exception ex){
                  thi = "turd";
                }
                String str = thi.substring(0, thi.lastIndexOf('/'));
                String string = str + "/KitPvP/";
                if (new File(string + kitname + ".kitarm").exists()){
                    System.out.println(new File(string + kitname + ".kitarm").exists());
                    File s = new File(string);
                    if (!s.delete()){
                        s.deleteOnExit();
                    }
                }
                if (new File(string + kitname + ".kitinv").exists()){
                    System.out.println(new File(string + kitname + ".kitinv").exists());
                    File s = new File(string);
                    if (s.delete()){
                        s.deleteOnExit();
                    }
                }
                sender.sendMessage(ChatColor.GOLD + "Done, but you'll need to actually delete the files...");                           //Bug
                return true;
            }
            if (args[0].equalsIgnoreCase("kit")){
                if (!(sender instanceof Player)){
                    sender.sendMessage(ChatColor.RED + "You can't use this.");                     //Console
                    return true;
                }
                Player p = (Player) sender;
                if (!(inGame.contains(p))){
                    p.sendMessage(ChatColor.RED + "You aren't in an arena!");                         //Make sure they're in a game
                    return true;
                }
                if (arg == 1){
                    p.sendMessage(ChatColor.RED + "Specify a new kit.");
                    p.sendMessage(ChatColor.GOLD + "These are available" + ChatColor.DARK_AQUA + kits.toString());  //print the kits
                    return true;
                }
                String kitname = args[1];
                if (!(kits.contains(kitname))){
                    p.sendMessage(ChatColor.RED + "That kit does not exist.");
                    p.sendMessage(ChatColor.GOLD + "These are available" + ChatColor.DARK_AQUA + kits.toString());
                    return true;
                }
                if (!(p.hasPermission("kitpvp.kit." + kitname))){
                    p.sendMessage(ChatColor.RED + "You do not have permission for that kit.");                                      //Permission
                    p.sendMessage(ChatColor.GOLD + "These are available" + ChatColor.DARK_AQUA + kits.toString());
                    return true;
                }
                LastKit.remove(p.getName());
                LastKit.put(p.getName(),kitname);
                p.sendMessage(ChatColor.GOLD + "Changed your kit to " + kitname + " .  You will have this kit next time you respawn.");
                return true;
            }
            if (args[0].equalsIgnoreCase("stats")){
                if (!(sender instanceof Player)){
                    sender.sendMessage(ChatColor.RED + "The console cannot check their stats!");
                    return true;
                }
                reloadConfig();
                String player = null;
                if (arg == 1){
                    player = ((Player) sender).getName();
                }else{
                    player = args[1];
                }

                int kills = getConfig().getInt("stats.kills." + player.toLowerCase());
                int deaths = getConfig().getInt("stats.deaths." + player.toLowerCase());


                sender.sendMessage(ChatColor.GOLD + "-=-=-=-= KitPvP Stats =-=-=-=-");

                sender.sendMessage(ChatColor.GREEN + player + "'s Player Kills: " + ChatColor.DARK_GREEN + kills + ChatColor.GREEN + "!");
                sender.sendMessage(ChatColor.GREEN + player + "'s Deaths: " + ChatColor.DARK_GREEN + deaths + ChatColor.GREEN + "!");
                sender.sendMessage(ChatColor.GREEN + player + "'s K/D Ratio: " + ChatColor.DARK_GREEN + kills / deaths + ChatColor.GREEN + "!");
                return true;
            }
            if (args[0].equalsIgnoreCase("join")){
                if (!(sender instanceof Player)){
                    sender.sendMessage(ChatColor.RED + "The console cannot play :(");
                    return true;
                }
                if (!(sender.hasPermission("kitpvp.join"))){
                    sender.sendMessage(ChatColor.RED + "No permission :'(");
                    return true;
                }
                Player player = (Player) sender;

                if (ArenaSpawn == null){
                    sender.sendMessage(ChatColor.RED + "Error! The KitPvP spawn has not been set!");
                    return true;
                }

                if (kits.isEmpty() || kit.isEmpty() || kitarm.isEmpty()){
                    sender.sendMessage(ChatColor.RED + "Error! There are no available kits!");
                    return true;
                }

                if (inGame.contains(player)){
                    sender.sendMessage(ChatColor.DARK_RED + "You're already in the game - I know it's good, but carry on with the one you're in!");
                    return true;
                }
                if (arg < 2){
                    sender.sendMessage(ChatColor.DARK_RED + "You have not specified a kit! These kits currently exist:");
                    sender.sendMessage(ChatColor.GOLD + kits.toString());
                    return true;
                }
                String kitname = args[1];
                if ((!(kits.contains(kitname))) || (!(sender.hasPermission("kitpvp.kit." + kitname)))){
                    sender.sendMessage(ChatColor.DARK_RED + "The "+ kitname +" kit does not exist or you don't have permission for it!  These kits exist:");
                    sender.sendMessage(ChatColor.GOLD + kits.toString());
                    return true;
                }
                Join(player,kitname);
                return true;
            }
            if (args[0].equalsIgnoreCase("leave")){
                if (!(sender instanceof Player)) return true;
                Player p = (Player) sender;
                if (!(inGame.contains(p))){
                    p.sendMessage("You cannot leave the game - as you're not in one!");
                    return true;
                }
                Leave(p);
                return true;
            }
            if (args[0].equalsIgnoreCase("setspawn")){
                if (!(sender instanceof Player)){
                    sender.sendMessage(ChatColor.DARK_RED + "The console cannot use this!");
                    return true;
                }
                if (!(sender.hasPermission("kitpvp.setspawn"))){
                    sender.sendMessage(ChatColor.DARK_RED + "You haven't got permission...");
                    return true;
                }
                Player player = (Player) sender;
                ArenaSpawn = player.getLocation();
                sender.sendMessage(ChatColor.GREEN + "Done");
                return true;
            }
            if (args[0].equalsIgnoreCase("setinv")){
                if (!(sender instanceof Player)){
                    sender.sendMessage(ChatColor.RED + "Sorry, you can't use this...");
                    return true;
                }
                if (sender.hasPermission("kitpvp.setinv")){
                    if (args.length < 2){
                        sender.sendMessage(ChatColor.DARK_RED + "Sorry, but you have to specify a kit name.");
                        return true;
                    }

                    Player pl = (Player) sender;
                    String kitName = args[1];

                    if (kits.contains(kitName)){
                        kits.remove(kitName);
                        kit.remove(kitName);
                        kitarm.remove(kitName);
                        kitEffects.remove(kitName);
                    }
                    kit.put(kitName,pl.getInventory().getContents());
                    kitarm.put(kitName,pl.getInventory().getArmorContents());
                    kits.add(kitName);
                    kitEffects.put(kitName,pl.getActivePotionEffects());

                    pl.sendMessage(ChatColor.DARK_GREEN + "Inventory saved :)");
                }else{
                    sender.sendMessage(ChatColor.DARK_RED + "You cannot use this :D");
                }
                return true;
            }
            sender.sendMessage(ChatColor.RED + "Not a KitPvP command. Do " + ChatColor.YELLOW + "/kitpvp" + ChatColor.RED + " for command help.");
        }
        return true;
    }
}