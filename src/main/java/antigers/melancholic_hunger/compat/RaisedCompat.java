package antigers.melancholic_hunger.compat;

import dev.yurisuika.raised.registry.LayerRegistry;
import dev.yurisuika.raised.util.Translate;
import net.minecraft.client.gui.GuiGraphics;

public class RaisedCompat {
    public static void startHotbarTranslate(GuiGraphics guiGraphics) {
//        Translate.start(guiGraphics.pose(), LayerRegistry.HOTBAR);
    }

    public static void endTranslate(GuiGraphics guiGraphics) {
//        Translate.end(guiGraphics.pose(), LayerRegistry.HOTBAR);
    }
}
