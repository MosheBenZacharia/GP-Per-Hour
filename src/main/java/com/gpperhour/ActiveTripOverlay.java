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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.SpriteID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.QuantityFormatter;

@Slf4j
//Renders the overlay for your current trip above the inventory.
class ActiveTripOverlay extends Overlay
{
	private static final int TEXT_Y_OFFSET = 17;
	private static final int HORIZONTAL_PADDING = 10;
	private static final int imageSize = 15;
	static final int COINS = ItemID.COINS_995;

	private final Client client;
	private final GPPerHourPlugin plugin;
	private final GPPerHourConfig config;

	private final ItemManager itemManager;
	private final SpriteManager spriteManager;

	private long lastGpPerHour;
	private long lastGpPerHourUpdateTime;

	private BufferedImage redXImage;

	private boolean hasLoadedCoinsImages;
    private BufferedImage coinsImage100;
    private BufferedImage coinsImage250;
    private BufferedImage coinsImage1000;
    private BufferedImage coinsImage10000;

	@RequiredArgsConstructor
	class LedgerEntry
	{
		final String leftText;
		final Color leftColor;
		final String rightText;
		final Color rightColor;
		final boolean addGapBefore;
		boolean center;
		boolean addGapAfter;
	}

	@Inject
	private ActiveTripOverlay(Client client, GPPerHourPlugin plugin, GPPerHourConfig config, ItemManager itemManager, SpriteManager spriteManager)
	{
		super(plugin);
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);

		this.client = client;
		this.plugin = plugin;
		this.config = config;

		this.itemManager = itemManager;
		this.spriteManager = spriteManager;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (plugin.getRunData() == null)
			return null;

		Widget inventoryWidget = plugin.getInventoryWidget();
		boolean isInvHidden = inventoryWidget == null || inventoryWidget.isHidden();
		if (isInvHidden)
		{
			return null;
		}

		int height = 20;

		long total = plugin.getProfitGp();
		String totalText = UI.formatGp(total, config.showExactGp());

		if (config.showGpPerHourOnOverlay() 
			&& plugin.getMode() == TrackingMode.PROFIT_LOSS
			 && plugin.getState() == RunState.RUN
			 && !plugin.getRunData().isBankDelay)
		{
			total = getGpPerHour(plugin.elapsedRunTime(), (int) total);
			totalText = UI.formatGp(total, config.showExactGp()) + "/hr";
		}

		String formattedRunTime = getFormattedRunTime();
		String runTimeText = null;

		if (formattedRunTime != null)
		{
			runTimeText = " (" + formattedRunTime + ")";
		}

		if (plugin.getRunData().isBankDelay)
		{
			total = 0;

			if (plugin.getMode() == TrackingMode.PROFIT_LOSS)
			{
				totalText = "0";
				if (config.showGpPerHourOnOverlay())
					totalText += "/hr";
			}
			else
			{
				totalText = UI.formatGp(plugin.getTotalGp(), config.showExactGp());
			}
		}

		renderTotal(config, graphics, plugin, inventoryWidget,
				total, totalText, runTimeText, height);

