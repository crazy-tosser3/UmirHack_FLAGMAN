package com.example.UmirHack_Flagman.common;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.UmirHack_Flagman.inventory.InventoryProperties;

@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(InventoryProperties.class)
public class AppConfig {
}
