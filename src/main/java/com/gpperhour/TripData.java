/*
 * Copyright (c) 2023, Moshe Ben-Zacharia <https://github.com/MosheBenZacharia>, Eric Versteeg <https://github.com/erversteeg>
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

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

//Data for a single 'trip'
public class TripData
{
    String identifier = null;
    long runStartTime = 0;
    // if this is null the trip is in progress
    Long runEndTime = null;
    long runtime = 0;
    boolean isPaused = false;

    Map<Integer, Float> initialItemQtys = new HashMap<>();
    Map<Integer, Float> bankedItemQtys = new HashMap<>();
    transient Map<Integer, Float> itemQtys = new HashMap<>();

    boolean isInProgress()
    {
        return runEndTime == null;
    }

    long getRuntime()
    {
        return runtime;
    }

    long getEndTime()
    {
        return (runEndTime == null ? Instant.now().toEpochMilli() : runEndTime);
    }

    // its in the period between banking finished (onNewRun) and two ticks later
    // when we call onPostNewRun. we have this delay because of how you can withdraw from the bank,
    // close it immediately, and still get the items in your inventory a tick later.
    transient boolean isBankDelay;
    //first run needs to be reinitialized on first game tick.
    transient boolean isFirstRun;
}
