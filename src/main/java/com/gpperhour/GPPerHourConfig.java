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

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup(GPPerHourConfig.GROUP)
public interface GPPerHourConfig extends Config
{
    String version = "version";
	String GROUP = "gpperhour";
	String logOutTimeKey = "log_out_time";
	String fish_barrel = "fish_barrel";
	String kharedsts_memoirs = "kharedsts_memoirs";
	String ash_sanctifier = "ash_sanctifier";
	String bottomless_compost_bucket = "bottomless_compost_bucket";
	String bottomless_compost_bucket_type = "bottomless_compost_bucket_type";
	String log_basket = "log_basket";
	String looting_bag = "looting_bag";
	String blood_essence = "blood_essence";
	String gem_bag = "gem_bag";
	String herb_sack = "herb_sack";
	String seed_box = "seed_box";
	String sessionKey = "session_stats_";
	String sessionIdentifiersKey = "session_ids";

	static String getSessionKey(String identifier)
	{
		return sessionKey + identifier;
	}






	@ConfigSection(
		name = "Shared Settings",
		description = "The options that apply to both the trip overlay and session panel.",
		position = 50,
		closedByDefault = false
	)
	String sharedSettingSection = "sharedSettingSection";

	public static final String valueModeKey = "valueMode";
	@ConfigItem(
			position = 5,
			keyName = valueModeKey,
			name = "Price Mode",
			description = "Use Runelite value (GE/Wiki or Store price), or low/high alch value.",
			section =  sharedSettingSection
	)
	default GPPerHourPlugin.ValueMode valueMode()
	{
		return GPPerHourPlugin.ValueMode.RUNELITE_VALUE;
	}

	@ConfigItem(
			position = 10,
			keyName = "showExactGp",
			name = "Show Exact Gp",
			description = "Configures whether or not the exact gp value is visible.",
			section =  sharedSettingSection
	)
	default boolean showExactGp()
	{
		return false;
	}

	public static final String showGoldDropsKey = "goldDrops";
    @ConfigItem(
			position = 20,
            keyName = showGoldDropsKey,
			hidden = true,
            name = "Show Gold Drops",
            description = "Show each profit increase or decrease as an XP drop (only works in profit/loss mode).",
			section =  sharedSettingSection
    )
    default boolean goldDrops()
    {
        return false;
    }
	@ConfigItem(
		keyName = showGoldDropsKey,
		name = "",
		description = ""
	)
	void setGoldDrops(boolean value);

	public static final String goldDropDisplayModeKey = "goldDropsDisplayMode";
    @ConfigItem(
			position = 20,
            keyName = goldDropDisplayModeKey,
            name = "Gold Drop Display Mode",
            description = "Show each profit increase or decrease as an XP drop. Vanilla uses normal XP drops. Static appears next to inventory for compat with custom xp drops plugin.",
			section =  sharedSettingSection
    )
    default GoldDropManager.GoldDropDisplayMode goldDropsDisplayMode()
    {
        return GoldDropManager.GoldDropDisplayMode.DISABLED;
    }
	@ConfigItem(
		keyName = goldDropDisplayModeKey,
		name = "",
		description = ""
	)
	void setGoldDropsDisplayMode(GoldDropManager.GoldDropDisplayMode value);

    @ConfigItem(
			position = 21,
            keyName = "goldDropThreshold",
            name = "Gold Drop Threshold",
            description = "Minimum amount of coins that will trigger a gold drop.",
			section =  sharedSettingSection
    )
    default int goldDropThreshold()
    {
        return 0;
    }

    @ConfigItem(
			position = 22,
            keyName = "onlyShowPositiveGoldDrops",
            name = "Only Positive Gold Drops",
            description = "Only show positive gold drops for the good vibes.",
			section =  sharedSettingSection
    )
    default boolean onlyShowPositiveGoldDrops()
    {
        return false;
    }

	@Alpha
	@ConfigItem(
			position = 23,
			keyName = "goldDropsPositiveColor",
			name = "Gold Drop Positive Color",
			description = "Configures the color for a positive gold drop.",
			section =  sharedSettingSection
	)
	default Color goldDropsPositiveColor()
	{
		return new Color(255,255,255,255);
	}

	@Alpha
	@ConfigItem(
			position = 24,
			keyName = "goldDropsNegativeColor",
			name = "Gold Drop Negative Color",
			description = "Configures the color for a negative gold drop.",
			section =  sharedSettingSection
	)
	default Color goldDropsNegativeColor()
	{
		return new Color(255,255,255,255);
	}



