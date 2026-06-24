/*
 * Copyright (c) 2023, Moshe Ben-Zacharia <https://github.com/MosheBenZacharia>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gpperhour.itemcharges.items;

import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gpperhour.GPPerHourConfig;
import com.gpperhour.itemcharges.ChargedItem;
import com.gpperhour.itemcharges.ChargesItem;
import com.gpperhour.itemcharges.triggers.TriggerChatMessage;
import com.gpperhour.itemcharges.triggers.TriggerItem;
import com.gpperhour.itemcharges.triggers.TriggerItemContainer;
import com.gpperhour.itemcharges.triggers.TriggerItemDespawn;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.TileItem;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;

// Gem pouch: lowest tier of the gem bag expansion. Holds 5 each of the three semi-precious
// gems (opal, jade, red topaz). Semi-precious gems only come from gem rocks (always "mined",
// never "found"), so there is no "You just found ..." variant to handle here.
@Slf4j
public class U_GemPouch extends ChargedItem
{
    private final int CAPACITY = 5;
    private static final String checkRegex = "Opal: (\\d+) / Jade: (\\d+) / Red Topaz: (\\d+)";
    private static final Pattern checkPattern = Pattern.compile(checkRegex);
    // "You just mined an Opal!" / "You just mined a piece of Jade!" / "You just mined a Red Topaz!"
    private static final String acquireRegex = "You just mined (?:a|an) (?:piece of )?(Opal|Jade|Red Topaz)!";
    private static final Pattern acquirePattern = Pattern.compile(acquireRegex);

    public U_GemPouch(
            final Client client,
            final ClientThread client_thread,
            final ConfigManager configs,
            final ItemManager items,
            final ChatMessageManager chat_messages,
            final Notifier notifier,
            final Gson gson,
            final ScheduledExecutorService executorService
    ) {
        super(ChargesItem.GEM_POUCH, ItemID.GEM_POUCH, client, client_thread, configs, items, chat_messages, notifier, gson, executorService);

        this.config_key = GPPerHourConfig.gem_pouch;
        this.zero_charges_is_positive = true;
        this.triggers_items = new TriggerItem[]{
                new TriggerItem(ItemID.GEM_POUCH),
                new TriggerItem(ItemID.GEM_POUCH_OPEN, true),
        };
        this.trigger_item_despawn = new TriggerItemDespawn((TileItem tileItem) ->
        {
            if (tileItem.getId() == ItemID.UNCUT_OPAL ||
                tileItem.getId() == ItemID.UNCUT_JADE ||
                tileItem.getId() == ItemID.UNCUT_RED_TOPAZ)
            {
                addDespawnedGemIfHasCapacity(tileItem);
            }
        });
        this.triggers_chat_messages = new TriggerChatMessage[]{
            // Gems mined at gem rocks go straight into the pouch (only while open) and print a
            // "You just mined ..." message but no per-gem "put into pouch" line that names the gem,
            // so we read the gem name from the mining message itself.
            new TriggerChatMessage(acquireRegex).extraConsumer((message) -> {
                if (!hasChargeData())
                    return;
                if (this.item_id != ItemID.GEM_POUCH_OPEN)
                    return;
                final Matcher matcher = acquirePattern.matcher(message);
                while (matcher.find())
                {
                    try
                    {
                        String gemName = matcher.group(1);
                        int gemID;
                        if (gemName.equals("Opal"))
                            gemID = ItemID.UNCUT_OPAL;
                        else if (gemName.equals("Jade"))
                            gemID = ItemID.UNCUT_JADE;
                        else if (gemName.equals("Red Topaz"))
                            gemID = ItemID.UNCUT_RED_TOPAZ;
                        else
                            throw new Exception("Gem name not matched.");

                        if ((!super.itemQuantities.containsKey(gemID) || super.itemQuantities.get(gemID) < CAPACITY))
                        {
                            super.addItems(gemID, 1f);
                        }
                    }
                    catch (Exception e)
                    {
                        log.error("couldn't find group match in gem pouch acquire: " + message, e);
                    }
                }
            }),
            // Both empties literally say "gem bag" regardless of tier. onItemClick() disambiguates
            // by the clicked menu target so we don't clear the pouch when a different gem container
            // (e.g. the original gem bag) is the one being emptied.
            new TriggerChatMessage("The gem bag is now empty.").onItemClick().extraConsumer((message) -> { super.emptyOrClear(); }),
            new TriggerChatMessage("You empty your gem bag into the bank.").onItemClick().extraConsumer((message) -> { super.emptyOrClear(); }),
            // "Check", and partial empty-to-inventory when the inventory fills up
            // ("Left in bag: Opal: x / Jade: y / Red Topaz: z"), both report the exact contents
            // remaining in the bag. matcher.find() matches the substring in either message, so this
            // single trigger resyncs contents for a check or a partial empty.
            new TriggerChatMessage(checkRegex).extraConsumer(message -> {

                super.emptyOrClear();
                final Matcher matcher = checkPattern.matcher(message);
                while (matcher.find())
                {
                    try
                    {
                        int opals = Integer.parseInt(matcher.group(1));
                        int jades = Integer.parseInt(matcher.group(2));
                        int redTopazes = Integer.parseInt(matcher.group(3));

                        super.addItems(ItemID.UNCUT_OPAL, (float) opals);
                        super.addItems(ItemID.UNCUT_JADE, (float) jades);
                        super.addItems(ItemID.UNCUT_RED_TOPAZ, (float) redTopazes);
                    }
                    catch (NumberFormatException e)
                    {
                        log.error("couldn't parse gem pouch check: " + message, e);
                    }
                }
            }),
        };
        this.triggers_item_containers = new TriggerItemContainer[]{
            new TriggerItemContainer(InventoryID.INV).menuTarget("Open gem pouch").menuOption("Fill").addDifference(),
            new TriggerItemContainer(InventoryID.INV).menuTarget("Gem pouch").menuOption("Fill").addDifference(),
        };
        this.supportsWidgetOnWidget = true;
    }

    private void addDespawnedGemIfHasCapacity(TileItem tileItem)
    {
        if (tileItem.getQuantity() == 1
                && (!super.itemQuantities.containsKey(tileItem.getId()) || super.itemQuantities.get(tileItem.getId()) < CAPACITY))
        {
            super.addItems(tileItem.getId(), 1f);
        }
    }
}
