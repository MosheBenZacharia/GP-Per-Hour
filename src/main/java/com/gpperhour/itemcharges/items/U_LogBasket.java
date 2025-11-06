/*
 * Copyright (c) 2023, Moshe Ben-Zacharia <https://github.com/MosheBenZacharia>, TicTac7x <https://github.com/TicTac7x>
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
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;

@Slf4j
public class U_LogBasket extends ChargedItem
{
	private final int CAPACITY = 28;
	private static final String logMessage = "^You get some ([a-zA-Z ]+)[.!]?$";
	private static final Pattern logPattern = Pattern.compile(logMessage);
	private static final String checkRegex = "([0-9]+) x ([a-zA-Z ]+),? ?";
	private static final Pattern checkPattern = Pattern.compile(checkRegex);
	private int lastNatureOfferingTickCount = 0;
	
	public U_LogBasket(
		final Client client,
		final ClientThread client_thread,
		final ConfigManager configs,
		final ItemManager items,
		final ChatMessageManager chat_messages,
		final Notifier notifier,
		final Gson gson,
		final ScheduledExecutorService executorService
	) {
		super(ChargesItem.LOG_BASKET, ItemID.LOG_BASKET_CLOSED, client, client_thread, configs, items, chat_messages, notifier, gson, executorService);

		this.config_key = GPPerHourConfig.log_basket;
		this.zero_charges_is_positive = true;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.LOG_BASKET_CLOSED),
            new TriggerItem(ItemID.LOG_BASKET_OPEN),
        };
		this.triggers_chat_messages = new TriggerChatMessage[]{
            new TriggerChatMessage("(Your|The) basket is empty.").onItemClick().extraConsumer((message) -> { super.emptyOrClear(); }),
            new TriggerChatMessage("You empty your basket into the bank.").onItemClick().extraConsumer((message) -> { super.emptyOrClear(); }),
			new TriggerChatMessage("(You get some.* logs)").extraConsumer(message -> {
				if ((item_id == ItemID.LOG_BASKET_OPEN || item_id == ItemID.FORESTRY_BASKET_OPEN) && getItemCount() < CAPACITY && super.hasChargeData()) {
					final Matcher matcher = logPattern.matcher(message);
					if (matcher.matches())
					{
						final String logName = matcher.group(1);
						Integer itemId = tryFindItemIdFromName(logName);
						if (itemId != null)
						{
							super.addItems(itemId, 1f);
							if (getItemCount() < CAPACITY && lastNatureOfferingTickCount == client.getTickCount())
							{
								super.addItems(itemId, 1f);
							}
						}
					}
					else
					{
						log.error("no log match found for message: " + message);
					}
				}
			}),
			new TriggerChatMessage("(The nature offerings enabled you to chop an extra log.)").extraConsumer(message -> {
				
				lastNatureOfferingTickCount = client.getTickCount();
			}),
			new TriggerChatMessage("The basket contains:").extraConsumer(message -> {
				
				super.emptyOrClear();
				final Matcher matcher = checkPattern.matcher(message);
				while (matcher.find())
				{
					try
					{
						int amount = Integer.parseInt(matcher.group(1));
						String name = matcher.group(2);
						Integer itemId = tryFindItemIdFromName(name);
						if (itemId != null)
							super.addItems(itemId, (float) amount);
					}
					catch (NumberFormatException e)
					{
						log.error("couldn't parse log basket check", e);
					}
				}
			}),
		};
		this.triggers_item_containers = new TriggerItemContainer[]{
			new TriggerItemContainer(InventoryID.INV).menuTarget("Open log basket").menuOption("Fill").addDifference(),
			new TriggerItemContainer(InventoryID.INV).menuTarget("Log basket").menuOption("Fill").addDifference(),
		};
		this.supportsWidgetOnWidget = true;
	}
}
