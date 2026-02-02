package team.terrafirmagreg.bot.command;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.terrafirmagreg.bot.Locales;
import team.terrafirmagreg.bot.Scraper;
import team.terrafirmagreg.bot.api.ISlashCommand;
import team.terrafirmagreg.bot.util.CommandUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Handles all guide-related Discord slash commands and interactions.
 */
public class GuideCommand implements ISlashCommand {

    private static final Logger logger = LoggerFactory.getLogger("[Guide]");

    // Fragments containing these substrings will be ignored.
    private static final List<String> FRAGMENT_BLACKLIST_SUBSTRINGS = List.of(
            "glb-viewer",
            "nav-primary",
            "navbar-content",
            "lang-dropdown-button",
            "bd-theme",
            "bd-theme-text"
    );

    private static final Map<String, SearchSession> searchSessions = new ConcurrentHashMap<>();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        try {
            String sub = event.getSubcommandName();
            if (sub == null) {
                event.reply("Please specify a subcommand.").setEphemeral(true).queue();
                return;
            }

            switch (sub) {
                case "path" -> handlePath(event);
                case "top" -> handleTop(event);
                case "search" -> handleSearch(event);
                case "scare" -> handleScare(event);
                default -> event.reply("Unknown subcommand.").setEphemeral(true).queue();
            }
        } catch (Exception e) {
            logger.error("Top-level handler error:", e);
            try {
                event.reply("Failed to fetch that page.").setEphemeral(true).queue();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public String getName() {
        return "guide";
    }

    @Override
    public SlashCommandData getCommandData() {
        OptionData languageOption = new OptionData(OptionType.STRING, "language", "Locale (default en_us)")
                .addChoices(Locales.LANGS.stream()
                        .map(l -> new Command.Choice(Locales.LANG_LABELS.getOrDefault(l, l), l))
                        .collect(Collectors.toList()));

        return Commands.slash(getName(), "Unified guide command")
                .addSubcommands(new SubcommandData("top", "Top Field Guide links")
                        .addOptions(languageOption)
                )
                .addSubcommands(new SubcommandData("search", "Search the guide!")
                        .addOptions(new OptionData(OptionType.STRING, "query", "Example: 'climate'", true))
                        .addOptions(languageOption)
                )
                .addSubcommands(new SubcommandData("path", "Fetch a page by URL path.")
                        .addOptions(new OptionData(OptionType.STRING, "path", "Example: 'mechanics/animal_husbandry'", true))
                        .addOptions(languageOption)
                )
                .addSubcommands(new SubcommandData("scare", "New player jump scare"));
    }

    /**
     * Handles guide string select interactions.
     * @param event The string select interaction event.
     */
    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        switch (event.getComponentId()) {
            case "guide:search-select" -> handleSearchSelect(event);
            case "guide:top-select" -> handleTopSelect(event);
            default -> event.reply("Unknown subcommand.").setEphemeral(true).queue();
        }
    }

    /**
     * Handles guide button interactions.
     * @param event The button interaction event.
     */
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        try {
            String componentId = event.getComponentId();

            switch (componentId) {
                case "guide:share" -> handleShareButton(event);
                case "guide:share-link" -> handleShareLinkButton(event);
                default -> {
                    if (componentId.startsWith("guide:search-prev:") || componentId.startsWith("guide:search-next:")) {
                        handleSearchPaging(event);
                    } else {
                        logger.warn("Unknown button interaction: {}", componentId);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("button handler error:", e);
        }
    }

    // `/guide path`: fetch and display a guide page by the url path given.
    private static void handlePath(SlashCommandInteractionEvent event) {
        String path = event.getOption("path").getAsString();
        String langOpt = event.getOption("language") != null ? event.getOption("language").getAsString() : Locales.DEFAULT_LANG;
        String selectedLang = Locales.LANGS.contains(langOpt) ? langOpt : Locales.DEFAULT_LANG;

        event.reply("Working on it...").setEphemeral(true).queue(hook -> {
            try {
                MessageEmbed embed = Scraper.fetchGuideEmbed(path, selectedLang);
                Button shareBtn = Button.primary("guide:share", "Share Messages");
                Button shareLinkBtn = Button.secondary("guide:share-link", "Share link");
                hook.editOriginalEmbeds(embed).setComponents(ActionRow.of(shareBtn, shareLinkBtn)).queue();
            } catch (Exception e) {
                logger.error("guide path error:", e);
                try {
                    hook.editOriginal("Failed to fetch that page.").queue();
                } catch (Exception ignored) {
                }
            }
        });
    }

    // `/guide top`: present a selector for the most important field guide links for quick access.
    private static void handleTop(SlashCommandInteractionEvent event) {
        String langOpt = event.getOption("language") != null ? event.getOption("language").getAsString() : Locales.DEFAULT_LANG;
        String selectedLang = Locales.LANGS.contains(langOpt) ? langOpt : Locales.DEFAULT_LANG;

        event.reply("Choose a link…").setEphemeral(true).queue(hook -> {
            try {
                String langBase = Scraper.BASE + selectedLang + "/";
                List<Scraper.TopTarget> targets = List.of(
                        new Scraper.TopTarget("📙", langBase),
                        new Scraper.TopTarget("🖥️", "https://guide.appliedenergistics.org/1.20.1/"),
                        new Scraper.TopTarget("⛏️", langBase + "tfg_ores.html"),
                        new Scraper.TopTarget("🌎", langBase + "the_world/geology.html"),
                        new Scraper.TopTarget("🐖", langBase + "mechanics/animal_husbandry.html"),
                        new Scraper.TopTarget("🌾", langBase + "mechanics/crops.html"),
                        new Scraper.TopTarget("🍕", langBase + "firmalife.html"),
                        new Scraper.TopTarget("🛣️", langBase + "roadsandroofs.html"),
                        new Scraper.TopTarget("⛵", langBase + "firmaciv.html"),
                        new Scraper.TopTarget("💡", langBase + "tfg_tips.html")
                );

                List<SelectOption> options = new ArrayList<>();
                for (Scraper.TopTarget target : targets) {
                    try {
                        Scraper.SearchResult result = Scraper.fetchPageTitle(target.url(), selectedLang);
                        String labelText = result.title() != null ? target.emoji() + " " + result.title() : target.emoji() + " " + result.url();
                        if (labelText.length() > 100)
                            labelText = labelText.substring(0, 100);
                        options.add(SelectOption.of(labelText, result.url()));
                    } catch (Exception e) {
                        String labelText = target.emoji() + " " + target.url();
                        if (labelText.length() > 100)
                            labelText = labelText.substring(0, 100);
                        options.add(SelectOption.of(labelText, target.url()));
                    }
                }

                StringSelectMenu select = StringSelectMenu.create("guide:top-select")
                        .setPlaceholder("Select a link")
                        .addOptions(options)
                        .build();

                hook.editOriginal("Top links:")
                        .setComponents(ActionRow.of(select))
                        .queue();
            } catch (Exception e) {
                logger.error("guide top error:", e);
                try {
                    hook.editOriginal("Failed to show top links.").queue();
                } catch (Exception ignored) {
                }
            }
        });
    }

    // `/guide search`: search the guide for pages and sections matching query keywords. Like a browser.
    private static void handleSearch(SlashCommandInteractionEvent event) {
        String query = event.getOption("query").getAsString();
        String langOpt = event.getOption("language") != null ? event.getOption("language").getAsString() : Locales.DEFAULT_LANG;
        String selectedLang = Locales.LANGS.contains(langOpt) ? langOpt : Locales.DEFAULT_LANG;

        event.reply("Searching for \"" + query + "\"...").setEphemeral(true).queue(hook -> {
            try {
                // Prefer JSON index search.
                List<Scraper.SearchResult> results = Scraper.searchGuideFast(query, selectedLang, 250);
                logger.info("search (fast) query=\"{}\" results={}", query, results.size());

                if (results.isEmpty()) {
                    hook.editOriginal("No results for \"" + query + "\".").queue();
                    return;
                }

                // If more than 25, enable paging via Prev/Next buttons
                int totalPages = (int) Math.ceil(results.size() / 25.0);
                if (totalPages == 0)
                    totalPages = 1;
                String token = UUID.randomUUID().toString();
                searchSessions.put(token, new SearchSession(results, query, System.currentTimeMillis() + (15 * 60 * 1000)));

                int page = 1;
                List<SelectOption> options = buildSearchOptions(results, page);
                String placeholder = "Select a result (Page " + page + "/" + totalPages + ")";
                List<ActionRow> rows = buildSearchComponents(token, page, totalPages, options, placeholder);
                String note = results.size() > 25 ? "Showing " + Math.min(25, results.size()) + " of " + results.size() : "";

                hook.editOriginal("Results for \"" + query + "\": " + note)
                        .setComponents(rows)
                        .queue();
            } catch (Exception e) {
                logger.error("search error:", e);
                try {
                    hook.editOriginal("Failed to search/fetch.").queue();
                } catch (Exception ignored) {
                }
            }
        });
    }

    // `/guide scare`: sends GIF then posts embed.
    private static void handleScare(SlashCommandInteractionEvent event) {
        String gifUrl = "https://cdn.discordapp.com/attachments/1167131539046400010/1434364792507731988/newplayer.gif?ex=695486cf&is=6953354f&hm=a244ca5b649b934ae29513698012797f070c232bc9a9242aa8c215e13fd16e94&";
        String guideUrl = Scraper.BASE + Locales.DEFAULT_LANG + "/";
        String text = "We have an [online field guide](" + guideUrl + ")! You can use the following commands to find answers to most of your questions:\n\n" +
                "- `/guide search` Browses field guide entries for your keywords.\n" +
                "- `/guide path` Use a specific url path to find entries (eg. \"mechanics/animal_husbandry\").\n" +
                "- `/guide top` Displays a list of the most useful field guide entries.\n" +
                "- `/guide scare` Make others read too.";

        try {
            event.reply(gifUrl).queue(hook -> {
                MessageEmbed embed = new EmbedBuilder().setDescription(text).build();
                event.getHook().sendMessageEmbeds(embed).queue();
            });
        } catch (Exception e) {
            logger.error("scare error:", e);
            try {
                event.reply("Failed to post message.").setEphemeral(true).queue();
            } catch (Exception ignored) {
            }
        }
    }

    private static void handleSearchSelect(StringSelectInteractionEvent event) {
        long rem = CommandUtils.checkAndTouch(event.getUser().getId(), "sel:" + event.getComponentId());
        if (rem > 0) {
            long wait = (rem + 999) / 1000;
            event.reply("Please wait " + wait + "s before selecting again.").setEphemeral(true).queue();
            return;
        }

        String rel = event.getValues().isEmpty() ? null : event.getValues().get(0);
        if (rel == null || rel.isEmpty()) {
            event.editMessage("No selection received.").setComponents().queue();
            return;
        }

        String url = rel.startsWith("http") ? rel : Scraper.BASE + rel;
        String langPattern = String.join("|", Locales.LANGS);
        Pattern pattern = Pattern.compile("Field-Guide(?:-Modern)?/(" + langPattern + ")/");
        Matcher matcher = pattern.matcher(url);
        String selectedLang = matcher.find() ? matcher.group(1) : Locales.DEFAULT_LANG;

        event.deferEdit().queue(hook -> {
            try {
                MessageEmbed embed = Scraper.fetchGuideEmbed(url, selectedLang);
                Button shareBtn = Button.primary("guide:share", "Share Messages");
                Button shareLinkBtn = Button.secondary("guide:share-link", "Share link");
                hook.editOriginal("Result:")
                        .setEmbeds(embed)
                        .setComponents(ActionRow.of(shareBtn, shareLinkBtn))
                        .queue();
            } catch (Exception e) {
                logger.error("search-select fetch error:", e);
                hook.editOriginal("Failed to fetch the selected page.").setComponents().queue();
            }
        }, error -> {
            logger.error("search-select defer error:", error);
        });
    }

    // Share button for `/guide top`.
    private static void handleTopSelect(StringSelectInteractionEvent event) {
        long rem = CommandUtils.checkAndTouch(event.getUser().getId(), "sel:" + event.getComponentId());
        if (rem > 0) {
            long wait = (rem + 999) / 1000;
            event.reply("Please wait " + wait + "s before selecting again.").setEphemeral(true).queue();
            return;
        }

        String sel = event.getValues().isEmpty() ? null : event.getValues().get(0);
        if (sel == null || sel.isEmpty()) {
            event.editMessage("No selection received.").setComponents().queue();
            return;
        }

        try {
            String langPattern = String.join("|", Locales.LANGS);
            Pattern pattern = Pattern.compile("Field-Guide(?:-Modern)?/(" + langPattern + ")/");
            Matcher matcher = pattern.matcher(sel);
            String selectedLang = matcher.find() ? matcher.group(1) : Locales.DEFAULT_LANG;

            event.deferEdit().queue(hook -> {
                try {
                    MessageEmbed embed = Scraper.fetchGuideEmbed(sel, selectedLang);
                    Button shareBtn = Button.primary("guide:share", "Share Messages");
                    Button shareLinkBtn = Button.secondary("guide:share-link", "Share link");
                    hook.editOriginal("Selected:")
                            .setEmbeds(embed)
                            .setComponents(ActionRow.of(shareBtn, shareLinkBtn))
                            .queue();
                } catch (Exception e) {
                    logger.error("top-select fetch error:", e);
                    hook.editOriginal("Failed to fetch the selected page.").setComponents().queue();
                }
            });
        } catch (Exception e) {
            logger.error("top-select handler error:", e);
            event.editMessage("Failed to fetch the selected page.").setComponents().queue();
        }
    }

    private static void handleSearchPaging(ButtonInteractionEvent event) {
        String cid = event.getComponentId();
        long rem = CommandUtils.checkAndTouch(event.getUser().getId(), "btn:" + cid.split(":")[0]);
        if (rem > 0) {
            long wait = (rem + 999) / 1000;
            event.reply("Please wait " + wait + "s before paging again.").setEphemeral(true).queue();
            return;
        }

        String[] parts = cid.split(":");
        String action = parts[0];
        String token = parts[1];
        int pageNum = Integer.parseInt(parts[2]);

        SearchSession session = searchSessions.get(token);
        if (session == null) {
            event.reply("This search session expired.").setEphemeral(true).queue();
            return;
        }
        if (session.expiresAt < System.currentTimeMillis()) {
            searchSessions.remove(token);
            event.reply("This search session expired.").setEphemeral(true).queue();
            return;
        }

        int totalPages = (int) Math.ceil(session.results.size() / 25.0);
        if (totalPages == 0)
            totalPages = 1;
        int nextPage = pageNum;
        if (action.equals("guide:search-prev"))
            nextPage = Math.max(1, pageNum - 1);
        if (action.equals("guide:search-next"))
            nextPage = Math.min(totalPages, pageNum + 1);

        List<SelectOption> options = buildSearchOptions(session.results, nextPage);
        String placeholder = "Select a result (Page " + nextPage + "/" + totalPages + ")";
        List<ActionRow> rows = buildSearchComponents(token, nextPage, totalPages, options, placeholder);

        event.editComponents(rows).queue();
    }

    private static void handleShareButton(ButtonInteractionEvent event) {
        long rem = CommandUtils.checkAndTouch(event.getUser().getId(), "btn:" + event.getComponentId());
        if (rem > 0) {
            long wait = (rem + 999) / 1000;
            event.reply("Please wait " + wait + "s before sharing again.").setEphemeral(true).queue();
            return;
        }

        List<MessageEmbed> embeds = event.getMessage().getEmbeds();
        if (embeds.isEmpty()) {
            event.reply("No embed to share.").setEphemeral(true).queue();
            return;
        }

        MessageEmbed srcEmbed = embeds.get(0);
        MessageChannel channel = event.getChannel();

        // Acknowledge interaction and delete ephemeral message
        event.deferReply().setEphemeral(true).queue(hook -> {
            // Send embed with user mention
            channel.sendMessageEmbeds(srcEmbed)
                    .setContent("Shared by " + event.getUser().getAsMention())
                    .queue(
                            msg -> {
                                // Delete the ephemeral message
                                event.getMessage().delete().queue();
                                hook.deleteOriginal().queue();
                            },
                            error -> hook.editOriginal("Failed to share embed.").queue()
                    );
        });
    }

    private static void handleShareLinkButton(ButtonInteractionEvent event) {
        long rem = CommandUtils.checkAndTouch(event.getUser().getId(), "btn:" + event.getComponentId());
        if (rem > 0) {
            long wait = (rem + 999) / 1000;
            event.reply("Please wait " + wait + "s before sharing again.").setEphemeral(true).queue();
            return;
        }

        List<MessageEmbed> embeds = event.getMessage().getEmbeds();
        if (embeds.isEmpty()) {
            event.reply("No embed to share.").setEphemeral(true).queue();
            return;
        }

        MessageEmbed srcEmbed = embeds.get(0);
        String url = srcEmbed.getUrl();
        if (url == null || url.isEmpty()) {
            event.reply("No URL found in embed.").setEphemeral(true).queue();
            return;
        }

        String title = srcEmbed.getTitle();
        String linkText;
        if (title != null && !title.isEmpty()) {
            linkText = "[" + title + "](" + url + ")";
        } else {
            linkText = url;
        }

        MessageChannel channel = event.getChannel();
        channel.sendMessage(linkText).queue();
        event.reply("Shared link to channel.").setEphemeral(true).queue();
    }

    /**
     * Builds select menu options for a page of results (25 max).
     * @param results All search results.
     * @param page Page index.
     * @return Options for the select menu.
     */
    private static List<SelectOption> buildSearchOptions(List<Scraper.SearchResult> results, int page) {
        int start = (Math.max(1, page) - 1) * 25;
        List<Scraper.SearchResult> slice = results.subList(start, Math.min(start + 25, results.size()));

        return slice.stream()
                .map(searchResult -> {
                    String rel = searchResult.url().startsWith(Scraper.BASE) ? searchResult.url().substring(Scraper.BASE.length()) : searchResult.url();
                    if (rel.contains("#")) {
                        String frag = rel.substring(rel.lastIndexOf('#') + 1);
                        String lc = frag.toLowerCase();
                        if (FRAGMENT_BLACKLIST_SUBSTRINGS.stream().anyMatch(lc::contains))
                            return null;
                    }
                    if (rel.length() > 100)
                        return null;
                    String label = (searchResult.title() != null ? searchResult.title() : "Result");
                    if (label.length() > 100)
                        label = label.substring(0, 100);
                    String desc = rel.length() > 100 ? rel.substring(0, 100) : rel;
                    return SelectOption.of(label, rel).withDescription(desc);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Select menu for current page and Prev/Next buttons.
     * @param token Unique session token.
     * @param page Current page index.
     * @param totalPages Total number of pages.
     * @param options Select options for current page.
     * @param placeholder Placeholder text.
     * @return Array of component rows.
     */
    private static List<ActionRow> buildSearchComponents(String token, int page, int totalPages, List<SelectOption> options, String placeholder) {
        StringSelectMenu select = StringSelectMenu.create("guide:search-select")
                .setPlaceholder(placeholder)
                .addOptions(options)
                .build();

        ActionRow row1 = ActionRow.of(select);
        if (totalPages <= 1)
            return List.of(row1);

        Button prev = Button.secondary("guide:search-prev:" + token + ":" + page, "Prev")
                .withDisabled(page <= 1);
        Button next = Button.secondary("guide:search-next:" + token + ":" + page, "Next")
                .withDisabled(page >= totalPages);

        ActionRow row2 = ActionRow.of(prev, next);
        return List.of(row1, row2);
    }

    // Helper classes
    private static class SearchSession {
        List<Scraper.SearchResult> results;
        String query;
        long expiresAt;

        SearchSession(List<Scraper.SearchResult> results, String query, long expiresAt) {
            this.results = results;
            this.query = query;
            this.expiresAt = expiresAt;
        }
    }

}
