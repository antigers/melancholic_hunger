package antigers.melancholic_hunger.nostalgic_tweaks.mixin;

import antigers.melancholic_hunger.config.*;
import antigers.melancholic_hunger.nostalgic_tweaks.NostalgicTweaksConfigHandlerWriter;
import mod.adrenix.nostalgic.config.ClientConfig;
import mod.adrenix.nostalgic.config.ServerConfig;
import mod.adrenix.nostalgic.config.factory.ConfigHandler;
import mod.adrenix.nostalgic.config.factory.ConfigMeta;
import mod.adrenix.nostalgic.tweak.listing.ItemMap;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.LinkedHashMap;

@Mixin(ConfigHandler.class)
public abstract class ConfigHandlerMixin<T extends ConfigMeta> implements NostalgicTweaksConfigHandlerWriter {
    @Shadow(remap=false) private @Nullable T loaded;
    @Shadow(remap=false) public abstract boolean load();
    @Shadow(remap=false) public abstract void save();
    @Shadow(remap=false) @Final private Runnable onLoad;

    @Unique
    void setGameplayConfigFromNT(
            boolean disableHunger, boolean preventHungerEffect, boolean disableSprint, ItemMap<Integer> customFoodHealth,
            boolean oldFoodStacking, ItemMap<Integer> customFoodStacking, ItemMap<Integer> customItemStacking,
            boolean instantEat
    ) {
        var currentServerData = YACLConfig.getServerData();
        var serverData = new ServerConfigData();
        serverData.disableHunger = disableHunger;
        serverData.instantEating = instantEat;
        if (preventHungerEffect) {
            serverData.hungerEffect = HungerEffectOption.DISABLED;
        }
        else if (currentServerData.hungerEffect() != HungerEffectOption.DISABLED) {
            serverData.hungerEffect = currentServerData.hungerEffect();
        }
        else if (disableHunger) {
            serverData.hungerEffect = HungerEffectOption.REPLACED_WITH_POISON;
        }
        else {
            serverData.hungerEffect = HungerEffectOption.VANILLA;
        }

        if (disableSprint) {
            serverData.sprinting = SprintingOption.DISABLED;
        }
        else if (currentServerData.sprinting() != SprintingOption.DISABLED) {
            serverData.sprinting = currentServerData.sprinting();
        }
        else if (disableHunger) {
            serverData.sprinting = SprintingOption.LIMITED_BY_HEALTH;
        }
        else {
            serverData.sprinting = SprintingOption.VANILLA;
        }

        if (oldFoodStacking) {
            serverData.useCustomFoodStackSizes = true;
            var customFoodStackSizesHashMap = new LinkedHashMap<String, Integer>();
            for (var entry : customFoodStacking.entrySet()) {
                customFoodStackSizesHashMap.put(entry.getKey(), entry.getValue());
            }
            serverData.customFoodStackSizes = customFoodStackSizesHashMap;
        }
        YACLConfig.setServerData(serverData.getImmutable());

        var customFoodHealthHashMap = new HashMap<String, Integer>();
        for (var entry : customFoodHealth.entrySet()) {
            customFoodHealthHashMap.put(entry.getKey(), entry.getValue());
        }
        YACLConfig.setCustomFoodHealthMap(customFoodHealthHashMap);

        var customItemStackSizesHashMap = new HashMap<String, Integer>();
        for (var entry : customItemStacking.entrySet()) {
            customItemStackSizesHashMap.put(entry.getKey(), entry.getValue());
        }
        YACLConfig.setCustomItemStackSizesMap(customItemStackSizesHashMap);
    }

    @Unique
    void setConfigFromNT() {
        if (this.loaded instanceof ClientConfig clientConfig) {
            var gameplayConfig = clientConfig.gameplay;
            setGameplayConfigFromNT(
                    gameplayConfig.disableHunger, gameplayConfig.preventHungerEffect, gameplayConfig.disableSprint,
                    gameplayConfig.customFoodHealth, gameplayConfig.oldFoodStacking, gameplayConfig.customFoodStacking,
                    gameplayConfig.customItemStacking, gameplayConfig.instantEat
            );
            var clientData = new ClientConfigData();
            clientData.hideHungerBar = clientConfig.eyeCandy.hideHungerBar;
            clientData.hideExperienceBar = clientConfig.eyeCandy.hideExperienceBar;
            YACLConfig.setClientData(clientData.getImmutable());
        }
        else if (this.loaded instanceof ServerConfig serverConfig) {
            var gameplayConfig = serverConfig.gameplay;
            setGameplayConfigFromNT(
                    gameplayConfig.disableHunger, gameplayConfig.preventHungerEffect, gameplayConfig.disableSprint,
                    gameplayConfig.customFoodHealth, gameplayConfig.oldFoodStacking, gameplayConfig.customFoodStacking,
                    gameplayConfig.customItemStacking, gameplayConfig.instantEat
            );
        }
    }

    @Override
    public void melancholic_hunger$writeConfigToNT(
            ServerConfigData.ImmutableServerConfigData serverData, ClientConfigData.ImmutableClientConfigData clientData
    ) {
        if (loaded == null) {
            this.load();
        }
        boolean disableHunger = serverData.disableHunger();
        boolean instantEat = serverData.instantEating();
        boolean preventHungerEffect = serverData.hungerEffect() == HungerEffectOption.DISABLED;
        boolean disableSprint = serverData.sprinting() == SprintingOption.DISABLED;
        boolean useCustomFoodStackSizes = serverData.useCustomFoodStackSizes();
        var customFoodStacking = new ItemMap<>(1).startWith(serverData.customFoodStackSizes());
        if (this.loaded instanceof ClientConfig clientConfig) {
            var gameplayConfig = clientConfig.gameplay;
            gameplayConfig.disableHunger = disableHunger;
            gameplayConfig.preventHungerEffect = preventHungerEffect;
            gameplayConfig.instantEat = instantEat;
            gameplayConfig.disableSprint = disableSprint;
            if (useCustomFoodStackSizes && gameplayConfig.oldFoodStacking) {
                gameplayConfig.customFoodStacking = customFoodStacking;
            }
            clientConfig.eyeCandy.hideHungerBar = clientData.hideHungerBar();
            clientConfig.eyeCandy.hideExperienceBar = clientData.hideExperienceBar();
        }
        else if (this.loaded instanceof ServerConfig serverConfig) {
            var gameplayConfig = serverConfig.gameplay;
            gameplayConfig.disableHunger = disableHunger;
            gameplayConfig.preventHungerEffect = preventHungerEffect;
            gameplayConfig.instantEat = instantEat;
            gameplayConfig.disableSprint = disableSprint;
            if (useCustomFoodStackSizes && gameplayConfig.oldFoodStacking) {
                gameplayConfig.customFoodStacking = customFoodStacking;
            }
        }
        this.save();
        this.onLoad.run();
    }

    @Inject(
        method="load",
        at=@At("RETURN"),
        remap=false
    )
    void load_nostalgic_tweaks_config(CallbackInfoReturnable<Boolean> cir) {
        setConfigFromNT();
    }

    @Inject(
        method="save",
        at=@At("RETURN"),
        remap=false
    )
    void save_nostalgic_tweaks_config(CallbackInfo ci) {
        setConfigFromNT();
    }
}
