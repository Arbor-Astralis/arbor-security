package discord.arbor.security.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import discord4j.common.util.Snowflake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public final class SettingsManager {
    
    private static final Path DATA_DIR = Paths.get("botData");
    private static final Path SETTINGS_DIR = DATA_DIR.resolve("settings");
    
    private static final Map<Long, GuildSettings> CACHED_SETTINGS = new HashMap<>(1);
    
    private SettingsManager() {}
    
    public static String initializeAndGetToken() throws IOException {
        @Nullable String token = System.getenv().get("BOT_TOKEN");
        if (token != null) {
            return token;
        }

        if (!Files.exists(DATA_DIR)) {
            Files.createDirectories(DATA_DIR);
        }
        
        Path tokenFile = DATA_DIR.resolve("token.txt");
        if (!Files.exists(tokenFile)) {
            Files.createFile(tokenFile);
        }
        
        try (Scanner scan = new Scanner(tokenFile)) {
            if (scan.hasNextLine()) {
                token = scan.nextLine();
            }
        }
        
        if (token == null) {
            System.err.println("No token found. Please provide a token in 'botData/token.txt'");
            System.exit(1);
        }
        
        if (!Files.exists(SETTINGS_DIR)) {
            Files.createDirectories(SETTINGS_DIR);
        }
        
        return token;
    }
    
    @NotNull
    public static GuildSettings getForGuild(Snowflake guildId) {
        Preconditions.checkNotNull(guildId);

        synchronized (CACHED_SETTINGS) {
            GuildSettings settings = CACHED_SETTINGS.get(guildId.asLong());
            if (settings != null) {
                return settings;
            }

            Path guildSettingsFile = SETTINGS_DIR.resolve(getSettingsFile(guildId));
            if (Files.exists(guildSettingsFile)) {
                settings = readSettingsFile(guildSettingsFile);
            } else {
                settings = new GuildSettings();
                saveForGuild(guildId, settings);
            }

            CACHED_SETTINGS.put(guildId.asLong(), settings);

            return settings;
        }
    }

    private static GuildSettings readSettingsFile(Path guildSettingsFile) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(guildSettingsFile.toFile(), GuildSettings.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getSettingsFile(Snowflake guildId) {
        return guildId.asString() + ".json";
    }

    public static void saveForGuild(Snowflake guildId, GuildSettings settings) {
        Preconditions.checkNotNull(guildId);
        Preconditions.checkNotNull(settings);
        
        Path guildSettingsFile = SETTINGS_DIR.resolve(getSettingsFile(guildId));
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(guildSettingsFile.toFile(), settings);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
