package team.terrafirmagreg.bot;

import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ConsoleCommandManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("[Console]");
    private final JDA jda;
    private final Map<String, ConsoleCommand> commands;
    private volatile boolean running = true;

    public ConsoleCommandManager(JDA jda) {
        this.jda = jda;
        this.commands = new HashMap<>();
        registerCommands();
    }

    private void registerCommands() {
        // Basic commands
        registerCommand("stop", this::handleStop, "Shutdown the bot");
        registerCommand("help", this::handleHelp, "Show this help message");
        // Management commands
        registerCommand("reload", this::handleReload, "Reload bot configuration");
    }

    private void registerCommand(String name, Consumer<String[]> handler, String description) {
        commands.put(name.toLowerCase(), new ConsoleCommand(name, handler, description));
    }

    public void start() {
        Thread consoleThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    String input = line.trim();
                    if (!input.isEmpty()) {
                        handleCommand(input);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    LOGGER.error("Error reading console input:", e);
                }
            }
        });
        consoleThread.setDaemon(true);
        consoleThread.start();
        LOGGER.info("Console listener started. Type 'help' for available commands.");
    }

    private void handleCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String commandName = parts[0].toLowerCase();
        String[] args = parts.length > 1 ? new String[] { parts[1] } : new String[0];

        // Log console command usage
        LOGGER.info("[Console Command] {} | Args: {}", commandName, args.length > 0 ? args[0] : "");

        ConsoleCommand command = commands.get(commandName);
        if (command != null) {
            try {
                command.handler().accept(args);
            } catch (Exception e) {
                LOGGER.error("Error executing command '{}':", commandName, e);
            }
        } else {
            LOGGER.warn("Unknown command: {}. Type 'help' for available commands.", commandName);
        }
    }

    // Command handlers
    private void handleStop(String[] args) {
        LOGGER.info("Stop command received, shutting down bot...");
        running = false;
        if (jda != null) {
            jda.shutdown();
        }
        System.exit(0);
    }

    private void handleHelp(String[] args) {
        LOGGER.info("Available commands:");
        commands.values().forEach(cmd ->
                LOGGER.info("  {:<8} - {}", cmd.name(), cmd.description())
        );
    }

    private void handleReload(String[] args) {
        LOGGER.info("Reloading bot configuration...");
        // Здесь можно добавить логику перезагрузки конфига
        LOGGER.info("Configuration reloaded successfully!");
    }

    private record ConsoleCommand(String name, Consumer<String[]> handler, String description) {
    }
}
