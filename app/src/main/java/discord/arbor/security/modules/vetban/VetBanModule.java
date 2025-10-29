package discord.arbor.security.modules.vetban;

import discord.arbor.security.AppCommand;
import discord.arbor.security.modules.AppModule;
import discord4j.core.GatewayDiscordClient;

import java.util.Collection;
import java.util.List;

public final class VetBanModule implements AppModule {
    
    @Override
    public Collection<AppCommand> createCommands() {
        return List.of(new VetBanCommand());
    }

    @Override
    public void createSubscriptions(GatewayDiscordClient gatewayClient) {
        
    }

    public static final class Settings {

        // Mod role required to trigger command
        public Long modRoleId;

        // Target user join age must be < this number in days
        public long joinDayThreshold;
        
    }
}
