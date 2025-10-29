package discord.arbor.security.modules.screener;

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
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;

import java.util.Map;

public class ScreenerAddExemptCommand implements AppCommand {
    
    private static final String PARAM_USER_ID = "user_id";
    
    @Override
    public ApplicationCommandRequest createRequest() {
        return ApplicationCommandRequest.builder()
            .name("security-exempt")
            .description("Exempt a user from the screening process")
            .type(ApplicationCommand.Type.CHAT_INPUT.getValue())
            .options(
                ApplicationCommandOptionData.builder()
                    .name(PARAM_USER_ID)
                    .description("The user to exempt")
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
        
        if (ci == null) {
            return Mono.empty();
        }

        Member member = interaction.getMember().orElse(null);
        Snowflake guildId = interaction.getGuildId().orElse(null);
            
        if (member == null || guildId == null) {
            return Mono.empty();
        }

        PermissionSet permissions = member.getBasePermissions().block();
        
        if (permissions == null) {
            return Mono.empty();
        }
        
        if (!permissions.contains(Permission.ADMINISTRATOR)) {
            return event.reply("Sorry this command is for administrators only.");
        }

        Map<String, Object> cmdData = Utilities.unwrapData(ci.getData());
        Object rawUserId = cmdData.get(PARAM_USER_ID);
        
        if (!(rawUserId instanceof String userIdString)) {
            return event.reply(Utilities.createMessageForMaintainer(guildId, "Missing value for `user_id`"));
        }

        long userId;
        
        try {
            userId = Long.parseUnsignedLong(userIdString);
        } catch (NumberFormatException e) {
            return event.reply(Utilities.createMessageForMaintainer(guildId, "Invalid value for `user_id`"));
        }

        GuildSettings settings = SettingsManager.getForGuild(guildId);
        settings.screener.autoModExemptUserIds.add(userId);
        
        SettingsManager.saveForGuild(guildId, settings);
        
        return event.reply("User <@" + userId + "> is now exempt from the screening process.");
    }
}
