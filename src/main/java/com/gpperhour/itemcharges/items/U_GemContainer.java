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
package com.gpperhour.itemcharges.items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gpperhour.itemcharges.ChargedItem;
import com.gpperhour.itemcharges.ChargesItem;
import com.gpperhour.itemcharges.triggers.TriggerChatMessage;
import com.gpperhour.itemcharges.triggers.TriggerItem;
import com.gpperhour.itemcharges.triggers.TriggerItemContainer;
import com.gpperhour.itemcharges.triggers.TriggerItemDespawn;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.TileItem;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;

/**
 * Shared base for the whole gem-container family - the original gem bag plus the gem pouch /
 * satchel / tote / sack added in the gem bag expansion. They are one mechanism with different
 * dials: which uncut gems they hold, how many of each, their item ids/name, and whether they
 * receive vyre pickpocket loot. Each concrete subclass just supplies those dials.
 *
 * All the gem-specific parsing (mining, checking, emptying, filling, ground pickup, pickpocket)
 * is scoped to this container's own gem set, which is what keeps coexisting containers (e.g. a
 * gem pouch held alongside the original gem bag) from mis-attributing each other's gems.
 */
@Slf4j
public abstract class U_GemContainer extends ChargedItem
{
    /** One uncut gem a container can hold, with the wording the game uses for it. */
    protected static final class Gem
    {
        final int itemId;
        // As it appears in "You just mined a <minedName>!" e.g. "Opal", "Red Topaz", "Sapphire".
        final String minedName;
        // As it appears in the check message "<checkLabel>: 5" - semi-precious are singular,
        // precious are plural (e.g. "Opal" but "Sapphires").
        final String checkLabel;
        // False only for dragonstone, which is never mined or found (not on the gem drop table);
        // it only enters via Fill, ground pickup, or a check sync.
        final boolean acquirable;
        final Pattern checkPattern;

        Gem(int itemId, String minedName, String checkLabel, boolean acquirable)
        {
            this.itemId = itemId;
            this.minedName = minedName;
            this.checkLabel = checkLabel;
            this.acquirable = acquirable;
            this.checkPattern = Pattern.compile(Pattern.quote(checkLabel) + ": (\\d+)");
        }
    }

    protected static final Gem OPAL = new Gem(ItemID.UNCUT_OPAL, "Opal", "Opal", true);
    protected static final Gem JADE = new Gem(ItemID.UNCUT_JADE, "Jade", "Jade", true);
    protected static final Gem RED_TOPAZ = new Gem(ItemID.UNCUT_RED_TOPAZ, "Red Topaz", "Red Topaz", true);
    protected static final Gem SAPPHIRE = new Gem(ItemID.UNCUT_SAPPHIRE, "Sapphire", "Sapphires", true);
    protected static final Gem EMERALD = new Gem(ItemID.UNCUT_EMERALD, "Emerald", "Emeralds", true);
    protected static final Gem RUBY = new Gem(ItemID.UNCUT_RUBY, "Ruby", "Rubies", true);
    protected static final Gem DIAMOND = new Gem(ItemID.UNCUT_DIAMOND, "Diamond", "Diamonds", true);
    protected static final Gem DRAGONSTONE = new Gem(ItemID.UNCUT_DRAGONSTONE, "Dragonstone", "Dragonstones", false);

    // The three semi-precious gems the pouch/satchel/tote hold.
    protected static final Gem[] SEMI_PRECIOUS = { OPAL, JADE, RED_TOPAZ };
    // The five gems the original gem bag holds.
    protected static final Gem[] GEM_BAG_GEMS = { SAPPHIRE, EMERALD, RUBY, DIAMOND, DRAGONSTONE };
    // All eight gems the gem sack holds.
    protected static final Gem[] ALL_GEMS = { OPAL, JADE, RED_TOPAZ, SAPPHIRE, EMERALD, RUBY, DIAMOND, DRAGONSTONE };

