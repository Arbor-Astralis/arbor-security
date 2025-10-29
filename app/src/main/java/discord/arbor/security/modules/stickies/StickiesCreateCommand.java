package discord.arbor.security.modules.stickies;

import discord.arbor.security.AppCommand;
import discord.arbor.security.Utilities;
import discord.arbor.security.settings.GuildSettings;
import discord.arbor.security.settings.SettingsManager;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.command.ApplicationCommand;
import discord4j.core.object.command.ApplicationCommandInteraction;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ChannelService;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

public final class StickiesCreateCommand implements AppCommand {
    
    private static final String PARAM_MESSAGE_ID = "message_id";
    
    private final StickiesModule module;
    
    StickiesCreateCommand(StickiesModule module) {
        this.module = module;
    }
    
    @Override
    public ApplicationCommandRequest createRequest() {
        return ApplicationCommandRequest.builder()
            .name("stickies-set")
            .description("Sets the sticky message in this channel")
            .type(ApplicationCommand.Type.CHAT_INPUT.getValue())
            .options(
                ApplicationCommandOptionData.builder()
                    .name(PARAM_MESSAGE_ID)
                    .description("The message to make sticky")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(true)
                    .build()
            )
            .build();
    }

    @Override
    public Mono<Void> onExecute(ApplicationCommandInteractionEvent event) {
        Interaction interaction = event.getInteraction();
        ApplicationCommandInteraction ci = interaction.getCommandInteraction().orElse(null);

        Snowflake channelId = interaction.getChannelId();
        Snowflake guildId = interaction.getGuildId().orElse(null);
        Optional<Member> member = interaction.getMember();
        
        if (ci == null) {
            return event.reply(Utilities.createMessageForMaintainer(guildId, "missing command interaction data"));
        }
        
        if (member.isEmpty() || guildId == null) {
            return event.reply("This can only be done in a server.");
        }

        PermissionSet permissions = member.get().getBasePermissions().block();
        if (permissions == null) {
            return event.reply(Utilities.createMessageForMaintainer(guildId, "failed to retrieve permissions for caller"));
        }
        
        if (!permissions.contains(Permission.ADMINISTRATOR)) {
            return event.reply("Sorry pal, only an admin can do this.");
        }
        
        Map<String, Object> data = Utilities.unwrapData(ci.getData());
        Object rawMessageId = data.get(PARAM_MESSAGE_ID);
        if (!(rawMessageId instanceof String messageIdString)) {
            return event.reply(Utilities.createMessageForMaintainer(guildId, "missing value for `messageId`"));
        }
        
        Snowflake messageId = Snowflake.of(messageIdString.trim());
        Message message = interaction.getClient().getMessageById(channelId, messageId).block();
        if (message == null) {
            return event.reply("Hmm, that message does not exist in this channel. Try again.");
        }

        String contentTemplate = message.getContent();

        var entry = new StickiesModule.Settings.Entry();
        entry.content = contentTemplate;

        GuildSettings settings = SettingsManager.getForGuild(guildId);
        settings.stickies.stickiesByChannelId.put(channelId.asLong(), entry);
        SettingsManager.saveForGuild(guildId, settings);

        ChannelService chan = interaction.getClient().getRestClient().getChannelService();
        chan.deleteMessage(channelId.asLong(), messageId.asLong(), null).block();
        
        this.module.refreshPinnedMessage(interaction.getClient(), guildId, channelId);
        
        return event.reply("All done. Sticky message is set.").withEphemeral(true);
    }
    
}
