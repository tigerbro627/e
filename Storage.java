package dev.serverhub;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** JSON file storage in config/serverhub/. */
public final class Storage {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private Storage() {}

    public static Path dir() {
        Path d = FabricLoader.getInstance().getConfigDir().resolve("serverhub");
        try {
            Files.createDirectories(d);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return d;
    }

    public static <T> T read(String file, Class<T> type) {
        Path p = dir().resolve(file);
        if (!Files.exists(p)) return null;
        try (Reader r = Files.newBufferedReader(p)) {
            return GSON.fromJson(r, type);
        } catch (Exception e) {
            ServerHub.LOGGER.error("Could not read {} - keeping a backup copy and starting fresh.", file, e);
            try {
                Files.move(p, dir().resolve(file + ".broken-" + System.currentTimeMillis()));
            } catch (IOException ignored) {}
            return null;
        }
    }

    public static void write(String file, Object data) {
        Path p = dir().resolve(file);
        Path tmp = dir().resolve(file + ".tmp");
        try {
            Files.writeString(tmp, GSON.toJson(data));
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            ServerHub.LOGGER.error("Could not write {}", file, e);
        }
    }
}
