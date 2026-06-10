package team.terrafirmagreg.bot.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CommandUtils {

    public static long checkAndTouch(String userId, String key) {
        return checkAndTouch(userId, key, Constant.RATE_LIMIT_MS);
    }

    public static long checkAndTouch(String userId, String key, long rateLimitMs) {
        if (userId == null || key == null)
            return 0;
        long now = System.currentTimeMillis();
        String k = userId + ":" + key;
        Long last = Constant.RATE_LIMIT_MAP.getOrDefault(k, 0L);
        long diff = now - last;
        if (diff < rateLimitMs)
            return rateLimitMs - diff;
        Constant.RATE_LIMIT_MAP.put(k, now);
        return 0;
    }
}
