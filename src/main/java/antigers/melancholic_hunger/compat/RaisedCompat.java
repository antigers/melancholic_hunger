package antigers.melancholic_hunger.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.yurisuika.raised.registry.LayerRegistry;
import dev.yurisuika.raised.util.Translate;

public class RaisedCompat {
    public static void startHotbarTranslate(PoseStack poseStack) {
        Translate.start(poseStack, LayerRegistry.HOTBAR);
    }

    public static void endTranslate(PoseStack poseStack) {
        Translate.end(poseStack, LayerRegistry.HOTBAR);
    }
}