	public static final String ignoredItemsKey = "ignoredItems";
	@ConfigItem(
			position = 30,
			keyName = ignoredItemsKey,
			name = "Ignored Items",
			description = "Ignore these items in your inventory (applies after banking).",
			section =  sharedSettingSection
	)
	default String ignoredItems() {
		return "Cannon barrels, Cannon base, Cannon furnace, Cannon stand";
	}










	@ConfigSection(
		name = "Inventory Overlay Settings",
		description = "The options that control the inventory overlay, used to display stats on your active trip.",
		position = 100,
		closedByDefault = true
	)
	String tripOverlaySection = "tripOverlaySection";

	public static final String showTripOverlayKeyName = "showTripOverlay";
	@ConfigItem(
			position = 0,
			keyName = showTripOverlayKeyName,
			name = "Show Inventory Overlay",
			description = "Enables/disables the inventory overlay.",
			section =  tripOverlaySection
	)
	default boolean showTripOverlay()
	{
		return true;
	}

	@ConfigItem(
			position = 1,
			keyName = "alwaysShowTripOverlay",
			name = "Always Show Overlay",
			description = "Show overlay even when the inventory is hidden. Open inventory to refresh the widget position after resizing Runelite.",
			section =  tripOverlaySection
	)
	default boolean alwaysShowTripOverlay()
	{
		return false;
	}

    @ConfigItem(
		position = 5,
		keyName = "inventoryOverlayDisplayMode",
		name = "Overlay Display",
		description = "What data to show on the inventory overlay.",
		section =  tripOverlaySection
	)
	default ActiveTripOverlay.InventoryOverlayDisplayMode inventoryOverlayDisplayMode()
	{
		return ActiveTripOverlay.InventoryOverlayDisplayMode.TRIP_GP_PER_HOUR;
	}

	@ConfigItem(
			position = 10,
			keyName = "showLapTime",
			name = "Show Run Time",
			description = "Configures whether the runtime of the current trip or session is visible.",
			section =  tripOverlaySection
	)
	default boolean showRunTime()
	{
		return false;
	}


	@ConfigItem(
			position = 20,
			keyName = "showCoinStack",
			name = "Show Coin Stack",
			description = "Configures whether or not the coin stack image is visible.",
			section =  tripOverlaySection
	)
	default boolean showCoinStack()
	{
		return true;
	}

	@ConfigItem(
			position = 25,
			keyName = "showWhileBanking",
			name = "Show While Banking",
			description = "Configures whether or not the total is visible while banking.",
			section =  tripOverlaySection
	)
	default boolean showWhileBanking()
	{
		return true;
	}

	@ConfigItem(
			position = 30,
			keyName = "showOnEmpty",
			name = "Show On Empty",
			description = "Configures whether or not to show the total when inventory is empty.",
			section =  tripOverlaySection
	)
	default boolean showOnEmpty()
	{
		return true;
	}

	@ConfigItem(
			position = 35,
			keyName = "showTooltip",
			name = "Show Ledger on Hover",
			description = "Show the ledger when hovering over the trip overlay.",
			section =  tripOverlaySection
	)
	default boolean showLedgerOnHover()
	{
		return true;
	}

	@ConfigItem(
			position = 40,
			keyName = "roundedCorners",
			name = "Rounded Corners",
			description = "Toggle rounded corners.",
			section =  tripOverlaySection
	)
	default boolean roundCorners()
	{
		return true;
	}

	@ConfigItem(
			position = 41,
			keyName = "cornerRadius",
			name = "Corner Radius",
			description = "Configures the corner radius.",
			section =  tripOverlaySection
	)
	default int cornerRadius()
	{
		return 10;
	}

	@ConfigItem(
			position = 45,
			keyName = "alignment",
			name = "Alignment",
			description = "Configures alignment.",
			section =  tripOverlaySection
	)
	default TripOverlayAlignment horizontalAlignment()
	{
		return TripOverlayAlignment.CENTER;
	}

	@ConfigItem(
			position = 50,
			keyName = "inventoryOffsetX",
			name = "Inventory Offset X",
			description = "Configures x-axis offset.",
			section =  tripOverlaySection
	)
	default int inventoryXOffset()
	{
		return 0;
	}

	@ConfigItem(
			position = 51,
			keyName = "inventoryOffsetXNegative",
			name = "Inventory Offset X Negative",
			description = "Configures whether or not the y-axis offset is a negative number.",
			section =  tripOverlaySection
	)
	default boolean isInventoryXOffsetNegative()
	{
		return false;
	}

	@ConfigItem(
			position = 52,
			keyName = "inventoryOffsetY",
			name = "Inventory Offset Y",
			description = "Configures y-axis offset.",
			section =  tripOverlaySection
	)
	default int inventoryYOffset()
	{
		return 42;
	}

