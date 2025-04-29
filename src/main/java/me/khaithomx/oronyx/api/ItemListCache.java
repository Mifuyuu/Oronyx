package me.khaithomx.oronyx.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.khaithomx.oronyx.Oronyx; // For Logger
import me.khaithomx.oronyx.util.ChatUtils;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collections;
import java.util.Map; // Import Map
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages caching and updating the Hypixel SkyBlock item list.
 */
public class ItemListCache {

    private String itemListUrl;
    private final Map<String, String> itemNamesCache = new ConcurrentHashMap<>();
    private final AtomicLong lastKnownItemUpdateTimestamp = new AtomicLong(-1L);
    private final AtomicBoolean isItemListCurrentlyFetching = new AtomicBoolean(false);
    private final AtomicBoolean cachePopulated = new AtomicBoolean(false);

    public ItemListCache(String url) {
        this.itemListUrl = url;
        if (this.itemListUrl == null || this.itemListUrl.trim().isEmpty() || !this.itemListUrl.startsWith("http")) {
            Oronyx.LOGGER.error("Initial ItemListCache URL is invalid: '{}'. Using default fallback.", url);
            this.itemListUrl = "https://api.hypixel.net/v2/resources/skyblock/items";
        }
    }

    /**
     * Updates the API URL used by this cache, e.g., when changed in config.
     * Optionally triggers an immediate fetch if the URL changes and cache is empty.
     * @param newUrl The new URL for the item list API.
     */
    public void updateApiUrl(String newUrl) {
        if (newUrl != null && !newUrl.trim().isEmpty() && newUrl.startsWith("http") && !newUrl.equals(this.itemListUrl)) {
            Oronyx.LOGGER.info("Item list API URL changed from '{}' to '{}'", this.itemListUrl, newUrl);
            this.itemListUrl = newUrl;
            if (!cachePopulated.get()) {
                Oronyx.LOGGER.info("Triggering item list fetch due to URL change and empty cache.");
                isItemListCurrentlyFetching.set(false);
                triggerItemListUpdateCheck(true);
            }
        } else if (newUrl == null || newUrl.trim().isEmpty() || !newUrl.startsWith("http")) {
            Oronyx.LOGGER.warn("Attempted to update item list URL with an invalid value: {}", newUrl);
        }
    }

    /**
     * Initiates the first fetch of the item list when the mod starts.
     */
    public void initialItemListFetch() {
        Oronyx.LOGGER.info("Initiating first item list fetch...");
        triggerItemListUpdateCheck(true);
    }

    /**
     * Triggers a check for item list updates. Fetches if forced or timestamp is newer.
     * @param forceUpdate Ignore timestamp check and fetch anyway.
     */
    public void triggerItemListUpdateCheck(boolean forceUpdate) {
        if (!isItemListCurrentlyFetching.compareAndSet(false, true)) {
            Oronyx.LOGGER.debug("Item list fetch already in progress, skipping trigger.");
            return;
        }

        Oronyx.LOGGER.info("Checking for item list updates (Force={})... URL: {}", forceUpdate, itemListUrl);

        CompletableFuture<HypixelApiFetcher.ItemListFetchResult> fetchFuture = HypixelApiFetcher.fetchItemListData(itemListUrl);

        fetchFuture.whenCompleteAsync((result, throwable) -> {
            try {
                if (throwable != null) {
                    Oronyx.LOGGER.error("Error fetching item list update (Future Exception)", throwable);
                    if (!cachePopulated.get()) {
                        ChatUtils.sendModMessage(EnumChatFormatting.RED + "Critical Error: Could not fetch initial item list!");
                    }
                    return;
                }

                if (result.success) {
                    if (forceUpdate || result.lastUpdated > this.lastKnownItemUpdateTimestamp.get()) {
                        Oronyx.LOGGER.info("New item list data found (Timestamp: {}). Updating cache.", result.lastUpdated);
                        updateItemNameCache(result.itemsArray); // <-- Call updated method
                        this.lastKnownItemUpdateTimestamp.set(result.lastUpdated);
                    } else {
                        Oronyx.LOGGER.info("Item list data is up-to-date (Timestamp: {}). No cache update needed.", result.lastUpdated);
                        if (!cachePopulated.get()){
                            Oronyx.LOGGER.info("Populating cache for the first time even though timestamp wasn't newer.");
                            updateItemNameCache(result.itemsArray); // <-- Call updated method
                            this.lastKnownItemUpdateTimestamp.set(result.lastUpdated);
                        }
                    }
                } else {
                    Oronyx.LOGGER.error("Failed to fetch item list update: {}", result.errorMessage);
                    if (!cachePopulated.get()) {
                        ChatUtils.sendModMessage(EnumChatFormatting.RED + "Critical Error: Could not fetch initial item list! " + result.errorMessage);
                    } else {
                        ChatUtils.sendModMessage(EnumChatFormatting.YELLOW + "Warning: Failed to update item list. Using old data. ("+ result.errorMessage + ")");
                    }
                }
            } catch (Exception e) {
                Oronyx.LOGGER.error("Unexpected error processing item list fetch result", e);
                if (!cachePopulated.get()) {
                    ChatUtils.sendModMessage(EnumChatFormatting.RED + "Critical Error: Internal error processing item list!");
                }
            } finally {
                isItemListCurrentlyFetching.set(false);
            }
        });
    }

