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

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ChargesDialogHandler
{
	@FunctionalInterface
	public interface MatchHandler
	{
		void handleDialog(DialogStateMatcher.DialogStateMatchers matchers, DialogTracker.DialogState dialogState, String optionSelected, WeaponChargesManager manager);
	}

	private final DialogStateMatcher dialogStateMatcher;
	private final MatchHandler matchHandler;

	public boolean handleDialog(DialogTracker.DialogState dialogState, WeaponChargesManager manager)
	{
		DialogStateMatcher.DialogStateMatchers matchers = dialogStateMatcher.matchDialog(dialogState);
		boolean matched = matchers != null;
		if (matched)
		{
			matchHandler.handleDialog(matchers, dialogState, null, manager);
		}
		return matched;
	}

	public boolean handleDialogOptionSelected(DialogTracker.DialogState dialogState, String optionSelected, WeaponChargesManager manager)
	{
		DialogStateMatcher.DialogStateMatchers matchers = dialogStateMatcher.matchDialogOptionSelected(dialogState, optionSelected);
		boolean matched = matchers != null;
		if (matched)
		{
			matchHandler.handleDialog(matchers, dialogState, optionSelected, manager);
		}
		return matched;
	}

	public static MatchHandler genericSpriteDialogChargesMessage(boolean chargesAbsolute, int group) {
		return (matchers, dialogState, optionSelected, manager) -> {
			if (dialogState.spriteDialogItemId == null) throw new IllegalArgumentException("This handler is for sprite dialogs only.");

			String chargeCountString = matchers.getTextMatcher().group(group).replaceAll(",", "");
			int charges = Integer.parseInt(chargeCountString);
			ChargedWeapon chargedWeapon = ChargedWeapon.getChargedWeaponFromId(matchers.getSpriteDialogId());
			if (chargedWeapon != null)
			{
				if (chargesAbsolute)
				{
					manager.setCharges(chargedWeapon, charges);
				} else {
					manager.addCharges(chargedWeapon, charges, true);
				}
			}
		};
	}

	public static MatchHandler genericSpriteDialogUnchargeMessage()
	{
		return (matchers, dialogState, optionSelected, manager) -> {
			if (dialogState.spriteDialogItemId == null) throw new IllegalArgumentException("This handler is for sprite dialogs only.");

			ChargedWeapon chargedWeapon = ChargedWeapon.getChargedWeaponFromId(matchers.getSpriteDialogId());
			if (chargedWeapon != null)
			{
				manager.setCharges(chargedWeapon, 0);
			}
		};
	}

	public static MatchHandler genericSpriteDialogFullChargeMessage()
	{
		return (matchers, dialogState, optionSelected, manager) -> {
			if (dialogState.spriteDialogItemId == null) throw new IllegalArgumentException("This handler is for sprite dialogs only.");

			ChargedWeapon chargedWeapon = ChargedWeapon.getChargedWeaponFromId(matchers.getSpriteDialogId());
			if (chargedWeapon != null)
			{
				manager.setCharges(chargedWeapon, chargedWeapon.getRechargeAmount());
			}
		};
	}

	public static MatchHandler genericInputChargeMessage()
	{
		return genericInputChargeMessage(1);
	}

	public static MatchHandler genericInputChargeMessage(int multiplier)
	{
		return (matchers, dialogState, optionSelected, manager) -> {
			if (manager.lastUsedOnWeapon == null) return;

			String chargeCountString = matchers.getNameMatcher().group(1).replaceAll(",", "");
			int maxChargeCount = Integer.parseInt(chargeCountString);
			int chargesEntered;
			try
			{
				chargesEntered = Integer.parseInt(optionSelected.replaceAll("k", "000").replaceAll("m", "000000").replaceAll("b", "000000000"));
			} catch (NumberFormatException e) {
				// can happen if the input is empty for example.
				return;
			}

			if (chargesEntered > maxChargeCount) chargesEntered = maxChargeCount;

			manager.addCharges(manager.lastUsedOnWeapon, chargesEntered * multiplier, true);
		};
	}

	public static MatchHandler genericUnchargeDialog()
	{
		return (matchers, dialogState, optionSelected, manager) -> {
			manager.setCharges(manager.lastUnchargeClickedWeapon, 0, true);
		};
	}
}
