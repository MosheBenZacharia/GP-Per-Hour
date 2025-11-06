/*
 * Copyright (c) 2023, Moshe Ben-Zacharia <https://github.com/MosheBenZacharia>, molo-pl <https://github.com/molo-pl>
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

import com.google.gson.Gson;
import com.gpperhour.GPPerHourConfig;
import com.gpperhour.itemcharges.ChargedItem;
import com.gpperhour.itemcharges.ChargesItem;
import com.gpperhour.itemcharges.triggers.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.TileItem;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;

import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class U_CoalBag extends ChargedItem
{
	private final int COAL_BAG_SIZE = 27;
    private static final String checkRegex = "^The coal bag (still |)contains (one|[0-9]+) ([a-zA-Z ]+)";
    private static final Pattern checkPattern = Pattern.compile(checkRegex);

	public U_CoalBag(
		final Client client,
		final ClientThread client_thread,
		final ConfigManager configs,
		final ItemManager items,
		final ChatMessageManager chat_messages,
		final Notifier notifier,
		final Gson gson,
		final ScheduledExecutorService executorService
	) {
		super(ChargesItem.COAL_BAG, ItemID.COAL_BAG, client, client_thread, configs, items, chat_messages, notifier, gson, executorService);

		this.config_key = GPPerHourConfig.coal_bag;
		this.zero_charges_is_positive = true;
		this.triggers_items = new TriggerItem[]{
			new TriggerItem(ItemID.COAL_BAG),
			new TriggerItem(ItemID.COAL_BAG_OPEN,true),
		};
		this.triggers_chat_messages = new TriggerChatMessage[]{
			new TriggerChatMessage("(Your|The) coal bag is (now |)empty.").onItemClick().extraConsumer((message) ->
			{
				super.emptyOrClear();
			}),
			new TriggerChatMessage("You manage to mine some coal.").extraConsumer(message -> {
				if ((item_id == ItemID.COAL_BAG_OPEN) && getItemCount() < COAL_BAG_SIZE && super.hasChargeData()) {
                    super.addItems(ItemID.COAL,1f);
				}
			}),
            new TriggerChatMessage("The coal bag (still |)contains").extraConsumer(message -> {
                super.emptyOrClear();
                final Matcher matcher = checkPattern.matcher(message);
                while (matcher.find())
                {
                    try
                    {
                        int coalAmount = 0;
                        if (matcher.group(2).equals("one")) {
                            coalAmount = 1;
                        } else {
                            coalAmount = Integer.parseInt(matcher.group(2));
                        }
                        super.addItems(ItemID.COAL, (float) coalAmount);
                    }
                    catch (NumberFormatException e)
                    {
                        log.error("couldn't parse coal bag check", e);
                    }
                }
            }),
		};
        this.trigger_item_despawn = new TriggerItemDespawn((TileItem tileItem) ->
        {
            if (tileItem.getId() == ItemID.COAL)
            {
                addDespawnedCoalIfHasCapacity(tileItem);
            }
        });
		this.supportsWidgetOnWidget = true;
	}

    private void addDespawnedCoalIfHasCapacity(TileItem tileItem)
    {
        if (tileItem.getQuantity() == 1
                && (!super.itemQuantities.containsKey(tileItem.getId()) || super.itemQuantities.get(tileItem.getId()) < COAL_BAG_SIZE))
        {
            super.addItems(tileItem.getId(), 1f);
        }
    }
}
