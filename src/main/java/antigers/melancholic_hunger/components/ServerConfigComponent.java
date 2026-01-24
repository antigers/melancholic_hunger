package antigers.melancholic_hunger.components;

import antigers.melancholic_hunger.MelancholicHunger;
import antigers.melancholic_hunger.config.ServerConfigData;
import antigers.melancholic_hunger.config.YACLConfig;
import antigers.melancholic_hunger.nostalgic_tweaks.NostalgicTweaksConfigHandlerWriter;
import com.google.gson.Gson;
import mod.adrenix.nostalgic.config.factory.ConfigBuilder;
import mod.adrenix.nostalgic.tweak.factory.Tweak;
import mod.adrenix.nostalgic.tweak.factory.TweakPool;
import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.entity.C2SSelfMessagingComponent;
import org.ladysnake.cca.api.v3.util.CheckEnvironment;

public class ServerConfigComponent implements AutoSyncedComponent, C2SSelfMessagingComponent {
    private static MinecraftServer serverInstance;

    private final Player player;
    private final Gson gson = new Gson();

    public ServerConfigComponent(Player player) {
        this.player = player;
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {}

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {}

    @Override
    public boolean shouldSyncWith(ServerPlayer player) {
        return player == this.player; // only sync with the provider itself
    }

    @Override
    @CheckEnvironment(EnvType.SERVER)
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        buf.writeUtf(gson.toJson(YACLConfig.getServerData()));
    }

    @Override
    @CheckEnvironment(EnvType.CLIENT)
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        if (!Minecraft.getInstance().isSingleplayer() && buf.isReadable()) {
            YACLConfig.setServerData(
                    gson.fromJson(buf.readUtf(), ServerConfigData.ImmutableServerConfigData.class)
            );
            if (MelancholicHunger.nostalgicTweaksInstalled) {
                var handler = (NostalgicTweaksConfigHandlerWriter) ConfigBuilder.getHandler();
                // updating client config in NT (ONLY client config), so it's in sync with melancholic
                handler.melancholic_hunger$writeConfigToNT(YACLConfig.getServerData(), YACLConfig.getClientData());
            }
        }
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
            PlayerComponents.SERVER_CONFIG.sync(player);
        }
    }

    public static void syncNostalgicTweaksToAllPlayers() {
        TweakPool.filter(Tweak::isMultiplayerLike).forEach(Tweak::sendToAll);
    }

    /**
     * Handles config update from a player on the server side
     */
    @Override
    @CheckEnvironment(EnvType.SERVER)
    public void handleC2SMessage(RegistryFriendlyByteBuf buf) {
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
            var handler = (NostalgicTweaksConfigHandlerWriter) ConfigBuilder.getHandler();
            handler.melancholic_hunger$writeConfigToNT(YACLConfig.getServerData(), null);
            syncNostalgicTweaksToAllPlayers();
        }
        syncAllPlayersExceptOf(player.getId());
        YACLConfig.saveToDisk();
    }

    /**
     * Sends config update to the server
     */
    @CheckEnvironment(EnvType.CLIENT)
    public void sendToServer(ServerConfigData.ImmutableServerConfigData serverConfigData) {
        sendC2SMessage(buf -> buf.writeUtf(gson.toJson(serverConfigData)));
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverInstance = server;
        });
    }
}
