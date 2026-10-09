package com.example.UmirHack_Flagman.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.inventory")
public record InventoryProperties(
        @DefaultValue("24") int expiryWarningHours,
        @DefaultValue("true") boolean autoWriteOff,
        @DefaultValue("72") int offersHorizonHours,
        @DefaultValue("10") int offersDefaultLimit) {
}
