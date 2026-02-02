package antigers.melancholic_hunger.components;

import antigers.melancholic_hunger.MelancholicHunger;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class Networking {
	private static final ResourceLocation CHANNEL_ID = ResourceLocation.fromNamespaceAndPath(
			MelancholicHunger.MOD_ID, "networking"
	);

	private static final String PROTOCOL_VERSION = "1";
	private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
			CHANNEL_ID,
			() -> PROTOCOL_VERSION,
			PROTOCOL_VERSION::equals,
			PROTOCOL_VERSION::equals
	);

	private static int id = 0;

	public static <T> void registerPacket(
			Class<T> packetClass, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, NetworkEvent.Context> handler
	) {
		id++;
		INSTANCE.registerMessage(id, packetClass, encoder, decoder, (packet,  supplier) -> handler.accept(packet, supplier.get()));
	}

	public static <T> void sendToServer(T packet) {
		INSTANCE.sendToServer(packet);
	}

	public static <T> void sendToPlayer(ServerPlayer player, T packet) {
		INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
	}

	public static <T> void sendToAllPlayers(T packet) {
		INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
	}
}
