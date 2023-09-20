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
package com.gpperhour.itemcharges.triggers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class TriggerChatMessage {
    @Nonnull public final Pattern message;

    public boolean menu_target;
    public boolean equipped;
    public boolean inventory_same;
    public boolean inventory_count_same;
    public boolean multiple_charges;
    public boolean increase_dynamically;
    public boolean decrease_dynamically;
    public boolean notification;

    @Nullable public Consumer<String> consumer;
    @Nullable public Integer fixed_charges;
    @Nullable public Integer decrease_charges;
    @Nullable public Integer increase_charges;
    @Nullable public String notification_message;

    public TriggerChatMessage(@Nonnull final String message) {
        this.message = Pattern.compile(message);
    }

    public TriggerChatMessage fixedCharges(final int charges) {
        this.fixed_charges = charges;
        return this;
    }

    public TriggerChatMessage onItemClick() {
        this.menu_target = true;
        return this;
    }

    public TriggerChatMessage increaseCharges(final int charges) {
        this.increase_charges = charges;
        return this;
    }

    public TriggerChatMessage decreaseCharges(final int charges) {
        this.decrease_charges = charges;
        return this;
    }

    public TriggerChatMessage equipped() {
        this.equipped = true;
        return this;
    }

    public TriggerChatMessage inventorySame() {
        this.inventory_same = true;
        return this;
    }

    public TriggerChatMessage inventoryCountSame() {
        this.inventory_count_same = true;
        return this;
    }

    public TriggerChatMessage extraConsumer(final Consumer<String> consumer) {
        this.consumer = consumer;
        return this;
    }

    public TriggerChatMessage multipleCharges() {
        this.multiple_charges = true;
        return this;
    }

    public TriggerChatMessage increaseDynamically() {
        this.increase_dynamically = true;
        return this;
    }

    public TriggerChatMessage decreaseDynamically() {
        this.decrease_dynamically = true;
        return this;
    }

    public TriggerChatMessage notification() {
        this.notification = true;
        return this;
    }

    public TriggerChatMessage notification(final String message) {
        this.notification = true;
        this.notification_message = message;
        return this;
    }
}
