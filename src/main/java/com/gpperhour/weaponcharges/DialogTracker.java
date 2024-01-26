/*
 * Copyright (c) 2023, geheur <https://github.com/geheur>
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

import com.google.inject.Inject;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.VarClientStr;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyListener;
import net.runelite.client.util.Text;

// Things to test: clicks and keystrokes for click here to continue in NPC, PLAYER, SPRITE, and NPC DIALOG OPTIONS
// (all 5 options!).

/**
 * need to register it to the eventbus and keymanager for it to work.
 *
 * Not working: meslayer (I'm assuming, cause I don't know what meslayer actually is), sprite (partial).
 * double sprite dialog (e.g. removing fang from swamp trident).
 */
@Slf4j
public class DialogTracker implements KeyListener
{
	private static final int COMPONENT_ID_DIALOG_PLAYER_CLICK_HERE_TO_CONTINUE = 5;
	private static final int COMPONENT_ID_DIALOG_NPC_CLICK_HERE_TO_CONTINUE = 5;
	private static final int COMPONENT_ID_DIALOG_PLAYER_NAME = 4;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	private Consumer<DialogState> dialogStateChanged;
	private BiConsumer<DialogState, String> dialogOptionSelected;

	private DialogState lastDialogState = null;

	public void setStateChangedListener(Consumer<DialogState> listener) {
		dialogStateChanged = listener;
	}

	public void setOptionSelectedListener(BiConsumer<DialogState, String> listener) {
		dialogOptionSelected = listener;
	}

