package team.terrafirmagreg.bot.config;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.terrafirmagreg.bot.util.Constant;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
public class BotConfig {
    private static final Logger logger = LoggerFactory.getLogger(BotConfig.class);

    private final String token;
    private final String clientId;
    private final String guildId;
    private final boolean devMode;
    private final long rateLimitMs;
    private final GuideConfig guide;

    public BotConfig() {
        Path configPath = Paths.get("config.toml");

        if (Files.notExists(configPath)) {
            createDefaultConfig(configPath);
            logger.info("config.toml has been created. Please edit it with your bot credentials and restart.");
            System.exit(0);
        }

        CommentedFileConfig config = CommentedFileConfig.builder(configPath).sync().autosave().build();
        config.load();

        ConfigSpec spec = buildSpec();
        spec.correct(config);
        config.save();

        this.token = config.get("discord.token");
        this.clientId = config.get("discord.client_id");
        this.guildId = config.get("discord.guild_id");
        this.devMode = Boolean.TRUE.equals(config.get("bot.dev_mode"));
        Number rateLimit = config.get("bot.rate_limit_ms");
        this.rateLimitMs = rateLimit != null ? rateLimit.longValue() : 3000L;
        this.guide = new GuideConfig(config);

        Constant.RATE_LIMIT_MS = this.rateLimitMs;
    }

    private static void createDefaultConfig(Path path) {
        CommentedFileConfig config = CommentedFileConfig.builder(path).sync().autosave().build();

        config.set("discord.token", "");
        config.set("discord.client_id", "");
        config.set("discord.guild_id", "");
        config.set("bot.dev_mode", false);
        config.set("bot.rate_limit_ms", 3000L);
        config.set("guide.base_url", "https://terrafirmagreg-team.github.io/Field-Guide-Modern/");
        config.set("guide.search_index_url", "");
        config.set("guide.index_cache_ttl_ms", 600_000L);
        config.set("guide.http_timeout_sec", 15);

        config.setComment("discord", "Discord bot credentials");
        config.setComment("discord.token", "Your bot token from Discord Developer Portal");
        config.setComment("discord.client_id", "Your bot's client/application ID");
        config.setComment("discord.guild_id", "Guild ID for guild-specific slash commands (used in dev mode)");

        config.setComment("bot", "Bot behavior settings");
        config.setComment("bot.dev_mode", "Enable dev mode: guild commands for instant updates, verbose logging");
        config.setComment("bot.rate_limit_ms", "Minimum milliseconds between commands per user");

        config.setComment("guide", "TerraFirmaGreg Field Guide settings");
        config.setComment("guide.base_url", "Base URL of the Field Guide website");
        config.setComment("guide.search_index_url", "Override the search index URL (leave empty to derive from base_url/lang)");
        config.setComment("guide.index_cache_ttl_ms", "Search index cache TTL in milliseconds");
        config.setComment("guide.http_timeout_sec", "HTTP request timeout in seconds");

        config.save();
    }

    private static ConfigSpec buildSpec() {
        ConfigSpec spec = new ConfigSpec();

        spec.define("discord.token", "");
        spec.define("discord.client_id", "");
        spec.define("discord.guild_id", "");

        spec.define("bot.dev_mode", false);
        spec.define("bot.rate_limit_ms", 3000L);

        spec.define("guide.base_url", "https://terrafirmagreg-team.github.io/Field-Guide-Modern/");
        spec.define("guide.search_index_url", "");
        spec.define("guide.index_cache_ttl_ms", 600_000L);
        spec.define("guide.http_timeout_sec", 15);

        return spec;
    }

    public boolean isValid() {
        return token != null && !token.isEmpty() && clientId != null && !clientId.isEmpty();
    }

    public void logConfiguration() {
        logger.info("[Config] DEV_MODE={} guildId={} clientId={}",
                devMode,
                guildId != null && !guildId.isEmpty() ? guildId : "unset",
                clientId != null && !clientId.isEmpty() ? clientId : "unset");
    }
}
