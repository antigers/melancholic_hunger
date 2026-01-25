package antigers.melancholic_hunger.components;

import antigers.melancholic_hunger.MelancholicHunger;
import antigers.melancholic_hunger.config.ServerConfigData;
import antigers.melancholic_hunger.config.YACLConfig;
import antigers.melancholic_hunger.nostalgic_tweaks.NostalgicTweaksConfigHandlerWriter;
import com.google.gson.Gson;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import mod.adrenix.nostalgic.config.factory.ConfigBuilder;
import mod.adrenix.nostalgic.tweak.factory.Tweak;
import mod.adrenix.nostalgic.tweak.factory.TweakPool;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class ServerConfigComponent {
    private static MinecraftServer serverInstance;
    private static final Gson gson = new Gson();

    private static final ResourceLocation CONFIG_DATA_ID = new ResourceLocation(
            "melancholic_hunger", "server_config_component"
    );

    private static FriendlyByteBuf createConfigDataBuf() {
        FriendlyByteBuf buff = PacketByteBufs.create();
        buff.writeUtf(gson.toJson(YACLConfig.getServerData()));
        return buff;
    }

    public static void syncAllPlayers() {
        syncAllPlayersExceptOf(null);
    }

    public static void syncAllPlayersExceptOf(Integer ignoredPlayerId) {
        for (var player : serverInstance.getPlayerList().getPlayers()) {
            if (ignoredPlayerId != null && player.getId() == ignoredPlayerId) {
                continue;
            }
            // sending update to every player
            ServerPlayNetworking.send(player, CONFIG_DATA_ID, createConfigDataBuf());
        }
    }

    private static void handleS2CPacket(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
        if (!client.isSingleplayer() && buf.isReadable()) {
            YACLConfig.setServerData(
                    gson.fromJson(buf.readUtf(), ServerConfigData.ImmutableServerConfigData.class)
            );

            if (MelancholicHunger.nostalgicTweaksInstalled) {
                var configHandler = (NostalgicTweaksConfigHandlerWriter) ConfigBuilder.getHandler();
                // updating client config in NT (ONLY client config), so it's in sync with melancholic
                configHandler.melancholic_hunger$writeConfigToNT(YACLConfig.getServerData(), YACLConfig.getClientData());
            }
        }
    }

    public static void syncNostalgicTweaksToAllPlayers() {
        TweakPool.filter(Tweak::isMultiplayerLike).forEach(Tweak::sendToAll);
    }

    /**
     * Handles config update from a player on the server side
     */
    public static void handleC2SPacket(
            MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender
    ) {
        if (!player.hasPermissions(2)) {
            // only for operators
            return;
        }
        boolean dataUpdated = YACLConfig.setServerData(
                gson.fromJson(buf.readUtf(), ServerConfigData.ImmutableServerConfigData.class)
        );
        if (!dataUpdated) {
            return;
        }
        if (MelancholicHunger.nostalgicTweaksInstalled) {
            var configHandler = (NostalgicTweaksConfigHandlerWriter) ConfigBuilder.getHandler();
            configHandler.melancholic_hunger$writeConfigToNT(YACLConfig.getServerData(), null);
            syncNostalgicTweaksToAllPlayers();
        }
        syncAllPlayersExceptOf(player.getId());
        YACLConfig.saveToDisk();
    }

    /**
     * Sends config update to the server
     */
    public static void sendToServer() {
        ClientPlayNetworking.send(CONFIG_DATA_ID, createConfigDataBuf());
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverInstance = server;
        });
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(CONFIG_DATA_ID, ServerConfigComponent::handleS2CPacket);
        }
        ServerPlayNetworking.registerGlobalReceiver(CONFIG_DATA_ID, ServerConfigComponent::handleC2SPacket);

        // syncing server config to the player after they join the server
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> sender.sendPacket(CONFIG_DATA_ID, createConfigDataBuf())
        );
    }
}
