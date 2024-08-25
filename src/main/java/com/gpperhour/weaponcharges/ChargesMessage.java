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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.config.ConfigManager;

@RequiredArgsConstructor
public class ChargesMessage
{
	@Getter
	private final Pattern pattern;
	private final matcherthing chargeLeft;

	@FunctionalInterface private interface matcherthing {
		Integer customHandler(Matcher matcher, ConfigManager configManager);
	}

	public ChargesMessage(Pattern pattern, Function<Matcher, Integer> chargeLeft2) {
		this(pattern, (matcher, configManager) -> chargeLeft2.apply(matcher));
	}

	public int getChargesLeft(Matcher matcher, ConfigManager configManager)
	{
		return chargeLeft.customHandler(matcher, configManager);
	}

	public static ChargesMessage staticChargeMessage(String s, int charges)
	{
		return new ChargesMessage(Pattern.compile(s), matcher -> charges);
	}

	public static ChargesMessage matcherGroupChargeMessage(String s, int group)
	{
		return new ChargesMessage(Pattern.compile(s), matcher -> {
			String chargeCountString = matcher.group(group);
			return parseCharges(chargeCountString);
		}
		);
	}

	private static int parseCharges(String chargeCountString)
	{
		if (chargeCountString.equals("one")) {
			return 1;
		}
		return Integer.parseInt(chargeCountString.replaceAll(",", ""));
	}

	@FunctionalInterface public interface CustomChargeMatcher {
		Integer customHandler(Matcher matcher, Integer chargeCount, ConfigManager configManager);
	}

	public static ChargesMessage matcherGroupChargeMessage(String s, int group, CustomChargeMatcher customMatcher)
	{
		return new ChargesMessage(Pattern.compile(s), (matcher, configManager) -> {
			String chargeCountString = matcher.group(group).replaceAll(",", "");
			int chargeCount = parseCharges(chargeCountString);
			return customMatcher.customHandler(matcher, chargeCount, configManager);
		}
		);
	}
}
