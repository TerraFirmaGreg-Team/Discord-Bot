package team.terrafirmagreg.bot.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Constant {

    public static final Map<String, Long> RATE_LIMIT_MAP = new ConcurrentHashMap<>();
    public static long RATE_LIMIT_MS = 3000L;

}
