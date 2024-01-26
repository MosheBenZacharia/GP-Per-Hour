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

import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

//The root side panel that can display either the active session or session history
class GPPerHourPanel extends PluginPanel
{
	private static final Color lineColor = ColorScheme.BRAND_ORANGE;

	// this panel will hold either the active session panel or the session history panel
	private final JPanel display = new JPanel();

	private final MaterialTabGroup tabGroup = new MaterialTabGroup(display);
	private final MaterialTab sessionHistoryTab;
	private final JPanel titlePanel = new JPanel();

	@Getter
	private final ActiveSessionPanel activeSessionPanel;
	@Getter
	private final SessionHistoryPanel sessionHistoryPanel;
	@Getter
	private boolean active;

	GPPerHourPanel(ActiveSessionPanel activeSessionPanel, SessionHistoryPanel sessionHistoryPanel)
	{
		super(false);

		this.activeSessionPanel = activeSessionPanel;
		this.sessionHistoryPanel = sessionHistoryPanel;

		buildTitlePanel();

		this.setLayout(new BorderLayout());
		this.setBackground(ColorScheme.DARK_GRAY_COLOR);
		this.setBorder(new EmptyBorder(10, 5, 10, 5));

		MaterialTab activeSessionTab = new MaterialTab("Active Session", tabGroup, activeSessionPanel);
		sessionHistoryTab = new MaterialTab("Session History", tabGroup, sessionHistoryPanel);

		tabGroup.setBorder(new EmptyBorder(5, 0, 0, 0));
		tabGroup.addTab(activeSessionTab);
		tabGroup.addTab(sessionHistoryTab);
		tabGroup.select(activeSessionTab); // selects the default selected tab

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BorderLayout());
		centerPanel.add(tabGroup, BorderLayout.NORTH);
		centerPanel.add(display, BorderLayout.CENTER);

		add(titlePanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
	}

	@Override
	public void onActivate()
	{
		active = true;
	}

	@Override
	public void onDeactivate()
	{
		active = false;
	}

	private JPanel buildTitlePanel()
	{
		titlePanel.setBorder(
			new CompoundBorder(new EmptyBorder(5, 0, 5, 0), new MatteBorder(0, 0, 1, 0, lineColor)));
		titlePanel.setLayout(new BorderLayout());
		PluginErrorPanel errorPanel = new PluginErrorPanel();
		errorPanel.setBorder(new EmptyBorder(2, 0, 10, 0));
		errorPanel.setContent("GP Per Hour", "Tracks your GP/hr over various trips.");
		titlePanel.add(errorPanel, "Center");
		return titlePanel;
	}

	boolean isShowingActiveSession()
	{
		return activeSessionPanel.isShowing();
	}

	boolean isShowingSessionHistory()
	{
		return sessionHistoryPanel.isShowing();
	}

	void showActiveSession()
	{
		if (activeSessionPanel.isShowing())
		{
			return;
		}

		tabGroup.select(sessionHistoryTab);
		revalidate();
	}
}