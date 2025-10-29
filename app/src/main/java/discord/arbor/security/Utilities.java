package discord.arbor.security;

import discord.arbor.security.settings.GuildSettings;
import discord.arbor.security.settings.SettingsManager;
import discord4j.common.util.Snowflake;
import discord4j.discordjson.json.ApplicationCommandInteractionData;

import java.util.HashMap;
import java.util.Map;

public class Utilities {
    
    private Utilities() {}
    
    public static String createMessageForMaintainer(Snowflake guildId, String message) {
        GuildSettings settings = SettingsManager.getForGuild(guildId);
        if (settings.general.isValid()) {
            String maintainerPing = "<@" + settings.general.maintainerRoleId + ">";
            return "Sorry, something went wrong: " + message + " (cc " + maintainerPing + ")";
        } else {
            return "Sorry, something went wrong: " + message + " -- please contact the admin.";
        }
    }
    
    public static Map<String, Object> unwrapData(ApplicationCommandInteractionData data) {
        Map<String, Object> flattened = new HashMap<>();
        if (data.options().isPresent()) {
            data.options().get().forEach(option -> {
                flattened.put(option.name(), option.value().toOptional().orElse(null));
            });
        }
        return flattened;
    }
}
