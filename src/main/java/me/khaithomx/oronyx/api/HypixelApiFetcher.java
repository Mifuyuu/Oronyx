package me.khaithomx.oronyx.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.khaithomx.oronyx.Oronyx; // For Logger

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for fetching data from the Hypixel API asynchronously.
 */
public class HypixelApiFetcher {

    private static final String USER_AGENT = Oronyx.NAME + "/" + Oronyx.VERSION + " (Minecraft Mod)";
    private static final int CONNECT_TIMEOUT = 5000; // 5 seconds
    private static final int READ_TIMEOUT = 15000;  // 15 seconds

    /**
     * Fetches the main SkyBlock Bazaar data.
     *
     * @param apiUrl The URL for the Bazaar API endpoint.
     * @return A CompletableFuture containing the parsed JsonObject, or null on failure.
     */
    public static CompletableFuture<JsonObject> fetchBazaarData(String apiUrl) {
        return CompletableFuture.supplyAsync(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(apiUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", USER_AGENT);
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setDoOutput(false); // We are only reading input

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                        JsonObject jsonResponse = new JsonParser().parse(reader).getAsJsonObject();
                        // Basic check for API success flag
                        if (jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                            return jsonResponse;
                        } else {
                            String cause = jsonResponse.has("cause") ? jsonResponse.get("cause").getAsString() : "API success flag was false";
                            Oronyx.LOGGER.error("Bazaar API request was not successful: {}", cause);
                            return null; // Indicate failure
                        }
                    }
                } else {
                    Oronyx.LOGGER.error("Bazaar API HTTP Error: {} for URL {}", responseCode, apiUrl);
                    return null; // Indicate failure
                }
            } catch (Exception e) {
                Oronyx.LOGGER.error("Exception fetching Bazaar data from {}: {}", apiUrl, e.getMessage());
                // Oronyx.LOGGER.catching(e); // Uncomment for full stack trace if needed
                return null; // Indicate failure
            } finally {
                if (connection != null) {
                    connection.disconnect(); // Ensure connection is closed
                }
            }
        });
    }

    /**
     * Result class for fetching the item list, containing status and data.
     */
    public static class ItemListFetchResult {
        public final boolean success;
        public final long lastUpdated;
        public final JsonArray itemsArray;
        public final String errorMessage;

        // Constructor for success
        public ItemListFetchResult(long lastUpdated, JsonArray itemsArray) {
            this.success = true;
            this.lastUpdated = lastUpdated;
            this.itemsArray = itemsArray;
            this.errorMessage = null;
        }

        // Constructor for failure
        public ItemListFetchResult(String errorMessage) {
            this.success = false;
            this.lastUpdated = -1L;
            this.itemsArray = null;
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Fetches the SkyBlock item list data.
     *
     * @param apiUrl The URL for the Item List API endpoint.
     * @return A CompletableFuture containing an ItemListFetchResult.
     */
    public static CompletableFuture<ItemListFetchResult> fetchItemListData(String apiUrl) {
        return CompletableFuture.supplyAsync(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(apiUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", USER_AGENT);
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setDoOutput(false);

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                        JsonObject jsonResponse = new JsonParser().parse(reader).getAsJsonObject();

                        if (jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                            long lastUpdated = -1L;
                            if (jsonResponse.has("lastUpdated")) {
                                lastUpdated = jsonResponse.get("lastUpdated").getAsLong();
                            } else {
                                Oronyx.LOGGER.warn("Item list API response missing 'lastUpdated' field!");
                            }
                            JsonArray itemsArray = null;
                            if (jsonResponse.has("items") && jsonResponse.get("items").isJsonArray()) {
                                itemsArray = jsonResponse.getAsJsonArray("items");
                            } else {
                                Oronyx.LOGGER.error("Item list API response missing 'items' array!");
                                return new ItemListFetchResult("Missing 'items' array in API response");
                            }
                            return new ItemListFetchResult(lastUpdated, itemsArray);
                        } else {
                            String cause = jsonResponse.has("cause") ? jsonResponse.get("cause").getAsString() : "API success flag was false";
                            Oronyx.LOGGER.error("Item list API request was not successful: {}", cause);
                            return new ItemListFetchResult("API Error: " + cause);
                        }
                    }
                } else {
                    Oronyx.LOGGER.error("Item list API HTTP Error: {} for URL {}", responseCode, apiUrl);
                    return new ItemListFetchResult("HTTP Error: " + responseCode);
                }
            } catch (Exception e) {
                Oronyx.LOGGER.error("Exception fetching Item list data from {}: {}", apiUrl, e.getMessage());
                // Oronyx.LOGGER.catching(e); // Uncomment for full stack trace
                return new ItemListFetchResult("Exception: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
}