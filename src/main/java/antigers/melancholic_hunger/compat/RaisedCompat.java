package antigers.melancholic_hunger.compat;

import dev.yurisuika.raised.registry.LayerRegistry;
import dev.yurisuika.raised.util.Translate;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class RaisedCompat {
    public static void startHotbarTranslate(GuiGraphicsExtractor graphics) {
//        Translate.start(graphics.pose(), LayerRegistry.HOTBAR);
    }

    public static void endTranslate(GuiGraphicsExtractor graphics) {
//        Translate.end(graphics.pose(), LayerRegistry.HOTBAR);
    }
}
