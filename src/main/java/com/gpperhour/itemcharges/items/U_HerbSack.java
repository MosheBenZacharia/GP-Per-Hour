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
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;

@Slf4j
public class U_HerbSack extends ChargedItem {
    private static final String pickupRegex = "^You put the (Grimy\\s+[A-Za-z\\s]+)\\s+herb into your herb sack\\.$";
    private static final Pattern pickupPattern = Pattern.compile(pickupRegex);
    private static final String checkRegex = "(\\d+)\\s*x\\s+(.*)";
    private static final Pattern checkPattern = Pattern.compile(checkRegex);

    public U_HerbSack(
            final Client client,
            final ClientThread client_thread,
            final ConfigManager configs,
            final ItemManager items,
            final ChatMessageManager chat_messages,
            final Notifier notifier,
            final Gson gson,
            final ScheduledExecutorService executorService
    ) {
        super(ChargesItem.HERB_SACK, ItemID.HERB_SACK, client, client_thread, configs, items, chat_messages, notifier, gson, executorService);

        this.config_key = GPPerHourConfig.herb_sack;
        this.zero_charges_is_positive = true;
        this.triggers_items = new TriggerItem[]{
                new TriggerItem(ItemID.HERB_SACK),
                new TriggerItem(ItemID.OPEN_HERB_SACK),
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
                new TriggerChatMessage("The herb sack is empty.").onItemClick().extraConsumer((message) -> super.emptyOrClear()),
                new TriggerChatMessage(pickupRegex).extraConsumer(message -> {
                    if (super.hasChargeData()) {
                        final Matcher matcher = pickupPattern.matcher(message);
                        if (matcher.matches()) {
                            final String itemName = matcher.group(1);
                            Integer itemId = tryFindItemIdFromName(itemName);
                            if (itemId != null) {
                                super.addItems(itemId, 1f);
                            }
                        } else {
                            log.error("no herb match found for message: " + message);
                        }
                    }
                }),
                //check
                new TriggerChatMessage("You look in your herb sack and see:").onItemClick().extraConsumer((message) -> super.emptyOrClear()),
                new TriggerChatMessage("x Grimy").onItemClick().extraConsumer(message -> {

                    final Matcher matcher = checkPattern.matcher(message);
                    while (matcher.find()) {
                        try {
                            int amount = Integer.parseInt(matcher.group(1));
                            String name = matcher.group(2);
                            Integer itemId = tryFindItemIdFromName(name);
                            if (itemId != null)
                                super.addItems(itemId, (float) amount);
                        } catch (NumberFormatException e) {
                            log.error("couldn't parse herb sack check", e);
                        }
                    }
                }),
        };
        this.triggers_item_containers = new TriggerItemContainer[]{
                new TriggerItemContainer(InventoryID.INVENTORY.getId()).menuTarget("Open herb sack").menuOption("Fill").addDifference(),
                new TriggerItemContainer(InventoryID.INVENTORY.getId()).menuTarget("Herb sack").menuOption("Fill").addDifference(),
                new TriggerItemContainer(InventoryID.INVENTORY.getId()).menuTarget("Open herb sack").menuOption("Empty").addDifference(),
                new TriggerItemContainer(InventoryID.INVENTORY.getId()).menuTarget("Herb sack").menuOption("Empty").addDifference(),
                //Empty into bank doesn't make a chat message (unless it's already empty)
                new TriggerItemContainer(InventoryID.BANK.getId()).menuTarget("Open herb sack").menuOption("Empty to bank").extraConsumer(super::emptyOrClear),
                new TriggerItemContainer(InventoryID.BANK.getId()).menuTarget("Herb sack").menuOption("Empty to bank").extraConsumer(super::emptyOrClear),
        };
        //for herb sack this only works for single herbs, if dialog pops up we don't capture it. too complicated...
        this.supportsWidgetOnWidget = true;
    }
}
