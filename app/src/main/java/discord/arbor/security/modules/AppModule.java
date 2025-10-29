package discord.arbor.security.modules;

import discord.arbor.security.AppCommand;
import discord4j.core.GatewayDiscordClient;

import java.util.Collection;

public interface AppModule {
    Collection<AppCommand> createCommands();
    void createSubscriptions(GatewayDiscordClient gatewayClient);
}
