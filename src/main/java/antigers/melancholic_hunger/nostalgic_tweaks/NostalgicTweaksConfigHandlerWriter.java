package antigers.melancholic_hunger.nostalgic_tweaks;

import antigers.melancholic_hunger.config.ClientConfigData;
import antigers.melancholic_hunger.config.ServerConfigData;

public interface NostalgicTweaksConfigHandlerWriter {
    void melancholic_hunger$writeConfigToNT(
            ServerConfigData.ImmutableServerConfigData serverData, ClientConfigData.ImmutableClientConfigData clientData
    );
}
