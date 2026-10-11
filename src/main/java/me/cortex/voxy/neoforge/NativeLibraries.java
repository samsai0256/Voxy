package me.cortex.voxy.neoforge;

import org.lwjgl.system.Configuration;
import org.lwjgl.system.Platform;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Supplies LMDB and Zstd resources across NeoForge's isolated Java modules. */
public final class NativeLibraries {
    private static boolean initialized;

    public static synchronized void initialize() {
        if (initialized) return;
        String os = switch (Platform.get()) {
            case LINUX -> "linux";
            case WINDOWS -> "windows";
            default -> throw new UnsupportedOperationException("Voxy's bundled natives support Windows and Linux x64");
        };
        String architecture = System.getProperty("os.arch");
        if (!architecture.equals("amd64") && !architecture.equals("x86_64"))
            throw new UnsupportedOperationException("Voxy's bundled natives require x64: " + architecture);
        try {
            Path directory = Files.createTempDirectory("voxy-natives-");
            directory.toFile().deleteOnExit();
            for (String module : new String[]{"lmdb", "zstd"}) {
                String filename = os.equals("windows") ? "lwjgl_" + module + ".dll" : "liblwjgl_" + module + ".so";
                String resource = "/" + os + "/x64/org/lwjgl/" + module + "/" + filename;
                try (var stream = NativeLibraries.class.getResourceAsStream(resource)) {
                    if (stream == null) throw new IOException("Missing bundled native " + resource);
                    Path target = directory.resolve(filename);
                    Files.copy(stream, target);
                    target.toFile().deleteOnExit();
                }
            }
            String previous = Configuration.LIBRARY_PATH.get();
            Configuration.LIBRARY_PATH.set(directory + (previous == null || previous.isBlank() ? "" : File.pathSeparator + previous));
            initialized = true;
        } catch (IOException e) {
            throw new IllegalStateException("Could not extract Voxy's native libraries", e);
        }
    }
}
