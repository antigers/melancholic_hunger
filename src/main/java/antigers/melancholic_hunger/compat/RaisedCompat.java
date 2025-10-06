package antigers.melancholic_hunger.compat;

import dev.yurisuika.raised.registry.LayerRegistry;
import dev.yurisuika.raised.util.Translate;
import net.minecraft.client.gui.DrawContext;

public class RaisedCompat {
    public static void startHotbarTranslate(DrawContext drawContext) {
        Translate.start(drawContext.getMatrices(), LayerRegistry.HOTBAR);
    }

    public static void endTranslate(DrawContext drawContext) {
        Translate.end(drawContext.getMatrices(), LayerRegistry.HOTBAR);
    }
}