	/*
    It's possible to miss a click but I've only seen then when I'm moving the mouse as fast as I possibly can.
     */
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		int widgetId = event.getParam1();
		int interfaceId = WidgetUtil.componentToInterface(widgetId);
		int componentId = WidgetUtil.componentToId(widgetId);
		if (widgetId == ComponentID.DIALOG_OPTION_OPTIONS) {
			Widget widget = client.getWidget(ComponentID.DIALOG_OPTION_OPTIONS);
			int dynamicChildIndex = event.getParam0();
			Widget[] dynamicChildren = widget.getDynamicChildren();
			Widget dynamicChild = dynamicChildren[dynamicChildIndex];
			if (dynamicChild == null)
			{
				log.debug("dynamic child option was null, index " + dynamicChildIndex + " total children: " + dynamicChildren.length);
				return; // not sure why this would happen.
			}
			optionSelected(lastDialogState, dynamicChild.getText());
		} else if (interfaceId == InterfaceID.DIALOG_NPC && componentId == COMPONENT_ID_DIALOG_NPC_CLICK_HERE_TO_CONTINUE) {
			optionSelected(lastDialogState, null);
		} else if (interfaceId == InterfaceID.DIALOG_PLAYER && componentId == COMPONENT_ID_DIALOG_PLAYER_CLICK_HERE_TO_CONTINUE) {
			optionSelected(lastDialogState, null);
		} else if (interfaceId == InterfaceID.DIALOG_SPRITE && componentId == 0) {
			optionSelected(lastDialogState, null);
		}
	}

	public DialogState getDialogState() {
		DialogState.DialogType type = getDialogType();

		DialogState state;
		switch (type) {
			case NPC:
			{
				Widget nameWidget = client.getWidget(ComponentID.DIALOG_NPC_NAME);
				Widget textWidget = client.getWidget(ComponentID.DIALOG_NPC_TEXT);

				String name = (nameWidget != null) ? nameWidget.getText() : null;
				String text = (textWidget != null) ? textWidget.getText() : null;

				state = DialogState.npc(name, text);
				break;
			}
			case PLAYER:
			{
				Widget nameWidget = client.getWidget(InterfaceID.DIALOG_PLAYER, COMPONENT_ID_DIALOG_PLAYER_NAME);
				Widget textWidget = client.getWidget(ComponentID.DIALOG_PLAYER_TEXT);

				String name = (nameWidget != null) ? nameWidget.getText() : null;
				String text = (textWidget != null) ? textWidget.getText() : null;

				state = DialogState.player(name, text);
				break;
			}
			case OPTIONS:
			{
				String text = null;

				Widget optionsWidget = client.getWidget(ComponentID.DIALOG_OPTION_OPTIONS);
				List<String> options = null;
				if (optionsWidget != null) {
					options = new ArrayList<>();
					for (Widget child : optionsWidget.getDynamicChildren()) {
						if (child.getText() != null && !child.getText().isEmpty())
						{
							options.add(child.getText());
						}
					}
					text = options.remove(0); // remove "Select an Option".
				}

				state = DialogState.options(text, options);
				break;
			}
			case SPRITE:
			{
				Widget textWidget = client.getWidget(ComponentID.DIALOG_SPRITE_TEXT);
				String text = (textWidget != null) ? textWidget.getText() : null;

				Widget itemWidget = client.getWidget(ComponentID.DIALOG_SPRITE_SPRITE);
				int itemId = (itemWidget != null) ? itemWidget.getItemId() : -1;

				state = DialogState.sprite(text, itemId);
				break;
			}
			case INPUT:
			{
				Widget titleWidget = client.getWidget(ComponentID.CHATBOX_TITLE);
				String title = (titleWidget != null) ? titleWidget.getText() : null;
				String input = client.getVarcStrValue(VarClientStr.INPUT_TEXT);

				state = DialogState.input(title, input);
				break;
			}
			case NO_DIALOG:
			{
				state = DialogState.noDialog();
				break;
			}
			default:
				throw new IllegalStateException("Unexpected value: " + type);
		}

		return state;
	}

	private DialogState.DialogType getDialogType()
	{
		Widget npcDialog = client.getWidget(InterfaceID.DIALOG_NPC, 0);
		if (npcDialog != null && !npcDialog.isHidden())
		{
			return DialogState.DialogType.NPC;
		}

		Widget playerDialog = client.getWidget(InterfaceID.DIALOG_PLAYER, 0);
		if (playerDialog != null && !playerDialog.isHidden())
		{
			return DialogState.DialogType.PLAYER;
		}

		Widget optionsDialog = client.getWidget(InterfaceID.DIALOG_OPTION, 0);
		if (optionsDialog != null && !optionsDialog.isHidden())
		{
			return DialogState.DialogType.OPTIONS;
		}

		Widget spriteDialog = client.getWidget(InterfaceID.DIALOG_SPRITE, 0);
		if (spriteDialog != null && !spriteDialog.isHidden())
		{
			return DialogState.DialogType.SPRITE;
		}

		Widget inputDialog = client.getWidget(ComponentID.CHATBOX_FULL_INPUT);
		if (inputDialog != null && !inputDialog.isHidden())
		{
			return DialogState.DialogType.INPUT;
		}

		return DialogState.DialogType.NO_DIALOG;
	}

	@Subscribe
	public void onGameTick(GameTick gameTick) {
//        log.debug("Game tick: {}", client.getTickCount());
		optionSelected = false;

		DialogState dialogState = getDialogState();
		if (!Objects.equals(dialogState, lastDialogState)) {
			log.debug("dialog changed: {} previous: {} (game tick: {})", dialogState, lastDialogState, client.getTickCount());

			if (dialogStateChanged != null) dialogStateChanged.accept(dialogState);
		}
		lastDialogState = dialogState;
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == 2153)
		{
			Widget w = client.getWidget(ComponentID.DIALOG_OPTION_OPTIONS);
			if (w != null && !w.isHidden())
			{
				for (int i = 0; i < w.getDynamicChildren().length; i++)
				{
					Widget dynamicChild = w.getDynamicChildren()[i];
					if ("Please wait...".equals(Text.removeTags(dynamicChild.getText()))) {
						String option = null;
						if (lastDialogState.type == DialogState.DialogType.OPTIONS) {
							if (lastDialogState.options != null) {
								if (lastDialogState.options.size() > i - 1) { // -1 because we skip "Select an Option".
									option = lastDialogState.options.get(i - 1); // -1 because we skip "Select an Option".
								}
							}
						}
						optionSelected(lastDialogState, option);
					}
				}
			}
			w = client.getWidget(InterfaceID.DIALOG_NPC, COMPONENT_ID_DIALOG_NPC_CLICK_HERE_TO_CONTINUE);
			if (w != null && !w.isHidden() && "Please wait...".equals(Text.removeTags(w.getText())))
			{
				optionSelected(lastDialogState, null);
			}
			w = client.getWidget(InterfaceID.DIALOG_PLAYER, COMPONENT_ID_DIALOG_PLAYER_CLICK_HERE_TO_CONTINUE);
			if (w != null && !w.isHidden() && "Please wait...".equals(Text.removeTags(w.getText())))
			{
				optionSelected(lastDialogState, null);
			}
		} else if (event.getScriptId() == 2869) {
			Widget w = client.getWidget(InterfaceID.DIALOG_SPRITE, 0);
			if (w != null && !w.isHidden())
			{
				Widget dynamicChild = w.getDynamicChildren()[2];
				if ("Please wait...".equals(Text.removeTags(dynamicChild.getText())))
				{
					optionSelected(lastDialogState, null);
				}
			}
		}
	}

	/**
	 * To prevent multiple selections from occuring in the same game tick. Only the first one should count.
	 */
	private boolean optionSelected = false;

	private void optionSelected(DialogState state, String option) {
		if (optionSelected) return;
		optionSelected = true;
		if (state.type == DialogState.DialogType.OPTIONS) {
			log.debug("option selected: \"" + option + "\" " + state);
		} else {
			log.debug("clicked here to continue: " + state);
		}
		if (dialogOptionSelected != null) dialogOptionSelected.accept(state, option);
	}

	public void reset()
	{
		lastDialogState = null;
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
		// unused.
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		try
		{
			if (e.getKeyCode() == KeyEvent.VK_ENTER)
			{
				clientThread.invoke(() -> {
					String inputText = client.getVarcStrValue(VarClientStr.INPUT_TEXT);

					if (lastDialogState.type == DialogState.DialogType.INPUT)
					{
						optionSelected(lastDialogState, inputText);
					}
				});
			}
		} catch (RuntimeException ex) {
			// Exceptions thrown from here can prevent other keylisteners from receiving the key event.
			log.error("", ex);
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		// unused.
	}

	@RequiredArgsConstructor
	public static class DialogState
	{
		enum DialogType
		{
			/**
			 * NO_DIALOG does NOT indicate the end of a dialog. For example you can end a dialog with an npc by doing something like checking a kharedst memoirs, without seeing the NO_DIALOG state inbetween.
			 */
			NO_DIALOG,
			PLAYER,
			NPC,
			OPTIONS,
			SPRITE,
			INPUT,
		}

		@NonNull
		final DialogTracker.DialogState.DialogType type;

		// Meaningful only when type is PLAYER or NPC or INPUT.
		@Nullable
		final String name;

		@Nullable
		final String text;

		@Nullable
		final Integer spriteDialogItemId;

		// Meaningful only when type is OPTIONS
		@Nullable
		final List<String> options;

		public static DialogState sprite(String text, int itemId) {
			return new DialogState(DialogType.SPRITE, null, text, itemId, null);
		}

		public static DialogState player(String name, String text) {
			return new DialogState(DialogType.PLAYER, name, text, null, null);
		}

		public static DialogState npc(String name, String text) {
			return new DialogState(DialogType.NPC, name, text, null, null);
		}

		public static DialogState options(String text, List<String> options) {
			return new DialogState(DialogType.OPTIONS, null, text, null, options);
		}

		public static DialogState options(String text, String... options) {
			return new DialogState(DialogType.OPTIONS, null, text, null, Arrays.asList(options));
		}

		public static DialogState input(String title, String input)
		{
			return new DialogState(DialogType.INPUT, title, input, null, null);
		}

		public static DialogState noDialog() {
			return new DialogState(DialogType.NO_DIALOG, null, null, null, null);
		}

		@Override
		public boolean equals(Object o)
		{
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			DialogState that = (DialogState) o;
			return type == that.type && Objects.equals(text, that.text) && Objects.equals(name, that.name) && Objects.equals(options, that.options);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(type, text, name, options);
		}

		@Override
		public String toString()
		{
			switch (type) {
				case NO_DIALOG:
					return "DialogState{" + type +
							"}";
				case PLAYER:
				case NPC:
					return "DialogState{" + type +
							", name='" + name + "'" +
							", text='" + text + "'" +
							"}";
				case SPRITE:
					return "DialogState{" + type +
							", text='" + text + "'" +
							", itemId=" + spriteDialogItemId +
							"}";
				case OPTIONS:
					return "DialogState{" + type +
							", text='" + text + "'" +
							", options=" + options +
							"}";
				case INPUT:
					return "DialogState{" + type +
							", title='" + name + "'" +
							", input='" + text + "'" +
							"}";
				default:
					throw new IllegalStateException();
			}
		}
	}
}