package antigers.melancholic_hunger;

import antigers.melancholic_hunger.config.MelancholicConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuScreen implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> MelancholicConfig.getYACLInstance().generateScreen(parent);
    }
}