	@ConfigItem(
			position = 53,
			keyName = "inventoryOffsetYNegative",
			name = "Inventory Offset Y Negative",
			description = "Configures whether or not the y-axis offset is a negative number.",
			section =  tripOverlaySection
	)
	default boolean isInventoryYOffsetNegative()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
			position = 60,
			keyName = "totalBackgroundColor",
			name = "Background Color",
			description = "Configures the background color.",
			section =  tripOverlaySection
	)
	default Color totalColor()
	{
		return Color.decode("#99903D");
	}

	@ConfigItem(
			position = 61,
			keyName = "totalTextColor",
			name = "Text Color",
			description = "Configures the text color.",
			section =  tripOverlaySection
	)
	default Color textColor()
	{
		return Color.decode("#FFF7E3");
	}

	@Alpha
	@ConfigItem(
			position = 62,
			keyName = "totalBorderColor",
			name = "Border Color",
			description = "Configures the border color.",
			section =  tripOverlaySection
	)
	default Color borderColor()
	{
		return Color.decode("#0E0E0E");
	}

	@Alpha
	@ConfigItem(
			position = 63,
			keyName = "profitBackgroundColor",
			name = "Profit Color",
			description = "Configures profit background color.",
			section =  tripOverlaySection
	)
	default Color profitColor()
	{
		return Color.decode("#245C2D");
	}

	@ConfigItem(
			position = 64,
			keyName = "profitTextColor",
			name = "Profit Text Color",
			description = "Configures profit text color.",
			section =  tripOverlaySection
	)
	default Color profitTextColor()
	{
		return Color.decode("#FFF7E3");
	}

	@Alpha
	@ConfigItem(
			position = 65,
			keyName = "profitBorderColor",
			name = "Profit Border Color",
			description = "Configures profit border color.",
			section =  tripOverlaySection
	)
	default Color profitBorderColor()
	{
		return Color.decode("#0E0E0E");
	}

	@Alpha
	@ConfigItem(
			position = 66,
			keyName = "lossBackgroundColor",
			name = "Loss Color",
			description = "Configures loss background color.",
			section =  tripOverlaySection
	)
	default Color lossColor()
	{
		return Color.decode("#5F1515");
	}

	@ConfigItem(
			position = 67,
			keyName = "lossTextColor",
			name = "Loss Text Color",
			description = "Configures loss text color.",
			section =  tripOverlaySection
	)
	default Color lossTextColor()
	{
		return Color.decode("#FFF7E3");
	}

	@Alpha
	@ConfigItem(
			position = 68,
			keyName = "lossBorderColor",
			name = "Loss Border Color",
			description = "Configures loss border color.",
			section =  tripOverlaySection
	)
	default Color lossBorderColor()
	{
		return Color.decode("#0E0E0E");
	}







	@ConfigSection(
		name = "Session Panel Settings",
		description = "The options that control the side panel, showing active session stats and session history.",
		position = 150,
		closedByDefault = true
	)
	String sessionPanelSection = "sessionPanelSection";

	public static final String enableSessionPanelKeyName = "enableSessionPanel";
	@ConfigItem(
			position = 0,
			keyName = enableSessionPanelKeyName,
			name = "Enable Session Panel",
			description = "Enables/disables the session side panel.",
			section =  sessionPanelSection
	)
	default boolean enableSessionPanel()
	{
		return true;
	}

	public static final String enableSessionTrackingKeyName = "enableSessionTracking";
	@ConfigItem(
			position = 1,
			hidden = true,
			keyName = enableSessionTrackingKeyName,
			name = "Enable Session Tracking",
			description = "Enables/disables session tracking.",
			section =  sessionPanelSection
	)
	default boolean getEnableSessionTracking()
	{
		return true;
	}
	@ConfigItem(
		keyName = enableSessionTrackingKeyName,
		name = "",
		description = ""
	)
	void setEnableSessionTracking(boolean value);

	@ConfigItem(
			position = 5,
			keyName = "autoResumeTrip",
			name = "Auto Resume Trip",
			description = "Automatically resume a paused trip when a profit change is detected.",
			section =  sessionPanelSection
	)
	default boolean autoResumeTrip() {
		return true;
	}

	@ConfigItem(
			position = 10,
			keyName = "ignoreBankTime",
			name = "Ignore Bank time",
			description = "Don't count time with a bank UI open towards session time.",
			section =  sessionPanelSection
	)
	default boolean ignoreBankTime() {
		return false;
	}

	public static final String sidePanelPositionKeyName = "sidePanelPosition";
    @ConfigItem(
            position = 20,
            keyName = sidePanelPositionKeyName,
            name = "Side Panel Position",
            description = "Panel icon position, Lower # = higher pos, Higher # = lower pos ",
			section =  sessionPanelSection
    )
    default int sidePanelPosition() { return 6; }








	@ConfigSection(
		name = "Untradeable Values",
		description = "Customize value for items that are not tradeable.",
		position = 200,
		closedByDefault = true
	)
	String untradeableValuesSection = "untradeableValuesSection";

	@ConfigItem(
			position = 5,
			keyName = "tokkulKaramjaGloves",
			name = "Tokkul: Include Karamja Gloves",
			description = "Include karamja glove discount for tokkul price.",
			section =  untradeableValuesSection
	)
	default boolean tokkulKaramjaGloves()
	{
		return false;
	}

	@ConfigItem(
			position = 6,
			keyName = "tokkulValue",
			name = "Tokkul",
			description = "Uses overstock price for buy value, normal stock for sell value.",
			section =  untradeableValuesSection
	)
	default ValueRemapper.TokkulOverride tokkulValue()
	{
		return ValueRemapper.TokkulOverride.NO_VALUE;
	}

	@ConfigItem(
			position = 10,
			keyName = "crystalShardValue",
			name = "C Shard",
			description = "Crystal Shard: Uses enhanced crystal teleport seed for buy value, divine potion profit for sell value.",
			section =  untradeableValuesSection
	)
	default ValueRemapper.CrystalShardOverride crystalShardValue()
	{
		return ValueRemapper.CrystalShardOverride.NO_VALUE;
	}

	@ConfigItem(
			position = 15,
			keyName = "crystalDustValue",
			name = "C Dust",
			description = "Crystal Dust: Uses divine potion profit for value.",
			section =  untradeableValuesSection
	)
	default ValueRemapper.CrystalDustOverride crystalDustValue()
	{
		return ValueRemapper.CrystalDustOverride.NO_VALUE;
	}

	@ConfigItem(
			position = 20,
			keyName = "mermaidsTearValue",
			name = "Mermaid's T",
			description = "Option to use merfolk trident to derive the value.",
			section =  untradeableValuesSection
	)
	default ValueRemapper.MermaidsTearOverride mermaidsTearValue()
	{
		return ValueRemapper.MermaidsTearOverride.NO_VALUE;
	}

	@ConfigItem(
			position = 25,
			keyName = "stardustValue",
			name = "Stardust",
			description = "Option to use value of buyable items to derive the value.",
			section =  untradeableValuesSection
	)
	default ValueRemapper.StardustOverride stardustValue()
	{
		return ValueRemapper.StardustOverride.NO_VALUE;
	}

	@ConfigItem(
			position = 30,
			keyName = "unidentifiedMineralsValue",
			name = "U Minerals",
			description = "Unidentified Minerals: Option to use value of buyable items to derive the value.",
			section =  untradeableValuesSection
	)
	default ValueRemapper.UnidentifiedMineralsOverride unidentifiedMineralsValue()
	{
		return ValueRemapper.UnidentifiedMineralsOverride.NO_VALUE;
	}

	@ConfigItem(
			position = 35,
			keyName = "goldenNuggetValue",
			name = "G Nuggets",
			description = "Golden Nuggets: Option to use value of buyable items to derive the value.",
			section =  untradeableValuesSection
	)
	default ValueRemapper.GoldenNuggetOverride goldenNuggetValue()
	{
		return ValueRemapper.GoldenNuggetOverride.NO_VALUE;
	}

	@ConfigItem(
			position = 40,
			keyName = "hallowedMarkValue",
			name = "Hallowed M",
			description = "Hallowed Mark: Option to use value of buyable items to derive the value.",
			section =  untradeableValuesSection
	)
	default ValueRemapper.HallowedMarkOverride hallowedMarkValue()
	{
		return ValueRemapper.HallowedMarkOverride.NO_VALUE;
	}

	@ConfigItem(
			position = 45,
			keyName = "abyssalPearlsValue",
			name = "Abyssal P",
			description = "Abyssal Pearls: Option to use value of buyable items to derive the value.",
			section =  untradeableValuesSection
	)
	default ValueRemapper.AbyssalPearlsOverride abyssalPearlsValue()
	{
		return ValueRemapper.AbyssalPearlsOverride.NO_VALUE;
	}

	@ConfigItem(
			position = 50,
			keyName = "brimstoneKeyValue",
			name = "Brimstone Key",
			description = "Brimstone Key: Option to set value of zero or average value.",
			section =  untradeableValuesSection
	)
	default ValueRemapper.BrimstoneKeyOverride brimstoneKeyValue()
	{
		return ValueRemapper.BrimstoneKeyOverride.AVERAGE_VALUE;
	}
}
