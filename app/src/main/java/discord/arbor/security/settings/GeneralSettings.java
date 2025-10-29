package discord.arbor.security.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;

public final class GeneralSettings {
    
    public Long staffRoleId;
    public Long maintainerRoleId;
    
    @JsonIgnore
    public boolean isValid() {
        return staffRoleId != null && maintainerRoleId != null;
    }
    
}
