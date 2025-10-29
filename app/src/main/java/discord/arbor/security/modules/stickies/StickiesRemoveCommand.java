package discord.arbor.security.modules.stickies;

import discord.arbor.security.AppCommand;
import discord.arbor.security.settings.GuildSettings;
import discord.arbor.security.settings.SettingsManager;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.command.ApplicationCommand;
import discord4j.core.object.command.ApplicationCommandInteraction;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.http.client.ClientException;
import reactor.core.publisher.Mono;

public final class StickiesRemoveCommand implements AppCommand {
    
    @Override
    public ApplicationCommandRequest createRequest() {
        return ApplicationCommandRequest.builder()
            .name("stickies-remove")
            .description("Removes the sticky message in this channel")
            .type(ApplicationCommand.Type.CHAT_INPUT.getValue())
            .build();
    }

    @Override
    public Mono<Void> onExecute(ApplicationCommandInteractionEvent event) {
        Interaction interaction = event.getInteraction();
        ApplicationCommandInteraction ci = interaction.getCommandInteraction().orElse(null);
        Snowflake guildId = interaction.getGuildId().orElse(null);
        
        if (ci == null) {
            return event.reply("Missing command interaction data").withEphemeral(true);
        }
        if (guildId == null) {
            return event.reply("This command can only be run in a server.").withEphemeral(true);
        }
        
        Snowflake channelId = interaction.getChannelId();
        GuildSettings settings = SettingsManager.getForGuild(guildId);

        StickiesModule.Settings.Entry removedEntry = settings.stickies.stickiesByChannelId.remove(channelId.asLong());
        
        if (removedEntry == null) {
            return event.reply("There is no sticky message set in this channel.").withEphemeral(true);
        }
        
        SettingsManager.saveForGuild(guildId, settings);
        
        if (removedEntry.lastStickyMessageId != null) {
            try {
                Message message = event.getClient().getMessageById(channelId, Snowflake.of(removedEntry.lastStickyMessageId)).block();
                if (message != null) {
                    message.delete().subscribe();
                }
            } catch (ClientException e) {
                // Ignore, message is probably gone
            }
        }

        return event.reply("The sticky message has been removed.").withEphemeral(true);
    }
    
}
