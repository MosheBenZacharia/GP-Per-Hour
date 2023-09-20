/*
 * Copyright (c) 2023, TicTac7x <https://github.com/TicTac7x>
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

public class TriggerAnimation {
    public final int animation_id;

    public int charges;
    public boolean decrease_charges;
    public boolean equipped;
    public boolean menu_target;

    @Nullable public int[] unallowed_items;
    @Nullable public String menu_option;

    public TriggerAnimation(final int animation_id) {
        this.animation_id = animation_id;
    }

    public TriggerAnimation increaseCharges(final int charges) {
        this.charges = charges;
        this.decrease_charges = false;
        return this;
    }

    public TriggerAnimation decreaseCharges(final int decharges) {
        this.charges = decharges;
        this.decrease_charges = true;
        return this;
    }

    public TriggerAnimation equipped() {
        this.equipped = true;
        return this;
    }

    public TriggerAnimation onItemClick() {
        this.menu_target = true;
        return this;
    }

    public TriggerAnimation unallowedItems(@Nonnull final int[] unallowed_items) {
        this.unallowed_items = unallowed_items;
        return this;
    }

    public TriggerAnimation onMenuOption(@Nonnull final String menu_option) {
        this.menu_option = menu_option;
        return this;
    }
}
