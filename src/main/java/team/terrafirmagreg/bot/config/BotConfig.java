package team.terrafirmagreg.bot.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.terrafirmagreg.bot.util.Constant;

@Getter
public class BotConfig {
    private static final Logger logger = LoggerFactory.getLogger(BotConfig.class);

    private final String token;
    private final String clientId;
    private final String guildId;
    private final boolean devMode;
    private final long rateLimitMs;

    public BotConfig() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        this.token = getConfigValue(dotenv, "DISCORD_TOKEN");
        this.clientId = getConfigValue(dotenv, "DISCORD_CLIENT_ID");
        this.guildId = getConfigValue(dotenv, "DISCORD_GUILD_ID");

        String rateLimitStr = getConfigValue(dotenv, "RATE_LIMIT_MS");
        this.rateLimitMs = rateLimitStr != null ? Long.parseLong(rateLimitStr) : 3000L;

        String rawDev = getConfigValue(dotenv, "DEV_MODE");
        this.devMode = rawDev != null || Constant.DEV_MODE;

    }

    private String getConfigValue(Dotenv dotenv, String key) {
        String value = dotenv.get(key);
        if (value == null) {
            value = System.getenv(key);
        }
        return value;
    }

    public boolean isValid() {
        return token != null && !token.isEmpty() && clientId != null && !clientId.isEmpty();
    }

    public void logConfiguration() {
        logger.info("[Config] DEV_MODE={} guildId={} clientId={}",
                devMode,
                guildId != null ? guildId : "unset",
                clientId != null ? clientId : "unset");
    }
}
