package team.terrafirmagreg.bot.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Constant {
    // set to true to enable terminal logging and guild based command registration.
    public static final boolean DEV_MODE = false;
    // Rate limiting per user per user.
    public static final long RATE_LIMIT_MS;

    public static final Map<String, Long> RATE_LIMIT_MAP = new ConcurrentHashMap<>();

    static {
        String rateLimitEnv = System.getenv("RATE_LIMIT_MS");
        RATE_LIMIT_MS = rateLimitEnv != null ? Long.parseLong(rateLimitEnv) : 3000L;
    }

}
