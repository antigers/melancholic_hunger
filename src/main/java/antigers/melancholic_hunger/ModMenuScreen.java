package antigers.melancholic_hunger;

import antigers.melancholic_hunger.config.YACLConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuScreen implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> YACLConfig.getYACLInstance().generateScreen(parent);
    }
}
