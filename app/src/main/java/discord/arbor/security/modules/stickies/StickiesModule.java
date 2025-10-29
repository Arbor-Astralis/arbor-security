package discord.arbor.security.modules.stickies;

import discord.arbor.security.AppCommand;
import discord.arbor.security.modules.AppModule;
import discord.arbor.security.settings.GuildSettings;
import discord.arbor.security.settings.SettingsManager;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import discord4j.discordjson.Id;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.http.client.ClientException;
import reactor.core.publisher.Mono;

import java.util.*;

public final class StickiesModule implements AppModule {
    
    @Override
    public Collection<AppCommand> createCommands() {
        return List.of(new StickiesCreateCommand(this), new StickiesRemoveCommand());
    }

    @Override
    public void createSubscriptions(GatewayDiscordClient gatewayClient) {
        gatewayClient.on(MessageCreateEvent.class, event -> {
            Member author = event.getMessage().getAuthorAsMember().block();
            
            // Ignore message from self
            if (author != null && author.getUserData().id().asLong() == gatewayClient.getSelfId().asLong()) {
                return Mono.empty();
            }
            
            Snowflake guildId = event.getGuildId().orElse(null);
            Snowflake channelId = event.getMessage().getChannelId();
            refreshPinnedMessage(gatewayClient, guildId, channelId);
            
            return Mono.empty();
        }).subscribe();
    }

    public void refreshPinnedMessage(GatewayDiscordClient client, Snowflake guildId, Snowflake channelId) {
        GuildSettings settings = SettingsManager.getForGuild(guildId);
        Settings.Entry entry = settings.stickies.stickiesByChannelId.get(channelId.asLong());
        
        if (entry == null) {
            return;
        }

        Channel channel = client.getChannelById(channelId).block();
        if (channel == null) {
            return;
        }

        Possible<Optional<Id>> lastMessageId = channel.getData().lastMessageId();
        boolean sendIt = entry.lastStickyMessageId == null
            || lastMessageId.isAbsent()
            || lastMessageId.get().isEmpty()
            || (lastMessageId.get().get().asLong() != entry.lastStickyMessageId);

        if (!sendIt) {
            return;
        }

        // Check if need to delete last sticky message
        if (entry.lastStickyMessageId != null) {
            Snowflake messageId = Snowflake.of(entry.lastStickyMessageId);

            try {
                Message oldMessage = client.getMessageById(channelId, messageId).block();
                if (oldMessage != null) {
                    oldMessage.delete().subscribe();
                }
            } catch (ClientException e) {
                // Just ignore, message is probably gone
            }
            
            entry.lastStickyMessageId = null;
        }

        MessageData newMessage = client.getRestClient()
            .getChannelById(channelId) 
            .createMessage(entry.content)
            .block();
        
        if (newMessage != null) {
            entry.lastStickyMessageId = newMessage.id().asLong();
        }

        SettingsManager.saveForGuild(guildId, settings);
    }

    public static final class Settings {
        
        public Map<Long, Entry> stickiesByChannelId = new HashMap<>();
        
        public static final class Entry {
            public Long lastStickyMessageId;
            public String content;
        }
    }
    
}
