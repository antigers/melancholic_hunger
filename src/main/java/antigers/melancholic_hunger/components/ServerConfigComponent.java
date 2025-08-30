package antigers.melancholic_hunger.components;

import antigers.melancholic_hunger.config.ServerConfigData;
import antigers.melancholic_hunger.config.YACLConfig;
import com.google.gson.Gson;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.entity.C2SSelfMessagingComponent;
import org.ladysnake.cca.api.v3.util.CheckEnvironment;

public class ServerConfigComponent implements AutoSyncedComponent, C2SSelfMessagingComponent {
    private static MinecraftServer serverInstance;

    private final PlayerEntity player;
    private final Gson gson = new Gson();

    public ServerConfigComponent(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {}

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {}

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player; // only sync with the provider itself
    }

    @Override
    @CheckEnvironment(EnvType.SERVER)
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeString(gson.toJson(YACLConfig.getServerData()));
    }

    @Override
    @CheckEnvironment(EnvType.CLIENT)
    public void applySyncPacket(RegistryByteBuf buf) {
        if (!MinecraftClient.getInstance().isInSingleplayer() && buf.isReadable()) {
            YACLConfig.setServerData(
                    gson.fromJson(buf.readString(), ServerConfigData.ImmutableServerConfigData.class)
            );
        }
    }

    public static void syncAllPlayers() {
        syncAllPlayersExceptOf(null);
    }

    public static void syncAllPlayersExceptOf(Integer ignoredPlayerId) {
        for (var player : serverInstance.getPlayerManager().getPlayerList()) {
            if (ignoredPlayerId != null && player.getId() == ignoredPlayerId) {
                continue;
            }
            // sending update to every player
            PlayerComponents.SERVER_CONFIG.sync(player);
        }
    }

    /**
     * Handles config update from a player on the server side
     */
    @Override
    @CheckEnvironment(EnvType.SERVER)
    public void handleC2SMessage(RegistryByteBuf buf) {
        if (!player.hasPermissionLevel(2)) {
            // only for operators
            return;
        }
        boolean dataUpdated = YACLConfig.setServerData(
                gson.fromJson(buf.readString(), ServerConfigData.ImmutableServerConfigData.class)
        );
        if (!dataUpdated) {
            return;
        }
        syncAllPlayersExceptOf(player.getId());
        YACLConfig.saveToDisk();
    }

    /**
     * Sends config update to the server
     */
    @CheckEnvironment(EnvType.CLIENT)
    public void sendToServer(ServerConfigData.ImmutableServerConfigData serverConfigData) {
        sendC2SMessage(buf -> buf.writeString(gson.toJson(serverConfigData)));
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverInstance = server;
        });
    }
}
