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
package com.gpperhour;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import net.runelite.api.ItemID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

//Tab in the side panel for showing all your sessions
public class SessionHistoryPanel extends JPanel
{
	private static final String HTML_LABEL_TEMPLATE = "<html><body style='color:%s'>%s<span style='color:white'>%s</span></body></html>";
	private static final String durationLabelPrefix = "Duration: ";
	private static final String gpPerHourLabelPrefix = "GP/hr: ";
	private static final String netTotalLabelPrefix = "Net Total: ";
	private static final String totalGainsLabelPrefix = "Gains: ";
	private static final String totalLossesLabelPrefix = "Losses: ";
	private static final String tripCountLabelPrefix = "Trip Count: ";
	private static final String avgTripDurationLabelPrefix = "Avg Trip Time: ";
	private static final Color borderColor = new Color(57, 57, 57);

	private final GPPerHourConfig config;
	private final GPPerHourPlugin plugin;
	private final ItemManager itemManager;
	private final ClientThread clientThread;
	private final SessionManager sessionManager;
	GridBagConstraints constraints = new GridBagConstraints();

	private final List<SessionHistoryPanelData> historyPanels = new LinkedList<>();
	private final IconTextField searchBar = new IconTextField();
	private final JPanel historyPanelContainer = new JPanel();
	private final JScrollPane resultsWrapper;

