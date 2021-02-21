package honeb1.levelheartskk;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import  honeb1.levelheartskk.Utilities;
import org.bukkit.scheduler.BukkitRunnable;
import org.w3c.dom.Attr;
import sun.nio.ch.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class LevelHeartsKK extends JavaPlugin implements Listener {
    LHConfiguration configuration;
    //levelheartsKK.maxhealth.n -> 体力nまで増加可能

    Map<String,Double> healthOnLogout = new HashMap<>();
    Map<String,Double> maxHealthOnLogout = new HashMap<>();
    FileConfiguration personalThreshold;
    File thresholdFile;
    public final int BASE_HEALTH =  20;
    

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);
        FileConfiguration fileConfiguration = getConfig();
        configuration = new LHConfiguration(this);
        thresholdFile = new File("plugins/LevelHeartsKK/personalThreshold.yml");
        if(!thresholdFile.exists()){
            try{
                thresholdFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        personalThreshold = new YamlConfiguration();
        try {
            personalThreshold.load(thresholdFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        try {
            personalThreshold.save(thresholdFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerLevelChange(PlayerLevelChangeEvent event){
        Player p = event.getPlayer();
        int newLevel = event.getNewLevel();
        if(newLevel < configuration.baseLevel + configuration.levelPerHeart) return;//Lv不足

        int newHealth = (int) Math.floor((newLevel - configuration.baseLevel)/configuration.levelPerHeart) * 2 + BASE_HEALTH;
        if(newHealth < Utilities.getBaseMaxHealth(p)) return;

        //権限確認
        if(getPermittedMaxHealth(p) <= newHealth) newHealth = getPermittedMaxHealth(p);

        //体力更新
        Utilities.setMaxHealth(p,newHealth);

        //体力コンパクト化
        rescale(p);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event){
        if(!event.getKeepInventory()){
            Player p = event.getEntity();
            Utilities.setMaxHealth(p,BASE_HEALTH);
            p.setHealthScaled(false);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        //ログアウト時の体力を記録
        Player p = event.getPlayer();
        healthOnLogout.put(p.getName(),p.getHealth());
        maxHealthOnLogout.put(p.getName(),Utilities.getBaseMaxHealth(p));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        //ログアウト時の体力を復元
        Player p = event.getPlayer();
        Double maxHealth = maxHealthOnLogout.get(p.getName());
        Double health = healthOnLogout.get(p.getName());
        if(maxHealth != null) Utilities.setMaxHealth(p,maxHealth);
        if(health != null) p.setHealth(health);
        //コンパクト化
        BukkitRunnable rescalingRunnable = new BukkitRunnable(){
            @Override
            public void run() {
                rescale(p);
            }
        };
        rescalingRunnable.runTaskLater(this,configuration.scalingDelay);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
        if(args.length == 0) {//チェック
            if(Utilities.checkAndNoticePermission(sender, "levelheartskk.check")){
                if(!(sender instanceof Player)){
                    sender.sendMessage("ゲーム内でのみ使用可能です。");
                    return false;
                }
                Player p = (Player)sender;
                p.sendMessage(ChatColor.WHITE + String.format("体力: %.1f / %.1f", p.getHealth(), Utilities.getMaxHealth(p)));
                return true;
            }
        }else if(args.length == 1 && args[0].equalsIgnoreCase("reload")){//再読み込み
            if(Utilities.checkAndNoticePermission(sender, "levelheartskk.reload")){
                configuration.loadConfig();
                for(Player p : getServer().getOnlinePlayers()){
                    rescale(p);
                }
                sender.sendMessage("[LevelHeartsKK] 設定ファイルを再読み込みしました。");
                return true;
            }
        }else if(args.length == 3 && args[0].equalsIgnoreCase("set")){
            if(Utilities.checkAndNoticePermission(sender, "levelheartskk.modify")){
                Player target = getServer().getPlayer(args[1]);
                if(target == null){
                    sender.sendMessage("プレイヤーが見つかりません。");
                    return false;
                }
                int healthToSet;
                try {
                    healthToSet = Integer.valueOf(args[2]);
                }catch (NumberFormatException e){
                    sender.sendMessage("有効な数字を入力してください。");
                    return false;
                }
                //設定
                Utilities.setMaxHealth(target,healthToSet);
                rescale(target);
                sender.sendMessage(target.getName() + "の最大体力を" + healthToSet + "に設定しました。");
            }
        }else if(args.length == 2 && args[0].equalsIgnoreCase("scale")){
            if(Utilities.checkAndNoticePermission(sender,"levelheartskk.check"))
            if(!(sender instanceof Player)){
                sender.sendMessage("ゲーム内でのみ使用可能です。");
                return false;
            }
            Player p = (Player)sender;
            int threshold;
            try {
                 threshold = Integer.valueOf(args[1]);
            }catch (NumberFormatException e){
                sender.sendMessage("有効な数字を入力してください。");
                return false;
            }
            personalThreshold.set(p.getName(),threshold);
            rescale(p);
            p.sendMessage("あなたの最大体力が"+threshold+"を超えた場合に割合で表示されるようになります。");
        }
        return false;
    }

    int getPermittedMaxHealth(Player p){
        int res = BASE_HEALTH;
        for(int i : configuration.healthPermList){
            Permission perm = new Permission(("levelheartsKK.maxhealth." + i), PermissionDefault.OP);
            if(!p.hasPermission(perm)) continue;
            //更新
            if(i > res ) res = i;
        }
        if(res > configuration.maxHealthForEveryone) res = configuration.maxHealthForEveryone;
        return res;
    }

    public void rescale(Player p){
        double maxHealth = Utilities.getMaxHealth(p);
        int threshold = personalThreshold.getInt(p.getName().toString());
        if(threshold == 0) threshold = configuration.scalingThreshold;
        if(maxHealth < threshold){
            p.setHealthScaled(false);
        }else{
            p.setHealthScaled(true);
            p.setHealthScale(threshold);
        }
    }
}
