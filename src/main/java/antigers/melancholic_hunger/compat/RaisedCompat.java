package antigers.melancholic_hunger.compat;

import dev.yurisuika.raised.util.Translate;
import net.minecraft.client.gui.GuiGraphics;

public class RaisedCompat {
    public static void startHotbarTranslate(GuiGraphics drawContext) {
        Translate.start(drawContext.pose(), "minecraft:hotbar");
    }

    public static void endTranslate(GuiGraphics drawContext) {
        Translate.end(drawContext.pose(), "minecraft:hotbar");
    }
}
