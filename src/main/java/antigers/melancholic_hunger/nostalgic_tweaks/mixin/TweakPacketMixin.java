package antigers.melancholic_hunger.nostalgic_tweaks.mixin;

import antigers.melancholic_hunger.components.ServerConfigComponent;
import antigers.melancholic_hunger.utils.ClientOnlyHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mod.adrenix.nostalgic.network.packet.tweak.TweakPacket;
import mod.adrenix.nostalgic.util.client.ClientTimer;
import mod.adrenix.nostalgic.util.server.ServerTimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.TimeUnit;

@Mixin(TweakPacket.class)
public interface TweakPacketMixin {

    @WrapOperation(
            method="changeOnServer",
            at=@At(
                    value="INVOKE",
                    target="Lmod/adrenix/nostalgic/util/server/ServerTimer;runAfter(JLjava/util/concurrent/TimeUnit;Ljava/lang/Runnable;)V"
            ),
            remap=false
    )
    private void melancholicHunger$changeOnServer(
            ServerTimer instance, long l, TimeUnit timeUnit, Runnable runnable, Operation<Void> original
    ) {
        // changeOnServer is called when server receives a tweak change from client (when a player changes some settings
        // through the Nostalgic Tweaks GUI).
        // It schedules a runnable that will call ConfigCache::save, which will call ConfigHandler::save
        original.call(instance, l, timeUnit, (Runnable) () -> {
            runnable.run();
            // Our ConfigHandlerMixin syncs received NT config changes to Melancholic config, however we still need to
            // sync these changes to all other players, so their Melancholic config also updates
            ServerConfigComponent.syncAllPlayers();
        });
    }

    @WrapOperation(
            method="changeOnClient",
            at=@At(
                    value="INVOKE",
                    target="Lmod/adrenix/nostalgic/util/client/ClientTimer;runAfter(JLjava/util/concurrent/TimeUnit;Ljava/lang/Runnable;)V"
            ),
            remap=false
    )
    private void melancholicHunger$changeOnClient(
            ClientTimer instance, long l, TimeUnit timeUnit, Runnable runnable, Operation<Void> original
    ) {
        // changeOnClient is called when client receives a tweak change from server.
        // runAfter is called when there's a LAN server on the current client (which makes the client also a server).
        // It schedules a runnable that will call ConfigCache::save, which will call ConfigHandler::save
        original.call(instance, l, timeUnit, (Runnable) () -> {
            runnable.run();
            // We need to sync all other players
            ServerConfigComponent.syncAllPlayersExceptOf(ClientOnlyHelper.getLocalPlayerId());
        });
    }
}