	SessionHistoryPanel(GPPerHourPlugin plugin, GPPerHourConfig config, ItemManager itemManager,
			ClientThread clientThread, SessionManager sessionManager)
	{
		this.plugin = plugin;
		this.config = config;
		this.itemManager = itemManager;
		this.clientThread = clientThread;
		this.sessionManager = sessionManager;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		/* The main container, this holds the search bar and the center panel */
		JPanel container = new JPanel();
		container.setLayout(new BorderLayout(5, 15));
		container.setBorder(new EmptyBorder(15, 5, 5, 5));
		container.setBackground(ColorScheme.DARK_GRAY_COLOR);

		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(100, 30));
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchBar.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent e)
			{
			}

			@Override
			public void keyPressed(KeyEvent e)
			{
			}

			@Override
			public void keyReleased(KeyEvent e)
			{
				redrawPanels(true);
			}
		});
		searchBar.addClearListener(() -> redrawPanels(true));

		historyPanelContainer.setLayout(new GridBagLayout());
		historyPanelContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.insets = new Insets(0, 0, 10, 0); // Add vertical gap

		/* This panel wraps the results panel and guarantees the scrolling behaviour */
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(historyPanelContainer, BorderLayout.NORTH);

		/* The results wrapper, this scrolling panel wraps the results container */
		resultsWrapper = new JScrollPane(wrapper);
		resultsWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		resultsWrapper.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
		resultsWrapper.getVerticalScrollBar().setBorder(new EmptyBorder(0, 5, 0, 0));
		resultsWrapper.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		resultsWrapper.setVisible(true);

		container.add(searchBar, BorderLayout.NORTH);
		container.add(resultsWrapper, BorderLayout.CENTER);

		add(container, BorderLayout.CENTER);
	}

	void redrawPanels(boolean resetScroll)
	{
		updateSessions();
		if (resetScroll)
		{
			resultsWrapper.getVerticalScrollBar().setValue(0);
		}
	}

	void updateSessions()
	{
		List<SessionStats> sessions = sessionManager.sessionHistory;
		if (!searchBar.getText().isEmpty())
		{
			sessions = filterSessions(sessions, searchBar.getText());
		}
		sessions = sessions.stream().sorted(Comparator.comparingLong(o -> -o.getSessionSaveTime()))
				.collect(Collectors.toList());
		int sessionIndex;
		for (sessionIndex = 0; sessionIndex < sessions.size(); ++sessionIndex)
		{
			ensurePanelCount(sessionIndex + 1);
			renderHistoryPanel(sessions.get(sessionIndex), historyPanels.get(sessionIndex));
		}
		for (int i = sessionIndex; i < historyPanels.size(); ++i)
		{
			historyPanels.get(i).masterPanel.setVisible(false);
		}

		repaint();
		revalidate();
	}

	public List<SessionStats> filterSessions(List<SessionStats> sessionStats, String textToFilter)
	{
		final String textToFilterLower = textToFilter.toLowerCase();
		return sessionStats.stream().filter(i -> i.getSessionName().toLowerCase().contains(textToFilterLower))
				.collect(Collectors.toList());
	}

	void renderHistoryPanel(SessionStats stats, SessionHistoryPanelData panelData)
	{
		panelData.masterPanel.setVisible(true);
		panelData.nameField.setData(stats.sessionName, (String newName) ->
		{
			stats.sessionName = newName;
			sessionManager.overwriteSession(stats);
		});
		SimpleDateFormat sdfLeft = new SimpleDateFormat("MMM dd, yyyy   h:mm a", Locale.US);
		Date date = new Date(stats.getSessionSaveTime());
		String formattedDateLeft = sdfLeft.format(date);
		panelData.subtitleLeft.setText(formattedDateLeft);

		long gpPerHourLong = UI.getGpPerHour(stats.getSessionRuntime(), stats.getNetTotal());
		String gpPerHour = UI.formatGp(gpPerHourLong,
				config.showExactGp()) + "/hr";

		panelData.gpPerHourTabLabel.setText(gpPerHour);
		int gpPerHourCoinsImage = Math.abs((int) gpPerHourLong);
		gpPerHourCoinsImage /= 100;//divide by 100 to get more variation in coins image
		getCoinsImage(gpPerHourCoinsImage, (BufferedImage image) ->
		{
			panelData.coinsLabel.setIcon(new ImageIcon(image));
		}, stats);

		panelData.detailsPanel.setVisible(stats.showDetails);

		if (stats.showDetails)
		{
			panelData.gpPerHourLabel.setText(htmlLabel(gpPerHourLabelPrefix, gpPerHour));
			panelData.netTotalLabel.setText(htmlLabel(netTotalLabelPrefix, UI.formatQuantity(stats.getNetTotal(), false)));
			panelData.totalGainsLabel
					.setText(htmlLabel(totalGainsLabelPrefix, UI.formatGp(stats.getTotalGain(), config.showExactGp())));
			panelData.totalLossesLabel
					.setText(htmlLabel(totalLossesLabelPrefix, UI.formatGp(stats.getTotalLoss(), config.showExactGp())));
			panelData.durationLabel.setText(htmlLabel(durationLabelPrefix, UI.formatTime(stats.getSessionRuntime())));
			boolean showTripCountAndTime = stats.getTripCount() > 1;
			panelData.setTripCountAndDurationVisible(showTripCountAndTime);
			if(showTripCountAndTime)
			{
				panelData.tripCountLabel.setText(htmlLabel(tripCountLabelPrefix, Integer.toString(stats.getTripCount())));
				panelData.avgTripDurationLabel
						.setText(htmlLabel(avgTripDurationLabelPrefix, UI.formatTime(stats.getAvgTripDuration())));
			}
			UI.updateLootGrid(
					UI.sortLedger(
							GPPerHourPlugin.getProfitLossLedger(stats.getInitialQtys(), stats.getQtys())),
					panelData.sessionLootPanelData, itemManager, config, 0);
		}

		panelData.onDetailsPressed = () ->
		{
			stats.showDetails = !stats.showDetails;
			redrawPanels(false);
		};
		panelData.onDeletePressed = () ->
		{
			int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this session?", "Warning",
			JOptionPane.OK_CANCEL_OPTION);

			if (confirm == 0)
			{
				clientThread.invokeLater(()-> sessionManager.deleteSession(stats));
			}
		};
	}

	void ensurePanelCount(int size)
	{
		while (historyPanels.size() < size)
		{
			constraints.gridy = historyPanels.size();
			SessionHistoryPanelData data = buildHistoryPanel();
			this.historyPanelContainer.add(data.masterPanel, constraints);
			historyPanels.add(data);
		}
	}

	private class SessionHistoryPanelData
	{
		final JPanel detailsPanel;
		final JLabel gpPerHourTabLabel;
		final JPanel masterPanel = new JPanel();
		final EditableNameField nameField;
		private final JLabel coinsLabel = new JLabel();
		private final JLabel subtitleLeft = new JLabel("Left");
		private final JLabel durationLabel = new JLabel(htmlLabel(durationLabelPrefix, "N/A"));
		private final JLabel gpPerHourLabel = new JLabel(htmlLabel(gpPerHourLabelPrefix, "N/A"));
		private final JLabel netTotalLabel = new JLabel(htmlLabel(netTotalLabelPrefix, "N/A"));
		private final JLabel totalGainsLabel = new JLabel(htmlLabel(totalGainsLabelPrefix, "N/A"));
		private final JLabel totalLossesLabel = new JLabel(htmlLabel(totalLossesLabelPrefix, "N/A"));
		private final JLabel tripCountLabel = new JLabel(htmlLabel(tripCountLabelPrefix, "N/A"));
		private final JLabel avgTripDurationLabel = new JLabel(htmlLabel(avgTripDurationLabelPrefix, "N/A"));
		private final Component tripCountSpacing;
		private final Component avgTripDurationSpacing;
		private final UI.LootPanelData sessionLootPanelData = new UI.LootPanelData();
		Runnable onDetailsPressed;
		Runnable onDeletePressed;

		void setTripCountAndDurationVisible(boolean visible)
		{
			tripCountLabel.setVisible(visible);
			avgTripDurationLabel.setVisible(visible);
			tripCountSpacing.setVisible(visible);
			avgTripDurationSpacing.setVisible(visible);
		}

		SessionHistoryPanelData(SessionHistoryPanel parentPanel)
		{
			masterPanel.setLayout(new BorderLayout(0, 0));

			gpPerHourTabLabel = new JLabel();
			gpPerHourTabLabel.setText("xxx/hr");
			gpPerHourTabLabel.setFont(FontManager.getRunescapeBoldFont());

			RoundedPanel gpPerHourPanel = new RoundedPanel();
			gpPerHourPanel.setLayout(new BorderLayout(5, 0));
			gpPerHourPanel.setBorder(new EmptyBorder(3, 10, 3, 10));
			gpPerHourPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			gpPerHourPanel.setForeground(borderColor);
			gpPerHourPanel.add(coinsLabel, BorderLayout.WEST);
			gpPerHourPanel.add(gpPerHourTabLabel, BorderLayout.CENTER);

			JPanel gpPerHourWrapperPanel = new JPanel();
			gpPerHourWrapperPanel.setLayout(new BorderLayout());
			gpPerHourWrapperPanel.add(gpPerHourPanel, BorderLayout.WEST);

			nameField = new EditableNameField(parentPanel, 50, ColorScheme.DARKER_GRAY_COLOR, null);

			JLabel detailsButton = UI.createIconButton(UI.SESSIONINFO_INFO_ICON, UI.SESSIONINFO_INFO_HOVER_ICON,
					"Show Details", () ->
					{
						onDetailsPressed.run();
					});
			JLabel deleteButton = UI.createIconButton(UI.SESSIONINFO_TRASH_ICON, UI.SESSIONINFO_TRASH_HOVER_ICON,
					"Delete Session", () ->
					{
						onDeletePressed.run();
					});

			JPanel subtitlePanel = new JPanel();
			subtitlePanel.setLayout(new BorderLayout());
			subtitlePanel.setBorder(new EmptyBorder(5, 10, 5, 10));
			subtitlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			subtitlePanel.add(subtitleLeft, BorderLayout.WEST);
			subtitlePanel.add(detailsButton, BorderLayout.CENTER);
			subtitlePanel.add(deleteButton, BorderLayout.EAST);


			JPanel nameAndSubtitlePanel = new JPanel();
			nameAndSubtitlePanel.setLayout(new BorderLayout());
			nameAndSubtitlePanel.setBorder(new MatteBorder(1,1,1,1,borderColor));
			nameAndSubtitlePanel.add(nameField, BorderLayout.NORTH);
			nameAndSubtitlePanel.add(subtitlePanel, BorderLayout.CENTER);

			// Always visible header area
			JPanel headerPanel = new JPanel();
			headerPanel.setLayout(new BorderLayout());
			headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			headerPanel.add(gpPerHourWrapperPanel, BorderLayout.NORTH);
			headerPanel.add(nameAndSubtitlePanel, BorderLayout.CENTER);

			JPanel infoLabels = new JPanel();
			infoLabels.setLayout(new BoxLayout(infoLabels, BoxLayout.Y_AXIS));
			infoLabels.setBorder(new EmptyBorder(8, 10, 8, 10));
			infoLabels.setBackground(ColorScheme.DARKER_GRAY_COLOR);

			int vGap = 8;
			infoLabels.add(gpPerHourLabel);
			UI.addVerticalRigidBox(infoLabels, vGap);
			infoLabels.add(netTotalLabel);
			UI.addVerticalRigidBox(infoLabels, vGap);
			infoLabels.add(totalGainsLabel);
			UI.addVerticalRigidBox(infoLabels, vGap);
			infoLabels.add(totalLossesLabel);
			UI.addVerticalRigidBox(infoLabels, vGap);
			infoLabels.add(tripCountLabel);
			tripCountSpacing = UI.addVerticalRigidBox(infoLabels, vGap);
			infoLabels.add(avgTripDurationLabel);
			avgTripDurationSpacing = UI.addVerticalRigidBox(infoLabels, vGap);
			infoLabels.add(durationLabel);

			sessionLootPanelData.lootPanel.setLayout(new BorderLayout());
			sessionLootPanelData.lootPanel.setBorder(new MatteBorder(1,0,0,0,borderColor));

			detailsPanel = new JPanel();
			detailsPanel.setLayout(new BorderLayout());
			detailsPanel.setBorder(new MatteBorder(0,1,1,1,borderColor));
			detailsPanel.add(infoLabels, BorderLayout.NORTH);
			detailsPanel.add(sessionLootPanelData.lootPanel, BorderLayout.SOUTH);

			masterPanel.add(headerPanel, BorderLayout.NORTH);
			masterPanel.add(detailsPanel, BorderLayout.CENTER);
		}
	}

	SessionHistoryPanelData buildHistoryPanel()
	{
		return new SessionHistoryPanelData(this);
	}

	static String htmlLabel(String key, String valueStr)
	{
		return String.format(HTML_LABEL_TEMPLATE, ColorUtil.toHexColor(ColorScheme.LIGHT_GRAY_COLOR), key, valueStr);
	}

	void getCoinsImage(int quantity, Consumer<BufferedImage> consumer, SessionStats stats)
	{
		if (stats.coinsImage == null)
		{
			AsyncBufferedImage asyncImage = itemManager.getImage(ItemID.COINS_995, quantity, false);
			Runnable resizeImage = ()->
			{
				stats.coinsImage = ImageUtil.resizeImage(asyncImage, 24, 24);
				consumer.accept(stats.coinsImage);
			};
			asyncImage.onLoaded(resizeImage);
		} else
		{
			consumer.accept(stats.coinsImage);
		}
	}
}
