package me.cortex.voxy.port;

import me.cortex.voxy.common.config.storage.StorageBackend;
import me.cortex.voxy.common.config.storage.rocksdb.RocksDBStorageBackend;
import me.cortex.voxy.common.config.storage.lmdb.LMDBStorageBackend;
import me.cortex.voxy.common.util.MemoryBuffer;
import me.cortex.voxy.common.world.other.Mapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import me.cortex.voxy.neoforge.NativeLibraries;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.zstd.Zstd;
import java.nio.file.Path;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

class StorageCompatibilityTest {
    @TempDir Path directory;
    @BeforeAll static void loadBundledNatives() { NativeLibraries.initialize(); }

    @Test void biomeMappingsRoundTripThrough1211Nbt() {
        var expected = new Mapper.BiomeEntry(19, "minecraft:cherry_grove");
        var actual = Mapper.BiomeEntry.deserialize(19, expected.serialize());
        assertEquals(expected.id, actual.id);
        assertEquals(expected.biome, actual.biome);
        assertThrows(IllegalStateException.class, () -> Mapper.BiomeEntry.deserialize(20, expected.serialize()));
    }

    @Test void rocksDbPersistsSectionsAndMappings() {
        roundTrip(() -> new RocksDBStorageBackend(directory.resolve("rocks").toString()));
    }

    @Test void lmdbPersistsSectionsAndMappings() {
        roundTrip(() -> new LMDBStorageBackend(directory.resolve("world.lmdb").toString()));
    }

    private static void roundTrip(Supplier<StorageBackend> factory) {
        byte[] payload = new byte[8192];
        for (int i = 0; i < payload.length; i++) payload[i] = (byte) (i * 31);
        var write = new MemoryBuffer(payload.length);
        var mapping = MemoryUtil.memAlloc(4).put(new byte[]{10, 20, 30, 40}).flip();
        var database = factory.get();
        try {
            write.asByteBuffer().put(payload);
            database.setSectionData(0x123456789L, write);
            database.putIdMapping(7, mapping);
            database.flush();
        } finally {
            database.close(); write.free(); MemoryUtil.memFree(mapping);
        }
        database = factory.get();
        MemoryBuffer read = new MemoryBuffer(payload.length);
        try {
            var result = database.getSectionData(0x123456789L, read);
            assertNotNull(result);
            read = result;
            byte[] actual = new byte[(int) result.size]; result.asByteBuffer().get(actual);
            assertArrayEquals(payload, actual);
            assertArrayEquals(new byte[]{10, 20, 30, 40}, database.getIdMappingsData().get(7));
            database.deleteSectionData(0x123456789L);
            assertNull(database.getSectionData(0x123456789L, read));
        } finally { database.close(); read.free(); }
    }

    @Test void bundledZstdNativeCompressesAndDecompresses() {
        var input = MemoryUtil.memAlloc(1024);
        var compressed = MemoryUtil.memAlloc((int) Zstd.ZSTD_compressBound(1024));
        var output = MemoryUtil.memAlloc(1024);
        try {
            for (int i = 0; i < 1024; i++) input.put((byte) (i % 11));
            input.flip();
            long count = Zstd.ZSTD_compress(compressed, input, 3);
            assertFalse(Zstd.ZSTD_isError(count)); compressed.limit((int) count);
            long size = Zstd.ZSTD_decompress(output, compressed);
            assertEquals(1024, size);
            for (int i = 0; i < 1024; i++) assertEquals(input.get(i), output.get(i));
        } finally {
            MemoryUtil.memFree(input); MemoryUtil.memFree(compressed); MemoryUtil.memFree(output);
        }
    }
}
