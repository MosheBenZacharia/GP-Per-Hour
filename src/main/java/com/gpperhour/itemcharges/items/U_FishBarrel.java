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

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gpperhour.GPPerHourConfig;
import com.gpperhour.itemcharges.ChargedItem;
import com.gpperhour.itemcharges.ChargesItem;
import com.gpperhour.itemcharges.triggers.TriggerChatMessage;
import com.gpperhour.itemcharges.triggers.TriggerItem;
import com.gpperhour.itemcharges.triggers.TriggerItemContainer;
import com.gpperhour.itemcharges.triggers.TriggerMenuOption;
import com.google.common.collect.ImmutableMap;
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
public class U_FishBarrel extends ChargedItem
{
	private final int FISH_BARREL_SIZE = 28;
	private static final String catchMessage = "^You catch (an?|some) ([a-zA-Z ]+)[.!]( It hardens as you handle it with your ice gloves\\.)?$";
	private static final Pattern catchPattern = Pattern.compile(catchMessage);
	private static final String checkRegex = "([0-9]+) x ([a-zA-Z ]+),? ?";
	private static final Pattern checkPattern = Pattern.compile(checkRegex);
	private Integer lastFishCaught = null;
	
	// maps the name of the fish as it appears in chat message to corresponding item ID
	private static final Map<String, Integer> FISH_TYPES_BY_NAME = ImmutableMap.<String, Integer>builder()
		// singular 'shrimp' may occur when fishing for Karambwanji
		.put("shrimp", ItemID.RAW_SHRIMP)
		.put("shrimps", ItemID.RAW_SHRIMP)
		.put("sardine", ItemID.RAW_SARDINE)
		.put("herring", ItemID.RAW_HERRING)
		.put("anchovies", ItemID.RAW_ANCHOVIES)
		.put("mackerel", ItemID.RAW_MACKEREL)
		.put("trout", ItemID.RAW_TROUT)
		.put("cod", ItemID.RAW_COD)
		.put("pike", ItemID.RAW_PIKE)
		.put("slimy swamp eel", ItemID.MORT_SLIMEY_EEL)
		.put("salmon", ItemID.RAW_SALMON)
		.put("tuna", ItemID.RAW_TUNA)
		.put("rainbow fish", ItemID.HUNTING_RAW_FISH_SPECIAL)
		.put("cave eel", ItemID.RAW_CAVE_EEL)
		.put("lobster", ItemID.RAW_LOBSTER)
		.put("bass", ItemID.RAW_BASS)
		.put("leaping trout", ItemID.BRUT_SPAWNING_TROUT)
		.put("swordfish", ItemID.RAW_SWORDFISH)
		.put("lava eel", ItemID.RAW_LAVA_EEL)
		.put("leaping salmon", ItemID.BRUT_SPAWNING_SALMON)
		.put("monkfish", ItemID.RAW_MONKFISH)
		.put("karambwan", ItemID.TBWT_RAW_KARAMBWAN)
		.put("leaping sturgeon", ItemID.BRUT_STURGEON)
		.put("shark", ItemID.RAW_SHARK)
		.put("infernal eel", ItemID.INFERNAL_EEL)
		.put("anglerfish", ItemID.RAW_ANGLERFISH)
		.put("dark crab", ItemID.RAW_DARK_CRAB)
		.put("sacred eel", ItemID.SNAKEBOSS_EEL)
		.build();

