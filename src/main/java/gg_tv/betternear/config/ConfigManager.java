package gg_tv.betternear.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gg_tv.betternear.BetterNearClient;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "betternear.json";

    private ConfigManager() {
    }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static void load() {
        Path p = path();
        if (!Files.exists(p)) {
            BetterNearClient.CONFIG = new BetterNearConfig();
            save();
            return;
        }
        try {
            String json = Files.readString(p, StandardCharsets.UTF_8);
            BetterNearConfig cfg = GSON.fromJson(json, BetterNearConfig.class);
            if (cfg == null) {
                cfg = new BetterNearConfig();
            }
            sanitize(cfg);
            BetterNearClient.CONFIG = cfg;
        } catch (Exception e) {
            BetterNearClient.LOGGER.warn("[BetterNear] Не удалось прочитать betternear.json, использую значения по умолчанию", e);
            BetterNearClient.CONFIG = new BetterNearConfig();
        }
    }

    public static void save() {
        sanitize(BetterNearClient.CONFIG);
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(BetterNearClient.CONFIG), StandardCharsets.UTF_8);
        } catch (IOException e) {
            BetterNearClient.LOGGER.warn("[BetterNear] Не удалось сохранить betternear.json", e);
        }
    }

    static void sanitize(BetterNearConfig cfg) {
        if (cfg.radius < 1 || cfg.radius > 5000 || Double.isNaN(cfg.radius)) {
            cfg.radius = 150.0;
        }
        if (cfg.highlightSeconds < 1 || cfg.highlightSeconds > 120 || Double.isNaN(cfg.highlightSeconds)) {
            cfg.highlightSeconds = 5.0;
        }
    }
}