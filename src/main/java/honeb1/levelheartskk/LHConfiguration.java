package honeb1.levelheartskk;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class LHConfiguration {
    Plugin pl;
    ArrayList<Integer> healthPermList = new ArrayList<>();
    int maxHealthForEveryone;
    int scalingThreshold;
    int baseLevel;
    int levelPerHeart;
    int baseHealth;
    public LHConfiguration(JavaPlugin plugin){
        pl = plugin;
        loadConfig();
    }
    public void loadConfig(){
        pl.reloadConfig();
        FileConfiguration fileConfiguration = pl.getConfig();
        maxHealthForEveryone = fileConfiguration.getInt("max");
        scalingThreshold = fileConfiguration.getInt("scalingThreshold");
        baseLevel = fileConfiguration.getInt("baseLevel");
        levelPerHeart = fileConfiguration.getInt("levelPerHeart");
        healthPermList = (ArrayList<Integer>) fileConfiguration.getList("permittedHealthValues");
    }
}
