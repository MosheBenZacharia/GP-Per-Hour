/*
 * Copyright (c) 2023, Moshe Ben-Zacharia <https://github.com/MosheBenZacharia>, Patrick Watts <https://github.com/pwatts6060>
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
package com.gpperhour;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.MenuAction;
import net.runelite.api.Varbits;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
//Keeps track of the items in your looting bag, supports all operations except adding items by using them on it when a dialog appears (if there are multiple).
public class LootingBagManager
{
	@Data
	@AllArgsConstructor
	public class PickupAction
	{
		int itemId;
		WorldPoint worldPoint;
	}

	public static final int LOOTING_BAG_CONTAINER = 516;
	private static final Set<Integer> FEROX_REGION = ImmutableSet.of(12600, 12344);
	private static final int LOOTING_BAG_SIZE = 28;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	@Inject
	private ScheduledExecutorService executor;

	private Map<Integer, Integer> bagItems = null;
	private int lastLootingBagUseOn = -2;

	private PickupAction lastPickUpAction;

	public void loadConfigData()
	{
		Type mapType = new com.google.gson.reflect.TypeToken<Map<Integer, Integer>>() {}.getType();

		String json = configManager.getRSProfileConfiguration(GPPerHourConfig.GROUP, GPPerHourConfig.looting_bag);
		try 
		{
			bagItems = gson.fromJson(json, mapType);
		}
		catch(Exception e)
		{
			log.error("Failed to load looting bag data from json: " + json, e);
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widgetLoaded)
	{
		if (widgetLoaded.getGroupId() != WidgetInfo.LOOTING_BAG_CONTAINER.getGroupId())
		{
			return;
		}
		updateValue();
	}

	private void updateValue()
	{
		ItemContainer itemContainer = client.getItemContainer(LOOTING_BAG_CONTAINER);
		if (itemContainer == null)
		{
			if (bagItems == null)
				bagItems = new HashMap<>();
			bagItems.clear();
			saveData();
			return;
		}
		if (bagItems == null)
			bagItems = new HashMap<>();
		bagItems.clear();
		for (Item item : itemContainer.getItems())
		{
			if (item.getId() >= 0)
			{
				bagItems.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}
		saveData();
	}

	void addLootingBagContents(Map<Integer,Float> qtyMap)
	{
		//needs to be checked/calibrated
		if (bagItems == null)
		{
			return;
		}
		for (Integer itemId: bagItems.keySet())
		{
			qtyMap.merge(itemId, (float) bagItems.get(itemId), Float::sum);
		}
	}

	//avoid GC
	private Map<Integer, Integer> differenceMap = new HashMap<>();
	private Item[] inventory_items;

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getItemContainer() == null)
			return;

		if (event.getContainerId() == InventoryID.INVENTORY.getId())
		{
			if (inventory_items != null && bagItems != null &&
				(lastLootingBagUseOn == client.getTickCount() || lastLootingBagUseOn + 1 == client.getTickCount()))
			{
				differenceMap.clear();
				Item[] before = inventory_items;
				Item[] after = event.getItemContainer().getItems();
				for (Item beforeItem : before)
				{
					differenceMap.merge(beforeItem.getId(), 1, Integer::sum);
				}
				for (Item afterItem : after)
				{
					differenceMap.merge(afterItem.getId(), -1, Integer::sum);
				}
				for (Integer itemId : differenceMap.keySet())
				{
					Integer count = differenceMap.get(itemId);
					if (count > 0 && canAddItem(itemId))
					{
						bagItems.merge(itemId, count, Integer::sum);
					}
				}
				saveData();
			}
			inventory_items = event.getItemContainer().getItems();
		}
		if (event.getContainerId() == LOOTING_BAG_CONTAINER)
		{
			updateValue();
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuAction() == MenuAction.GROUND_ITEM_THIRD_OPTION && event.getMenuOption().equals("Take"))
		{
			WorldPoint point = WorldPoint.fromScene(client, event.getParam0(), event.getParam1(), client.getPlane());
			lastPickUpAction = new PickupAction(event.getId(), point);
		}

		if (event.getMenuAction() == MenuAction.WIDGET_TARGET_ON_WIDGET) {
			ItemContainer itemContainer = client.getItemContainer(InventoryID.INVENTORY);
			if (itemContainer == null)
				return;
			Widget widgetA = client.getSelectedWidget();
			if (widgetA == null)
				return;
			Item itemA = itemContainer.getItem(widgetA.getIndex());
			if (itemA == null) 
				return;
			int itemAId = itemA.getId();
			Widget widgetB = event.getWidget();
			if (widgetB == null)
				return;
			Item itemB = itemContainer.getItem(widgetB.getIndex());
			if (itemB == null) 
				return;
			int itemBId = itemB.getId();

			boolean usedItemOnLootingBag = isLootingBag(itemAId) || isLootingBag(itemBId);
			if (usedItemOnLootingBag)
			{
				lastLootingBagUseOn = client.getTickCount();
			}
		}
	}

	private boolean isLootingBag(int itemId)
	{
		return itemId == ItemID.LOOTING_BAG || itemId == ItemID.LOOTING_BAG_22586;
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		if (bagItems == null)
		{
			return;
		}

		// not in wilderness or ferox -> can't pick up items directly into looting bag
		if (client.getVarbitValue(Varbits.IN_WILDERNESS) == 0
			&& !FEROX_REGION.contains(client.getLocalPlayer().getWorldLocation().getRegionID()))
		{
			return;
		}

		// doesn't have open looting bag
		ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
		if (inv == null || !inv.contains(ItemID.LOOTING_BAG_22586))
		{
			return;
		}

		if (lastPickUpAction == null)
		{
			return;
		}

		// not on same tile
		if (!event.getTile().getWorldLocation().equals(client.getLocalPlayer().getWorldLocation()))
		{
			return;
		}

		if (!event.getTile().getWorldLocation().equals(lastPickUpAction.getWorldPoint()))
		{
			return;
		}

		int itemId = event.getItem().getId();

		if (itemId != lastPickUpAction.getItemId())
		{
			return;
		}

		ItemComposition itemComposition = itemManager.getItemComposition(itemId);

		if (!itemComposition.isTradeable())
		{
			return;
		}

		if (!canAddItem(itemId))
		{
			return;
		}

		int quantity = event.getItem().getQuantity();
		bagItems.merge(itemId, quantity, Integer::sum);
		saveData();
	}

	private boolean canAddItem(int itemId)
	{
		if (isStackable(itemId) && bagItems.containsKey(itemId))
		{
			return true;
		}
		else
		{
			int slotsUsed = 0;
			for (Integer bagItemId : bagItems.keySet())
			{
				slotsUsed += isStackable(bagItemId) ? 1 : bagItems.get(bagItemId);
			}
			return slotsUsed < LOOTING_BAG_SIZE;
		}
	}

	private boolean isStackable(int itemId)
	{
		return itemManager.getItemComposition(itemId).isStackable();
	}

	private void saveData()
	{
		executor.execute(() ->
		{
			configManager.setRSProfileConfiguration(GPPerHourConfig.GROUP, GPPerHourConfig.looting_bag, gson.toJson(this.bagItems));
		});
	}

	public long getPrice(int itemId)
	{
		return itemManager.getItemPrice(itemId);
	}

	boolean needsCheck()
	{
		return bagItems == null;
	}
}
