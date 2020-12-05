package honeb1.levelheartskk;

import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Utilities {
    public static void setMaxHealth(Player p, int health){
        p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
    }

    public static double getBaseMaxHealth(Player p){
        return p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
    }
    public static double getMaxHealth(Player p){
        return p.getMaxHealth();
    }
    public static boolean checkAndNoticePermission(CommandSender sender, String permStr){
        if(!sender.hasPermission(permStr)){
            sender.sendMessage("権限がありません。");
            return false;
        }else{
            return true;
        }
    }
}
