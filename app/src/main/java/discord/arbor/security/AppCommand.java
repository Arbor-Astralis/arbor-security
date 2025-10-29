package discord.arbor.security;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public interface AppCommand {
    ApplicationCommandRequest createRequest();
    Mono<Void> onExecute(ApplicationCommandInteractionEvent event);
}