    // Thieving chests/stalls (e.g. Dorgesh-Kaan rich chest) deposit one gem straight into the
    // container: "You put a gem in your sack: Uncut diamond". Gem name is lowercase; the container
    // word varies by tier (".+?" absorbs it); the same message format covers every tier.
    private static final Pattern THIEVE_PATTERN = Pattern.compile("You put a gem in your .+?: Uncut ([\\w ]+)");

    private final int openId;
    private final int capacity;
    private final Gem[] gems;
    private final String name;
    private final Set<Integer> heldIds;
    private final Pattern acquirePattern;
    private final Pattern pickpocketPattern;

    protected U_GemContainer(
            final ChargesItem chargesItem,
            final int closedId,
            final int openId,
            final int capacity,
            final String configKey,
            final String name,
            final Gem[] gems,
            final boolean supportsVyrePickpocket,
            final Client client,
            final ClientThread client_thread,
            final ConfigManager configs,
            final ItemManager items,
            final ChatMessageManager chat_messages,
            final Notifier notifier,
            final Gson gson,
            final ScheduledExecutorService executorService
    ) {
        super(chargesItem, closedId, client, client_thread, configs, items, chat_messages, notifier, gson, executorService);

        this.openId = openId;
        this.capacity = capacity;
        this.gems = gems;
        this.name = name;

        this.heldIds = new HashSet<>();
        final List<String> acquirableNames = new ArrayList<>();
        for (final Gem gem : gems)
        {
            heldIds.add(gem.itemId);
            if (gem.acquirable)
                acquirableNames.add(gem.minedName);
        }
        // e.g. "You just mined a piece of Jade!" / "You just found a Ruby!". The article (a/an) and
        // the "piece of" (jade only) both vary, so absorb them. "found" only ever happens for the
        // gem-drop-table gems, but including it is harmless for the semi-precious-only tiers.
        this.acquirePattern = Pattern.compile(
                "You just (?:found|mined) (?:a|an) (?:piece of )?(" + String.join("|", acquirableNames) + ")!");
        // Vyre pickpocketing, e.g. "...added to your gem bag: Uncut ruby x 2." (gem names lowercase).
        this.pickpocketPattern = supportsVyrePickpocket
                ? Pattern.compile("The following stolen loot gets added to your " + name + ": Uncut (.*) x (\\d+)")
                : null;

        final String closedTarget = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        final String openTarget = "Open " + name;

        this.config_key = configKey;
        this.zero_charges_is_positive = true;
        this.triggers_items = new TriggerItem[]{
                new TriggerItem(closedId),
                new TriggerItem(openId, true),
        };
        this.trigger_item_despawn = new TriggerItemDespawn((TileItem tileItem) ->
        {
            if (tileItem.getQuantity() == 1 && heldIds.contains(tileItem.getId()))
                addGemIfHasCapacity(tileItem.getId(), 1f);
        });

        final List<TriggerChatMessage> chatTriggers = new ArrayList<>();
        // Gems mined at gem rocks (and gem-drop-table "found" gems) enter while open; the "put it
        // into your gem X" confirmation doesn't name the gem, so we read it from this message.
        chatTriggers.add(new TriggerChatMessage(acquirePattern.pattern()).extraConsumer((message) -> {
            if (!hasChargeData())
                return;
            if (this.item_id != this.openId)
                return;
            final Matcher matcher = acquirePattern.matcher(message);
            while (matcher.find())
            {
                final Gem gem = findGem(matcher.group(1), false);
                if (gem != null)
                    addGemIfHasCapacity(gem.itemId, 1f);
            }
        }));
        // Both empty-to-inventory variants and the empty-to-bank message all literally say "gem bag"
        // regardless of tier. onItemClick() disambiguates by the clicked menu target so we only clear
        // the container that was actually emptied (e.g. not a pouch held alongside a gem bag).
        chatTriggers.add(new TriggerChatMessage("The gem bag is now empty.").onItemClick().extraConsumer((m) -> emptyOrClear()));
        chatTriggers.add(new TriggerChatMessage("The gem bag is empty.").onItemClick().extraConsumer((m) -> emptyOrClear()));
        chatTriggers.add(new TriggerChatMessage("You empty your gem bag into the bank.").onItemClick().extraConsumer((m) -> emptyOrClear()));
        // "Check", and the partial empty-to-inventory "Left in bag: ..." message, both report the exact
        // contents. We find each gem's "<label>: n" independently (matcher.find() matches the substring),
        // which uniformly handles the singular/plural labels, the multi-line <br> sack layout, and the
        // "Left in bag:" prefix. Triggered on the first gem's label, which is always present.
        chatTriggers.add(new TriggerChatMessage(gems[0].checkLabel + ": \\d+").extraConsumer((message) -> {
            emptyOrClear();
            for (final Gem gem : gems)
            {
                final Matcher matcher = gem.checkPattern.matcher(message);
                if (matcher.find())
                {
                    try
                    {
                        addItems(gem.itemId, (float) Integer.parseInt(matcher.group(1)));
                    }
                    catch (NumberFormatException e)
                    {
                        log.error("couldn't parse " + name + " check: " + message, e);
                    }
                }
            }
        }));
        // Thieving deposit (e.g. Dorgesh-Kaan rich chest, gem stalls). The message only prints when
        // the gem actually went into the container, so no open-guard is needed; gem-set scoping
        // attributes it to the right container when several are held.
        chatTriggers.add(new TriggerChatMessage(THIEVE_PATTERN.pattern()).extraConsumer((message) -> {
            if (!hasChargeData())
                return;
            final Matcher matcher = THIEVE_PATTERN.matcher(message);
            while (matcher.find())
            {
                final Gem gem = findGem(matcher.group(1).trim(), true);
                if (gem != null)
                    addGemIfHasCapacity(gem.itemId, 1f);
            }
        }));
        if (pickpocketPattern != null)
        {
            chatTriggers.add(new TriggerChatMessage(pickpocketPattern.pattern()).extraConsumer((message) -> {
                if (!hasChargeData())
                    return;
                final Matcher matcher = pickpocketPattern.matcher(message);
                while (matcher.find())
                {
                    final Gem gem = findGem(matcher.group(1), true);
                    if (gem == null)
                        continue;
                    try
                    {
                        addItems(gem.itemId, (float) Integer.parseInt(matcher.group(2)));
                    }
                    catch (NumberFormatException e)
                    {
                        log.error("couldn't parse " + name + " pickpocket: " + message, e);
                    }
                }
            }));
        }
        this.triggers_chat_messages = chatTriggers.toArray(new TriggerChatMessage[0]);

        this.triggers_item_containers = new TriggerItemContainer[]{
            new TriggerItemContainer(InventoryID.INV).menuTarget(openTarget).menuOption("Fill").addDifference(),
            new TriggerItemContainer(InventoryID.INV).menuTarget(closedTarget).menuOption("Fill").addDifference(),
            // The original gem bag empties to bank silently via this menu option (no chat message). The
            // newer containers print "You empty your gem bag into the bank." (handled above) and use the
            // "Empty" option, so this trigger is a harmless no-op for them.
            new TriggerItemContainer(InventoryID.BANK).menuTarget(openTarget).menuOption("Empty to bank").extraConsumer(() -> emptyOrClear()),
            new TriggerItemContainer(InventoryID.BANK).menuTarget(closedTarget).menuOption("Empty to bank").extraConsumer(() -> emptyOrClear()),
        };
        this.supportsWidgetOnWidget = true;
    }

    private Gem findGem(final String gemName, final boolean ignoreCase)
    {
        for (final Gem gem : gems)
        {
            if (ignoreCase ? gem.minedName.equalsIgnoreCase(gemName) : gem.minedName.equals(gemName))
                return gem;
        }
        return null;
    }

    private void addGemIfHasCapacity(final int itemId, final float count)
    {
        if (!itemQuantities.containsKey(itemId) || itemQuantities.get(itemId) < capacity)
            addItems(itemId, count);
    }
}
