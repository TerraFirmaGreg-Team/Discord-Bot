package team.terrafirmagreg.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.terrafirmagreg.bot.api.ISlashCommand;
import team.terrafirmagreg.bot.config.BotConfig;

public class Main {

    public static final Logger LOGGER = LoggerFactory.getLogger("[Bot]");

    public static void main(String[] args) {
        BotConfig config = new BotConfig();

        if (!config.isValid()) {
            LOGGER.error("Missing DISCORD_CLIENT_ID or DISCORD_TOKEN in .env");
            System.exit(1);
        }
        config.logConfiguration();
        CommandManager commandManager = new CommandManager();

        try {
            JDA jda = JDABuilder.createLight(config.getToken())
                    .enableIntents(GatewayIntent.GUILD_MESSAGES)
                    .addEventListeners(commandManager)
                    .build();

            jda.awaitReady();
            LOGGER.info("Bot connected, registering commands...");

            CommandListUpdateAction commandsAction;
            if (config.isDevMode() && config.getGuildId() != null && !config.getGuildId().isEmpty()) {
                LOGGER.info("[Commands] DEV_MODE=true: Registering GUILD slash commands for guild {}...", config.getGuildId());
                commandsAction = jda.getGuildById(config.getGuildId()).updateCommands();
            } else {
                LOGGER.info("[Commands] Registering GLOBAL slash commands...");
                commandsAction = jda.updateCommands();
            }

            for (ISlashCommand cmd : commandManager.getCommands().values()) {
                commandsAction.addCommands(cmd.getCommandData());
            }

            commandsAction.queue(
                    success -> {
                        if (config.isDevMode() && config.getGuildId() != null && !config.getGuildId().isEmpty()) {
                            LOGGER.info("[Commands] Guild registration complete (instant update).");
                        } else {
                            LOGGER.info("[Commands] Global registration complete (may take a few minutes).");
                        }
                        LOGGER.info("Bot is ready!");
                    },
                    error -> {
                        LOGGER.error("[Commands] Registration failed:", error);
                        jda.shutdown();
                        System.exit(1);
                    }
            );

        } catch (Exception e) {
            LOGGER.error("Failed to start bot:", e);
            System.exit(1);
        }
    }
}

