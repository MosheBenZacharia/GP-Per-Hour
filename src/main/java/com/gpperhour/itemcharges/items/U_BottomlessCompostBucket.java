/*
 * Copyright (c) 2023, Moshe Ben-Zacharia <https://github.com/MosheBenZacharia>, TicTac7x <https://github.com/TicTac7x>
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import com.gpperhour.GPPerHourConfig;
import com.gpperhour.itemcharges.ChargedItem;
import com.gpperhour.itemcharges.ChargesItem;
import com.gpperhour.itemcharges.triggers.TriggerAnimation;
import com.gpperhour.itemcharges.triggers.TriggerChatMessage;
import com.gpperhour.itemcharges.triggers.TriggerItem;
import com.google.gson.Gson;

import net.runelite.api.Client;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;

public class U_BottomlessCompostBucket extends ChargedItem {

    //avoid GC but don't confuse super class
    private final Map<Integer, Float> quantities = new HashMap<>();

    public U_BottomlessCompostBucket(
            final Client client,
            final ClientThread client_thread,
            final ConfigManager configs,
            final ItemManager items,
            final ChatMessageManager chat_messages,
            final Notifier notifier,
            final Gson gson,
            final ScheduledExecutorService executorService
    ) {
        super(ChargesItem.BOTTOMLESS_COMPOST_BUCKET, ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED, client, client_thread, configs, items, chat_messages, notifier, gson, executorService);
        this.config_key = GPPerHourConfig.bottomless_compost_bucket;
        this.extra_config_keys = new String[]{"type"};
        this.triggers_items = new TriggerItem[]{
                new TriggerItem(ItemID.BOTTOMLESS_COMPOST_BUCKET).fixedCharges(0),
                new TriggerItem(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED),
        };
        this.triggers_animations = new TriggerAnimation[]{
                new TriggerAnimation(8197).decreaseCharges(1),
                new TriggerAnimation(832).increaseCharges(2).onMenuOption("Take").unallowedItems(new int[]{ItemID.BUCKET_EMPTY, ItemID.DWARVEN_STOUT})
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
                new TriggerChatMessage("Your bottomless compost bucket has a single use of (?<type>.+) ?compost remaining.").fixedCharges(1),
                new TriggerChatMessage("Your bottomless compost bucket has (?<charges>.+) uses of (?<type>.+) ?compost remaining."),
                new TriggerChatMessage("Your bottomless compost bucket doesn't currently have any compost in it!(?<type>.*)").fixedCharges(0),
                new TriggerChatMessage("Your bottomless compost bucket is currently holding one use of (?<type>.+?) ?compost.").fixedCharges(1),
                new TriggerChatMessage("Your bottomless compost bucket is currently holding (?<charges>.+) uses of (?<type>.+?) ?compost."),
                new TriggerChatMessage("You discard the contents of your bottomless compost bucket.(?<type>.*)").fixedCharges(0),
                new TriggerChatMessage("You fill your bottomless compost bucket with .* buckets? of (?<type>.+?) ?compost. Your bottomless compost bucket now contains a total of (?<charges>.+) uses.")
        };
    }

    private String getCompostType() {
        return configs.getRSProfileConfiguration(GPPerHourConfig.GROUP, GPPerHourConfig.bottomless_compost_bucket_type);
    }

    @Override
    protected void onChargesUpdated()
    {
        quantities.clear();
        Integer itemId = null;
        String compostType = getCompostType();
        if (compostType == null)
        {
            return;
        }
        if (compostType.equals("ultra"))
        {
            itemId = ItemID.BUCKET_ULTRACOMPOST;
        }
        else if(compostType.equals("super"))
        {
            itemId = ItemID.BUCKET_SUPERCOMPOST;
        }
        else
        {
            itemId = ItemID.BUCKET_COMPOST;
        }
        quantities.put(itemId, ((float) charges)/2f);
    }

    @Override
    public Map<Integer, Float> getItemQuantities()
    {
        return quantities;
    }
}
