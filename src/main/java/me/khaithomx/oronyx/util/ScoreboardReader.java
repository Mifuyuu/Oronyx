package me.khaithomx.oronyx.util;

import me.khaithomx.oronyx.Oronyx;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.StringUtils;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to read information from the Minecraft scoreboard, specifically the player's purse.
 * Uses caching for performance.
 */
public class ScoreboardReader {

    private static final Pattern PURSE_PATTERN_TEXT = Pattern.compile("[Pp]urse:\\s*([\\d,.]+)");
    private static long lastReadPurse = -1L;
    private static long lastReadTime = 0L;
    private static final long CACHE_DURATION_MS = 500; // Only re-read scoreboard every 500ms max

    /**
     * Attempts to read the player's current purse value from the scoreboard sidebar.
     * Uses a short cache to avoid excessive scoreboard iteration.
     *
     * @return The player's purse value as a long, or -1 if unable to read/find it.
     */
    public static long getPlayerPurse() {
        long now = System.currentTimeMillis();

        // Return cached value if read recently
        if (now - lastReadTime < CACHE_DURATION_MS && lastReadPurse != -1) {
            return lastReadPurse;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) {
            lastReadPurse = -1;
            lastReadTime = now;
            return -1;
        }

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) {
            lastReadPurse = -1;
            lastReadTime = now;
            return -1;
        }

        ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);
        if (sidebarObjective == null) {
            lastReadPurse = -1;
            lastReadTime = now;
            return -1;
        }

        Collection<Score> scores = scoreboard.getSortedScores(sidebarObjective);
        if (scores == null || scores.isEmpty()) {
            lastReadPurse = -1;
            lastReadTime = now;
            return -1;
        }

        for (Score score : scores) {
            if (score.getScorePoints() <= 0 || score.getPlayerName() == null || score.getPlayerName().isEmpty()) {
                continue;
            }

            String playerName = score.getPlayerName();
            ScorePlayerTeam team = scoreboard.getPlayersTeam(playerName);

            String prefix = team == null ? "" : team.getColorPrefix();
            String suffix = team == null ? "" : team.getColorSuffix();

            String fullLine = prefix + playerName + suffix;
            String cleanLineText = StringUtils.stripControlCodes(fullLine);

            Matcher textMatcher = PURSE_PATTERN_TEXT.matcher(cleanLineText);
            if (textMatcher.find()) {
                Long value = parsePurseValue(textMatcher.group(1), cleanLineText);
                if (value != null) {
                    lastReadPurse = value;
                    lastReadTime = now;
                    Oronyx.LOGGER.trace("Parsed purse via TEXT pattern: {} from line '{}'", value, cleanLineText);
                    return value;
                }
            }
        }

        // If not found
        lastReadPurse = -1;
        lastReadTime = now;
        return -1;
    }

    /**
     * Helper method to parse the extracted string value (containing digits, commas, maybe dots) into a Long.
     *
     * @param valueStr The string potentially containing the numeric value (e.g., "20,000,000" or "1,234.5").
     * @param originalLine The original cleaned line text for error logging context.
     * @return The parsed Long value, or null if parsing fails.
     */
    private static Long parsePurseValue(String valueStr, String originalLine) {
        if (valueStr == null || valueStr.trim().isEmpty()) {
            return null;
        }
        try {
            String stringWithoutCommas = valueStr.replace(",", "");

            int decimalPointIndex = stringWithoutCommas.indexOf('.');
            if (decimalPointIndex != -1) {
                stringWithoutCommas = stringWithoutCommas.substring(0, decimalPointIndex);
            }

            if (stringWithoutCommas.isEmpty()) {
                Oronyx.LOGGER.warn("Purse value became empty after removing commas/decimals from '{}' in line '{}'", valueStr, originalLine);
                return null;
            }

            return Long.parseLong(stringWithoutCommas);

        } catch (NumberFormatException e) {
            Oronyx.LOGGER.error("Could not parse Long from extracted purse value '{}' found in line '{}'", valueStr, originalLine, e);
            return null;
        } catch (Exception e) {
            Oronyx.LOGGER.error("Unexpected error parsing purse value '{}' from line '{}'", valueStr, originalLine, e);
            return null;
        }
    }
}
