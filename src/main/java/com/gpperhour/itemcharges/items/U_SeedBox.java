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
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;

@Slf4j
public class U_SeedBox extends ChargedItem {
    private static final String checkRegex = "^(\\d+)\\s+x\\s+([A-Za-z\\s]+seed)\\.$";
    private static final Pattern checkPattern = Pattern.compile(checkRegex);
    private static final String pickupRegex = "^You put\\s+(\\d+)\\s+x\\s+([A-Za-z\\s]+seed)\\s+straight into your open seed box\\.$";
    private static final Pattern pickupPattern = Pattern.compile(pickupRegex);
    private static final String storeRegex = "^Stored\\s+(\\d+)\\s+x\\s+([A-Za-z\\s]+seed)\\s+in your seed box\\.$";
    private static final Pattern storePattern = Pattern.compile(storeRegex);
    private static final String removeRegex = "^Emptied\\s+(\\d+)\\s+x\\s+([A-Za-z\\s]+seed)\\s+to your inventory\\.$";
    private static final Pattern removePattern = Pattern.compile(removeRegex);


    public U_SeedBox(
            final Client client,
            final ClientThread client_thread,
            final ConfigManager configs,
            final ItemManager items,
            final ChatMessageManager chat_messages,
            final Notifier notifier,
            final Gson gson,
            final ScheduledExecutorService executorService
    ) {
        super(ChargesItem.SEED_BOX, ItemID.SEED_BOX, client, client_thread, configs, items, chat_messages, notifier, gson, executorService);

        this.config_key = GPPerHourConfig.seed_box;
        this.zero_charges_is_positive = true;
        this.triggers_items = new TriggerItem[]{
                new TriggerItem(ItemID.SEED_BOX),
                new TriggerItem(ItemID.SEED_BOX_OPEN),
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
                //Check
                new TriggerChatMessage("The seed box is empty.").onItemClick().extraConsumer((message) -> super.emptyOrClear()),
                //Empty into bank
                new TriggerChatMessage("Your seed box is now empty.").onItemClick().extraConsumer((message) -> super.emptyOrClear()),
                //check
                new TriggerChatMessage("The seed box contains:").onItemClick().extraConsumer((message) -> super.emptyOrClear()),
                new TriggerChatMessage(checkRegex).onItemClick().extraConsumer(message -> addMatches(checkPattern.matcher(message), false)),
                //Pickup
                new TriggerChatMessage(pickupRegex).extraConsumer(message -> addMatches(pickupPattern.matcher(message), false)),
                //Store
                new TriggerChatMessage(storeRegex).extraConsumer(message -> addMatches(storePattern.matcher(message), false)),
                //Remove
                new TriggerChatMessage(removeRegex).extraConsumer(message -> addMatches(removePattern.matcher(message), true)),
        };
    }

    private void addMatches(Matcher matcher, boolean remove)
    {   
        if (!super.hasChargeData())
            return;
        while (matcher.find()) {
            try {
                int amount = Integer.parseInt(matcher.group(1));
                String name = matcher.group(2);
                Integer itemId = tryFindItemIdFromName(name);
                if (itemId != null)
                    super.addItems(itemId, (float) (remove ? -amount : amount));
            } catch (NumberFormatException e) {
                log.error("couldn't parse seed box match", e);
            }
        }
    }
}