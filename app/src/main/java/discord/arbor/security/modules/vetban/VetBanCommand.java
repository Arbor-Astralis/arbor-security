package discord.arbor.security.modules.vetban;

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
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.BanQuerySpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class VetBanCommand implements AppCommand {
    
    private static final String PARAM_USER = "user";
    
    VetBanCommand() {
    }
    
    @Override
    public ApplicationCommandRequest createRequest() {
        return ApplicationCommandRequest.builder()
            .name("vet-ban")
            .description("Ban a new member in the server (sentinels-only)")
            .type(ApplicationCommand.Type.CHAT_INPUT.getValue())
            .options(
                ApplicationCommandOptionData.builder()
                    .name(PARAM_USER)
                    .description("The user to ban")
                    .type(ApplicationCommandOption.Type.USER.getValue())
                    .required(true)
                    .build()
            )
            .build();
    }

    @Override
    public Mono<Void> onExecute(ApplicationCommandInteractionEvent event) {
        Interaction interaction = event.getInteraction();
        Snowflake guildId = interaction.getGuildId().orElse(null);
        ApplicationCommandInteraction ci = interaction.getCommandInteraction().orElse(null);
        
        if (ci == null) {
            return event.reply(Utilities.createMessageForMaintainer(guildId, "missing command interaction data"));
        }
        
        Guild guild = interaction.getGuild().block();
        Member member = interaction.getMember().orElse(null);
        
        if (member == null || guildId == null || guild == null) {
            return event.reply(Utilities.createMessageForMaintainer(guildId, "at least one required state is `null`"));
        }
        
        GuildSettings settings = SettingsManager.getForGuild(guildId);
        if (!settings.general.isValid()) {
            return event.reply(Utilities.createMessageForMaintainer(guildId, "general settings are invalid"));
        }

        Long modRoleId = settings.vetBan.modRoleId;
        
        if (modRoleId == null) {
            return event.reply(Utilities.createMessageForMaintainer(guildId, "`sentinel` roleId is not configured"));
        }
        
        Set<Snowflake> roleIds = member.getRoleIds();
        if (!roleIds.contains(Snowflake.of(modRoleId))) {
            return event.reply("Sorry, you do not have permission to use this command.");
        }
        
        Map<String, Object> data = Utilities.unwrapData(ci.getData());
        Object userId = data.get(PARAM_USER);
        if (!(userId instanceof String userIdString)) {
            return event.reply(Utilities.createMessageForMaintainer(guildId, "missing `userId` in command data"));
        }
        
        Snowflake targetUserId = Snowflake.of(userIdString);
        Member targetMember = guild.getMemberById(targetUserId).block();
        
        if (targetMember == null) {
            return event.reply("Hmm, that user is not in the server. Did they just leave?");
        }
        
        if (targetMember.isBot()) {
            return event.reply("Haha, you cannot ban a bot though.");
        }
        
        Set<Snowflake> targetRoleIds = targetMember.getRoleIds();
        Snowflake staffRoleId = Snowflake.of(settings.general.staffRoleId);
        
        if (targetRoleIds.contains(staffRoleId)) {
            return event.reply("Did you just try to ban a staff member?");
        }

        Optional<Instant> joinTime = targetMember.getJoinTime();
        if (joinTime.isEmpty()) {
            return event.reply("Hmm, this member does not have a join time. Ask staff to ban them for you.");
        }
        
        Duration duration = Duration.between(joinTime.get(), Instant.now());
        if (duration.toDays() >= settings.vetBan.joinDayThreshold) {
            return event.reply("Sorry this member has been in the server for longer than " + settings.vetBan.joinDayThreshold + " days. Please contact staff for the ban.");
        }
        
        var query = BanQuerySpec.builder().reason("vet-banned by <@" + member.getId().asString() + ">").build();
        guild.ban(targetUserId, query).block();
        
        return event.reply("<@"+targetUserId+"> has been banned. Thank you for the report.");
    }
}
