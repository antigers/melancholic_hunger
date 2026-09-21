package antigers.melancholic_hunger.compat.matcha_flavoured;

import antigers.melancholic_hunger.MelancholicHunger;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class MatchaInstalledFlag {
    private static final Identifier MATCHA_TEST_RESOURCE_ID = Identifier.fromNamespaceAndPath("matcha", "recipe/blast/steel_alloy.json");

    private static final AttachmentType<Boolean> MATCHA_INSTALLED_ATTACHMENT = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(MelancholicHunger.MOD_ID, "matcha_installed"),
            builder -> builder.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all())
    );

    public static boolean get(Level level) {
        if (level == null)
            return false;
        return level.globalAttachments().getAttachedOrElse(MATCHA_INSTALLED_ATTACHMENT, false);
    }

    private static void updateFlagValue(MinecraftServer server) {
        Optional<Resource> resource = server.getResourceManager().getResource(MATCHA_TEST_RESOURCE_ID);
        server.globalAttachments().setAttached(MATCHA_INSTALLED_ATTACHMENT, resource.isPresent());
    }

    public static void register() {
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, _, _) -> updateFlagValue(server));
        ServerLifecycleEvents.SERVER_STARTED.register(MatchaInstalledFlag::updateFlagValue);
    }
}
