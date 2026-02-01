package team.terrafirmagreg.bot.api;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public interface ISlashCommand {

    String getName();

    SlashCommandData getCommandData();

    void onSlashCommandInteraction(SlashCommandInteractionEvent event);

    default void onStringSelectInteraction(StringSelectInteractionEvent event) {
    }

    default void onButtonInteraction(ButtonInteractionEvent event) {
    }

}
