package antigers.melancholic_hunger.components;

import antigers.melancholic_hunger.MelancholicHunger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class Components {
    // Create the DeferredRegister for attachment types
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(
            NeoForgeRegistries.ATTACHMENT_TYPES, MelancholicHunger.MOD_ID
    );

    public static <T> Supplier<AttachmentType<T>> registerAttachment(String name, Supplier<AttachmentType<T>> sup) {
        return ATTACHMENT_TYPES.register(name, sup);
    }

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
        HealthRegenerationComponent.register();
    }
}
