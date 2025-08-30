package antigers.melancholic_hunger.components;

import antigers.melancholic_hunger.config.ServerConfigData;
import antigers.melancholic_hunger.config.YACLConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class ServerConfigComponent {
    public static final StreamCodec<ByteBuf, ServerConfigData.ImmutableServerConfigData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ServerConfigData.ImmutableServerConfigData::toJson,
            ServerConfigData.ImmutableServerConfigData::fromJson
    );

    public static void syncAllPlayers() {
        PacketDistributor.sendToAllPlayers(YACLConfig.getServerData());
    }

    public static void syncAllPlayersExceptOf(int ignoredPlayerId) {
        ServerConfigData.ImmutableServerConfigData data = YACLConfig.getServerData();
        for (var player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (player.getId() == ignoredPlayerId) {
                continue;
            }
            // sending update to every player
            PacketDistributor.sendToPlayer(player, data);
        }
    }

    @SubscribeEvent // on the mod event bus
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar = registrar.executesOn(HandlerThread.NETWORK);
        registrar.commonBidirectional(
                ServerConfigData.TYPE,
                STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        (data, context) -> {
                            // this handler is client side
                            if (!Minecraft.getInstance().isSingleplayer()) {
                                YACLConfig.setServerData(data);
                            }
                        },
                        (data, context) -> {
                            // this handler is server side
                            if (!context.player().hasPermissions(2)) {
                                // only for operators
                                return;
                            }
                            boolean dataUpdated = YACLConfig.setServerData(data);
                            if (!dataUpdated) {
                                return;
                            }
                            syncAllPlayersExceptOf(context.player().getId());
                            YACLConfig.saveToDisk();
                        }
                )
        );
    }

    /**
     * Sends config update to the server
     */
    public static void sendToServer(ServerConfigData.ImmutableServerConfigData serverConfigData) {
        PacketDistributor.sendToServer(serverConfigData);
    }
}
