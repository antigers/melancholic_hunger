package antigers.melancholic_hunger.config;

import antigers.melancholic_hunger.ModLoader;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public class ConfigNetworkHandler {

    public static void syncAllPlayers() {
        syncAllPlayersExceptOf(null);
    }

    public static void syncAllPlayersExceptOf(Integer ignoredPlayerId) {
        ServerConfigData.ImmutableServerConfigData data = MelancholicConfig.getServerData();
        for (var player : ModLoader.getServerInstance().getPlayerList().getPlayers()) {
            if (ignoredPlayerId != null && player.getId() == ignoredPlayerId) {
                continue;
            }
            // sending update to every player
            ServerPlayNetworking.send(player, data);
        }
    }

    private static void handleS2CPacket(ServerConfigData.ImmutableServerConfigData data) {
        if (!Minecraft.getInstance().hasSingleplayerServer()) {
            MelancholicConfig.setServerData(data);
        }
    }

    private static void handleC2SPacket(ServerConfigData.ImmutableServerConfigData data, ServerPlayer player) {
        if (!player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))) {
            // only for operators
            return;
        }
        boolean dataUpdated = MelancholicConfig.setServerData(data);
        if (!dataUpdated) {
            return;
        }
        syncAllPlayersExceptOf(player.getId());
        MelancholicConfig.saveToDisk();
    }

    /**
     * Sends config update to the server
     */
    public static void sendToServer(ServerConfigData.ImmutableServerConfigData serverConfigData) {
        ClientPlayNetworking.send(serverConfigData);
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(ServerConfigData.PAYLOAD_TYPE, ServerConfigData.PAYLOAD_STREAM_CODEC);
        ClientPlayNetworking.registerGlobalReceiver(
                ServerConfigData.PAYLOAD_TYPE, (data, _) -> handleS2CPacket(data)
        );

        PayloadTypeRegistry.serverboundPlay().register(ServerConfigData.PAYLOAD_TYPE, ServerConfigData.PAYLOAD_STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(
                ServerConfigData.PAYLOAD_TYPE, (data, context) -> handleC2SPacket(data, context.player())
        );

        // syncing server config to the player after they join the server
        ServerPlayConnectionEvents.JOIN.register(
                (_, sender, _) -> sender.sendPacket(MelancholicConfig.getServerData())
        );
    }
}
