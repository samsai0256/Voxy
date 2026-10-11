package me.cortex.voxy.port;

import me.cortex.voxy.client.core.gl.shader.ShaderLoader;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ShaderResourcesTest {
    @Test void allBundledShadersResolveTheirImports() throws Exception {
        Path root = Path.of("src/main/resources/assets/voxy/shaders");
        int checked = 0;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                if (!Files.readString(path).stripLeading().startsWith("#version")) continue;
                String source = ShaderLoader.parse("voxy:" + root.relativize(path).toString().replace('\\', '/'));
                assertTrue(source.stripLeading().startsWith("#version"), path.toString());
                assertFalse(source.lines().anyMatch(line -> line.stripLeading().startsWith("#import")), path.toString());
                checked++;
            }
        }
        assertTrue(checked > 20, "Expected to check the rendering shaders");
    }
    @Test void missingShaderProducesUsefulFailure() {
        var error = assertThrows(IllegalArgumentException.class, () -> ShaderLoader.parse("voxy:missing.glsl"));
        assertTrue(error.getMessage().contains("missing.glsl"));
    }
}
