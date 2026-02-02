package antigers.melancholic_hunger.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

public class ClientOnlyHelper {
    public static boolean isInSingleplayer() {
        return Minecraft.getInstance().isSingleplayer();
    }

    public static boolean hasSingleplayerServer() {
        return Minecraft.getInstance().hasSingleplayerServer();
    }

    public static LocalPlayer getLocalPlayer() {
        return Minecraft.getInstance().player;
    }

    public static Player getPlayerOnClient() {
        return getLocalPlayer();
    }

    public static int getLocalPlayerId() {
        return getLocalPlayer().getId();
    }
}
