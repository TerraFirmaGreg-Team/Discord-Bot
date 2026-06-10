package team.terrafirmagreg.bot.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import lombok.Getter;

@Getter
public class GuideConfig {

    private final String baseUrl;
    private final String searchIndexUrl;
    private final long indexCacheTtlMs;
    private final int httpTimeoutSec;

    GuideConfig(UnmodifiableConfig config) {
        this.baseUrl = config.get("guide.base_url");
        this.searchIndexUrl = config.get("guide.search_index_url");
        Number ttl = config.get("guide.index_cache_ttl_ms");
        this.indexCacheTtlMs = ttl != null ? ttl.longValue() : 600_000L;
        Number timeout = config.get("guide.http_timeout_sec");
        this.httpTimeoutSec = timeout != null ? timeout.intValue() : 15;
    }

}
