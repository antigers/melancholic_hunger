package antigers.melancholic_hunger.components;

import antigers.melancholic_hunger.config.ServerConfigData;
import antigers.melancholic_hunger.config.YACLConfig;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public class ServerConfigComponent {
    private static MinecraftServer SERVER_INSTANCE;

    public static final StreamCodec<ByteBuf, ServerConfigData.ImmutableServerConfigData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ServerConfigData.ImmutableServerConfigData::toJson,
            ServerConfigData.ImmutableServerConfigData::fromJson
    );

    public static void syncAllPlayers() {
        syncAllPlayersExceptOf(null);
    }

    public static void syncAllPlayersExceptOf(Integer ignoredPlayerId) {
        ServerConfigData.ImmutableServerConfigData data = YACLConfig.getServerData();
        for (var player : SERVER_INSTANCE.getPlayerList().getPlayers()) {
            if (ignoredPlayerId != null && player.getId() == ignoredPlayerId) {
                continue;
            }
            // sending update to every player
            ServerPlayNetworking.send(player, data);
        }
    }

    public static void handleS2CPacket(ServerConfigData.ImmutableServerConfigData data, ClientPlayNetworking.Context context) {
        if (!Minecraft.getInstance().isSingleplayer()) {
            YACLConfig.setServerData(data);
        }
    }

    public static void handleC2SPacket(ServerConfigData.ImmutableServerConfigData data, ServerPlayNetworking.Context context) {
        if (!context.player().permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))) {
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

    public static void onPlayerLogin(ServerGamePacketListenerImpl listener, PacketSender sender, MinecraftServer server) {
        if (listener.getPlayer() instanceof ServerPlayer player) {
            // syncing config for the player at the moment when the player has connected
            ServerPlayNetworking.send(player, YACLConfig.getServerData());
        }
    }

    /**
     * Sends config update to the server
     */
    public static void sendToServer(ServerConfigData.ImmutableServerConfigData serverConfigData) {
        ClientPlayNetworking.send(serverConfigData);
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(ServerConfigData.TYPE, STREAM_CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ServerConfigData.TYPE, ServerConfigComponent::handleS2CPacket);

        PayloadTypeRegistry.serverboundPlay().register(ServerConfigData.TYPE, STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ServerConfigData.TYPE, ServerConfigComponent::handleC2SPacket);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVER_INSTANCE = server);
        ServerPlayConnectionEvents.JOIN.register(
                (_, sender, _) -> sender.sendPacket(YACLConfig.getServerData())
        );
    }
}
