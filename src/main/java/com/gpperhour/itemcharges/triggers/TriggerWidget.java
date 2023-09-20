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
import java.util.function.Consumer;

public class TriggerWidget {
    public static final int WIDGET_DEFAULT_GROUP_ID = 193;
    public static final int WIDGET_DEFAULT_CHILD_ID = 2;

    public static final int WIDGET_NPC_GROUP_ID = 231;
    public static final int WIDGET_NPC_CHILD_ID = 6;

    @Nonnull public final String message;

    public int group_id;
    public int child_id;
    public Integer sub_child_id;

    public boolean increase_dynamically;

    @Nullable public Consumer<String> consumer;
    @Nullable public Integer charges;

    public TriggerWidget(final int group_id, final int child_id, @Nonnull final String message) {
        this.group_id = group_id;
        this.child_id = child_id;
        this.message = message;
    }

    public TriggerWidget(final int group_id, final int child_id, final int sub_child_id, @Nonnull final String message) {
        this.group_id = group_id;
        this.child_id = child_id;
        this.sub_child_id = sub_child_id;
        this.message = message;
    }

    public TriggerWidget fixedCharges(final int charges) {
        this.charges = charges;
        return this;
    }

    public TriggerWidget extraConsumer(@Nonnull final Consumer<String> consumer) {
        this.consumer = consumer;
        return this;
    }

    public TriggerWidget increaseDynamically() {
        this.increase_dynamically = true;
        return this;
    }
}
