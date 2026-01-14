package antigers.melancholic_hunger.config;

import antigers.melancholic_hunger.MelancholicHunger;
import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;

public class ServerConfigData {
    public Boolean disableHunger;
    public HungerEffectOption hungerEffect;
    public Boolean gradualHealthRegeneration;
    public Float gradualHealthRegenerationSpeed;
    public Boolean useCustomFoodStackSizes;
    public LinkedHashMap<String, Integer> customFoodStackSizes;
    public SprintingOption sprinting;
    public Integer sprintingHealthLimit;
    public Boolean instantEating;
    public Boolean showFoodItemTooltips;

    public static final CustomPacketPayload.Type<ImmutableServerConfigData> PAYLOAD_TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(MelancholicHunger.MOD_ID, "server_config_component")
    );
    public static final StreamCodec<ByteBuf, ImmutableServerConfigData> PAYLOAD_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ServerConfigData.ImmutableServerConfigData::toJson,
            ServerConfigData.ImmutableServerConfigData::fromJson
    );

    public static final Gson gson = new Gson();

    public record ImmutableServerConfigData (
            Boolean disableHunger,
            HungerEffectOption hungerEffect,
            Boolean gradualHealthRegeneration,
            Float gradualHealthRegenerationSpeed,
            Boolean useCustomFoodStackSizes,
            LinkedHashMap<String, Integer> customFoodStackSizes,
            SprintingOption sprinting,
            Integer sprintingHealthLimit,
            Boolean instantEating,
            Boolean showFoodItemTooltips
    ) implements CustomPacketPayload {

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return PAYLOAD_TYPE;
        }

        public String toJson() {
            return gson.toJson(this);
        }

        public static ImmutableServerConfigData fromJson(String json) {
            return gson.fromJson(json, ImmutableServerConfigData.class);
        }
    }

    public ImmutableServerConfigData getImmutable() {
        return new ImmutableServerConfigData(
                disableHunger, hungerEffect, gradualHealthRegeneration, gradualHealthRegenerationSpeed,
                useCustomFoodStackSizes, customFoodStackSizes, sprinting, sprintingHealthLimit, instantEating,
                showFoodItemTooltips
        );
    }
}
