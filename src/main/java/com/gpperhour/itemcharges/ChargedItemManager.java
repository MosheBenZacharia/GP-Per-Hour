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
package com.gpperhour.itemcharges;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;

import com.gpperhour.GPPerHourConfig;
import com.gpperhour.itemcharges.items.S_KharedstMemoirs;
import com.gpperhour.itemcharges.items.U_AshSanctifier;
import com.gpperhour.itemcharges.items.U_BloodEssence;
import com.gpperhour.itemcharges.items.U_BottomlessCompostBucket;
import com.gpperhour.itemcharges.items.U_FishBarrel;
import com.gpperhour.itemcharges.items.U_CoalBag;
import com.gpperhour.itemcharges.items.U_GemBag;
import com.gpperhour.itemcharges.items.U_HerbSack;
import com.gpperhour.itemcharges.items.U_LogBasket;
import com.gpperhour.itemcharges.items.U_SeedBox;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;



@Slf4j
public class ChargedItemManager {


	public static final int CHARGES_UNKNOWN = -1;
	public static final int CHARGES_UNLIMITED = -2;

	@Inject
	private Client client;

	@Inject
	private ClientThread client_thread;

	@Inject
	private ItemManager items;

	@Inject
	private ConfigManager configs;

	@Inject
	private GPPerHourConfig config;

	@Inject
	private ChatMessageManager chat_messages;

	@Inject
	private ScheduledExecutorService executorService;

	@Inject
	private Notifier notifier;

	@Inject
	private Gson gson;

	private ChargedItem[] chargedItems = new ChargedItem[0];

	public void loadConfigData() {
		chargedItems = new ChargedItem[]{
			new U_FishBarrel(client, client_thread, configs, items, chat_messages, notifier, gson, executorService),
			new U_LogBasket(client, client_thread, configs, items, chat_messages, notifier, gson, executorService),
			new S_KharedstMemoirs(client, client_thread, configs, items, chat_messages, notifier, gson, executorService),
			new U_BottomlessCompostBucket(client, client_thread, configs, items, chat_messages, notifier, gson, executorService),
			new U_AshSanctifier(client, client_thread, configs, items, chat_messages, notifier, gson, executorService),
			new U_BloodEssence(client, client_thread, configs, items, chat_messages, notifier, gson, executorService),
            new U_GemBag(client, client_thread, configs, items, chat_messages, notifier, gson, executorService),
            new U_CoalBag(client, client_thread, configs, items, chat_messages, notifier, gson, executorService),
			new U_HerbSack(client, client_thread, configs, items, chat_messages, notifier, gson, executorService),
			new U_SeedBox(client, client_thread, configs, items, chat_messages, notifier, gson, executorService),
		};
	}

	@Subscribe
	public void onItemDespawned(final ItemDespawned event)
	{
		log.debug("ITEM DESPAWNED | " + event.getItem().getId());

		for (final ChargedItem chargedItem : this.chargedItems) {
			chargedItem.onItemDespawned(event);
		}
	}

	@Subscribe
	public void onStatChanged(final StatChanged event)
	{
		log.debug("STAT CHANGED | " + event.getSkill());

		for (final ChargedItem chargedItem : this.chargedItems) {
			chargedItem.onStatChanged(event);
		}
	}

	@Subscribe
	public void onItemContainerChanged(final ItemContainerChanged event) {
		log.debug("ITEM CONTAINER | " + event.getContainerId());

		for (final ChargedItem chargedItem : this.chargedItems) {
			chargedItem.onItemContainersChanged(event);
		}
	}

	@Subscribe
	public void onChatMessage(final ChatMessage event) {
		Arrays.stream(chargedItems).forEach(chargedItem -> chargedItem.onChatMessage(event));
		log.debug(
			"MESSAGE | " +
				"type: " + event.getType().name() +
				", message: " + event.getMessage().replaceAll("</?col.*?>", "") +
				", sender: " + event.getSender()
		);
	}

