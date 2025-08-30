package antigers.melancholic_hunger.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

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

    public static int getLocalPlayerId() {
        return getLocalPlayer().getId();
    }
}
