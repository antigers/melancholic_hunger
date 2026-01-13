package antigers.melancholic_hunger.components;

public class PlayerComponents {
    public static void register() {
        // registering HealthRegenerationComponent
        HealthRegenerationComponent.register();
        // registering ServerConfigComponent
        ServerConfigComponent.register();
    }
}
