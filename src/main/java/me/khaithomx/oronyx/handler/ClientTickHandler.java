package me.khaithomx.oronyx.handler;

import com.google.gson.JsonObject;
import me.khaithomx.oronyx.Oronyx;
import me.khaithomx.oronyx.api.HypixelApiFetcher;
import me.khaithomx.oronyx.bazaar.ProfitableItem;
import me.khaithomx.oronyx.util.ChatUtils;
import me.khaithomx.oronyx.util.NumberUtils;
// ลบ import ServerChecker
import me.khaithomx.oronyx.util.ScoreboardReader; // Import ScoreboardReader
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles client-side ticks for periodic checks and updates, activating when purse is detected.
 */
public class ClientTickHandler {

    private long lastBazaarCheckTime = 0L;
    private long lastItemListCheckTime = 0L;
    private static final long ITEM_LIST_CHECK_INTERVAL_MS = 15 * 60 * 1000; // 15 minutes
    private final AtomicBoolean isCheckingBazaar = new AtomicBoolean(false);

    // --- สถานะการทำงานใหม่ ---
    private boolean isActive = false; // สถานะปัจจุบันของ Mod (ทำงานหรือไม่)
    private long lastPurseDetectedTime = 0L; // เวลาล่าสุดที่เจอ Purse
    // หน่วงเวลา (มิลลิวินาที) ก่อนที่จะ Deactivate หลังจากไม่เจอ Purse (เช่น 10 วินาที)
    private static final long PURSE_DETECTION_TIMEOUT_MS = 10000L;
    // --- ---

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.thePlayer == null) {
                // Reset state completely when not in world
                if (isActive) {
                    Oronyx.LOGGER.info("Left world, deactivating Oronyx.");
                    isActive = false;
                    Oronyx.instance.updateLastProfitableItems(Collections.emptyList());
                    isCheckingBazaar.set(false);
                }
                lastPurseDetectedTime = 0L; // Reset last detection time
                return;
            }

            long currentTime = System.currentTimeMillis();

            // --- ตรวจสอบ Purse เพื่อกำหนดสถานะ Active ---
            long currentPurse = ScoreboardReader.getPlayerPurse(); // เรียกใช้ purse reader (ซึ่งมี cache ของตัวเอง)

            if (currentPurse >= 0) {
                // ถ้าเจอ Purse
                lastPurseDetectedTime = currentTime; // อัปเดตเวลาล่าสุดที่เจอ
                if (!isActive) {
                    // ถ้ายังไม่ได้ Active ให้เปลี่ยนเป็น Active
                    isActive = true;
                    Oronyx.LOGGER.info("Purse detected, activating Oronyx.");
                    // อาจจะ Reset lastBazaarCheckTime เพื่อให้เช็คเร็วขึ้นหลัง Activate
                    lastBazaarCheckTime = 0L;
                }
            } else {
                // ถ้าไม่เจอ Purse
                if (isActive && (currentTime - lastPurseDetectedTime > PURSE_DETECTION_TIMEOUT_MS)) {
                    // ถ้าเคย Active อยู่ และไม่เจอ Purse นานเกิน Timeout
                    isActive = false;
                    Oronyx.LOGGER.info("Purse not detected for {} ms, deactivating Oronyx.", PURSE_DETECTION_TIMEOUT_MS);
                    Oronyx.instance.updateLastProfitableItems(Collections.emptyList()); // เคลียร์ผลลัพธ์
                    isCheckingBazaar.set(false); // หยุดการเช็ค Bazaar ที่อาจจะค้างอยู่
                }
            }
            // --- สิ้นสุดการตรวจสอบ Purse ---


            // --- Item List Check (ยังคงทำงานแยกต่างหาก หรือจะรวมใน if (isActive) ก็ได้) ---
            if (currentTime - lastItemListCheckTime > ITEM_LIST_CHECK_INTERVAL_MS || Oronyx.instance.itemListCache.isCacheEmpty()) {
                if (!Oronyx.instance.itemListCache.isFetching()) {
                    Oronyx.instance.itemListCache.triggerItemListUpdateCheck(Oronyx.instance.itemListCache.isCacheEmpty());
                    lastItemListCheckTime = currentTime;
                }
            }

            // --- ส่วนการทำงานหลัก (Bazaar Check) ---
            // ทำงานเฉพาะเมื่อ Mod เปิดใช้งาน และ สถานะเป็น Active (เจอ Purse ล่าสุด)
            if (!Oronyx.modEnabled || !isActive) {
                return; // ออก ถ้า Mod ปิด หรือ ไม่ Active
            }

            // --- Bazaar Check Scheduling ---
            long requiredDelay = NumberUtils.parseDelayToMillis(Oronyx.delay);
            requiredDelay = Math.max(5000L, requiredDelay); // Min delay 5s

            if (currentTime - lastBazaarCheckTime > requiredDelay) {
                if (isCheckingBazaar.compareAndSet(false, true)) {
                    lastBazaarCheckTime = currentTime;
                    Oronyx.LOGGER.debug("Bazaar check triggered (Purse detected).");

                    // --- Pre-check: Item List Cache ---
                    if (Oronyx.instance.itemListCache.isCacheEmpty() || Oronyx.instance.itemListCache.isFetching()) {
                        Oronyx.LOGGER.warn("Item list cache not ready, delaying bazaar check.");
                        if (Oronyx.showEvaluatingMessages) {
                            ChatUtils.sendModMessage(EnumChatFormatting.YELLOW + "Waiting for item list data...");
                        }
                        isCheckingBazaar.set(false);
                        // ลดเวลารอเช็ครอบต่อไปเล็กน้อย
                        lastBazaarCheckTime = currentTime - (requiredDelay / 2);
                        return;
                    }

                    // --- Fetch Bazaar Data Asynchronously ---
                    CompletableFuture<JsonObject> bazaarFuture = HypixelApiFetcher.fetchBazaarData(Oronyx.bazaarUrl);

                    bazaarFuture.whenCompleteAsync((bazaarData, throwable) -> {
                        // --- Post-fetch Checks ---
                        // ตรวจสอบว่า Mod ยังคง Active อยู่หรือไม่ ตอนที่ผลลัพธ์กลับมา
                        if (!isActive || !Oronyx.modEnabled) {
                            Oronyx.LOGGER.info("Bazaar check result received, but Oronyx is no longer active/enabled. Discarding.");
                            isCheckingBazaar.set(false); // Release lock
                            return;
                        }

                        // --- Process Result ---
                        try {
                            if (throwable != null) {
                                Oronyx.LOGGER.error("Error fetching bazaar data (Future Exception)", throwable);
                                ChatUtils.sendModMessage(EnumChatFormatting.RED + "Error fetching Bazaar data. Check logs.");
                                Oronyx.instance.updateLastProfitableItems(Collections.emptyList());
                                return;
                            }
                            if (bazaarData == null || !bazaarData.has("success") || !bazaarData.get("success").getAsBoolean()) {
                                String cause = (bazaarData != null && bazaarData.has("cause")) ? bazaarData.get("cause").getAsString() : "Unknown API error";
                                Oronyx.LOGGER.error("Bazaar API request failed: {}", cause);
                                ChatUtils.sendModMessage(EnumChatFormatting.RED + "Bazaar API Error: " + cause);
                                Oronyx.instance.updateLastProfitableItems(Collections.emptyList());
                                return;
                            }

                            // --- Call Bazaar Processor ---
                            Oronyx.LOGGER.debug("Bazaar data fetched successfully. Processing...");
                            Map<String, String> itemNames = Oronyx.instance.itemListCache.getItemNamesMap();
                            List<ProfitableItem> profitableItems = Oronyx.instance.bazaarProcessor.findProfitableItemsStatic(
                                    bazaarData, itemNames, Oronyx.NAME
                            );

                            Oronyx.instance.updateLastProfitableItems(profitableItems);
                            Oronyx.LOGGER.info("Bazaar check complete. Found {} profitable items meeting criteria.", profitableItems.size());

                        } catch (Exception e) {
                            Oronyx.LOGGER.error("An unexpected error occurred during bazaar processing", e);
                            ChatUtils.sendModMessage(EnumChatFormatting.RED + "Internal error during processing. Check logs.");
                            Oronyx.instance.updateLastProfitableItems(Collections.emptyList());
                        } finally {
                            isCheckingBazaar.set(false); // Ensure lock is always released
                        }
                    }); // End whenCompleteAsync
                } // End if compareAndSet
            } // End if time check
        } // End if phase check
    } // End onClientTick
}