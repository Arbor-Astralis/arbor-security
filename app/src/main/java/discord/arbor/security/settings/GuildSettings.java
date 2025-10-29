package discord.arbor.security.settings;

import discord.arbor.security.modules.screener.ScreenerModule;
import discord.arbor.security.modules.stickies.StickiesModule;
import discord.arbor.security.modules.vetban.VetBanModule;

public final class GuildSettings {
    
    public GeneralSettings general = new GeneralSettings();
    public ScreenerModule.Settings screener = new ScreenerModule.Settings();
    public StickiesModule.Settings stickies = new StickiesModule.Settings();
    public VetBanModule.Settings vetBan = new VetBanModule.Settings();
    
    public GuildSettings() {
    }
    
}
