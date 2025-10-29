package discord.arbor.security;

import discord.arbor.security.modules.AppModule;
import discord.arbor.security.modules.screener.ScreenerModule;
import discord.arbor.security.modules.stickies.StickiesModule;
import discord.arbor.security.modules.vetban.VetBanModule;
import discord.arbor.security.settings.SettingsManager;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.service.ApplicationService;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class App {
    
    public static void main(String[] args) {
        String token;
        
        try {
             token = SettingsManager.initializeAndGetToken();
        } catch (IOException e) {
            System.err.println("Failed to initialize bot!");
            throw new RuntimeException(e);
        }
        
        Collection<AppModule> modules = initializeModules();
        
        startBot(token, modules).block();
    }

    private static Collection<AppModule> initializeModules() {
        return List.of(new VetBanModule(), new ScreenerModule(), new StickiesModule());
    }

    private static Mono<Void> startBot(String token, Collection<AppModule> modules) {
        DiscordClient client = DiscordClient.create(token);
        
        Map<String, AppCommand> appCommands = initializeApplicationCommands(client, modules);

        return client.gateway()
            .setEnabledIntents(IntentSet.all())
            .withGateway(gatewayClient -> {
                initializeGatewayClient(gatewayClient, appCommands, modules);
                return Mono.never();
            });
    }

    private static void initializeGatewayClient(GatewayDiscordClient gatewayClient, Map<String, AppCommand> appCommands, Collection<AppModule> modules) {
        gatewayClient.on(ApplicationCommandInteractionEvent.class, interaction -> {
            AppCommand cmd = appCommands.get(interaction.getCommandName());
            if (cmd != null) {
                return cmd.onExecute(interaction);
            }
            return Mono.empty();
        }).subscribe();

        modules.forEach(module -> {
            module.createSubscriptions(gatewayClient);
        });
    }

    private static Map<String, AppCommand> initializeApplicationCommands(DiscordClient client, Collection<AppModule> modules) {
        ApplicationService appSvc = client.getApplicationService();
        Long appId = client.getApplicationId().block();
        
        if (appId == null) {
            throw new RuntimeException("Failed to get application ID!");
        }
        
        Map<String, AppCommand> appCommands = new HashMap<>();
        
        for (AppModule module : modules) {
            for (AppCommand appCmd : module.createCommands()) {
                ApplicationCommandRequest request = appCmd.createRequest();
                appSvc.createGlobalApplicationCommand(appId, request).subscribe();
                appCommands.put(request.name(), appCmd);
            }
        }
        
        return appCommands;
    }

}
