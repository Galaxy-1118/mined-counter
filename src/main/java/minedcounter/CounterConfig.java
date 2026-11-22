package minedcounter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Persistent settings for the counter, stored at {@code config/mined_counter.json}.
 *
 * <p>{@code sidebar} is a permission gate for the sidebar slot: when it is
 * false the mod refuses to attach to {@link DisplaySlot#SIDEBAR}, but
 * {@code list} and {@code below_name} still work. The other fields are
 * persisted so that an admin's in-game changes survive a restart.</p>
 */
public final class CounterConfig {
    public static final String DEFAULT_TITLE = "&e&n挖掘榜";
    public static final String DEFAULT_POSITION = "sidebar";
    public static final int MAX_TITLE_LENGTH = 256;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final Path FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("mined_counter.json");

    public String title = DEFAULT_TITLE;
    public boolean sidebar = true;
    public String position = DEFAULT_POSITION;
    public boolean enabled = true;
    public boolean visible = true;

    public static CounterConfig load() {
        try {
            if (!Files.isRegularFile(FILE)) {
                CounterConfig config = new CounterConfig();
                config.save();
                return config;
            }
            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            CounterConfig config = GSON.fromJson(json, CounterConfig.class);
            if (config == null) {
                config = new CounterConfig();
            }
            config.normalize();
            return config;
        } catch (Exception exception) {
            MinedCounter.LOGGER.warn("[mined_counter] could not read {}, using defaults", FILE, exception);
            CounterConfig config = new CounterConfig();
            config.normalize();
            return config;
        }
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this) + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            MinedCounter.LOGGER.warn("[mined_counter] could not write {}", FILE, exception);
        }
    }

    public Component titleComponent() {
        return TitleParser.parse(title);
    }

    public DisplaySlot displaySlot() {
        return slotOf(position);
    }

    public boolean usesSidebar() {
        return DisplaySlot.SIDEBAR == displaySlot();
    }

    /** Returns a canonical position name, or {@code null} when the input is unknown. */
    public static String normalizePosition(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (value) {
            case "sidebar" -> "sidebar";
            case "list" -> "list";
            case "below_name", "belowname" -> "below_name";
            default -> null;
        };
    }

    public static DisplaySlot slotOf(String position) {
        return switch (position) {
            case "list" -> DisplaySlot.LIST;
            case "below_name" -> DisplaySlot.BELOW_NAME;
            default -> DisplaySlot.SIDEBAR;
        };
    }

    private void normalize() {
        if (title == null || title.isBlank()) {
            title = DEFAULT_TITLE;
        }
        title = title.replace('\n', ' ').replace('\r', ' ');
        if (title.length() > MAX_TITLE_LENGTH) {
            title = title.substring(0, MAX_TITLE_LENGTH);
        }
        String normalizedPosition = normalizePosition(position);
        position = normalizedPosition == null ? DEFAULT_POSITION : normalizedPosition;
    }
}
