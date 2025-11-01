/*
 * Copyright (c) 2023, Moshe Ben-Zacharia <https://github.com/MosheBenZacharia>, Eric Versteeg <https://github.com/erversteeg>
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

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;

import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.swing.SwingUtilities;

import com.gpperhour.GoldDropManager.GoldDropDisplayMode;
import com.gpperhour.itemcharges.ChargedItemManager;
import com.gpperhour.weaponcharges.WeaponChargesManager;
import com.google.gson.Gson;
import com.google.inject.Provides;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.MenuAction;
import net.runelite.api.ObjectID;
import net.runelite.api.Varbits;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
	name = "GP Per Hour",
	description = "Track your gp/hr across various trips and save your sessions for later viewing.",
	tags = {
		"inventory",
		"total",
		"profit",
		"tracker",
		"loss",
		"gp",
		"per",
		"hour",
		"prices",
		"gold farming",
		"gp/hr"
	}
)

@Slf4j
public class GPPerHourPlugin extends Plugin
{
	private static final String plugin_version = "1.13";
	private static final String plugin_message = "" +
		"GP Per Hour " + plugin_version + ":<br>" +
				"* Apply ignore list to runes in rune pouch.";
	static final int COINS = ItemID.COINS_995;
	static final int NO_PROFIT_LOSS_TIME = -1;
	static final int RUNEPOUCH_ITEM_ID = 12791;
	static final int DIVINE_RUNEPOUCH_ITEM_ID = 27281;
	public static final float roundAmount = 0.01f;

    // static item prices so that when ItemManager updates, the Profit / Loss value doesn't all of a sudden change
    // this is cleared and repopulated at the start of each new run (after bank) and whenever new items hit the inventory
    private static final Map<Integer, Float> itemPrices = new HashMap<>();
	//so we can do name lookups on the swing thread
	private static final Map<Integer, String> itemNames = new HashMap<>();

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private ActiveTripOverlay tripOverlay;

	@Inject
	private WeaponChargesManager weaponChargesManager;

	@Inject
	private ChargedItemManager chargedItemManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	@Getter
	private Client client;

	@Inject
	private GPPerHourConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemManager itemManager;

	@Inject
	private LootingBagManager lootingBagManager;
	
	@Inject
	private EventBus eventBus;
	
	@Inject
	private ConfigManager configManager;
	
	@Inject
	private Gson gson;
	
    @Inject
    private ClientToolbar clientToolbar;
	
	@Inject
	private ChatMessageManager chat_messages;

	@Getter
	private SessionManager sessionManager;

	private GPPerHourPanel gpPerHourPanel;
	private ActiveSessionPanel activeSessionPanel;
	private SessionHistoryPanel sessionHistoryPanel;

	@Getter
	private TripData runData;

	@Inject
	private GoldDropManager goldDropManager;

	@Getter
	private TrackingMode mode = TrackingMode.TOTAL;

	@Getter
	private RunState state = RunState.NONE;
	@Getter
	private RunState previousState = RunState.NONE;

	@Getter @Setter
	private long totalGp = 0;
	private Long previousTotalGp = null;

	private long initialGp = 0;
	
    private BufferedImage icon;
    private NavigationButton navButton;
	private final Map<Integer, Float> inventoryQtyMap = new HashMap<>();
	private final Map<Integer, Float> equipmentQtyMap = new HashMap<>();
	private final Map<Integer, Float> rewardsQtyMap = new HashMap<>();
	private final HashSet<String> ignoredItems = new HashSet<>();
	private int depositInteractionTick;
	//if its been more than 30 ticks you probably cancelled your interaction
	private static final int interactionTickThreshold = 30;

	@Getter
	private Widget inventoryWidget;
	private ItemContainer inventoryItemContainer;
	private ItemContainer equipmentItemContainer;
	private boolean postNewRun = false;
	private long newRunTick = 0;
	private boolean expectingPutAnimation = false;

	// from ClueScrollPlugin
	private static final int[] RUNEPOUCH_AMOUNT_VARBITS = {
			Varbits.RUNE_POUCH_AMOUNT1, Varbits.RUNE_POUCH_AMOUNT2, Varbits.RUNE_POUCH_AMOUNT3, Varbits.RUNE_POUCH_AMOUNT4
	};
	private static final int[] RUNEPOUCH_RUNE_VARBITS = {
			Varbits.RUNE_POUCH_RUNE1, Varbits.RUNE_POUCH_RUNE2, Varbits.RUNE_POUCH_RUNE3, Varbits.RUNE_POUCH_RUNE4
	};
    @AllArgsConstructor
    public enum ValueMode {
        RUNELITE_VALUE        	("Runelite (Default)"),
        LOW_ALCHEMY_VALUE    	("Low Alchemy"),
        HIGH_ALCHEMY_VALUE     	("High Alchemy");
        private final String configName;
        @Override
        public String toString() { return configName; }
    }

	@Override
	protected void startUp() throws Exception
	{
		if (config.showTripOverlay())
			overlayManager.add(tripOverlay);
		if (config.goldDropsDisplayMode() == GoldDropDisplayMode.STATIC)
			overlayManager.add(goldDropManager);

		eventBus.register(lootingBagManager);
		eventBus.register(weaponChargesManager);
		eventBus.register(chargedItemManager);
		weaponChargesManager.startUp();
		
		sessionManager = new SessionManager(this, config, executor, gson);
		buildSidePanel();
		updatePanels();
		refreshIgnoredItems();
		checkLoadingState(true);

		//Migrate old settings
		if (config.goldDrops())
		{
			config.setGoldDropsDisplayMode(GoldDropDisplayMode.VANILLA);
			config.setGoldDrops(false);
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(tripOverlay);
		overlayManager.remove(goldDropManager);
		eventBus.unregister(lootingBagManager);
		eventBus.unregister(weaponChargesManager);
		eventBus.unregister(chargedItemManager);
		weaponChargesManager.shutDown();
		clientToolbar.removeNavigation(navButton);
		if (this.currentProfileKey != null)
		{
			writeSavedData(this.currentProfileKey);
		}
	}

	private String currentProfileKey;

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged e)
	{
		checkLoadingState(false);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		String profileKey = configManager.getRSProfileKey();
		if (profileKey != null)
		{
			writeSavedData(profileKey);
		}

		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			showChangelog();
		}
	}

	private void showChangelog()
	{
		String currentVersion = configManager.getConfiguration(GPPerHourConfig.GROUP, GPPerHourConfig.version);
		if (currentVersion == null || !currentVersion.equals(plugin_version)) {
			configManager.setConfiguration(GPPerHourConfig.GROUP, GPPerHourConfig.version, plugin_version);
			chat_messages.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(plugin_message)
				.build()
			);
		}
	}

	private void checkLoadingState(boolean isStartingUp)
	{
		String profileKey = configManager.getRSProfileKey();

		if (profileKey != null)
		{
			//getting profile for first time
			if (this.currentProfileKey == null || isStartingUp)
			{
				loadData();
			}
			//profile switched
			else if (!profileKey.equals(this.currentProfileKey))
			{
				writeSavedData(this.currentProfileKey);
				loadData();
			}
		}
		//lost profile somehow
		else if (this.currentProfileKey != null)
		{
			writeSavedData(this.currentProfileKey);
		}

		this.currentProfileKey = profileKey;
	}

	private void loadData()
	{
		lootingBagManager.loadConfigData();
		chargedItemManager.loadConfigData();
		sessionManager.reloadSessions();
		sessionManager.deleteAllTrips();
		sessionManager.stopTracking();
		runData = getSavedData();
		sessionManager.startTracking();
		previousTotalGp = null;
	}

	void updatePanels()
	{
		if (gpPerHourPanel.isActive() && gpPerHourPanel.isShowingActiveSession())
		{
			SessionStats sessionStats = sessionManager.getActiveSessionStats();
			//ensure we load these after a restart
			if (sessionStats != null)
			{
				ensureSessionNameAndPriceLoaded(sessionStats);
			}
			SwingUtilities.invokeLater(() -> activeSessionPanel.updateTrips());
		}
		if (gpPerHourPanel.isActive() && gpPerHourPanel.isShowingSessionHistory() && sessionManager.sessionHistoryDirty)
		{
			//ensure we load these after a restart
			for (SessionStats sessionStats : sessionManager.sessionHistory)
			{
				ensureSessionNameAndPriceLoaded(sessionStats);
			}
			SwingUtilities.invokeLater(() -> sessionHistoryPanel.updateSessions());
			sessionManager.sessionHistoryDirty = false;
		}
	}

	void ensureSessionNameAndPriceLoaded(SessionStats sessionStats)
	{
		for(Integer intialItemId : sessionStats.getInitialQtys().keySet())
		{
			ensureNameAndPriceLoaded(intialItemId);
		}
		for(Integer itemId : sessionStats.getQtys().keySet())
		{
			ensureNameAndPriceLoaded(itemId);
		}
	}

	void ensureNameAndPriceLoaded(Integer itemId)
	{
		if (!GPPerHourPlugin.itemNames.containsKey(itemId))
		{
			ItemComposition composition = itemManager.getItemComposition(itemId);
			itemNames.put(itemId, composition.getName());
		}
		if (!GPPerHourPlugin.itemPrices.containsKey(itemId))
		{
			itemPrices.put(itemId, getPrice(itemId));
		}
	}

    private void buildSidePanel()
    {
        activeSessionPanel = new ActiveSessionPanel(this, config, itemManager, clientThread, sessionManager);
        activeSessionPanel.sidePanelInitializer();

		sessionHistoryPanel = new SessionHistoryPanel(this, config, itemManager, clientThread, sessionManager);

		gpPerHourPanel = new GPPerHourPanel(activeSessionPanel, sessionHistoryPanel);

        icon = ImageUtil.loadImageResource(getClass(), "/gpperhour-icon.png");
        navButton = buildNavButton();
		if (config.enableSessionPanel())
			clientToolbar.addNavigation(navButton);
    }

	@Subscribe(priority = -1)//run after xpdrop plugin to overwrite their colors
	public void onScriptPreFired(ScriptPreFired scriptPreFired)
	{
		goldDropManager.onScriptPreFired(scriptPreFired);
	}
	
    @Subscribe
    public void onGameTick(GameTick gameTick)
    {
		if (runData == null)
			return;
		updatePluginState(false);
		updatePanels();
		updateChargeableItemsNeedingCheck();
		
		if (this.state == RunState.RUN && !runData.isPaused)
		{
			runData.runtime +=  Constants.GAME_TICK_LENGTH;
		}

		checkTickProfit();
    }

	// If profit total changed generate gold drop (nice animation for showing gold earn or loss)
	void checkTickProfit()
	{
		boolean isRun = this.state == RunState.RUN;

		if (!isRun)
			return;
		if (runData.isBankDelay)
			return;

		if (previousTotalGp == null)
		{
			previousTotalGp = Long.valueOf(totalGp);
			return;
		}
        long tickProfit = (totalGp - previousTotalGp);
		previousTotalGp = Long.valueOf(totalGp);
		//avoid rounding issues with charges
		if (Math.abs(tickProfit) <= 1l)
			return;

		//unpause run automatically
		if (runData.isPaused && config.autoResumeTrip())
		{
			runData.isPaused = false;
		}
		if (config.goldDropsDisplayMode() != GoldDropDisplayMode.DISABLED && 
			Math.abs(tickProfit) >= config.goldDropThreshold() &&
			((tickProfit > 0) || !config.onlyShowPositiveGoldDrops()))
		{
			goldDropManager.requestGoldDrop(tickProfit);
		}
	}

	@Provides
	GPPerHourConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GPPerHourConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(GPPerHourConfig.GROUP))
		{
			if (event.getKey().equals(GPPerHourConfig.showTripOverlayKeyName))
			{
				if (config.showTripOverlay())
					overlayManager.add(tripOverlay);
				else
					overlayManager.remove(tripOverlay);
			}
			else if (event.getKey().equals(GPPerHourConfig.goldDropDisplayModeKey))
			{
				if (config.goldDropsDisplayMode() == GoldDropDisplayMode.STATIC)
					overlayManager.add(goldDropManager);
				else
					overlayManager.remove(goldDropManager);
			}
			else if (event.getKey().equals(GPPerHourConfig.enableSessionPanelKeyName))
			{
				if (config.enableSessionPanel())
					clientToolbar.addNavigation(navButton);
				else
					clientToolbar.removeNavigation(navButton);

				sessionManager.refreshSessionTracking();
			}
			else if (event.getKey().equals(GPPerHourConfig.enableSessionTrackingKeyName))
			{
				sessionManager.refreshSessionTracking();
			}
			else if (event.getKey().equals(GPPerHourConfig.sidePanelPositionKeyName))
			{
				clientToolbar.removeNavigation(navButton);
				//need to rebuild it for some reason (i think its a bug in core)
				navButton = buildNavButton();
				if (config.enableSessionPanel())
				{
					clientToolbar.addNavigation(navButton);
				}
			}
			else if (event.getKey().equals(GPPerHourConfig.ignoredItemsKey))
			{
				refreshIgnoredItems();
			}
			else if (event.getKey().equals(GPPerHourConfig.valueModeKey))
			{
				refreshPrices();
			}
			else if (event.getKey().equals(GPPerHourConfig.sessionLootGridMaxHeightKeyName))
			{
				if (activeSessionPanel != null)
				{
					activeSessionPanel.resetSessionLootGrid();
				}
			}
			else if (event.getKey().startsWith("tokkul"))
			{
				refreshPrice(ItemID.TOKKUL);
			}
			else if (event.getKey().startsWith("crystalShard"))
			{
				refreshPrice(ItemID.CRYSTAL_SHARD);
			}
			else if (event.getKey().startsWith("crystalDust"))
			{
				refreshPrice(ItemID.CRYSTAL_DUST_23964);
			}
			else if (event.getKey().startsWith("mermaidsTear"))
			{
				refreshPrice(ItemID.MERMAIDS_TEAR);
			}
			else if (event.getKey().startsWith("stardust"))
			{
				refreshPrice(ItemID.STARDUST);
			}
			else if (event.getKey().startsWith("unidentifiedMinerals"))
			{
				refreshPrice(ItemID.UNIDENTIFIED_MINERALS);
			}
			else if (event.getKey().startsWith("goldenNugget"))
			{
				refreshPrice(ItemID.GOLDEN_NUGGET);
			}
			else if (event.getKey().startsWith("hallowedMark"))
			{
				refreshPrice(ItemID.HALLOWED_MARK);
			}
			else if (event.getKey().startsWith("abyssalPearls"))
			{
				refreshPrice(ItemID.ABYSSAL_PEARLS);
			}
			else if (event.getKey().startsWith("brimstoneKey"))
			{
				refreshPrice(ItemID.BRIMSTONE_KEY);
			}
		}
	}

	private void refreshIgnoredItems()
	{
		ignoredItems.clear();

		String[] items = config.ignoredItems().split(",");
		for (int i=0;i<items.length;++i)
		{
			ignoredItems.add(items[i].trim().toLowerCase());
		}
	}

	private void refreshPrice(int itemID)
	{
		clientThread.invoke(()->{

			if (itemPrices.remove(itemID) != null)
			{
				getPrice(itemID);
			}
		});
	}

	private NavigationButton buildNavButton()
	{
		return NavigationButton.builder().tooltip("GP Per Hour").icon(icon).priority(config.sidePanelPosition()).panel(gpPerHourPanel).build();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.KINGDOM)
		{
			ItemContainer rewardsItemContainer = client.getItemContainer(InventoryID.KINGDOM_OF_MISCELLANIA);
			if (rewardsItemContainer != null)
			{
				refreshQtyMap(rewardsQtyMap, rewardsItemContainer);
				for (int itemId: rewardsQtyMap.keySet())
				{
					runData.bankedItemQtys.merge(itemId, rewardsQtyMap.get(itemId), Float::sum);
				}
			}
		}

		updatePluginState(false);
	}

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if ((event.getId() == ObjectID.BANK_DEPOSIT_BOX && event.getMenuAction() == MenuAction.WIDGET_TARGET_ON_GAME_OBJECT)
			|| (event.getId() == ObjectID.BANK_DEPOSIT_CHEST && event.getMenuAction() == MenuAction.WIDGET_TARGET_ON_GAME_OBJECT)
			|| event.getId() == ObjectID.DEPOSIT_POOL
			|| (event.getId() == ObjectID.DEPOSIT_POT && event.getMenuAction() == MenuAction.WIDGET_TARGET_ON_GAME_OBJECT))
		{
			//user clicked on one of these but they might get to it at some later tick.
			expectingPutAnimation = true;
			depositInteractionTick = client.getTickCount();
		}

		if ("Bank-all".equals(event.getMenuOption()))
		{
			InventoryID inventory = null;
			switch (WidgetUtil.componentToInterface(event.getParam1()))
			{
				case InterfaceID.CHAMBERS_OF_XERIC_REWARD:
					inventory = InventoryID.CHAMBERS_OF_XERIC_CHEST;
					break;
				case InterfaceID.DRIFT_NET_FISHING_REWARD:
					inventory = InventoryID.DRIFT_NET_FISHING_REWARD;
					break;
				case InterfaceID.FORTIS_COLOSSEUM_REWARD:
					inventory = InventoryID.FORTIS_COLOSSEUM_REWARD_CHEST;
					break;
				case InterfaceID.LUNAR_CHEST:
					inventory = InventoryID.LUNAR_CHEST;
					break;
				case InterfaceID.TOA_REWARD:
					inventory = InventoryID.TOA_REWARD_CHEST;
					break;
				case InterfaceID.TOB_REWARD:
					inventory = InventoryID.THEATRE_OF_BLOOD_CHEST;
					break;
				case InterfaceID.TRAWLER_REWARD:
					inventory = InventoryID.FISHING_TRAWLER_REWARD;
					break;
				case InterfaceID.WILDERNESS_LOOT_CHEST:
					inventory = InventoryID.WILDERNESS_LOOT_CHEST;
					break;
			}
			if (inventory != null)
			{
				ItemContainer rewardsItemContainer = client.getItemContainer(inventory);
				if (rewardsItemContainer != null)
				{
					refreshQtyMap(rewardsQtyMap, rewardsItemContainer);
					for (int itemId: rewardsQtyMap.keySet())
					{
						runData.bankedItemQtys.merge(itemId, rewardsQtyMap.get(itemId), Float::sum);
					}
					updatePluginState(false);
				}
			}
		}
	}
	
	@Subscribe
	public void onAnimationChanged(final AnimationChanged event) {
		
		if (!expectingPutAnimation || (client.getTickCount() - depositInteractionTick) > interactionTickThreshold)
		{
			return;
		}
		if (event.getActor() == client.getLocalPlayer()) {
			
			if (event.getActor().getAnimation() == 834 //put item
				|| event.getActor().getAnimation() == 9402)//deposit runes
			{
				updatePluginState(true);
				expectingPutAnimation = false;
			}
		}
	}

	boolean isBanking()
	{
		//Collect on bank
		//Don't want it to appear there but have it count as bank still
		Widget collectOnBank = client.getWidget(402, 2);
		if (collectOnBank != null && !collectOnBank.isHidden())
			return true;
		//Grand exchange can be open while inventory widget is closed, same functionality as above
		Widget grandExchange = client.getWidget(ComponentID.GRAND_EXCHANGE_WINDOW_CONTAINER);
		if (grandExchange != null && !grandExchange.isHidden())
			return true;
		//tool leprechauns
		Widget toolLeprechaun = client.getWidget(125, 0);
		if (toolLeprechaun != null && !toolLeprechaun.isHidden())
			return true;


		if (inventoryWidget == null || inventoryWidget.getCanvasLocation().getX() < 0 || inventoryWidget.isHidden())
		{
			Widget [] altInventoryWidgets = new Widget[]
			{
				//Group Ironman Bank
				client.getWidget(725, 0),
				//Bank
				client.getWidget(ComponentID.BANK_INVENTORY_ITEM_CONTAINER),
				//GE
				client.getWidget(ComponentID.GRAND_EXCHANGE_INVENTORY_INVENTORY_ITEM_CONTAINER),
				//Seed vault
				client.getWidget(ComponentID.SEED_VAULT_INVENTORY_ITEM_CONTAINER),
				//Bank with equipment view open
				client.getWidget(InterfaceID.BANK_INVENTORY, 4),
				//Bank with looting bag open
				client.getWidget(InterfaceID.BANK_INVENTORY, 5),
				//Bank with rune pouch open
				client.getWidget(InterfaceID.BANK_INVENTORY, 19),
				//Deposit box open
				client.getWidget(907, 0),
				//COX storage open
				client.getWidget(InterfaceID.CHAMBERS_OF_XERIC_INVENTORY, 1)
			};

			for (Widget altInventoryWidget: altInventoryWidgets)
			{
				inventoryWidget = altInventoryWidget;
				if (inventoryWidget != null && !inventoryWidget.isHidden())
				{
					return true;
				}
			}
		}
		return false;
	}
	
	void updatePluginState(boolean forceBanking)
	{
		if (runData == null)
			return;
		inventoryWidget = client.getWidget(ComponentID.INVENTORY_CONTAINER);

		inventoryItemContainer = client.getItemContainer(InventoryID.INVENTORY);
		equipmentItemContainer = client.getItemContainer(InventoryID.EQUIPMENT);

		if (config.inventoryOverlayDisplayMode() == ActiveTripOverlay.InventoryOverlayDisplayMode.INVENTORY_TOTAL)
		{
			setMode(TrackingMode.TOTAL);
		}
		else
		{
			setMode(TrackingMode.PROFIT_LOSS);
		}

		boolean isBank = runData.isFirstRun || forceBanking || isBanking();

		if (isBank)
		{
			setState(RunState.BANK);
		}
		else
		{
			setState(RunState.RUN);
		}

		boolean newRun = getPreviousState() == RunState.BANK && getState() == RunState.RUN;
		
		getRunData().itemQtys.clear();
		long inventoryTotal = getInventoryTotal(false);
		long equipmentTotal = getEquipmentTotal(false);
		long rewardsTotal = getRewardsTotal();

		long totalGp = inventoryTotal;
		if (getState() == RunState.RUN && getMode() == TrackingMode.PROFIT_LOSS)
		{
			totalGp += equipmentTotal + rewardsTotal;
		}

		setTotalGp(totalGp);

		if (newRun)
		{
			onNewRun();

			postNewRun = true;
			newRunTick = client.getTickCount();
		}
		else if (getPreviousState() != RunState.BANK && getState() == RunState.BANK)
		{
			onBank();
		}

		// check post new run, need to wait one tick because if you withdraw something and close the bank right after it shows up one tick later
		if (postNewRun && (client.getTickCount() - newRunTick) > 0)
		{
			//make sure user didn't open the bank back up in those two ticks
			if (getState() == RunState.RUN)
			{
				postNewRun();
			}
			else
			{
				getRunData().isBankDelay = false;
			}
			postNewRun = false;
		}
	}

	void onNewRun()
	{
		runData.isBankDelay = true;
		runData.runStartTime = Instant.now().toEpochMilli();

		previousTotalGp = null;
	}

	// to handle same tick bank closing
	void postNewRun()
	{
		runData.initialItemQtys.clear();
		runData.bankedItemQtys.clear();
		runData.itemQtys.clear();

		getInventoryTotal(true);
		getEquipmentTotal(true);

		if (mode == TrackingMode.PROFIT_LOSS)
		{
			initialGp = getInitialGp();
		}
		else
		{
			initialGp = 0;
		}

		runData.isBankDelay = false;
		writeSavedData(this.currentProfileKey);

		sessionManager.onTripStarted(runData);
	}

	void onBank()
	{
		runData.runEndTime = Instant.now().toEpochMilli();
		if (!runData.isFirstRun)
		{
			sessionManager.onTripCompleted(runData);
		}
		runData = createRunData();
		initialGp = 0;
	}

	long getInventoryTotal(boolean isNewRun)
	{
		if (inventoryItemContainer == null)
		{
			return 0l;
		}

		double totalGp = 0;
		refreshQtyMap(inventoryQtyMap, inventoryItemContainer);
		
		for (Integer itemId: inventoryQtyMap.keySet())
		{
			float gePrice = getPrice(itemId);
			float itemQty = inventoryQtyMap.get(itemId);
			totalGp += (itemQty * gePrice);
			updateRunData(isNewRun, itemId, itemQty, gePrice);
		}

		return (long) totalGp;
	}

	long getRewardsTotal()
	{
		double totalGp = 0;

		for (Integer itemId: runData.bankedItemQtys.keySet())
		{
			float gePrice = getPrice(itemId);
			float itemQty = runData.bankedItemQtys.get(itemId);
			totalGp += (itemQty * gePrice);
			updateRunData(false, itemId, itemQty, gePrice);
		}

		return (long) totalGp;
	}

    public void openConfiguration() {
		// We don't have access to the ConfigPlugin so let's just emulate an overlay click
		this.eventBus.post(new OverlayMenuClicked(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, null, null), this.tripOverlay));
    }

	public void refreshPrices()
	{
		List<Integer> itemIds = new LinkedList<>(itemPrices.keySet());
		itemPrices.clear();
		for(Integer itemId : itemIds)
		{
			itemPrices.put(itemId, getPrice(itemId));
		}
	}

	private void addChargedWeaponComponents(Map<Integer, Float> qtyMap)
	{
		Map<Integer, Float> chargedWeaponComponents = getChargedWeaponComponentQtyMap(qtyMap.keySet());
		for (int itemId: chargedWeaponComponents.keySet()) {
			qtyMap.merge(itemId, chargedWeaponComponents.get(itemId), Float::sum);
		}
	}

	private void addChargedItemComponents(Map<Integer, Float> qtyMap)
	{
		Map<Integer, Float> chargedItemComponents = getChargedItemQtyMap(qtyMap.keySet());
		for (int itemId: chargedItemComponents.keySet()) {
			qtyMap.merge(itemId, chargedItemComponents.get(itemId), Float::sum);
		}
	}

	long getEquipmentTotal(boolean isNewRun)
	{
		refreshQtyMap(equipmentQtyMap, equipmentItemContainer);

		double eTotal = 0;
		for (int itemId: equipmentQtyMap.keySet())
		{
			float qty = equipmentQtyMap.get(itemId);
			float gePrice = getPrice(itemId);
			eTotal += (qty * gePrice);
			updateRunData(isNewRun, itemId, qty, gePrice);
		}

		return (long) eTotal;
	}

	//avoid GC
	private final Map<Integer, Float> chargedWeaponComponentQtyMap = new HashMap<>();
	private Map<Integer, Float> getChargedWeaponComponentQtyMap(Set<Integer> itemIdsToCheck)
	{
		chargedWeaponComponentQtyMap.clear();
		for (int itemId: itemIdsToCheck) {
			if (weaponChargesManager.isChargeableWeapon(itemId) && weaponChargesManager.hasChargeData(itemId))
			{
				Map<Integer, Float> chargeComponents = weaponChargesManager.getChargeComponents(itemId);
				for (Integer chargeComponentItemId: chargeComponents.keySet())
				{
					chargedWeaponComponentQtyMap.merge(chargeComponentItemId, chargeComponents.get(chargeComponentItemId), Float::sum);
				}
			}
		}
		return chargedWeaponComponentQtyMap;
	}

	//avoid GC
	private final Map<Integer, Float> chargedItemQtyMap = new HashMap<>();
	private Map<Integer, Float> getChargedItemQtyMap(Set<Integer> itemIdsToCheck)
	{
		chargedItemQtyMap.clear();
		for (int itemId: itemIdsToCheck) {
			if (chargedItemManager.isChargeableItem(itemId) && chargedItemManager.hasChargeData(itemId))
			{
				Map<Integer, Float> itemContents = chargedItemManager.getItemQuantities(itemId);
				for (Integer itemContentId: itemContents.keySet())
				{
					chargedItemQtyMap.merge(itemContentId, itemContents.get(itemContentId), Float::sum);
				}
			}
		}
		return chargedItemQtyMap;
	}

	List<LedgerItem> getInventoryLedger()
	{
		List<LedgerItem> ledgerItems = new LinkedList<>();

		if (inventoryItemContainer == null)
		{
			return new LinkedList<>();
		}

		refreshQtyMap(inventoryQtyMap, inventoryItemContainer);

		for (Integer itemId: inventoryQtyMap.keySet())
		{
			final ItemComposition itemComposition = itemManager.getItemComposition(itemId);

			String itemName = itemComposition.getName();

			Float qty = inventoryQtyMap.get(itemId);

			Float price = itemPrices.get(itemId);
			if (price == null)
			{
				price = 1f;
			}

			ledgerItems.add(new LedgerItem(itemName, qty, price, itemId));
		}

		return ledgerItems;
	}


	void refreshQtyMap(Map<Integer,Float> qtyMap, ItemContainer container)
	{
		qtyMap.clear();
		if (container==null)
		{
			return;
		}

		final Item[] containerItems = container.getItems();
		for (int i = 0; i < containerItems.length; ++i)
		{
			int itemId = containerItems[i].getId();
			if (itemId == -1)
				continue;

			ItemComposition itemComposition = itemManager.getItemComposition(itemId);
			String itemName = itemComposition.getName();
			boolean ignore = ignoredItems.contains(itemName.toLowerCase());
			if (ignore) { continue; }
				
			qtyMap.merge(itemId, (float) containerItems[i].getQuantity(), Float::sum);

			if (itemId == RUNEPOUCH_ITEM_ID || itemId == DIVINE_RUNEPOUCH_ITEM_ID)
			{
				addRunepouchContents(qtyMap);
			}
			else if(itemId == ItemID.LOOTING_BAG || itemId == ItemID.LOOTING_BAG_22586)
			{
				lootingBagManager.addLootingBagContents(qtyMap);
			}
		}
		addChargedWeaponComponents(qtyMap);
		addChargedItemComponents(qtyMap);
		FractionalRemapper.Remap(qtyMap);
	}

	@Getter
	private final HashSet<String> chargeableItemsNeedingCheck = new HashSet<>();

	void updateChargeableItemsNeedingCheck()
	{
		chargeableItemsNeedingCheck.clear();
		if (this.state == RunState.BANK)
			return;

		checkQtyMapForCheck(inventoryQtyMap.keySet());
		checkQtyMapForCheck(equipmentQtyMap.keySet());
	}

	void checkQtyMapForCheck(Set<Integer> keySet)
	{
		for (Integer itemId : keySet)
		{
			if ((itemId == ItemID.LOOTING_BAG || itemId == ItemID.LOOTING_BAG_22586) && lootingBagManager.needsCheck())
			{
				chargeableItemsNeedingCheck.add("looting bag");
			}
			else if (weaponChargesManager.isChargeableWeapon(itemId) && !weaponChargesManager.hasChargeData(itemId))
			{
				chargeableItemsNeedingCheck.add(itemManager.getItemComposition(itemId).getName().toLowerCase());
			} 
			else if (chargedItemManager.isChargeableItem(itemId) && !chargedItemManager.hasChargeData(itemId))
			{
				chargeableItemsNeedingCheck.add(itemManager.getItemComposition(itemId).getName().toLowerCase());
			} 
		}
	}

	static List<LedgerItem> getProfitLossLedger(Map<Integer, Float> initialQtys, Map<Integer, Float> qtys)
	{
		Map<Integer, Float> qtyDifferences = new HashMap<>(initialQtys.size());

		HashSet<Integer> combinedQtyKeys = new HashSet<>(initialQtys.size());
		combinedQtyKeys.addAll(qtys.keySet());
		combinedQtyKeys.addAll(initialQtys.keySet());

		for (Integer itemId: combinedQtyKeys)
		{
			Float initialQty = initialQtys.get(itemId);
			Float qty = qtys.get(itemId);

			if (initialQty == null)
			{
				initialQty = 0f;
			}

			if (qty == null)
			{
				qty = 0f;
			}

			qtyDifferences.put(itemId, qty - initialQty);
		}

		Map<String, LedgerItem> ledgerItems  = new HashMap<>(qtyDifferences.size());

		for (Integer itemId: qtyDifferences.keySet())
		{
			String name = itemNames.get(itemId);
			if (name == null)
			{
				continue;
			}
			Float price = itemPrices.get(itemId);
			if (price == null)
			{
				price = 1f;
			}

			Float qtyDifference = qtyDifferences.get(itemId);

			if (ledgerItems.containsKey(name))
			{
				ledgerItems.get(name).addQuantityDifference(qtyDifference);
			}
			else
			{
				if (price != 0)
				{
					ledgerItems.put(name, new LedgerItem(name, qtyDifference, price, itemId));
				}
			}
		}

		//filter out quantities less than round amount here!
		List<LedgerItem> ledgerItemsFiltered = new LinkedList<>();
		for (LedgerItem item : ledgerItems.values())
		{
			if (Math.abs(item.getQty()) > (roundAmount/2f))
			{
				ledgerItemsFiltered.add(item);
			}
		}

		return ledgerItemsFiltered;
	}

	// from ClueScrollPlugin
	private void addRunepouchContents(Map<Integer, Float> qtyMap)
	{
		EnumComposition runepouchEnum = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		for (int i = 0; i < RUNEPOUCH_AMOUNT_VARBITS.length; i++)
		{
			int amount = client.getVarbitValue(RUNEPOUCH_AMOUNT_VARBITS[i]);
			if (amount <= 0)
			{
				continue;
			}

			int runeId = client.getVarbitValue(RUNEPOUCH_RUNE_VARBITS[i]);
			int itemId = runepouchEnum.getIntValue(runeId);
			ItemComposition itemComposition = itemManager.getItemComposition(itemId);
			String itemName = itemComposition.getName();
			boolean ignore = ignoredItems.contains(itemName.toLowerCase());
			if (runeId == 0 || ignore)
			{
				continue;
			}

			qtyMap.merge(itemId, (float) amount, Float::sum);
		}
	}

	void updateRunData(boolean isNewRun, int itemId, float itemQty, float gePrice)
	{
		if (itemId != COINS && !itemPrices.containsKey(itemId))
		{
			itemPrices.put(itemId, gePrice);
		}

		itemNames.put(itemId, itemManager.getItemComposition(itemId).getName());

		if (isNewRun)
		{
			if (runData.initialItemQtys.containsKey(itemId))
			{
				runData.initialItemQtys.put(itemId, runData.initialItemQtys.get(itemId) + itemQty);
			}
			else
			{
				runData.initialItemQtys.put(itemId, itemQty);
			}
		}

		if (runData.itemQtys.containsKey(itemId))
		{
			runData.itemQtys.put(itemId, runData.itemQtys.get(itemId) + itemQty);
		}
		else
		{
			runData.itemQtys.put(itemId, itemQty);
		}
	}

	float getPrice(int itemId)
	{
		if (itemId == COINS)
			return 1f;
		if (itemId == ItemID.PLATINUM_TOKEN)
			return 1000f;
		if (itemPrices.containsKey(itemId))
		{
			return itemPrices.get(itemId);
		}
		else
		{
			Float remappedValue = ValueRemapper.remapPrice(itemId, this, config);
			if (remappedValue != null)
			{
				return remappedValue;
			}
			else
			{
				if (config.valueMode() == ValueMode.RUNELITE_VALUE)
				{
					return itemManager.getItemPrice(itemId);
				}
				else 
				{
					ItemComposition itemDef = itemManager.getItemComposition(itemId);
					// Only check prices for things with store prices
					int itemPrice = itemDef.getPrice();
					if (itemPrice <= 0)
					{
						return 0;
					}
					//Low alch is always 0.4 of store price, high alch is always 0.6 of store price
					return (config.valueMode() == ValueMode.LOW_ALCHEMY_VALUE) ? (itemPrice * .4f) : (itemPrice * .6f);
				}
			}
		}
	}

	void writeSavedData(String profileKey)
	{
		if (runData == null || runData.isFirstRun)
			return;
		executor.execute(() ->
		{
			String json = gson.toJson(runData);
			configManager.setConfiguration(GPPerHourConfig.GROUP, profileKey, "inventory_total_data", json);
		});
	}

	private TripData getSavedData()
	{
		String json = readData( "inventory_total_data");
		TripData savedData = null;
		try 
		{
			savedData = gson.fromJson(json, TripData.class);
		}
		catch(Exception e)
		{
			log.error("Failed to load run data from json: " + json, e);
		}

		if (savedData == null)
		{
			TripData runData = createRunData();
			runData.isFirstRun = true;
			return runData;
		}
		return savedData;
	}

	private TripData createRunData()
	{
		TripData data = new TripData();
		data.identifier = UUID.randomUUID().toString();
		return data;
	}

	private LinkedList<String> getIgnoredItems() {
		return new LinkedList<>(
			Arrays.asList(
				config.ignoredItems().split("\\s*,\\s*")
			)
		);
	}

	long elapsedRunTime()
	{
		if (runData.runStartTime == 0)
		{
			return NO_PROFIT_LOSS_TIME;
		}

		return runData.getRuntime();
	}

	void setMode(TrackingMode mode)
	{
		this.mode = mode;

		switch(mode)
		{
			case TOTAL:
				initialGp = 0;
				break;
			case PROFIT_LOSS:
				initialGp = getInitialGp();
				break;
		}
	}

	long getInitialGp()
	{
		if (runData == null)
			return 0;
		double value = 0;
		for (java.util.Map.Entry<Integer, Float> entry : runData.initialItemQtys.entrySet())
		{
			value += getPrice(entry.getKey()) * entry.getValue();
		}
		return (long) value;
	}

	void setState(RunState state)
	{
		this.previousState = this.state;
		this.state = state;
	}

	public long getProfitGp()
	{
		return totalGp - initialGp;
	}

	void saveData(String key, String data)
	{
		configManager.setRSProfileConfiguration(GPPerHourConfig.GROUP, key, data);
	}

	String readData(String key)
	{
		return configManager.getRSProfileConfiguration(GPPerHourConfig.GROUP, key);
	}

	<T> void saveData(String key, T data)
	{
		configManager.setRSProfileConfiguration(GPPerHourConfig.GROUP, key, data);
	}

	void deleteData(String key)
	{
		configManager.unsetRSProfileConfiguration(GPPerHourConfig.GROUP, key);
	}
}
