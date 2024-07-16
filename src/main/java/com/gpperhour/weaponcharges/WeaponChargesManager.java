/*
 * Copyright (c) 2023, Moshe Ben-Zacharia <https://github.com/MosheBenZacharia>, geheur <https://github.com/geheur>, Sir Girion https://github.com/sirgirion
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
package com.gpperhour.weaponcharges;

import com.gpperhour.GPPerHourConfig;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.VarPlayer;
import net.runelite.api.annotations.HitsplatType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.kit.KitType;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.util.Text;

@Slf4j
public class WeaponChargesManager
{
	@Data
	@AllArgsConstructor
	public class PickupAction
	{
		int itemId;
		WorldPoint worldPoint;
	}
	
	public static final String CONFIG_GROUP_NAME = GPPerHourConfig.GROUP;
	private static final int BLOWPIPE_ATTACK_ANIMATION = 5061;
	private static final int BLAZING_BLOWPIPE_ATTACK_ANIMATION = 10656;
	private static final int AMMO_SAVING_SETTING_VARBIT = 5697;

	// TODO rename. This is used for when an item is used on a weapon, when a weapon is used on an item, and when "pages" is clicked.
	ChargedWeapon lastUsedOnWeapon;
	ChargedWeapon lastUnchargeClickedWeapon;

	@Inject Client client;
	@Inject private ClientThread clientThread;
	@Inject private ChatboxPanelManager chatboxPanelManager;
	@Inject private ChatMessageManager chatMessageManager;
	@Inject private EventBus eventBus;
	@Inject private KeyManager keyManager;
	@Inject private DialogTracker dialogTracker;

	private boolean verboseLogging = false;
	private PickupAction lastPickUpAction;

	public void startUp()
	{
		lastLocalPlayerAnimationChangedGameTick = -1;
		lastLocalPlayerAnimationChanged = -1;
		dialogTracker.reset();
		eventBus.register(dialogTracker);
		keyManager.registerKeyListener(dialogTracker);
		dialogTracker.setStateChangedListener(this::dialogStateChanged);
		dialogTracker.setOptionSelectedListener(this::optionSelected);
	}

	public void shutDown()
	{
		eventBus.unregister(dialogTracker);
		keyManager.unregisterKeyListener(dialogTracker);
	}

	void dialogStateChanged(DialogTracker.DialogState dialogState)
	{
		// TODO if you can calculate the total charges available in the inventory you could get an accurate count on the "add how many charges" dialog, because max charges - max charges addable = current charges.

		for (ChargesDialogHandler nonUniqueDialogHandler : ChargedWeapon.getNonUniqueDialogHandlers())
		{
			nonUniqueDialogHandler.handleDialog(dialogState, this);
		}

		outer_loop:
		for (ChargedWeapon chargedWeapon : ChargedWeapon.values())
		{
			for (ChargesDialogHandler dialogHandler : chargedWeapon.getDialogHandlers())
			{
				if (dialogHandler.handleDialog(dialogState, this)) break outer_loop;
			}
		}
	}

	void optionSelected(DialogTracker.DialogState dialogState, String optionSelected)
	{
		// I don't think adding a single charge by using the items on the weapon is going to be trackable if the user
		// skips the sprite dialog.

		for (ChargesDialogHandler nonUniqueDialogHandler : ChargedWeapon.getNonUniqueDialogHandlers())
		{
			nonUniqueDialogHandler.handleDialogOptionSelected(dialogState, optionSelected, this);
		}

		outer_loop:
		for (ChargedWeapon chargedWeapon : ChargedWeapon.values())
		{
			for (ChargesDialogHandler dialogHandler : chargedWeapon.getDialogHandlers())
			{
				if (dialogHandler.handleDialogOptionSelected(dialogState, optionSelected, this)) break outer_loop;
			}
		}
	}


	private int lastDegradedHitsplatTick = -1000; // 1000 is far more than 91, so the serp helm will be able to have its degrading tracked on login rather than having to wait 90 ticks.

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied e) {

		Actor target = e.getActor();
		Hitsplat hitsplat = e.getHitsplat();

		int hitType = hitsplat.getHitsplatType();
		ChargedWeapon helm = getEquippedChargedWeapon(EquipmentInventorySlot.HEAD);
		if (helm == ChargedWeapon.SERPENTINE_HELM) {
			if (hitsplat.isMine()) { // Caused by or dealt to the local player.
				if (client.getTickCount() - lastDegradedHitsplatTick > 90) {
					addCharges(helm, -10, false);
					lastDegradedHitsplatTick = client.getTickCount();
					if (verboseLogging)
						client.addChatMessage(ChatMessageType.FRIENDSCHAT, "WeaponCharges", "Serpentine Helmet has Degraded!", "DEVMODE");
				}
			}
		}
		ChargedWeapon body = getEquippedChargedWeapon(EquipmentInventorySlot.BODY);
		ChargedWeapon legs = getEquippedChargedWeapon(EquipmentInventorySlot.LEGS);
		// >0 will filter out all unsuccessful hits and a small amount of successful hits
		if (target == client.getLocalPlayer() && hitType == HitsplatID.DAMAGE_ME && e.getHitsplat().getAmount() > 0) {
			if (helm == ChargedWeapon.CRYSTAL_HELM) {
				addCharges(helm, -1, false);
			}
			if (body == ChargedWeapon.CRYSTAL_BODY) {
				addCharges(body, -1, false);
			}
			if (legs == ChargedWeapon.CRYSTAL_LEGS) {
				addCharges(legs, -1, false);
			}
		}
		checkMeleeHitsplat(target, hitsplat);
	}

	//taken from thrall damage counter plugin AnimationData class
	private final Set<Integer> meleeAttackAnimations = Set.of(8056, 245,376,381,386,390,8288,8290,8289,9471,6118,393,0,395,400,401,406,407,414,419,422,423,428,429,440,1058,1060,1062,1378,1658,1665,1667,2066,2067,2078,2661,3297,3298,3852,4503,5865,7004,7045,7054,7055,7514,7515,7516,7638,7639,7640,7641,7642,7643,7644,7645,8145,9171,1203, 5439, 8640);
	private Hitsplat lastMeleeHitsplatApplied = null;

	private void checkMeleeHitsplat(Actor target, Hitsplat hitsplat)
	{
		//We inflicted damage to NPC (could be recoil, venge, thrall, or from a weapon)
		if (!hitsplat.isMine() || target == client.getLocalPlayer())
			return;
		
		//melee hitsplat always comes 1 tick after animation
		if (client.getTickCount() != lastLocalPlayerAnimationChangedGameTick + 1)
			return;

		if (!meleeAttackAnimations.contains(lastLocalPlayerAnimationChanged))
			return;

		// The weapon hitsplat is always last, after other hitsplats which occur on the same tick such as from
		// venge or thralls.
		lastMeleeHitsplatApplied = hitsplat;
	}

	// There are two lists to keep a list of checked weapons not just in the last tick, but in the last 2. I do this because
	// I'm paranoid that someone will somehow check an item without getting a check message, or the check message
	// does not match any regexes for some reason. This can cause the plugin to assign charges to the wrong weapon.
	private List<ChargedWeapon> lastWeaponChecked = new ArrayList<>();
	private List<ChargedWeapon> lastWeaponChecked2 = new ArrayList<>();

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuAction() == MenuAction.GROUND_ITEM_THIRD_OPTION && event.getMenuOption().equals("Take"))
		{
			WorldPoint point = WorldPoint.fromScene(client, event.getParam0(), event.getParam1(), client.getPlane());
			lastPickUpAction = new PickupAction(event.getId(), point);
		}
		if (event.getMenuOption().equalsIgnoreCase("check")) {
			// TODO investigate shift-click.
			if (verboseLogging) log.info("clicked \"check\" on " + event.getMenuTarget());

			if (WidgetUtil.componentToInterface(event.getParam1()) == InterfaceID.EQUIPMENT) { // item is equipped.
				int childId = WidgetUtil.componentToId(event.getParam1());
				if (childId == 18) {
					ChargedWeapon chargedWeapon = getEquippedChargedWeapon(EquipmentInventorySlot.WEAPON);
					if (chargedWeapon != null) lastWeaponChecked.add(chargedWeapon);
				} else if (childId == 20) {
					ChargedWeapon chargedWeapon = getEquippedChargedWeapon(EquipmentInventorySlot.SHIELD);
					if (chargedWeapon != null) lastWeaponChecked.add(chargedWeapon);
				}
			} else {
				for (ChargedWeapon chargedWeapon : ChargedWeapon.values())
				{
					if (chargedWeapon.getItemIds().contains(event.getItemId()) && chargedWeapon.getCheckChargesRegexes().isEmpty())
					{
						if (verboseLogging) log.info("adding last weapon checked to " + chargedWeapon);
						lastWeaponChecked.add(chargedWeapon);
						break;
					}
				}
			}
		} else if (event.getMenuOption().equalsIgnoreCase("uncharge")) {
			for (ChargedWeapon chargedWeapon : ChargedWeapon.values())
			{
				if (chargedWeapon.getItemIds().contains(event.getItemId()))
				{
					if (verboseLogging) log.info("setting lastUnchargeClickedWeapon to " + chargedWeapon);
					lastUnchargeClickedWeapon = chargedWeapon;
					break;
				}
			}
		} else if (event.getMenuOption().equalsIgnoreCase("unload") && isToxicBlowpipe(event.getItemId())) {
			checkBlowpipeUnload = client.getTickCount();
		} else if (event.getMenuOption().equalsIgnoreCase("pages")) {
			if (WidgetUtil.componentToInterface(event.getParam1()) == InterfaceID.EQUIPMENT) { // item is equipped.
				lastUsedOnWeapon = getEquippedChargedWeapon(EquipmentInventorySlot.SHIELD);
			} else {
				lastUsedOnWeapon = ChargedWeapon.getChargedWeaponFromId(event.getItemId());
			}
			if (verboseLogging) log.info("pages checked. setting last used weapon to {}", lastUsedOnWeapon.toString());
		}

		checkWidgetOnWidget(event);
	}

	void checkWidgetOnWidget(MenuOptionClicked event)
	{
		if (event.getMenuAction() == MenuAction.WIDGET_TARGET_ON_WIDGET) {
			ItemContainer itemContainer = client.getItemContainer(InventoryID.INVENTORY);
			if (itemContainer == null)
			{
				return;
			}
			Widget selectedWidget = client.getSelectedWidget();
			if (selectedWidget == null)
			{
				return;
			}
			Item itemUsed = itemContainer.getItem(selectedWidget.getIndex());
			if (itemUsed == null) return;
			int itemUsedId = itemUsed.getId();
			Widget eventWidget = event.getWidget();
			if (eventWidget == null)
			{
				return;
			}
			Item itemUsedOn = itemContainer.getItem(eventWidget.getIndex());
			if (itemUsedOn == null) return;
			int itemUsedOnId = itemUsedOn.getId();
			lastUsedOnWeapon = ChargedWeapon.getChargedWeaponFromId(itemUsedId);
			if (lastUsedOnWeapon == null)
			{
				lastUsedOnWeapon = ChargedWeapon.getChargedWeaponFromId(itemUsedOnId);
				if (lastUsedOnWeapon != null)
				{
					if (verboseLogging) log.info("{}: used {} on {}", client.getTickCount(), itemUsedId, lastUsedOnWeapon);
					checkSingleCrystalShardUse(itemUsed, itemUsedId);
				} else {
					if (verboseLogging) log.info("{}: used {} on {}", client.getTickCount(), itemUsedId, itemUsedOnId);
				}
			} else {
				if (verboseLogging) log.info("{}: used {} on {}", client.getTickCount(), lastUsedOnWeapon, itemUsedOnId);
				checkSingleCrystalShardUse(itemUsedOn, itemUsedOnId);
			}
		}
	}

	private void checkSingleCrystalShardUse(Item itemUsed, int itemUsedId)
	{
		if (itemUsedId == ItemID.CRYSTAL_SHARD && itemUsed.getQuantity() == 1 && ChargedWeapon.CRYSTAL_SHARD_RECHARGABLE_ITEMS.contains(lastUsedOnWeapon)) {
			checkSingleCrystalShardUse = client.getTickCount();
		}
	}

	private boolean hasToxicBlowpipeEquippedOrInInventory()
	{
		final ItemContainer itemContainer = client.getItemContainer(InventoryID.INVENTORY);
		if (itemContainer != null)
		{
			Item[] inventoryItems = itemContainer.getItems();
			for (Item item : inventoryItems)
			{
				if (isToxicBlowpipe(item.getId()))
				{
					return true;
				}
			}
		}
		final ItemContainer equipmentContainer = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipmentContainer != null)
		{
			Item weapon = equipmentContainer.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
			if (weapon != null && isToxicBlowpipe(weapon.getId()))
			{
				return true;
			}
		}
		return false;
	}

	private boolean isToxicBlowpipe(int itemId)
	{
		return itemId == ItemID.TOXIC_BLOWPIPE || itemId == ItemID.BLAZING_BLOWPIPE;
	}

	private boolean hasBlowpipeData()
	{
		return (getDartsLeft() != null) && (getScalesLeft() != null) && (getDartType() != DartType.UNKNOWN);
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		//see if user has ammo saving enabled
		if (client.getVarbitValue(AMMO_SAVING_SETTING_VARBIT) != 1)
		{
			return;
		}

		//check has blowpipe data
		if (!hasBlowpipeData())
		{
			return;
		}

		//check item is correct dart
		int itemId = event.getItem().getId();
		if (itemId != getDartType().itemId)
		{
			return;
		}

		//check has blowpipe equipped or in inventory
		if (!hasToxicBlowpipeEquippedOrInInventory())
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

		if (itemId != lastPickUpAction.getItemId())
		{
			return;
		}

		int quantity = event.getItem().getQuantity();
		addDartsLeft(quantity, false);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged itemContainerChanged) {
		if (itemContainerChanged.getContainerId() != InventoryID.INVENTORY.getId()) {
			return;
		}

		if (checkBlowpipeUnload == client.getTickCount() || checkBlowpipeUnload + 1 == client.getTickCount()) {
			setDartsLeft(0);
			setDartType(DartType.UNKNOWN);
		}

		if (checkSingleCrystalShardUse == client.getTickCount() || checkSingleCrystalShardUse + 1 == client.getTickCount()) {
			addCharges(lastUsedOnWeapon, 100, false);
		}
	}

	private final List<Runnable> delayChargeUpdateUntilAfterAnimations = new ArrayList<>();

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		String message = Text.removeTags(event.getMessage());

		for (ChargesMessage checkMessage : ChargedWeapon.getNonUniqueCheckChargesRegexes())
		{
			Matcher matcher = checkMessage.getPattern().matcher(message);
			if (matcher.find()) {
				ChargedWeapon chargedWeapon = removeLastWeaponChecked();
				// TODO possible to mess stuff up by checking a weapon immediately after the tome of water/fire dialog?
				if (chargedWeapon != null) {
					setCharges(chargedWeapon, checkMessage.getChargesLeft(matcher));
				} else if (lastUsedOnWeapon != null) {
					setCharges(lastUsedOnWeapon, checkMessage.getChargesLeft(matcher));
					if (verboseLogging) log.info("applying charges to last used-on weapon: {}", lastUsedOnWeapon);
				} else {
					log.warn("saw check message without having seen a charged weapon checked or used: \"" + message + "\"" );
				}
				break;
			}
		}

		for (ChargesMessage checkMessage : ChargedWeapon.getNonUniqueUpdateMessageChargesRegexes())
		{
			Matcher matcher = checkMessage.getPattern().matcher(message);
			if (matcher.find()) {
				int chargeCount = checkMessage.getChargesLeft(matcher);
				delayChargeUpdateUntilAfterAnimations.add(() -> {
					ChargedWeapon equippedWeapon = getEquippedChargedWeapon(EquipmentInventorySlot.WEAPON);
					if (equippedWeapon != null) {
						setCharges(equippedWeapon, chargeCount);
					} else {
						log.warn("saw charge update message without a weapon being equipped: \"" + message + "\"");
					}
				});
				break;
			}
		}

		outer_loop:
		for (ChargedWeapon chargedWeapon : ChargedWeapon.values())
		{
			if (chargedWeapon.getCheckChargesRegexes().isEmpty()) continue;

			for (ChargesMessage checkMessage : chargedWeapon.getCheckChargesRegexes())
			{
				Matcher matcher = checkMessage.getPattern().matcher(message);
				if (matcher.find()) {
					setCharges(chargedWeapon, checkMessage.getChargesLeft(matcher));
					break outer_loop;
				}
			}

			for (ChargesMessage checkMessage : chargedWeapon.getUpdateMessageChargesRegexes())
			{
				Matcher matcher = checkMessage.getPattern().matcher(message);
				if (matcher.find()) {
					delayChargeUpdateUntilAfterAnimations.add(() -> setCharges(chargedWeapon, checkMessage.getChargesLeft(matcher)));
					break outer_loop;
				}
			}
		}

		chatMessageBlowpipe(message);
	}

	private ChargedWeapon removeLastWeaponChecked()
	{
		return !lastWeaponChecked2.isEmpty() ? lastWeaponChecked2.remove(0) :
			!lastWeaponChecked.isEmpty() ? lastWeaponChecked.remove(0) :
				null;
	}

	private ChargedWeapon getEquippedChargedWeapon(EquipmentInventorySlot slot)
	{
		ItemContainer itemContainer = client.getItemContainer(InventoryID.EQUIPMENT);
		if (itemContainer == null) return null;

		Item item = itemContainer.getItem(slot.getSlotIdx());
		if (item == null) return null;

		return ChargedWeapon.getChargedWeaponFromId(item.getId());
	}

	/* blowpipe:
	// checking:
	// 2021-08-29 14:22:09 [Client] INFO  n.r.c.plugins.weaponcharges.Devtools - 1135: GAMEMESSAGE "Darts: <col=007f00>None</col>. Scales: <col=007f00>99 (0.6%)</col>."
	// 2021-09-05 13:55:04 [Client] INFO  com.weaponcharges.Devtools - 9: GAMEMESSAGE "Darts: <col=007f00>Adamant dart x 16,383</col>. Scales: <col=007f00>16,383 (100.0%)</col>."
	// 2021-09-05 13:55:26 [Client] INFO  com.weaponcharges.Devtools - 46: GAMEMESSAGE "Darts: <col=007f00>Adamant dart x 16,383</col>. Scales: <col=007f00>0 (0.0%)</col>."

	// adding charges either uses the check messages, or one of the following:
	// using scales on full blowpipe: 2021-09-05 13:48:26 [Client] INFO  com.weaponcharges.Devtools - 640: GAMEMESSAGE "The blowpipe can't hold any more scales."
	// using darts on full blowpipe: 2021-09-05 13:48:25 [Client] INFO  com.weaponcharges.Devtools - 638: GAMEMESSAGE "The blowpipe can't hold any more darts."

	// run out of darts: 2021-08-29 14:19:11 [Client] INFO  n.r.c.plugins.weaponcharges.Devtools - 841: GAMEMESSAGE "Your blowpipe has run out of darts."
	// run out of scales: 2021-08-29 14:18:27 [Client] INFO  n.r.c.plugins.weaponcharges.Devtools - 767: GAMEMESSAGE "Your blowpipe needs to be charged with Zulrah's scales."
	// run out of both: 2021-09-05 13:45:24 [Client] INFO  com.weaponcharges.Devtools - 336: GAMEMESSAGE "Your blowpipe has run out of scales and darts."

	// (attacking with no darts: 2021-09-05 13:43:43 [Client] INFO  com.weaponcharges.Devtools - 169: GAMEMESSAGE "Your blowpipe contains no darts."
	// (attacking with no darts or scales (trying to equip blowpipe without EITHER scales or darts in it produces the same message, lol) : 2021-09-05 13:45:43 [Client] INFO  com.weaponcharges.Devtools - 369: GAMEMESSAGE "Your blowpipe needs to be charged with Zulrah's scales and loaded with darts."
	// (attacking with no scales, same as run out of scales message): 2021-09-05 13:47:42 [Client] INFO  com.weaponcharges.Devtools - 566: GAMEMESSAGE "Your blowpipe needs to be charged with Zulrah's scales."

	// unload
	// unload with no darts: 2021-09-05 13:59:25 [Client] INFO  com.weaponcharges.Devtools - 443: GAMEMESSAGE "The blowpipe has no darts in it."
	// unload with darts has no chat message.

	// don't care because when you add charges after it always produces a chat message.
	// uncharge 2021-09-05 14:40:47 [Client] INFO  com.weaponcharges.Devtools - 481: dialog state changed: DialogState{DESTROY_ITEM, title='Are you sure you want to uncharge it?', itemId=12926, item_name='Toxic blowpipe', text='If you uncharge the blowpipe, all scales and darts will fall out.'}
	 */

	// check messages.
	private static final Pattern NO_DARTS_CHECK_PATTERN = Pattern.compile("Darts: None. Scales: ([\\d,]+) \\(\\d+[.]?\\d%\\).");
	private static final Pattern DARTS_AND_SCALE_CHECK_PATTERN = Pattern.compile("Darts: (\\S*)(?: dart)? x ([\\d,]+). Scales: ([\\d,]+) \\(\\d+[.]?\\d%\\).");
	private static final Pattern USE_SCALES_ON_FULL_BLOWPIPE_PATTERN = Pattern.compile("The blowpipe can't hold any more scales.");
	private static final Pattern USE_DARTS_ON_FULL_BLOWPIPE_PATTERN = Pattern.compile("The blowpipe can't hold any more darts.");
	private static final Pattern UNLOAD_EMPTY_BLOWPIPE_PATTERN = Pattern.compile("The blowpipe has no darts in it.");

	// update messages.
	private static final Pattern NO_DARTS_PATTERN = Pattern.compile("Your blowpipe has run out of darts.");
	private static final Pattern NO_SCALES_PATTERN = Pattern.compile("Your blowpipe needs to be charged with Zulrah's scales.");
	private static final Pattern NO_DARTS_OR_SCALES_PATTERN = Pattern.compile("Your blowpipe has run out of scales and darts.");
	private static final Pattern NO_DARTS_PATTERN_2 = Pattern.compile("Your blowpipe contains no darts.");
	private static final Pattern NO_DARTS_OR_SCALES_PATTERN_2 = Pattern.compile("Your blowpipe needs to be charged with Zulrah's scales and loaded with darts.");

	private void chatMessageBlowpipe(String chatMsg)
	{
		Matcher matcher = DARTS_AND_SCALE_CHECK_PATTERN.matcher(chatMsg);
		if (matcher.find())
		{
			setDartsLeft(Integer.parseInt(matcher.group(2).replace(",", "")));
			setScalesLeft(Integer.parseInt(matcher.group(3).replace(",", "")));
			setDartType(DartType.getDartTypeByName(matcher.group(1)));
		}

		matcher = NO_DARTS_CHECK_PATTERN.matcher(chatMsg);
		if (matcher.find())
		{
			setDartsLeft(0);
			setScalesLeft(Integer.parseInt(matcher.group(1).replace(",", "")));
			setDartType(DartType.UNKNOWN);
		}

		matcher = USE_SCALES_ON_FULL_BLOWPIPE_PATTERN.matcher(chatMsg);
		if (matcher.find()) {
			setScalesLeft(MAX_SCALES_BLOWPIPE);
		}

		matcher = USE_DARTS_ON_FULL_BLOWPIPE_PATTERN.matcher(chatMsg);
		if (matcher.find()) {
			setDartsLeft(MAX_DARTS);
		}

		matcher = UNLOAD_EMPTY_BLOWPIPE_PATTERN.matcher(chatMsg);
		if (matcher.find()) {
			setDartsLeft(0);
			setDartType(DartType.UNKNOWN);
		}

		matcher = NO_DARTS_PATTERN.matcher(chatMsg);
		if (matcher.find()) {
			delayChargeUpdateUntilAfterAnimations.add(() -> {
				setDartsLeft(0);
				setDartType(DartType.UNKNOWN);
			});
		}

		matcher = NO_SCALES_PATTERN.matcher(chatMsg);
		if (matcher.find())
		{
			delayChargeUpdateUntilAfterAnimations.add(() -> setScalesLeft(0));
		}

		matcher = NO_DARTS_OR_SCALES_PATTERN.matcher(chatMsg);
		if (matcher.find())
		{
			delayChargeUpdateUntilAfterAnimations.add(() -> {
				setScalesLeft(0);
				setDartsLeft(0);
				setDartType(DartType.UNKNOWN);
			});
		}

		matcher = NO_DARTS_PATTERN_2.matcher(chatMsg);
		if (matcher.find())
		{
			delayChargeUpdateUntilAfterAnimations.add(() -> {
				setDartsLeft(0);
				setDartType(DartType.UNKNOWN);
			});
		}

		matcher = NO_DARTS_OR_SCALES_PATTERN_2.matcher(chatMsg);
		if (matcher.find())
		{
			delayChargeUpdateUntilAfterAnimations.add(() -> {
				setScalesLeft(0);
				setDartsLeft(0);
				setDartType(DartType.UNKNOWN);
			});
		}
	}

	private boolean checkTomeOfFire = false;
	private boolean checkTomeOfWater = false;
	private int checkBlowpipeUnload = -100;
	private int checkSingleCrystalShardUse = -100;

	@Subscribe
	public void onClientTick(ClientTick clientTick)
	{
		if (checkTomeOfFire) {
			Player localPlayer = client.getLocalPlayer();
			if (
				localPlayer.hasSpotAnim(99) ||
					localPlayer.hasSpotAnim(126) ||
					localPlayer.hasSpotAnim(129) ||
					localPlayer.hasSpotAnim(155) ||
					localPlayer.hasSpotAnim(1464)
			) {
				addCharges(ChargedWeapon.TOME_OF_FIRE, -1, false);
			}
		}
		checkTomeOfFire = false;

		if (checkTomeOfWater) {
			Player localPlayer = client.getLocalPlayer();
			if (
					localPlayer.hasSpotAnim(177) || //bind/snare/entangle
					localPlayer.hasSpotAnim(102) || //curse spells
					localPlayer.hasSpotAnim(105) ||
					localPlayer.hasSpotAnim(108) ||
					localPlayer.hasSpotAnim(167) ||
					localPlayer.hasSpotAnim(170) ||
					localPlayer.hasSpotAnim(173) ||
					localPlayer.hasSpotAnim( 93) || //water spells
					localPlayer.hasSpotAnim(120) ||
					localPlayer.hasSpotAnim(135) ||
					localPlayer.hasSpotAnim(161) ||
					localPlayer.hasSpotAnim(1458)
			) {
				addCharges(ChargedWeapon.TOME_OF_WATER, -1, false);
			}
			checkTomeOfWater = false;
		}
	}

	private int lastLocalPlayerAnimationChangedGameTick = -1;
	// I record the animation id so that animation changing plugins that change the animation (e.g. weapon animation replacer) can't interfere.
	private int lastLocalPlayerAnimationChanged = -1;

	@Subscribe(priority = 10.0f) // I want to get ahead of those pesky animation modifying plugins.
	public void onAnimationChanged(AnimationChanged event)
	{
		final Actor actor = event.getActor();
		if (actor != client.getLocalPlayer()) return;

		lastLocalPlayerAnimationChangedGameTick = client.getTickCount();
		lastLocalPlayerAnimationChanged = actor.getAnimation();
	}

	private static final int TICKS_RAPID_PVM = 2;
	//	private static final int TICKS_RAPID_PVP = 3;
	private static final int TICKS_NORMAL_PVM = 3;
	//	private static final int TICKS_NORMAL_PVP = 4;
	public static final int MAX_SCALES_BLOWPIPE = 16383;
	public static final int MAX_DARTS = 16383;

	private int ticks = 0;
	private int ticksInAnimation;
	private int lastAnimationStart = 0;

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		//doesn't properly account for successful hits that roll 0 damage
		if (lastMeleeHitsplatApplied != null && lastMeleeHitsplatApplied.getAmount() > 0)
		{
			ChargedWeapon amulet = getEquippedChargedWeapon(EquipmentInventorySlot.AMULET);
			if (amulet == ChargedWeapon.BLOOD_FURY)
			{
				// assume scythe does three hits and all are successful if one is
				if (lastLocalPlayerAnimationChanged == 8056)
				{
					addCharges(amulet, -3, false);
				}
				else
				{
					addCharges(amulet, -1, false);
				}
			}
		}
		lastMeleeHitsplatApplied = null;
		// This delay is necessary because equipped items are updated after onAnimationChanged, so with items that share
		// a game message it will not be possible to tell which item the message is for.
		// The order must be: check messages, animation, charge update messages.
		// Runelite's order is: onChatMessage, onAnimationChanged, onGameTick.
		// charge update messages must also be delayed due to equipment slot info not being current in onChatMessage.
		if (lastLocalPlayerAnimationChangedGameTick == client.getTickCount()) 
			checkAnimation();

		if (lastLocalPlayerAnimationChanged == BLOWPIPE_ATTACK_ANIMATION || lastLocalPlayerAnimationChanged == BLAZING_BLOWPIPE_ATTACK_ANIMATION)
		{
			blowpipeOnGameTick();
		}

		if (!delayChargeUpdateUntilAfterAnimations.isEmpty()) {
			for (Runnable runnable : delayChargeUpdateUntilAfterAnimations)
			{
				runnable.run();
			}
			delayChargeUpdateUntilAfterAnimations.clear();
		}

		if (!lastWeaponChecked2.isEmpty()) {
			log.warn("checked weapons with no check message: " + lastWeaponChecked2);
		}
		lastWeaponChecked2.clear();
		lastWeaponChecked2 = lastWeaponChecked;
		lastWeaponChecked = new ArrayList<>();
	}

	private void blowpipeOnGameTick()
	{
		if (ticks == 0) {
			lastAnimationStart = client.getTickCount();
		} else {
			if (client.getTickCount() - lastAnimationStart > ticksInAnimation) {
				ticks = 0;
				lastAnimationStart = client.getTickCount();
			}
		}

		ticks++;
//		System.out.println(client.getTickCount() + " blowpipe: " + ticks + " " + ticksInAnimation);

		if (ticks == ticksInAnimation)
		{
//			System.out.println(client.getTickCount() + " blowpipe hits (animation update): " + ++blowpipeHits + " " + blowpipeHitsBySound);
			consumeBlowpipeCharges();
			ticks = 0;
		}
	}

	private void checkAnimation()
	{
		ItemContainer itemContainer = client.getItemContainer(InventoryID.EQUIPMENT);
		if (itemContainer == null) return;

		Item weapon = itemContainer.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		int weaponItemId = (weapon == null) ? -1 : weapon.getId();

		Item offhand = itemContainer.getItem(EquipmentInventorySlot.SHIELD.getSlotIdx());
		int offhandItemId = (offhand == null) ? -1 : offhand.getId();

		for (ChargedWeapon chargedWeapon : ChargedWeapon.values()) {
			if (
				(chargedWeapon.getItemIds().contains(weaponItemId) || chargedWeapon.getItemIds().contains(offhandItemId)) &&
					chargedWeapon.animationIds.contains(lastLocalPlayerAnimationChanged))
			{
				if (chargedWeapon == ChargedWeapon.TOME_OF_FIRE) {
					checkTomeOfFire = true;
				} else if (chargedWeapon == ChargedWeapon.TOME_OF_WATER) {
					checkTomeOfWater = true;
				} else {
					addCharges(chargedWeapon, -1, false);
				}
			}
		}
	}

	private void consumeBlowpipeCharges()
	{
		addDartsLeft(-1 * getAmmoLossChance(), false);
		addScalesLeft(-2/3f, false);
	}

	private float getAmmoLossChance()
	{
		int attractorEquippedId = client.getLocalPlayer().getPlayerComposition().getEquipmentId(KitType.CAPE);
		switch (attractorEquippedId) {
			case ItemID.AVAS_ATTRACTOR:
				return 0.4f;
			case ItemID.AVAS_ACCUMULATOR:
			case ItemID.ACCUMULATOR_MAX_CAPE:
				return 0.28f;
			case ItemID.RANGING_CAPE:
			case ItemID.RANGING_CAPET:
			case ItemID.MAX_CAPE:
				return 0.28f;
			case ItemID.AVAS_ASSEMBLER:
			case ItemID.AVAS_ASSEMBLER_L:
			case ItemID.ASSEMBLER_MAX_CAPE:
			case ItemID.ASSEMBLER_MAX_CAPE_L:
			case ItemID.MASORI_ASSEMBLER:
			case ItemID.MASORI_ASSEMBLER_L:
			case ItemID.MASORI_ASSEMBLER_MAX_CAPE:
			case ItemID.MASORI_ASSEMBLER_MAX_CAPE_L:
				return 0.2f;
			default:
				// no ammo-saving thing equipped.
				return 1f;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarpId() == VarPlayer.ATTACK_STYLE) {
			ticksInAnimation = event.getValue() == 1 ? TICKS_RAPID_PVM : TICKS_NORMAL_PVM;
		}
	}

	@Inject
	ConfigManager configManager;

	public Integer getCharges(ChargedWeapon weapon) {
		String configString = configManager.getRSProfileConfiguration(CONFIG_GROUP_NAME, weapon.configKeyName);
		if (configString == null) return null;
		return Integer.parseInt(configString);
	}

	public void setCharges(ChargedWeapon weapon, int charges) {
		setCharges(weapon, charges, true);
	}

	public void setCharges(ChargedWeapon weapon, int charges, boolean logChange) {
		configManager.setRSProfileConfiguration(CONFIG_GROUP_NAME, weapon.configKeyName, Math.max(charges, 0));
		if (verboseLogging)
		{
			log.info("set charges for " + weapon + " to " + charges);
		}
	}

	public void addCharges(ChargedWeapon weapon, int change, boolean logChange) {
		Integer charges = getCharges(weapon);
		if (verboseLogging)
		{
			log.info("Adding " + change + " charges to " + weapon.name + "which has " + charges + "charges.");
		}
		setCharges(weapon, (charges == null ? 0 : charges) + change, logChange);
	}

	public Float getDartsLeft()
	{
		String configString = configManager.getRSProfileConfiguration(CONFIG_GROUP_NAME, "blowpipeDarts");
		if (configString == null) return null;
		return Float.parseFloat(configString);
	}

	void setDartsLeft(float dartsLeft)
	{
		setDartsLeft(dartsLeft, true);
	}

	private void setDartsLeft(float dartsLeft, boolean logChange)
	{
		configManager.setRSProfileConfiguration(CONFIG_GROUP_NAME, "blowpipeDarts", dartsLeft);
		if (verboseLogging)
		{
			log.info("set darts left to " + dartsLeft);
		}
	}

	private void addDartsLeft(float change, boolean logChange) {
		Float dartsLeft = getDartsLeft();
		setDartsLeft((dartsLeft == null ? 0 : dartsLeft) + change, logChange);
	}

	public DartType getDartType()
	{
		String configString = configManager.getRSProfileConfiguration(CONFIG_GROUP_NAME, "blowpipeDartType");
		if (configString == null) return DartType.UNKNOWN;
		return DartType.valueOf(configString);
	}

	void setDartType(DartType dartType)
	{
		configManager.setRSProfileConfiguration(CONFIG_GROUP_NAME, "blowpipeDartType", dartType);
		if (verboseLogging)
			log.info("set dart type to " + dartType);
	}

	public Float getScalesLeft()
	{
		String configString = configManager.getRSProfileConfiguration(CONFIG_GROUP_NAME, "blowpipeScales");
		if (configString == null) return null;
		return Float.parseFloat(configString);
	}

	void setScalesLeft(float scalesLeft)
	{
		setScalesLeft(scalesLeft, true);
	}

	private void setScalesLeft(float scalesLeft, boolean logChange)
	{
		configManager.setRSProfileConfiguration(CONFIG_GROUP_NAME, "blowpipeScales", scalesLeft);
		if (verboseLogging)
		{
			log.info("set scales left to " + scalesLeft);
		}
	}

	private void addScalesLeft(float change, boolean logChange) {
		Float scalesLeft = getScalesLeft();
		setScalesLeft((scalesLeft == null ? 0 : scalesLeft) + change, logChange);
	}

	@RequiredArgsConstructor
	public enum DartType {
		UNKNOWN(-1, Color.LIGHT_GRAY, null),
		BRONZE(ItemID.BRONZE_DART, new Color(0x6e5727), "bronze"),
		IRON(ItemID.IRON_DART, new Color(0x52504c), "iron"),
		STEEL(ItemID.STEEL_DART, new Color(0x7a7873), "steel"),
		MITHRIL(ItemID.MITHRIL_DART, new Color(0x414f78), "mithril"),
		ADAMANT(ItemID.ADAMANT_DART, new Color(0x417852), "adamant"),
		RUNE(ItemID.RUNE_DART, new Color(0x67e0f5), "rune"),
		AMETHYST(ItemID.AMETHYST_DART, new Color(0xc87dd4), "amethyst"),
		DRAGON(ItemID.DRAGON_DART, new Color(0x3e7877), "dragon"),
		;

		public final int itemId;
		public final Color displayColor;
		public final String checkBlowpipeMessageName;

		public static DartType getDartTypeByName(String group)
		{
			group = group.toLowerCase();
			for (DartType dartType : DartType.values())
			{
				if (dartType.checkBlowpipeMessageName != null && dartType.checkBlowpipeMessageName.equals(group)) {
					return dartType;
				}
			}
			return null;
		}
	}

	/// API for Plugin

	private  Map<Integer, Float> emptyMap = new HashMap<>();

	public boolean isChargeableWeapon(Integer itemId)
	{
		for (ChargedWeapon chargedWeapon : ChargedWeapon.values()) {
			if (chargedWeapon.getItemIds().contains(itemId)) {
				return true;
			}
		}
		return false;
	}

	private ChargedWeapon getChargedWeapon(Integer itemId)
	{
		for (ChargedWeapon chargedWeapon : ChargedWeapon.values()) {
			if (chargedWeapon.getItemIds().contains(itemId)) {
				return chargedWeapon;
			}
		}
		return null;
	}

	public boolean hasChargeData(Integer itemId)
	{
		if (isToxicBlowpipe(itemId))
		{
			return hasBlowpipeData();
		}
		for (ChargedWeapon chargedWeapon : ChargedWeapon.values()) {
			if (chargedWeapon.getItemIds().contains(itemId)) {
				return getCharges(chargedWeapon) != null;
			}
		}
		log.warn("Didn't find a chargeable weapon for this itemID, this shouldn't happen.");
		return false;
	}

	//avoid GC
	private  Map<Integer, Float> blowpipeMap = new HashMap<>();

	public Map<Integer, Float> getChargeComponents(Integer itemId)
	{
		ChargedWeapon weapon = null;
		for (ChargedWeapon chargedWeapon : ChargedWeapon.values()) {
			if (chargedWeapon.getItemIds().contains(itemId)) {
				weapon = chargedWeapon;
				break;
			}
		}
		if (weapon == null)
		{
			log.warn("Weapon is NULL");
			return emptyMap;
		}
		//special case cause of how fucked the blowpipe is
		if (weapon == ChargedWeapon.TOXIC_BLOWPIPE)
		{
			blowpipeMap.clear();
			Float dartCount = getDartsLeft();
			Float scaleCount = getScalesLeft();
			DartType dartType = getDartType();
			blowpipeMap.put(dartType.itemId, dartCount);
			blowpipeMap.put(ItemID.ZULRAHS_SCALES, scaleCount);
			return blowpipeMap;
		}
		return weapon.getChargeComponents(getCharges(weapon));
	}
}