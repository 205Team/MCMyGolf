package net.fabricmc.mygolf.tools;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DebugUtil {
    private static final Map<String, Object> LAST_VALUES = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_LOG_TIMES = new ConcurrentHashMap<>();

    /**
     * Logs ONLY when the value changes from the previous call.
     * Works for primitives, Objects, Vec3d, etc.
     *
     * Example: DebugUtil.logOnChange("Ball Speed", ball.getVelocity().length());
     */
    public static void logOnChange(String key, Object newValue) {
        Object lastValue = LAST_VALUES.get(key);
        if (!Objects.equals(lastValue, newValue)) {
            LAST_VALUES.put(key, newValue);
            System.out.println("[DEBUG] " + key + " changed: " + lastValue + " -> " + newValue);
        }
    }

    /**
     * Logs numeric values ONLY when the change exceeds a specific delta threshold.
     * Prevents micro-floating point changes (0.0000001) from spamming logs during physics.
     *
     * Example: DebugUtil.logThreshold("Spin Magnitude", spin.length(), 0.01);
     */
    public static void logThreshold(String key, double newValue, double deltaThreshold) {
        Object lastValue = LAST_VALUES.get(key);
        if (lastValue == null || Math.abs(((Number) lastValue).doubleValue() - newValue) >= deltaThreshold) {
            LAST_VALUES.put(key, newValue);
            System.out.println(String.format("[DEBUG] %s shifted: %.4f", key, newValue));
        }
    }

    /**
     * Logs a value at most once every X milliseconds regardless of change.
     *
     * Example: DebugUtil.logThrottled("Ball Position", ball.getPos(), 1000); // 1 sec interval
     */
    public static void logThrottled(String key, Object value, long intervalMs) {
        long now = System.currentTimeMillis();
        long lastTime = LAST_LOG_TIMES.getOrDefault(key, 0L);

        if (now - lastTime >= intervalMs) {
            LAST_LOG_TIMES.put(key, now);
            System.out.println("[DEBUG] " + key + " = " + value);
        }
    }

    /**
     * Resets stored state for a specific key or all keys.
     */
    public static void clear(String key) { LAST_VALUES.remove(key); }
    public static void clearAll() { LAST_VALUES.clear(); LAST_LOG_TIMES.clear(); }
}