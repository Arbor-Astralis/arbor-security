package discord.arbor.security.modules.screener;

import com.fasterxml.jackson.annotation.JsonIgnore;
import discord.arbor.security.AppCommand;
import discord.arbor.security.modules.AppModule;
import discord.arbor.security.settings.GuildSettings;
import discord.arbor.security.settings.SettingsManager;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class ScreenerModule implements AppModule {
    
    @Override
    public Collection<AppCommand> createCommands() {
        return List.of(new ScreenerAddExemptCommand());
    }

    @Override
    public void createSubscriptions(GatewayDiscordClient gatewayClient) {
        gatewayClient.on(MemberJoinEvent.class, this::screenAccount).subscribe();
    }

    private Mono<Void> screenAccount(MemberJoinEvent event) {
        Guild guild = event.getGuild().block();
        Member member = event.getMember();
        
        if (guild == null) {
            return Mono.empty();
        }
        
        Snowflake guildId = guild.getId();
        GuildSettings settings = SettingsManager.getForGuild(guildId);
        
        if (!settings.general.isValid() || !settings.screener.isValid()) {
            return Mono.empty();
        }
        
        Snowflake userId = Snowflake.of(member.getUserData().id().asLong());
        
        if (settings.screener.autoModExemptUserIds.contains(userId.asLong())) {
            return Mono.empty();
        }
        
        Instant creationTime = userId.getTimestamp();
        Duration accountAge = Duration.between(creationTime, Instant.now());
        long totalDays = accountAge.toDays();
        long minimumRequiredDays = settings.screener.accountAgeThresholdDays;
        
        if (totalDays < minimumRequiredDays) {
            guild.kick(userId, "Account is created is less than " + minimumRequiredDays + " days ago").subscribe();
            return Mono.empty();
        }
        
        if (settings.screener.kickEmptyPfp && member.getAvatar().block() == null) {
            guild.kick(userId, "Account has no PFP").subscribe();
            return Mono.empty();
        }
        
        welcomeNewMember(event.getClient(), userId, settings);

        return Mono.empty();
    }

    private static final String[] WELCOME_MESSAGES = {
        "%s has joined the space! Excited to have you here.",
        "Welcome, %s! Hope you enjoy your time with us.",
        "%s just arrived—let’s make this a great place to hang out.",
        "Say hello to %s, who’s now part of the community!",
        "%s is here! The crowd just got a little bigger~",
        "Glad you’re here, %s! Take a look around and make yourself at home.",
        "%s joins the community—let’s make this a friendly spot for everyone.",
        "Welcome aboard, %s! Good to have you with us.",
        "%s just stepped in! Here’s to great conversations ahead.",
        "%s has arrived! Make yourself comfortable and enjoy your stay.",
        "Welcome, %s! There’s plenty to explore here, so dive in.",
        "%s joins us today! Happy to have you in the community.",
        "A fresh face! %s is now part of the Arbor—glad you’re here.",
        "%s just joined! Hope you find this a fun and friendly place.",
        "Welcome, %s! Take your time, explore, and enjoy your stay.",
        "%s is now here! Excited to have another voice in this space.",
        "Glad you could join us, %s! Make yourself at home.",
        "%s has joined! Let this be the start of a great experience here."
    };
    
    private void welcomeNewMember(GatewayDiscordClient client, Snowflake memberId, GuildSettings settings) {
        Snowflake welcomeChannelId = Snowflake.of(settings.screener.newMemberLandingChannelId);
        Channel channel = client.getChannelById(welcomeChannelId).block();
        
        if (!(channel instanceof GuildMessageChannel messageChannel)) {
            return;
        }

        String message = WELCOME_MESSAGES[(int) (Math.random() * WELCOME_MESSAGES.length)];
        message = message.replace("%s", "<@" + memberId.asString() + ">");
        messageChannel.createMessage(message).block();
    }

    public static final class Settings {
        // Target user join age must be < this number in days
        public long accountAgeThresholdDays;
        public boolean kickEmptyPfp;
        public Set<Long> autoModExemptUserIds; 
        
        public Long newMemberLandingChannelId;
        
        @JsonIgnore
        public boolean isValid() {
            return newMemberLandingChannelId != null;
        }
    }
}
