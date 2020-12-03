package honeb1.levelheartskk;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import  honeb1.levelheartskk.Utilities;
import org.w3c.dom.Attr;
import sun.nio.ch.Util;

import javax.security.auth.login.Configuration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class LevelHeartsKK extends JavaPlugin implements Listener {
    ArrayList<Integer> healthPermList = new ArrayList<>();
    int maxHealthForEveryone = 1000;
    int scalingThreshold = 200;
    int baseLevel = 30;
    int levelPerHeart = 10;
    int baseHealth = 20;
    //levelheartsKK.maxhealth.n -> 体力nまで増加可能

    Map<String,Double> healthOnLogout = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);
        FileConfiguration configuration = getConfig();
        healthPermList = (ArrayList<Integer>) configuration.getList("permittedHealthValues");
        maxHealthForEveryone = configuration.getInt("max");
        scalingThreshold = configuration.getInt("scalingThreshold");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

    }

    @EventHandler
    public void onPlayerLevelChange(PlayerLevelChangeEvent event){
        Player p = event.getPlayer();
        int newLevel = event.getNewLevel();
        if(newLevel < baseLevel + levelPerHeart) return;//Lv不足

        int newHealth = (int) Math.floor((newLevel - baseLevel)/levelPerHeart) * 2 + baseHealth;
        if(newHealth < Utilities.getMaxHealth(p)) return;

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
            Utilities.setMaxHealth(p,baseHealth);
            p.setHealthScaled(false);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        //ログアウト時の体力を記録
        Player p = event.getPlayer();
        healthOnLogout.put(p.getName(),p.getHealth());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        //ログアウト時の体力を復元
        Player p = event.getPlayer();
        Double health = healthOnLogout.get(p.getName());
        if(health != null){
            p.setHealth(health);
        }
        //コンパクト化
        rescale(p);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
        if(args.length == 0) {//チェック
            if(!(sender instanceof Player)){
                sender.sendMessage("ゲーム内でのみ使用可能です。");
            }
            Player p = (Player)sender;
            p.sendMessage(ChatColor.WHITE + String.format("基礎体力: %.1f", Utilities.getMaxHealth(p)));
            return true;
        }else if(args.length == 3 && args[0].equalsIgnoreCase("set")){
            if(!sender.hasPermission("levelheartskk.modify")){
                sender.sendMessage("権限がありません。");
                return false;
            }
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
        return false;
    }

    int getPermittedMaxHealth(Player p){
        int res = baseHealth;
        for(int i : healthPermList){
            if(!p.hasPermission("levelheartsKK.maxhealth." + i)) continue;
            //更新
            if(i > res ) res = i;
        }
        if(res > maxHealthForEveryone) res = maxHealthForEveryone;
        return res;
    }

    public void rescale(Player p){
        double maxHealth = Utilities.getMaxHealth(p);
        if(maxHealth < scalingThreshold){
            p.setHealthScaled(false);
        }else{
            p.setHealthScaled(true);
            p.setHealthScale(scalingThreshold);
        }
    }
}
