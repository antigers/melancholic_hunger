package antigers.melancholic_hunger.config;

public class ClientConfigData {
    public Boolean hideHungerBar;
    public Boolean highlightRegeneratedHearts;
    public Boolean highlightRestoredHearts;
    public Boolean hideExperienceBar;
    public Boolean showExperienceInInventory;
    public Boolean showExperienceOnScreens;
    public Boolean showExperienceOnGain;
    public Boolean enableExperienceAnimation;
    public Boolean renderExperienceOverBackground;
    public Boolean hideLocatorBar;

    public record ImmutableClientConfigData (
            Boolean hideHungerBar,
            Boolean highlightRegeneratedHearts,
            Boolean highlightRestoredHearts,
            Boolean hideExperienceBar,
            Boolean showExperienceInInventory,
            Boolean showExperienceOnScreens,
            Boolean showExperienceOnGain,
            Boolean enableExperienceAnimation,
            Boolean renderExperienceOverBackground,
            Boolean hideLocatorBar
    ) {}

    public ImmutableClientConfigData getImmutable() {
        return new ImmutableClientConfigData(
                hideHungerBar, highlightRegeneratedHearts, highlightRestoredHearts, hideExperienceBar,
                showExperienceInInventory, showExperienceOnScreens, showExperienceOnGain, enableExperienceAnimation,
                renderExperienceOverBackground, hideLocatorBar
        );
    }
}
