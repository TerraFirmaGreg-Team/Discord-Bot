package team.terrafirmagreg.bot;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import team.terrafirmagreg.bot.api.ISlashCommand;
import team.terrafirmagreg.bot.command.GuideCommand;
import team.terrafirmagreg.bot.command.PingCommand;
import team.terrafirmagreg.bot.util.CommandUtils;
import team.terrafirmagreg.bot.util.Constant;

import java.util.HashMap;
import java.util.Map;

import static team.terrafirmagreg.bot.Main.LOGGER;

@Getter
public class DiscordCommandManager extends ListenerAdapter {

    private final Map<String, ISlashCommand> commands = new HashMap<>();

    public DiscordCommandManager() {

        if (Constant.DEV_MODE) {
            registerCommand(new PingCommand());
        }
        registerCommand(new GuideCommand());
    }

    public void registerCommand(ISlashCommand command) {
        commands.put(command.getName(), command);
        LOGGER.info("The command is registered: {}", command.getName());
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        if (Constant.DEV_MODE) {
            // Log IDs during testing.
            JDA jda = event.getJDA();
            LOGGER.info("Logged in as {}", jda.getSelfUser().getName());
            LOGGER.info("Bot user id: {}", jda.getSelfUser().getId());
            String envClientId = System.getenv("DISCORD_CLIENT_ID");
            if (envClientId != null) {
                LOGGER.info("Env client id: {}", envClientId);
                if (!envClientId.equals(jda.getSelfUser().getId())) {
                    LOGGER.warn("WARNING: DISCORD_CLIENT_ID mismatch.");
                }
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        try {
            // Log command usage
            logCommandUsage(event.getName(), event.getUser(), event.getChannel(), event.getGuild());

            if (Constant.DEV_MODE) {
                // Log all interactions during testing.
                LOGGER.info("Interaction received: command={} isChatInput=true", event.getName());
            }

            long rem = CommandUtils.checkAndTouch(event.getUser().getId(), "cmd:" + event.getName());
            if (rem > 0) {
                long wait = (rem + 999) / 1000;
                event.reply("Please wait " + wait + "s before using /" + event.getName() + " again.")
                        .setEphemeral(true).queue();
                return;
            }

            ISlashCommand command = commands.get(event.getName());
            if (command != null) {
                command.onSlashCommandInteraction(event);
            } else {
                event.reply("The command was not found!").setEphemeral(true).queue();
            }

        } catch (Exception e) {
            if (Constant.DEV_MODE)
                LOGGER.error("Top-level handler error:", e);
            try {
                event.reply("Failed to fetch that page.").setEphemeral(true).queue();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        try {
            String componentId = event.getComponentId();
            String commandName = componentId.split(":")[0];

            // Log component interaction
            logComponentUsage("select", componentId, event.getUser(), event.getChannel(), event.getGuild());

            ISlashCommand command = commands.get(commandName);
            if (command != null) {
                command.onStringSelectInteraction(event);
            } else {
                LOGGER.warn("Unknown select interaction: {}", componentId);
            }

        } catch (Exception e) {
            if (Constant.DEV_MODE)
                LOGGER.error("select handler error:", e);
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        try {
            String componentId = event.getComponentId();
            String commandName = componentId.split(":")[0];

            // Log component interaction
            logComponentUsage("button", componentId, event.getUser(), event.getChannel(), event.getGuild());

            ISlashCommand command = commands.get(commandName);
            if (command != null) {
                command.onButtonInteraction(event);
            } else {
                LOGGER.warn("Unknown button interaction: {}", componentId);
            }
        } catch (Exception e) {
            if (Constant.DEV_MODE)
                LOGGER.error("button handler error:", e);
        }
    }

    /**
     * Logs slash command usage with user, channel, and guild information
     */
    private void logCommandUsage(String commandName, User user, MessageChannel channel, Guild guild) {
        String channelName = channel != null ? channel.getName() : "Unknown";
        String guildName = guild != null ? guild.getName() : "DM";

        LOGGER.info("[Command] /{} | User: {} | Channel: {} | Guild: {}", commandName, user.getAsTag(), channelName, guildName);
    }

    /**
     * Logs component interaction (button/select) with user, channel, and guild information
     */
    private void logComponentUsage(String componentType, String componentId, User user, MessageChannel channel, Guild guild) {
        String channelName = channel != null ? channel.getName() : "Unknown";
        String guildName = guild != null ? guild.getName() : "DM";

        LOGGER.info("[Component] {}:{} | User: {} | Channel: {} | Guild: {}",
                componentType, componentId, user.getAsTag(), channelName, guildName);
    }

}
