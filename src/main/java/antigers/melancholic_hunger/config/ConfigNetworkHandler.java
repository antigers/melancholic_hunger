package antigers.melancholic_hunger.config;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class ConfigNetworkHandler {

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

    private static void handleS2CPacket(ServerConfigData.ImmutableServerConfigData data) {
        if (!Minecraft.getInstance().isSingleplayer()) {
            YACLConfig.setServerData(data);
        }
    }

    private static void handleC2SPacket(ServerConfigData.ImmutableServerConfigData data, ServerPlayer player) {
        if (!player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))) {
            // only for operators
            return;
        }
        boolean dataUpdated = YACLConfig.setServerData(data);
        if (!dataUpdated) {
            return;
        }
        syncAllPlayersExceptOf(player.getId());
        YACLConfig.saveToDisk();
    }

    /**
     * Sends config update to the server
     */
    public static void sendToServer(ServerConfigData.ImmutableServerConfigData serverConfigData) {
        ClientPacketDistributor.sendToServer(serverConfigData);
    }

    private static void registerPayloadHandlersEventHandler(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar = registrar.executesOn(HandlerThread.NETWORK);
        registrar.playBidirectional(
                ServerConfigData.PAYLOAD_TYPE,
                ServerConfigData.PAYLOAD_STREAM_CODEC,
                (data, _) -> handleS2CPacket(data),
                (data, context) -> handleC2SPacket(data, (ServerPlayer) context.player())
        );
    }

    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ServerLifecycleHooks.getCurrentServer().isSingleplayer() && event.getEntity() instanceof ServerPlayer player) {
            // syncing config for the player at the moment when the player has connected
            PacketDistributor.sendToPlayer(player, YACLConfig.getServerData());
        }
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ConfigNetworkHandler::registerPayloadHandlersEventHandler);
        NeoForge.EVENT_BUS.addListener(ConfigNetworkHandler::onPlayerLogin);
    }
}
