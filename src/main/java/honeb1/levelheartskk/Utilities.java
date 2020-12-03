package honeb1.levelheartskk;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

public class Utilities {
    public static void setMaxHealth(Player p, int health){
        p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
    }

    public static double getMaxHealth(Player p){
        return p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
    }
}
