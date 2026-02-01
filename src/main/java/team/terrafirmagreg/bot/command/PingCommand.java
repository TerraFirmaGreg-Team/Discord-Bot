package team.terrafirmagreg.bot.command;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import team.terrafirmagreg.bot.api.ISlashCommand;

public class PingCommand implements ISlashCommand {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        long gatewayPing = event.getJDA().getGatewayPing();
        long restPing = event.getJDA().getRestPing().complete();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏓 Pong!")
                .addField("Gateway delay", gatewayPing + "ms", true)
                .addField("REST delay", restPing + "ms", true)
                .setColor(0x00FF00);

        event.replyEmbeds(embed.build()).queue();
    }

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash(getName(), "Check the delay of the bot");
    }
}
