package newwindutils.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class NewwindUtilsConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("newwindutils-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("newwindutils.json");

    private static NewwindUtilsConfig config;

    public static NewwindUtilsConfig get() {
        if (config == null) {
            load();
        }
        return config;
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    config = GSON.fromJson(reader, NewwindUtilsConfig.class);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[NewWindUtils] Failed to load config, using defaults", e);
        }

        if (config == null) {
            config = new NewwindUtilsConfig();
        }

        // Migrate only the previous built-in placement. Custom positions are retained.
        if (config.selfHudX == 450 && config.selfHudY == 380) {
            config.selfHudX = -1;
            config.selfHudY = 10;
        }

        config.clamp();
        save();
    }

    public static void save() {
        if (config == null) {
            config = new NewwindUtilsConfig();
        }

        config.clamp();

        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (Exception e) {
            LOGGER.error("[NewWindUtils] Failed to save config", e);
        }
    }

    public static void resetToDefaults() {
        java.util.List<CustomItemRule> items = new java.util.ArrayList<>(get().customItems);
        config = new NewwindUtilsConfig();
        config.customItems = items;
        save();
    }
}
