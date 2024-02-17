package com.github.vareys.config;

import com.github.vareys.CobblemonAnalyser;
import com.google.gson.stream.JsonReader;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ConfigManager {

    private PriorityConfig priorityConfig;
    private File priorityConfigPath = null;

    public ConfigManager() {
        Path configDirectoryPath = FabricLoader.getInstance().getConfigDir().resolve(CobblemonAnalyser.MOD_ID);

        File configDirectory = configDirectoryPath.toFile();

        if (!configDirectory.exists() && !configDirectory.mkdirs()) {
            throw new RuntimeException("Config directory could not be created: " + configDirectory);
        }
        priorityConfigPath =  configDirectoryPath.resolve("priorities.json").toFile();

        loadConfig();
    }

    private void loadConfig() {
        if (priorityConfigPath.exists()) {
            try (JsonReader reader = new JsonReader(new FileReader(priorityConfigPath));) {
                priorityConfig = CobblemonAnalyser.GSON.fromJson(reader, PriorityConfig.class);
            } catch (Exception e) {
                CobblemonAnalyser.LOGGER.error("", e);
                priorityConfig = new PriorityConfig();
            }
        } else {
            priorityConfig = new PriorityConfig();
        }
        if (priorityConfig == null) priorityConfig = new PriorityConfig();
        saveConfig();
    }

    public void saveConfig() {
        if (priorityConfigPath == null) return;
        try (Writer writer = new FileWriter(priorityConfigPath)) {
            CobblemonAnalyser.GSON.toJson(priorityConfig, writer);
        } catch (IOException e) {
            CobblemonAnalyser.LOGGER.error("", e);
        }
    }

    public PriorityConfig getPriorityConfig() {
        return priorityConfig;
    }

    public void initPriorities(File[] zips) {
        if (priorityConfig.priorities.isEmpty() && zips.length > 0) {
            priorityConfig.priorities = Arrays.stream(zips).map(File::getName).collect(Collectors.toList());
            saveConfig();
        }
    }
}
