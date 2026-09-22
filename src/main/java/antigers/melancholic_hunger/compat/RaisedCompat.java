package antigers.melancholic_hunger.compat;

import dev.yurisuika.raised.util.Translate;
import net.minecraft.client.gui.GuiGraphics;

public class RaisedCompat {
    public static void startHotbarTranslate(GuiGraphics guiGraphics) {
        Translate.start(guiGraphics.pose(), "minecraft:hotbar");
    }

    public static void endTranslate(GuiGraphics guiGraphics) {
        Translate.end(guiGraphics.pose(), "minecraft:hotbar");
    }
}