		return null;
	}

	private boolean needsCheck()
	{
		return plugin.getChargeableItemsNeedingCheck().size() != 0;
	}


    private void loadCoinsImages()
    {
        coinsImage100 = loadCoinsImage(100);
        coinsImage250 = loadCoinsImage(250);
        coinsImage1000 = loadCoinsImage(1000);
        coinsImage10000 = loadCoinsImage(10000);
		hasLoadedCoinsImages = true;
    }

    private BufferedImage loadCoinsImage(int quantity)
    {
		BufferedImage image = itemManager.getImage(ItemID.COINS_995, quantity, false);
		image = ImageUtil.resizeImage(image, imageSize, imageSize);
		return image;
    }

	private BufferedImage getCoinsImage(int quantity)
	{
		if(!hasLoadedCoinsImages)
			loadCoinsImages();

		long absValue = Math.abs(quantity);
		if (absValue >= 10000)
		{
			return coinsImage10000;
		}
		else if (absValue >= 1000)
		{
			return coinsImage1000;
		}
		else if (absValue >= 250)
		{
			return coinsImage250;
		}
		else
		{
			return coinsImage100;
		}
	}

	private BufferedImage getRedXImage()
	{
		if (redXImage == null)
		{
			redXImage = spriteManager.getSprite(SpriteID.UNKNOWN_DISABLED_ICON, 0);
			redXImage = ImageUtil.resizeImage(redXImage, imageSize, imageSize);
		}
		return redXImage;
	}

	private void renderTotal(GPPerHourConfig config, Graphics2D graphics, GPPerHourPlugin plugin,
							 Widget inventoryWidget, long total, String totalText,
							 String runTimeText, int height) {
		
		boolean showCoinStack = config.showCoinStack();
		boolean showCheckIcon = needsCheck();
		int numCoins;
		if (total > Integer.MAX_VALUE)
		{
			numCoins = Integer.MAX_VALUE;
		}
		else if (total < Integer.MIN_VALUE)
		{
			numCoins = Integer.MIN_VALUE;
		}
		else
		{
			numCoins = (int) total;
			if (numCoins == 0)
			{
				numCoins = 1000000;
			}
		}
		numCoins = Math.abs(numCoins);

		if ((total == 0 && !config.showOnEmpty()) || (plugin.getState() == RunState.BANK && !config.showWhileBanking())) {
			return;
		}

		graphics.setFont(FontManager.getRunescapeSmallFont());
		final int totalWidth = graphics.getFontMetrics().stringWidth(totalText);

		int fixedRunTimeWidth = 0;
		int actualRunTimeWidth = 0;
		int imageWidthWithPadding = 0;

		if (runTimeText != null && runTimeText.length() >= 2) {
			fixedRunTimeWidth = 5 * (runTimeText.length() - 2) + (3 * 2) + 5;
			actualRunTimeWidth = graphics.getFontMetrics().stringWidth(runTimeText);
		}

		if (showCoinStack)
		{
			imageWidthWithPadding = imageSize + 3;
		}
		if (showCheckIcon)
		{
			imageWidthWithPadding += imageSize + 3;
		}

		int width = totalWidth + fixedRunTimeWidth + imageWidthWithPadding + HORIZONTAL_PADDING * 2;

		int x = (inventoryWidget.getCanvasLocation().getX() + inventoryWidget.getWidth() / 2) - (width / 2);
		switch (config.horizontalAlignment())
		{
			case CENTER:
				break;

			case LEFT:
				x = inventoryWidget.getCanvasLocation().getX();
				break;

			case RIGHT:
				x = inventoryWidget.getCanvasLocation().getX() + inventoryWidget.getWidth() - width;
				break;
		}

		int xOffset = config.inventoryXOffset();
		if (config.isInventoryXOffsetNegative())
		{
			xOffset *= -1;
		}
		x += xOffset;

		int yOffset = config.inventoryYOffset();
		if (config.isInventoryYOffsetNegative())
		{
			yOffset *= -1;
		}
		int y = inventoryWidget.getCanvasLocation().getY() - height - yOffset;

		Color backgroundColor;
		Color borderColor;
		Color textColor;

		if (plugin.getState() == RunState.BANK || plugin.getMode() == TrackingMode.TOTAL) {
			backgroundColor = config.totalColor();
			borderColor = config.borderColor();
			textColor = config.textColor();
		}
		else if (total >= 0) {
			backgroundColor = config.profitColor();
			borderColor = config.profitBorderColor();
			textColor = config.profitTextColor();
		}
		else {
			backgroundColor = config.lossColor();
			borderColor = config.lossBorderColor();
			textColor = config.lossTextColor();
		}

		int cornerRadius = config.cornerRadius();
		if (!config.roundCorners())
		{
			cornerRadius = 0;
		}

		int containerAlpha = backgroundColor.getAlpha();

		if (containerAlpha > 0) {
			graphics.setColor(borderColor);
			graphics.drawRoundRect(x, y, width + 1, height + 1, cornerRadius, cornerRadius);
		}

		graphics.setColor(backgroundColor);

		graphics.fillRoundRect(x + 1, y + 1, width, height, cornerRadius, cornerRadius);

		TextComponent textComponent = new TextComponent();

		textComponent.setColor(textColor);
		textComponent.setText(totalText);
		textComponent.setPosition(new Point(x + HORIZONTAL_PADDING, y + TEXT_Y_OFFSET));
		textComponent.render(graphics);

		if (runTimeText != null)
		{
			textComponent = new TextComponent();

			textComponent.setColor(textColor);
			textComponent.setText(runTimeText);
			textComponent.setPosition(new Point((x + width) - HORIZONTAL_PADDING - actualRunTimeWidth - imageWidthWithPadding, y + TEXT_Y_OFFSET));
			textComponent.render(graphics);
		}

		if (showCoinStack)
		{
			int imageOffset = 4;
			if (showCheckIcon)
				imageOffset -= imageWidthWithPadding / 2;

			BufferedImage image = getCoinsImage(numCoins / 100);//divide by 100 to get more variation in coins image
			graphics.drawImage(image, (x + width) - HORIZONTAL_PADDING - imageSize + imageOffset, y + 3, null);
		}

		if (showCheckIcon)
		{
			int imageOffset = 4;

			BufferedImage redXImage = getRedXImage();
			graphics.drawImage(redXImage, (x + width) - HORIZONTAL_PADDING - imageSize + imageOffset, y + 3, null);
		}

		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		int mouseX = mouse.getX();
		int mouseY = mouse.getY();

		RoundRectangle2D roundRectangle2D = new RoundRectangle2D.Double(x, y, width + 1, height + 1, cornerRadius, cornerRadius);
		if (roundRectangle2D.contains(mouseX, mouseY) && plugin.getState() != RunState.BANK
				&& !plugin.getRunData().isBankDelay && config.showLedgerOnHover())
		{
			if (plugin.getMode() == TrackingMode.PROFIT_LOSS)
			{
				renderProfitLossLedger(graphics);
			}
			else
			{
				renderLedger(graphics);
			}
		}
	}

	private void renderLedger(Graphics2D graphics)
	{
		FontMetrics fontMetrics = graphics.getFontMetrics();

		java.util.List<LedgerItem> ledger = plugin.getInventoryLedger().stream()
				.filter(item -> item.getQty() > (GPPerHourPlugin.roundAmount/2f)).collect(Collectors.toList());

		LinkedList<LedgerEntry> ledgerEntries = new LinkedList<>();
		ledgerEntries.add(createTitleEntry());
		if (ledger.isEmpty())
		{
			ledgerEntries.add(createEmptyEntry());
		}
		else
		{
			ledger = ledger.stream().sorted(Comparator.comparingLong(o ->
					-o.getCombinedValue())
			).collect(Collectors.toList());

			String [] descriptions = ledger.stream().map(item -> {
				String desc = item.getDescription();
				if (item.getQty() != 0 && Math.abs(item.getQty()) != 1 && !item.getDescription().contains("Coins"))
				{
					desc = UI.formatQuantity(item.getQty(), true) + " " + desc;
				}
				return desc;
			}).toArray(String[]::new);
			Long [] prices = ledger.stream().map(item -> item.getCombinedValue()).toArray(Long[]::new);

			if (descriptions.length == prices.length)
			{
				for (int i = 0; i < descriptions.length; i++)
				{
					String desc = descriptions[i];
					long price = prices[i];
					String rightText = formatNumber(price);
					Color leftColor = Color.decode("#FFF7E3");
					Color rightColor = price > 0 ? Color.GREEN : Color.WHITE;
					ledgerEntries.add(new LedgerEntry(desc, leftColor, rightText, rightColor, false));
				}
			}
			long total = ledger.stream().mapToLong(item -> item.getCombinedValue()).sum();
			ledgerEntries.add(new LedgerEntry("Total", Color.ORANGE, formatNumber(total), priceToColor(total), true));
		}
		boolean firstCharge = true;
		for (String itemName : plugin.getChargeableItemsNeedingCheck())
		{
			ledgerEntries.add(new LedgerEntry("Check " + itemName + " to calibrate.", Color.RED, "", Color.WHITE, firstCharge));
			firstCharge = false;
		}

		int maxRowW = 0;
		int sectionPadding = 5;
		int sectionPaddingTotal = 0;
		for(LedgerEntry entry : ledgerEntries)
		{
			int width = fontMetrics.stringWidth(entry.leftText) + fontMetrics.stringWidth(entry.rightText);
			if (width > maxRowW)
				maxRowW = width;
			if (entry.addGapBefore)
				sectionPaddingTotal += sectionPadding;
			if (entry.addGapAfter)
				sectionPaddingTotal += sectionPadding;
		}

		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		int mouseX = mouse.getX();
		int mouseY = mouse.getY();


		int rowW = maxRowW + 20 + HORIZONTAL_PADDING * 2;
		int rowH = fontMetrics.getHeight();

		int h = ledgerEntries.size() * rowH + TEXT_Y_OFFSET / 2 + sectionPaddingTotal + 2;

		int x = mouseX - rowW - 10;
		int y = mouseY - h / 2;

		int cornerRadius = 0;

		graphics.setColor(Color.decode("#1b1b1b"));
		graphics.fillRoundRect(x, y, rowW, h, cornerRadius, cornerRadius);

		int borderWidth = 1;

		graphics.setColor(Color.decode("#0b0b0b"));
		graphics.setStroke(new BasicStroke(borderWidth));
		graphics.drawRoundRect(x - borderWidth / 2, y - borderWidth / 2,
				rowW + borderWidth / 2, h + borderWidth / 2, cornerRadius, cornerRadius);

		renderLedgerEntries(ledgerEntries, x, y, rowW, rowH, sectionPadding, graphics);
	}

	private LedgerEntry createTitleEntry()
	{
		LedgerEntry titleEntry = new LedgerEntry("Active Trip Summary", Color.WHITE, "", Color.WHITE, false);
		titleEntry.addGapAfter = true;
		titleEntry.center = true;
		return titleEntry;
	}

	private LedgerEntry createEmptyEntry()
	{
		LedgerEntry emptyEntry = new LedgerEntry("Ledger items will appear here.", Color.WHITE, "", Color.WHITE, false);
		return emptyEntry;
	}

	private void renderProfitLossLedger(Graphics2D graphics)
	{
		FontMetrics fontMetrics = graphics.getFontMetrics();

		List<LedgerItem> ledger = GPPerHourPlugin.getProfitLossLedger(plugin.getRunData().initialItemQtys, plugin.getRunData().itemQtys);

		List<LedgerItem> gain = ledger.stream().filter(item -> item.getQty() > 0)
				.collect(Collectors.toList());

		List<LedgerItem> loss = ledger.stream().filter(item -> item.getQty() < 0)
				.collect(Collectors.toList());

		gain = gain.stream().sorted(Comparator.comparingLong(o -> -o.getCombinedValue())).collect(Collectors.toList());
		loss = loss.stream().sorted(Comparator.comparingLong(o -> -o.getCombinedValue())).collect(Collectors.toList());

		ledger = new LinkedList<>();
		ledger.addAll(gain);
		ledger.addAll(loss);

		LinkedList<LedgerEntry> ledgerEntries = new LinkedList<>();
		ledgerEntries.add(createTitleEntry());

		if (ledger.isEmpty())
		{
			ledgerEntries.add(createEmptyEntry());
		}
		else
		{
			String [] descriptions = ledger.stream().map(item -> {
				String desc = item.getDescription();
				if (item.getQty() != 0 && Math.abs(item.getQty()) != 1 && !item.getDescription().contains("Coins"))
				{
					desc = UI.formatQuantity(item.getQty(), true) + " " + desc;
				}
				return desc;
			}).toArray(String[]::new);
			Long [] prices = ledger.stream().map(item -> item.getCombinedValue()).toArray(Long[]::new);
	
			if (descriptions.length == prices.length)
			{
				String prevDesc = "";
				for (int i = 0; i < descriptions.length; i++)
				{
					Color leftColor = Color.decode("#FFF7E3");
					Color rightColor = Color.WHITE;
					boolean addGap = false;
					String desc = descriptions[i];
	
					if (i > 0 && prices[i - 1] >= 0 && prices[i] < 0 && !prevDesc.contains("Total"))
					{
						addGap = true;
					}
	
					prevDesc = desc;
	
					long price = prices[i];
					String formattedPrice = formatNumber(price);
					rightColor = priceToColor(price);
	
					ledgerEntries.add(new LedgerEntry(desc, leftColor, formattedPrice, rightColor, addGap));
				}
			}
	
			long totalGain = gain.stream().mapToLong(item ->  item.getCombinedValue()).sum();
			long totalLoss = loss.stream().mapToLong(item ->  item.getCombinedValue()).sum();
			long netTotal = ledger.stream().mapToLong(item -> item.getCombinedValue()).sum();
			ledgerEntries.add(new LedgerEntry("Total Gain", Color.YELLOW, formatNumber(totalGain), priceToColor(totalGain), true));
			ledgerEntries.add(new LedgerEntry("Total Loss", Color.YELLOW, formatNumber(totalLoss), priceToColor(totalLoss), false));
			ledgerEntries.add(new LedgerEntry("Net Total", Color.ORANGE, formatNumber(netTotal), priceToColor(netTotal), false));
	
			long runTime = plugin.elapsedRunTime();
			if (runTime != GPPerHourPlugin.NO_PROFIT_LOSS_TIME)
			{
				long gpPerHour = getGpPerHour(runTime, netTotal);
				String gpPerHourString = UI.formatGp(gpPerHour, config.showExactGp());
				ledgerEntries.add(new LedgerEntry("GP/hr", Color.ORANGE, gpPerHourString, priceToColor(gpPerHour), false));
			}
		}

		boolean firstCharge = true;
		for (String itemName : plugin.getChargeableItemsNeedingCheck())
		{
			ledgerEntries.add(new LedgerEntry("Check " + itemName + " to calibrate.", Color.RED, "", Color.WHITE, firstCharge));
			firstCharge = false;
		}

		int maxRowW = 0;
		int sectionPadding = 5;
		int sectionPaddingTotal = 0;
		for (LedgerEntry entry : ledgerEntries)
		{
			int width = fontMetrics.stringWidth(entry.leftText) + fontMetrics.stringWidth(entry.rightText);
			if (width > maxRowW)
				maxRowW = width;
			if (entry.addGapBefore)
				sectionPaddingTotal += sectionPadding;
			if (entry.addGapAfter)
				sectionPaddingTotal += sectionPadding;
		}

		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		int mouseX = mouse.getX();
		int mouseY = mouse.getY();

		int rowW = maxRowW + 20 + HORIZONTAL_PADDING * 2;
		int rowH = fontMetrics.getHeight();

		int h = ledgerEntries.size() * rowH + TEXT_Y_OFFSET / 2 + sectionPaddingTotal + 2;
		
		int x = mouseX - rowW - 10;
		int y = mouseY - h / 2;

		int cornerRadius = 0;

		graphics.setColor(Color.decode("#1b1b1b"));
		graphics.fillRoundRect(x, y, rowW, h, cornerRadius, cornerRadius);

		int borderWidth = 1;

		graphics.setColor(Color.decode("#0b0b0b"));
		graphics.setStroke(new BasicStroke(borderWidth));
		graphics.drawRoundRect(x - borderWidth / 2, y - borderWidth / 2,
				rowW + borderWidth / 2, h + borderWidth / 2, cornerRadius, cornerRadius);

		renderLedgerEntries(ledgerEntries, x, y, rowW, rowH, sectionPadding, graphics);
	}

	long getGpPerHour(long runTime, long total)
	{
		//dont want to update too often
		long timeNow = Instant.now().toEpochMilli();
		if (timeNow - lastGpPerHourUpdateTime < 1000)
		{
			return lastGpPerHour;
		}

		lastGpPerHourUpdateTime = timeNow;
		lastGpPerHour = UI.getGpPerHour(runTime, total);
		return lastGpPerHour;
	}

	String formatNumber(long number)
	{
		return QuantityFormatter.formatNumber(number);
	}

	Color priceToColor(long price)
	{
		if (price > 0)
		{
			return Color.GREEN;
		}
		else if (price < 0)
		{
			return Color.RED;
		}
		else
		{
			return Color.WHITE;
		}
	}

	private void renderLedgerEntries(LinkedList<LedgerEntry> ledgerEntries, int x, int y, int rowW, int rowH, int sectionPadding, Graphics2D graphics)
	{
		FontMetrics fontMetrics = graphics.getFontMetrics();
		int yPosition = TEXT_Y_OFFSET;
		for (LedgerEntry ledgerEntry: ledgerEntries)
		{
			if (ledgerEntry.addGapBefore)
				yPosition += sectionPadding;

			//only renders left text
			if (ledgerEntry.center)
			{
				String leftText = ledgerEntry.leftText;
				int textW = fontMetrics.stringWidth(leftText);
				int textX = x + rowW/2 - textW/2;
				int textY = y + yPosition;

				TextComponent textComponent = new TextComponent();
				textComponent.setColor(ledgerEntry.leftColor);
				textComponent.setText(leftText);
				textComponent.setPosition(new Point(textX, textY));
				textComponent.render(graphics);
			}
			else
			{
				int textX = x + HORIZONTAL_PADDING;
				int textY = y + yPosition;
	
				TextComponent textComponent = new TextComponent();
				textComponent.setColor(ledgerEntry.leftColor);
				textComponent.setText(ledgerEntry.leftText);
				textComponent.setPosition(new Point(textX, textY));
				textComponent.render(graphics);
				
				String rightText = ledgerEntry.rightText;
				int textW = fontMetrics.stringWidth(rightText);
				textX = x + rowW - HORIZONTAL_PADDING - textW;
	
				textComponent = new TextComponent();
				textComponent.setColor(ledgerEntry.rightColor);
				textComponent.setText(rightText);
				textComponent.setPosition(new Point(textX, textY));
				textComponent.render(graphics);
			}


			yPosition += rowH;

			if (ledgerEntry.addGapAfter)
				yPosition += sectionPadding;
		}
	}

	private String getFormattedRunTime()
	{
		if (!config.showRunTime())
			return null;

		long runTime = plugin.elapsedRunTime();

		if (runTime == GPPerHourPlugin.NO_PROFIT_LOSS_TIME)
		{
			return null;
		}

		return UI.formatTime(runTime);
	}
}