	@Subscribe
	public void onAnimationChanged(final AnimationChanged event) {
		Arrays.stream(chargedItems).forEach(chargedItem -> chargedItem.onAnimationChanged(event));
		if (event.getActor() == client.getLocalPlayer()) {
			log.debug("ANIMATION | " +
				"id: " + event.getActor().getAnimation()
			);
		}
	}

	@Subscribe
	public void onGraphicChanged(final GraphicChanged event) {
		Arrays.stream(chargedItems).forEach(chargedItem -> chargedItem.onGraphicChanged(event));
		if (event.getActor() == client.getLocalPlayer()) {
			log.debug("GRAPHIC | " +
				"id: " + event.getActor().getGraphic()
			);
		}
	}

	// @Subscribe
	// public void onConfigChanged(final ConfigChanged event) {
	// 	Arrays.stream(chargedItems).forEach(chargedItem -> chargedItem.onConfigChanged(event));
	// 	if (event.getGroup().equals(config.GROUP)) {
	// 		log.debug("CONFIG | " +
	// 			"key: " + event.getKey() +
	// 			", old value: " + event.getOldValue() +
	// 			", new value: " + event.getNewValue()
	// 		);
	// 	}
	// }

	@Subscribe
	public void onHitsplatApplied(final HitsplatApplied event) {
		Arrays.stream(chargedItems).forEach(chargedItem -> chargedItem.onHitsplatApplied(event));
		log.debug("HITSPLAT | " +
			"actor: " + (event.getActor() == client.getLocalPlayer() ? "self" : "enemy") +
			", type: " + event.getHitsplat().getHitsplatType() +
			", amount:" + event.getHitsplat().getAmount() +
			", others = " + event.getHitsplat().isOthers() +
			", mine = " + event.getHitsplat().isMine()
		);
	}

	@Subscribe
	public void onWidgetLoaded(final WidgetLoaded event) {
		Arrays.stream(chargedItems).forEach(chargedItem -> chargedItem.onWidgetLoaded(event));
		log.debug("WIDGET | " +
			"group: " + event.getGroupId()
		);
	}

	@Subscribe
	public void onMenuOptionClicked(final MenuOptionClicked event) {
		Arrays.stream(chargedItems).forEach(chargedItem -> chargedItem.onMenuOptionClicked(event));
		log.debug("OPTION | " +
			"option: " + event.getMenuOption() +
			", target: " + event.getMenuTarget() +
			", action name: " + event.getMenuAction().name() +
			", action id: " + event.getMenuAction().getId()
		);
	}

	@Subscribe
	public void onGameTick(final GameTick gametick) {
		for (final ChargedItem chargedItem : this.chargedItems) {
			chargedItem.onGameTick(gametick);
		}
	}


	/// API for Plugin

	private  Map<Integer, Float> emptyMap = new HashMap<>();

	public boolean isChargeableItem(Integer itemId)
	{
		return getChargedItem(itemId) != null;
	}

	private ChargedItem getChargedItem(Integer itemId)
	{
		for (ChargedItem chargedItem : chargedItems) {
			//note that the item's item_id is constantly updated based on which variation is in your inventory/equipment
			if (chargedItem.item_id == itemId) {
				return chargedItem;
			}
		}
		return null;
	}

	public boolean hasChargeData(Integer itemId)
	{
		ChargedItem chargedItem = getChargedItem(itemId);
		if (chargedItem == null)
		{
			log.warn("Didn't find a charged item for this itemID, this shouldn't happen.");
			return false;
		}
		return chargedItem.hasChargeData();
	}

	public Map<Integer, Float> getItemQuantities(Integer itemId)
	{
		ChargedItem chargedItem = getChargedItem(itemId);
		if (chargedItem == null)
		{
			log.warn("Didn't find a charged item for this itemID, this shouldn't happen.");
			return emptyMap;
		}
		return chargedItem.getItemQuantities();
	}
}

