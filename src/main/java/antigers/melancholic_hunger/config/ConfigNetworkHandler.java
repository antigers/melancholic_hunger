package antigers.melancholic_hunger.config;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public class ConfigNetworkHandler {
    private static MinecraftServer SERVER_INSTANCE;

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

    private static void handleS2CPacket(ServerConfigData.ImmutableServerConfigData data, ClientPlayNetworking.Context context) {
        if (!Minecraft.getInstance().isSingleplayer()) {
            YACLConfig.setServerData(data);
        }
    }

    private static void handleC2SPacket(ServerConfigData.ImmutableServerConfigData data, ServerPlayNetworking.Context context) {
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

    /**
     * Sends config update to the server
     */
    public static void sendToServer(ServerConfigData.ImmutableServerConfigData serverConfigData) {
        ClientPlayNetworking.send(serverConfigData);
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(ServerConfigData.PAYLOAD_TYPE, ServerConfigData.PAYLOAD_STREAM_CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ServerConfigData.PAYLOAD_TYPE, ConfigNetworkHandler::handleS2CPacket);

        PayloadTypeRegistry.serverboundPlay().register(ServerConfigData.PAYLOAD_TYPE, ServerConfigData.PAYLOAD_STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ServerConfigData.PAYLOAD_TYPE, ConfigNetworkHandler::handleC2SPacket);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVER_INSTANCE = server);
        // syncing server config to the player after they join the server
        ServerPlayConnectionEvents.JOIN.register(
                (_, sender, _) -> sender.sendPacket(YACLConfig.getServerData())
        );
    }
}
