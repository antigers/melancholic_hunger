package antigers.melancholic_hunger.components;

import antigers.melancholic_hunger.InstalledMods;
import antigers.melancholic_hunger.config.ServerConfigData;
import antigers.melancholic_hunger.config.MelancholicConfig;
import antigers.melancholic_hunger.nostalgic_tweaks.NostalgicTweaksConfigHandlerWriter;
import antigers.melancholic_hunger.utils.ClientOnlyHelper;
import com.google.gson.Gson;
import mod.adrenix.nostalgic.config.factory.ConfigBuilder;
import mod.adrenix.nostalgic.tweak.factory.Tweak;
import mod.adrenix.nostalgic.tweak.factory.TweakPool;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public class ServerConfigComponent {

    private static class ConfigNetworkPacket {
        private static final Gson gson = new Gson();
        ServerConfigData.ImmutableServerConfigData configData;

        public ConfigNetworkPacket() {
            configData = MelancholicConfig.getServerData();
        }

        public ConfigNetworkPacket(ServerConfigData.ImmutableServerConfigData configData) {
            this.configData = configData;
        }

        public static ConfigNetworkPacket decode(FriendlyByteBuf buf) {
            return new ConfigNetworkPacket(gson.fromJson(buf.readUtf(), ServerConfigData.ImmutableServerConfigData.class));
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(gson.toJson(configData));
        }

        public void handle(NetworkEvent.Context context) {
            context.enqueueWork(() -> {
                if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    handleS2CPacket(configData);
                } else {
                    ServerPlayer sender = context.getSender();
                    if (sender != null) {
                        handleC2SPacket(sender, configData);
                    }
                }
            });
        }
    }

    public static void syncAllPlayers() {
        Networking.sendToAllPlayers(new ConfigNetworkPacket());
    }

    public static void syncAllPlayersExceptOf(int ignoredPlayerId) {
        for (var player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (player.getId() == ignoredPlayerId) {
                continue;
            }
            // sending update to every player
            Networking.sendToPlayer(player, new ConfigNetworkPacket());
        }
    }

    private static void handleS2CPacket(ServerConfigData.ImmutableServerConfigData configData) {
        if (!ClientOnlyHelper.isInSingleplayer()) {
            MelancholicConfig.setServerData(configData);

            if (InstalledMods.NOSTALGIC_TWEAKS) {
                var configHandler = (NostalgicTweaksConfigHandlerWriter) ConfigBuilder.getHandler();
                // updating client config in NT (ONLY client config), so it's in sync with melancholic
                configHandler.melancholic_hunger$writeConfigToNT(MelancholicConfig.getServerData(), MelancholicConfig.getClientData());
            }
        }
    }

    public static void syncNostalgicTweaksToAllPlayers() {
        TweakPool.filter(Tweak::isMultiplayerLike).forEach(Tweak::sendToAll);
    }

    /**
     * Handles config update from a player on the server side
     */
    public static void handleC2SPacket(ServerPlayer player, ServerConfigData.ImmutableServerConfigData configData) {
        if (!player.hasPermissions(2)) {
            // only for operators
            return;
        }
        boolean dataUpdated = MelancholicConfig.setServerData(configData);
        if (!dataUpdated) {
            return;
        }
        if (InstalledMods.NOSTALGIC_TWEAKS) {
            var configHandler = (NostalgicTweaksConfigHandlerWriter) ConfigBuilder.getHandler();
            configHandler.melancholic_hunger$writeConfigToNT(MelancholicConfig.getServerData(), null);
            syncNostalgicTweaksToAllPlayers();
        }
        syncAllPlayersExceptOf(player.getId());
        MelancholicConfig.saveToDisk();
    }

    /**
     * Sends config update to the server
     */
    public static void sendToServer() {
        Networking.sendToServer(new ConfigNetworkPacket());
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // syncing config for the player at the moment when the player has connected
            Networking.sendToPlayer(player, new ConfigNetworkPacket());
        }
    }

    public static void register() {
        Networking.registerPacket(ConfigNetworkPacket.class, ConfigNetworkPacket::encode, ConfigNetworkPacket::decode, ConfigNetworkPacket::handle);
        MinecraftForge.EVENT_BUS.addListener(ServerConfigComponent::onPlayerLogin);
    }
}
