package antigers.melancholic_hunger.components;

import antigers.melancholic_hunger.InstalledMods;
import antigers.melancholic_hunger.config.ServerConfigData;
import antigers.melancholic_hunger.config.MelancholicConfig;
import io.netty.buffer.ByteBuf;
import antigers.melancholic_hunger.nostalgic_tweaks.NostalgicTweaksConfigHandlerWriter;
import mod.adrenix.nostalgic.config.factory.ConfigBuilder;
import mod.adrenix.nostalgic.tweak.factory.Tweak;
import mod.adrenix.nostalgic.tweak.factory.TweakPool;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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
        PacketDistributor.sendToAllPlayers(MelancholicConfig.getServerData());
    }

    public static void syncAllPlayersExceptOf(int ignoredPlayerId) {
        ServerConfigData.ImmutableServerConfigData data = MelancholicConfig.getServerData();
        for (var player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (player.getId() == ignoredPlayerId) {
                continue;
            }
            // sending update to every player
            PacketDistributor.sendToPlayer(player, data);
        }
    }

    public static void syncNostalgicTweaksToAllPlayers() {
        TweakPool.filter(Tweak::isMultiplayerLike).forEach(Tweak::sendToAll);
    }

    public static void registerPayloadHandlersEventHandler(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar = registrar.executesOn(HandlerThread.NETWORK);
        registrar.commonBidirectional(
                ServerConfigData.TYPE,
                STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        (data, context) -> {
                            // this handler is client side
                            if (!Minecraft.getInstance().isSingleplayer()) {
                                MelancholicConfig.setServerData(data);
                                if (InstalledMods.NOSTALGIC_TWEAKS) {
                                    var handler = (NostalgicTweaksConfigHandlerWriter) ConfigBuilder.getHandler();
                                    // updating client config in NT (ONLY client config), so it's in sync with melancholic
                                    handler.melancholic_hunger$writeConfigToNT(
                                            MelancholicConfig.getServerData(), MelancholicConfig.getClientData()
                                    );
                                }
                            }
                        },
                        (data, context) -> {
                            // this handler is server side
                            if (!context.player().hasPermissions(2)) {
                                // only for operators
                                return;
                            }
                            boolean dataUpdated = MelancholicConfig.setServerData(data);
                            if (!dataUpdated) {
                                return;
                            }
                            if (InstalledMods.NOSTALGIC_TWEAKS) {
                                var handler = (NostalgicTweaksConfigHandlerWriter) ConfigBuilder.getHandler();
                                handler.melancholic_hunger$writeConfigToNT(MelancholicConfig.getServerData(), null);
                                syncNostalgicTweaksToAllPlayers();
                            }
                            syncAllPlayersExceptOf(context.player().getId());
                            MelancholicConfig.saveToDisk();
                        }
                )
        );
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // syncing config for the player at the moment when the player has connected
            PacketDistributor.sendToPlayer(player, MelancholicConfig.getServerData());
        }
    }

    /**
     * Sends config update to the server
     */
    public static void sendToServer(ServerConfigData.ImmutableServerConfigData serverConfigData) {
        PacketDistributor.sendToServer(serverConfigData);
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ServerConfigComponent::registerPayloadHandlersEventHandler);
        NeoForge.EVENT_BUS.addListener(ServerConfigComponent::onPlayerLogin);
    }
}
