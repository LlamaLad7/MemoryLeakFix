package ca.fxco.memoryleakfix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterDefault;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.struct.InjectorGroupInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

import java.lang.reflect.Field;
import java.util.*;

public class MemoryLeakFix {

    public static final String MOD_ID = "memoryleakfix";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static void init() {}

    public static void forceLoadAllMixinsAndClearSpongePoweredCache() {
        LOGGER.info("[MemoryLeakFix] Attempting to ForceLoad All Mixins and clear cache");
        silenceAuditLogger();
        MixinEnvironment.getCurrentEnvironment().audit();
        try { //Why is SpongePowered stealing so much ram for this garbage?
            Field noGroupField = InjectorGroupInfo.Map.class.getDeclaredField("NO_GROUP");
            noGroupField.setAccessible(true);
            Object noGroup = noGroupField.get(null);
            Field membersField = noGroup.getClass().getDeclaredField("members");
            membersField.setAccessible(true);
            ((List<?>) membersField.get(noGroup)).clear(); // Clear spongePoweredCache
            emptyClassInfo();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        LOGGER.info("[MemoryLeakFix] Done ForceLoad and clearing SpongePowered cache");
    }

    private static Class<?> getMixinLoggerClass() throws ClassNotFoundException {
        Class<?> mixinLogger;
        try {
            mixinLogger = Class.forName("net.fabricmc.loader.impl.launch.knot.MixinLogger");
        } catch (ClassNotFoundException err) {
            mixinLogger = Class.forName("org.quiltmc.loader.impl.launch.knot.MixinLogger");
        }
        return mixinLogger;
    }

    private static void silenceAuditLogger() {
        try {
            Field loggerField = getMixinLoggerClass().getDeclaredField("LOGGER_MAP");
            loggerField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ILogger> loggerMap = (Map<String, ILogger>)loggerField.get(null);
            loggerMap.put("mixin.audit", new LoggerAdapterDefault("mixin.audit"));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
    
    private static final String OBJECT = "java/lang/Object";

    private static void emptyClassInfo() throws NoSuchFieldException, IllegalAccessException {
        if (MemoryLeakFixExpectPlatform.isModLoaded("not-that-cc"))
            return; // Crashes crafty crashes if it crashes
        Field cacheField = ClassInfo.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ClassInfo> cache = ((Map<String, ClassInfo>)cacheField.get(null));
        ClassInfo jlo = cache.get(OBJECT);
        cache.clear();
        cache.put(OBJECT, jlo);
    }
}
