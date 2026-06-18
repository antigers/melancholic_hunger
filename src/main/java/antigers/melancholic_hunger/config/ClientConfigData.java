package antigers.melancholic_hunger.config;

import java.awt.*;

public class ClientConfigData {
    public Boolean hideHungerBar;
    public Boolean highlightRegeneratedHearts;
    public HeartTextureOption regeneratedHeartsTexture;
    public Color regeneratedHeartsOverlayColor;
    public Float regeneratedHeartsOpacityMin;
    public Float regeneratedHeartsOpacityMax;
    public Integer regeneratedHeartsBlinkingPeriod;
    public Boolean highlightRestoredHearts;
    public HeartTextureOption restoredHeartsTexture;
    public Color restoredHeartsOverlayColor;
    public Boolean hideExperienceBar;
    public Boolean showExperienceInInventory;
    public Boolean showExperienceOnScreens;
    public Boolean showExperienceOnGain;
    public Boolean enableExperienceAnimation;
    public Integer experienceAnimationDuration;
    public Boolean renderExperienceOverBackground;

    public record ImmutableClientConfigData(
            Boolean hideHungerBar,
            Boolean highlightRegeneratedHearts,
            HeartTextureOption regeneratedHeartsTexture,
            Color regeneratedHeartsOverlayColor,
            Float regeneratedHeartsOpacityMin,
            Float regeneratedHeartsOpacityMax,
            Integer regeneratedHeartsBlinkingPeriod,
            Boolean highlightRestoredHearts,
            HeartTextureOption restoredHeartsTexture,
            Color restoredHeartsOverlayColor,
            Boolean hideExperienceBar,
            Boolean showExperienceInInventory,
            Boolean showExperienceOnScreens,
            Boolean showExperienceOnGain,
            Boolean enableExperienceAnimation,
            Integer experienceAnimationDuration,
            Boolean renderExperienceOverBackground
    ) {
    }

    public ImmutableClientConfigData getImmutable() {
        return new ImmutableClientConfigData(
                hideHungerBar, highlightRegeneratedHearts, regeneratedHeartsTexture, regeneratedHeartsOverlayColor, regeneratedHeartsOpacityMin, regeneratedHeartsOpacityMax,
                regeneratedHeartsBlinkingPeriod, highlightRestoredHearts, restoredHeartsTexture, restoredHeartsOverlayColor, hideExperienceBar, showExperienceInInventory,
                showExperienceOnScreens, showExperienceOnGain, enableExperienceAnimation, experienceAnimationDuration, renderExperienceOverBackground
        );
    }
}
