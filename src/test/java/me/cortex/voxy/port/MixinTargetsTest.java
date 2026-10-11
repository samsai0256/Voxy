package me.cortex.voxy.port;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Checks selectors against the actual pinned Minecraft, Sodium and Iris bytecode. */
class MixinTargetsTest {
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/";
    private static final Map<String, ClassNode> CACHE = new HashMap<>();
    private static ClassNode read(String name) throws IOException {
        if (CACHE.containsKey(name)) return CACHE.get(name);
        try (var in = MixinTargetsTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(in, "Missing target class " + name);
            var node = new ClassNode(); new ClassReader(in).accept(node, 0); CACHE.put(name, node); return node;
        }
    }
    private static Object value(AnnotationNode annotation, String key) {
        if (annotation.values != null) for (int i = 0; i < annotation.values.size(); i += 2)
            if (key.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        return null;
    }
    private static List<AnnotationNode> annotations(List<AnnotationNode> a, List<AnnotationNode> b) {
        var out = new ArrayList<AnnotationNode>(); if (a != null) out.addAll(a); if (b != null) out.addAll(b); return out;
    }
    private static List<MethodNode> methods(ClassNode target, String selector) {
        int pos = selector.indexOf('(');
        String name = pos < 0 ? selector : selector.substring(0, pos);
        return target.methods.stream().filter(m -> m.name.equals(name) && (pos < 0 || m.desc.equals(selector.substring(pos)))).toList();
    }
    @Test void allConfiguredSelectorsMatchPinnedDependencies() throws Exception {
        var failures = new ArrayList<String>(); int injections = 0;
        for (String config : List.of("client.voxy.mixins.json", "common.voxy.mixins.json")) {
            var json = JsonParser.parseString(Files.readString(Path.of("src/main/resources", config))).getAsJsonObject();
            String pack = json.get("package").getAsString().replace('.', '/');
            var entries = json.has("client") ? json.getAsJsonArray("client") : json.getAsJsonArray("mixins");
            for (var entry : entries) {
                String name = pack + "/" + entry.getAsString().replace('.', '/'); var mixin = read(name);
                var annotation = annotations(mixin.visibleAnnotations, mixin.invisibleAnnotations).stream().filter(a -> a.desc.equals(MIXIN + "Mixin;")).findFirst().orElseThrow();
                var targets = (List<Type>) value(annotation, "value");
                assertNotNull(targets, name);
                for (var targetType : targets) {
                    var target = read(targetType.getInternalName());
                    for (var field : mixin.fields) {
                        if (annotations(field.visibleAnnotations, field.invisibleAnnotations).stream().anyMatch(a -> a.desc.equals(MIXIN + "Shadow;"))) {
                            if (target.fields.stream().noneMatch(f -> f.name.equals(field.name) && f.desc.equals(field.desc)))
                                failures.add(name + ": missing shadow " + field.name + field.desc);
                        }
                    }
                    for (var method : mixin.methods) for (var a : annotations(method.visibleAnnotations, method.invisibleAnnotations)) {
                        if (a.desc.equals(MIXIN + "gen/Accessor;")) {
                            String field = (String) value(a, "value");
                            if (field == null) field = Character.toLowerCase(method.name.charAt(3)) + method.name.substring(4);
                            String selected = field;
                            if (target.fields.stream().noneMatch(f -> f.name.equals(selected))) failures.add(name + ": missing accessor " + field);
                        }
                        var selectors = value(a, "method");
                        if (!(selectors instanceof List<?> list)) continue;
                        injections++;
                        for (var selector : list) {
                            var candidates = methods(target, selector.toString());
                            if (candidates.isEmpty()) { failures.add(name + ": missing method " + selector); continue; }
                            Object atValue = value(a, "at");
                            var ats = atValue instanceof List<?> l ? l : List.of(atValue);
                            for (var atObject : ats) {
                                if (!(atObject instanceof AnnotationNode at)) continue;
                                String kind = (String) value(at, "value"), member = (String) value(at, "target");
                                if (member == null || !"INVOKE".equals(kind)) continue;
                                boolean found = candidates.stream().anyMatch(m -> {
                                    for (var instruction : m.instructions) if (instruction instanceof MethodInsnNode call &&
                                            member.equals("L" + call.owner + ";" + call.name + call.desc)) return true;
                                    return false;
                                });
                                if (!found) failures.add(name + ": " + selector + " does not invoke " + member);
                            }
                            if (a.desc.endsWith("/Inject;")) {
                                Type[] args = Type.getArgumentTypes(method.desc);
                                int callback = -1;
                                for (int i = 0; i < args.length; i++) if (args[i].getClassName().startsWith("org.spongepowered.asm.mixin.injection.callback.CallbackInfo")) { callback = i; break; }
                                final int count = callback;
                                if (count > 0 && candidates.stream().noneMatch(m -> Arrays.equals(Arrays.copyOf(args, count), Type.getArgumentTypes(m.desc))))
                                    failures.add(name + ": handler arguments differ from " + selector);
                                boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
                                if (candidates.stream().noneMatch(m -> ((m.access & Opcodes.ACC_STATIC) != 0) == isStatic))
                                    failures.add(name + ": handler static modifier differs from " + selector);
                            }
                        }
                    }
                }
            }
        }
        assertTrue(injections >= 20, "Expected all configured injections to be inspected");
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }
}
