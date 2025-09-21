package antigers.melancholic_hunger.components;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import org.jetbrains.annotations.Nullable;

public class HealthRegenerationSyncHandler implements AttachmentSyncHandler<HealthRegenerationComponent> {
    @Override
    public boolean sendToPlayer(IAttachmentHolder holder, ServerPlayer to) {
        return holder == to; // only sync with the provider itself
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf, HealthRegenerationComponent attachment, boolean initialSync) {
        buf.writeNbt(attachment.serializeSyncData());
    }

    @Override
    public @Nullable HealthRegenerationComponent read(
            IAttachmentHolder holder, RegistryFriendlyByteBuf buf, @Nullable HealthRegenerationComponent previousValue
    ) {
        HealthRegenerationComponent attachment = holder.getData(PlayerComponents.HEALTH_REGENERATION);
        CompoundTag tag = buf.readNbt();
        if (tag != null) {
            attachment.deserializeSyncData(tag);
        }
        return attachment;
    }
}
