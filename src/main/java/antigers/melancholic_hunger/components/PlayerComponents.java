package antigers.melancholic_hunger.components;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

import static antigers.melancholic_hunger.MelancholicHunger.MOD_ID;

public class PlayerComponents {
    // Create the DeferredRegister for attachment types
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(
            NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID
    );

    // Serialization via INBTSerializable
    public static final Supplier<AttachmentType<HealthRegenerationComponent>> HEALTH_REGENERATION = ATTACHMENT_TYPES.register(
            "handler", () -> AttachmentType
                    .serializable(
                            holder -> new HealthRegenerationComponent((Player) holder)
                    )
                    .sync(new HealthRegenerationSyncHandler())
                    .build()
    );

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
        // registering HealthRegenerationComponent in the bus, because it has ticking event handler
        NeoForge.EVENT_BUS.register(HealthRegenerationComponent.class);
        // registering HealthRegenerationComponent in the bus, because it has S2C and C2S payload event handlers
        modBus.register(ServerConfigComponent.class);
    }
}