    /**
     * Parses the JSON array of items and updates the internal cache map.
     * Includes enhanced logging for tracking parsed/skipped/error counts.
     * @param itemsArray The JsonArray containing item objects.
     */
    private void updateItemNameCache(JsonArray itemsArray) { // <-- Method with added logging
        if (itemsArray == null) {
            Oronyx.LOGGER.error("Cannot update item cache: input itemsArray is null.");
            return;
        }
        Oronyx.LOGGER.info("Attempting to update item name cache from {} potential entries.", itemsArray.size());

        Map<String, String> newCache = new ConcurrentHashMap<>();
        int parsedCount = 0;
        int errorCount = 0;
        int skippedCount = 0;

        for (JsonElement itemElement : itemsArray) {
            try {
                if (!itemElement.isJsonObject()) {
                    skippedCount++;
                    continue;
                }
                JsonObject itemObject = itemElement.getAsJsonObject();
                if (itemObject.has("id") && itemObject.get("id").isJsonPrimitive() &&
                        itemObject.has("name") && itemObject.get("name").isJsonPrimitive())
                {
                    String id = itemObject.get("id").getAsString();
                    String name = itemObject.get("name").getAsString();
                    if (id != null && !id.isEmpty() && name != null && !name.isEmpty()) {
                        newCache.put(id, name);
                        parsedCount++;
                    } else {
                        Oronyx.LOGGER.trace("Skipping item with empty ID or Name: ID='{}', Name='{}'", id, name); // Trace level
                        skippedCount++;
                    }
                } else {
                    Oronyx.LOGGER.trace("Skipping item entry missing 'id' or 'name' string field: {}", itemObject.toString().substring(0, Math.min(100, itemObject.toString().length()))); // Trace level
                    skippedCount++;
                }
            } catch (Exception e) {
                Oronyx.LOGGER.error("Error parsing single item entry: {}", itemElement.toString().substring(0, Math.min(100, itemElement.toString().length())), e);
                errorCount++;
            }
        }

        if (parsedCount > 0) {
            int oldSize = this.itemNamesCache.size();
            this.itemNamesCache.clear();
            this.itemNamesCache.putAll(newCache);
            this.cachePopulated.set(true);
            Oronyx.LOGGER.info("Successfully updated item name cache. Parsed: {}, Skipped/Invalid: {}, Errors: {}. Cache size changed from {} to {}.",
                    parsedCount, skippedCount + errorCount, errorCount, oldSize, this.itemNamesCache.size());
        } else {
            Oronyx.LOGGER.error("Parsed item list result was empty or contained only errors/skipped items (Parsed: {}, Skipped/Invalid: {}, Errors: {}). Cache not updated.",
                    parsedCount, skippedCount + errorCount, errorCount);
        }
    }

    /**
     * Gets an immutable view of the current item name cache.
     * @return An immutable Map<String, String> of item IDs to names.
     */
    public Map<String, String> getItemNamesMap() {
        return Collections.unmodifiableMap(this.itemNamesCache);
    }

    /**
     * Checks if the item cache has been successfully populated at least once.
     * @return true if the cache has data, false otherwise.
     */
    public boolean isCacheEmpty() {
        return !this.cachePopulated.get();
    }

    /**
     * Checks if an item list fetch is currently in progress.
     * @return true if fetching, false otherwise.
     */
    public boolean isFetching() {
        return this.isItemListCurrentlyFetching.get();
    }
}