	public U_FishBarrel(
		final Client client,
		final ClientThread client_thread,
		final ConfigManager configs,
		final ItemManager items,
		final ChatMessageManager chat_messages,
		final Notifier notifier,
		final Gson gson,
		final ScheduledExecutorService executorService
	) {
		super(ChargesItem.FISH_BARREL, ItemID.FISH_BARREL_CLOSED, client, client_thread, configs, items, chat_messages, notifier, gson, executorService);

		this.config_key = GPPerHourConfig.fish_barrel;
		this.zero_charges_is_positive = true;
		this.triggers_items = new TriggerItem[]{
			new TriggerItem(ItemID.FISH_BARREL_CLOSED),
			new TriggerItem(ItemID.FISH_BARREL_OPEN),
			new TriggerItem(ItemID.FISH_SACK_BARREL_CLOSED),
			new TriggerItem(ItemID.FISH_SACK_BARREL_OPEN)
		};
		this.triggers_chat_messages = new TriggerChatMessage[]{
			new TriggerChatMessage("(Your|The) barrel is empty.").onItemClick().extraConsumer((message) ->
			{
				super.emptyOrClear();
			}),
			new TriggerChatMessage("(You catch .*)").extraConsumer(message -> {
				if ((item_id == ItemID.FISH_BARREL_OPEN || item_id == ItemID.FISH_SACK_BARREL_OPEN) && getItemCount() < FISH_BARREL_SIZE && super.hasChargeData()) {
					final Matcher matcher = catchPattern.matcher(message);
					if (matcher.matches())
					{
						final String fishName = matcher.group(2).toLowerCase();
						if (FISH_TYPES_BY_NAME.containsKey(fishName))
						{
							Integer fishId = FISH_TYPES_BY_NAME.get(fishName);
							lastFishCaught = fishId;
							super.addItems(fishId, 1f);
						}
					}
					else
					{
						log.error("no match found");
					}
				}
			}),
			//new TriggerChatMessage("The barrel is full. It may be emptied at a bank.").onItemClick().fixedCharges(FISH_BARREL_SIZE),
			new TriggerChatMessage("(.* enabled you to catch an extra fish.)").extraConsumer(message -> {
				if ((item_id == ItemID.FISH_BARREL_OPEN || item_id == ItemID.FISH_SACK_BARREL_OPEN) && getItemCount() < FISH_BARREL_SIZE && super.hasChargeData()) {
					
					if (lastFishCaught != null)
					{
						super.addItems(lastFishCaught, 1f);
					}
					else
					{
						log.error("last fish caught is null");
					}
				}
			}),
			new TriggerChatMessage("The barrel contains:").extraConsumer(message -> {
				
				super.emptyOrClear();
				final Matcher matcher = checkPattern.matcher(message);
				while (matcher.find())
				{
					try
					{
						int fishAmount = Integer.parseInt(matcher.group(1));
						String fishName = matcher.group(2).toLowerCase().replace("raw ", "");
						if (FISH_TYPES_BY_NAME.containsKey(fishName))
						{
							Integer fishId = FISH_TYPES_BY_NAME.get(fishName);
							super.addItems(fishId, (float) fishAmount);
						}
						else
						{
							log.error("no match found");
						}
					}
					catch (NumberFormatException e)
					{
						log.error("couldn't parse fish barrel check", e);
					}
				}
			}),
		};
		this.triggers_item_containers = new TriggerItemContainer[]{
			new TriggerItemContainer(InventoryID.INV).menuTarget("Open fish barrel").menuOption("Fill").addDifference(),
			new TriggerItemContainer(InventoryID.INV).menuTarget("Fish barrel").menuOption("Fill").addDifference(),
			new TriggerItemContainer(InventoryID.INV).menuTarget("Open fish sack barrel").menuOption("Fill").addDifference(),
			new TriggerItemContainer(InventoryID.INV).menuTarget("Fish sack barrel").menuOption("Fill").addDifference(),
		};
		this.triggers_menu_options = new TriggerMenuOption[]{
			new TriggerMenuOption("Open fish barrel", "Empty to bank").extraConsumer((message) -> { super.emptyOrClear(); }),
			new TriggerMenuOption("Fish barrel", "Empty to bank").extraConsumer((message) -> { super.emptyOrClear(); }),
			new TriggerMenuOption("Open fish sack barrel", "Empty to bank").extraConsumer((message) -> { super.emptyOrClear(); }),
			new TriggerMenuOption("Fish sack barrel", "Empty to bank").extraConsumer((message) -> { super.emptyOrClear(); }),
		};
		this.supportsWidgetOnWidget = true;
	}
}
