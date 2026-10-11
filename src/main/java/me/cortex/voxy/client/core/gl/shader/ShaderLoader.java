package me.cortex.voxy.client.core.gl.shader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/** Loads Voxy's shaders from its own module, including nested Voxy imports. */
public final class ShaderLoader {
    private static final Pattern IMPORT = Pattern.compile("\\s*#import <([a-z0-9_.-]+):([a-zA-Z0-9_./-]+)>\\s*");

    public static String parse(String id) {
        return parse(id, new LinkedHashSet<>());
    }

    private static String parse(String id, Set<String> stack) {
        if (!stack.add(id)) throw new IllegalArgumentException("Cyclic shader import: " + stack + " -> " + id);
        int colon = id.indexOf(':');
        if (colon < 1 || id.contains("..")) throw new IllegalArgumentException("Invalid shader id: " + id);
        String path = "/assets/" + id.substring(0, colon) + "/shaders/" + id.substring(colon + 1);
        try (var stream = ShaderLoader.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalArgumentException("Shader not found: " + path);
            var output = new StringBuilder();
            for (String line : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                var matcher = IMPORT.matcher(line);
                if (matcher.matches()) output.append(parse(matcher.group(1) + ":" + matcher.group(2), stack));
                else output.append(line).append('\n');
            }
            return output.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read shader " + id, e);
        } finally { stack.remove(id); }
    }
}
