package antigers.melancholic_hunger.components;

import net.minecraft.resources.ResourceLocation;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

public class PlayerComponents implements EntityComponentInitializer {
    public static final ComponentKey<HealthRegenerationComponent> HEALTH_REGENERATION = ComponentRegistry
            .getOrCreate(
                    ResourceLocation.fromNamespaceAndPath("melancholic_hunger", "health_regeneration"),
                    HealthRegenerationComponent.class
            );

    public static final ComponentKey<ServerConfigComponent> SERVER_CONFIG = ComponentRegistry
            .getOrCreate(
                    ResourceLocation.fromNamespaceAndPath("melancholic_hunger", "server_config"),
                    ServerConfigComponent.class
            );

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(
                HEALTH_REGENERATION, HealthRegenerationComponent::new, RespawnCopyStrategy.LOSSLESS_ONLY
        );
        registry.registerForPlayers(
                SERVER_CONFIG, ServerConfigComponent::new, RespawnCopyStrategy.ALWAYS_COPY
        );
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            ServerConfigComponent.register();
        }
    }
}
