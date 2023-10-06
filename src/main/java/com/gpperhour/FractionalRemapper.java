/*
 * Copyright (c) 2023, Moshe Ben-Zacharia <https://github.com/MosheBenZacharia>, Seth <Sethtroll3@gmail.com>
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

import static net.runelite.api.ItemID.*;

//Remaps items with doses/charges to a fraction of their full item ID to keep the ledger clean
//IE: (Super Restore (2), 1) --> (Super Restore (4), 0.5)
//IE: (Games necklace (2), 1) --> (Games necklace (8), 0.25)
public class FractionalRemapper
{

    @RequiredArgsConstructor
    private static class RemapData
    {
        final Integer remappedId;
        final Float remappedQuantityMultiplier;
    }

    private static final Map<Integer, RemapData> remapData;

    //avoid GC
    private static final List<Integer> keySet = new LinkedList<>();

    public static void Remap(Map<Integer, Float> qtyMap)
    {
        keySet.clear();
        for (Integer integer : qtyMap.keySet())
        {
            keySet.add(integer);
        }

        for (Integer oldItemId : keySet)
        {
            if (remapData.containsKey(oldItemId))
            {
                RemapData data = remapData.get(oldItemId);
                float originalQty = qtyMap.get(oldItemId);
                float newQty = originalQty * data.remappedQuantityMultiplier;

                qtyMap.remove(oldItemId);
                qtyMap.merge(data.remappedId, newQty, Float::sum);
            }
        }
    }

    static
    {
        //Sourced from https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/itemcharges/ItemWithCharge.java
        remapData = new HashMap<>();

        /////////////////////// Rechargeable Jewelery ///////////////////////
        //These shouldn't really lose value as the charges deplete since the uncharged
        //version is only worth slightly less than the charged version.
        //But we still want them to only show up as one row in the ledger so we just make them
        //a tiny fraction less of the fully charged version

        //////// Skills necklace(6) /////////
        //Skills (6) = 12,241gp
        //Skills (0) = 11,914gp
        remapData.put(11970, new RemapData(11968, 1f));
        remapData.put(11105, new RemapData(11968, 1f));
        remapData.put(11107, new RemapData(11968, 1f));
        remapData.put(11109, new RemapData(11968, 1f));
        remapData.put(11111, new RemapData(11968, 1f));
        //Wealth (5) = 13,989gp
        //Wealth (0) = 11,903gp
        //////// Ring of wealth (i5) /////////
        remapData.put(20787, new RemapData(20786, 0.95f));
        remapData.put(20788, new RemapData(20786, 0.9f));
        remapData.put(20789, new RemapData(20786, 0.85f));
        remapData.put(20790, new RemapData(20786, 0.8f));
        //////// Ring of wealth (5) /////////
        remapData.put(11982, new RemapData(11980, 0.95f));
        remapData.put(11984, new RemapData(11980, 0.9f));
        remapData.put(11986, new RemapData(11980, 0.85f));
        remapData.put(11988, new RemapData(11980, 0.8f));
        //Glory (6) = 12,872gp
        //Glory (0) = 11,903gp
        //////// Amulet of glory (t6) /////////
        remapData.put(11966, new RemapData(11964, 1f));
        remapData.put(10354, new RemapData(11964, 1f));
        remapData.put(10356, new RemapData(11964, 1f));
        remapData.put(10358, new RemapData(11964, 1f));
        remapData.put(10360, new RemapData(11964, 1f));
        //////// Amulet of glory(6) /////////
        remapData.put(11976,new RemapData(11978, 1f));
        remapData.put(1712, new RemapData(11978, 1f));
        remapData.put(1710, new RemapData(11978, 1f));
        remapData.put(1708, new RemapData(11978, 1f));
        remapData.put(1706, new RemapData(11978, 1f));
        //Combat (6) = 12,436gp
        //Combat (0) = 12,370gp
        //////// Combat bracelet(6) /////////
        remapData.put(11974, new RemapData(11972, 1f));
        remapData.put(11118, new RemapData(11972, 1f));
        remapData.put(11120, new RemapData(11972, 1f));
        remapData.put(11122, new RemapData(11972, 1f));
        remapData.put(11124, new RemapData(11972, 1f));


        /////////////////////// Everything Else ///////////////////////

        //////// Forgotten brew(4) /////////
        remapData.put(27632, new RemapData(27629, 0.75f));
        remapData.put(27635, new RemapData(27629, 0.5f));
        remapData.put(27638, new RemapData(27629, 0.25f));
        //////// Smelling salts (2) /////////
        remapData.put(27345, new RemapData(27343, 0.5f));
        //////// Silk dressing (2) /////////
        remapData.put(27325, new RemapData(27323, 0.5f));
        //////// Blessed crystal scarab (2) /////////
        remapData.put(27337, new RemapData(27335, 0.5f));
        //////// Ambrosia (2) /////////
        remapData.put(27349, new RemapData(27347, 0.5f));
        //////// Liquid adrenaline (2) /////////
        remapData.put(27341, new RemapData(27339, 0.5f));
        //////// Tears of elidinis (4) /////////
        remapData.put(27329, new RemapData(27327, 0.75f));
        remapData.put(27331, new RemapData(27327, 0.5f));
        remapData.put(27333, new RemapData(27327, 0.25f));
        //////// Nectar (4) /////////
        remapData.put(27317, new RemapData(27315, 0.75f));
        remapData.put(27319, new RemapData(27315, 0.5f));
        remapData.put(27321, new RemapData(27315, 0.25f));
        //////// Overload (+)(4) /////////
        remapData.put(20995, new RemapData(20996, 0.75f));
        remapData.put(20994, new RemapData(20996, 0.5f));
        remapData.put(20993, new RemapData(20996, 0.25f));
        //////// Overload (4) /////////
        remapData.put(20991, new RemapData(20992, 0.75f));
        remapData.put(20990, new RemapData(20992, 0.5f));
        remapData.put(20989, new RemapData(20992, 0.25f));
        //////// Overload (-)(4) /////////
        remapData.put(20987, new RemapData(20988, 0.75f));
        remapData.put(20986, new RemapData(20988, 0.5f));
        remapData.put(20985, new RemapData(20988, 0.25f));
        //////// Prayer enhance (+)(4) /////////
        remapData.put(20971, new RemapData(20972, 0.75f));
        remapData.put(20970, new RemapData(20972, 0.5f));
        remapData.put(20969, new RemapData(20972, 0.25f));
        //////// Prayer enhance (4) /////////
        remapData.put(20967, new RemapData(20968, 0.75f));
        remapData.put(20966, new RemapData(20968, 0.5f));
        remapData.put(20965, new RemapData(20968, 0.25f));
        //////// Prayer enhance (-)(4) /////////
        remapData.put(20963, new RemapData(20964, 0.75f));
        remapData.put(20962, new RemapData(20964, 0.5f));
        remapData.put(20961, new RemapData(20964, 0.25f));
        //////// Xeric's aid (+)(4) /////////
        remapData.put(20983, new RemapData(20984, 0.75f));
        remapData.put(20982, new RemapData(20984, 0.5f));
        remapData.put(20981, new RemapData(20984, 0.25f));
        //////// Xeric's aid (4) /////////
        remapData.put(20979, new RemapData(20980, 0.75f));
        remapData.put(20978, new RemapData(20980, 0.5f));
        remapData.put(20977, new RemapData(20980, 0.25f));
        //////// Xeric's aid (-)(4) /////////
        remapData.put(20975, new RemapData(20976, 0.75f));
        remapData.put(20974, new RemapData(20976, 0.5f));
        remapData.put(20973, new RemapData(20976, 0.25f));
        //////// Revitalisation (+)(4) /////////
        remapData.put(20959, new RemapData(20960, 0.75f));
        remapData.put(20958, new RemapData(20960, 0.5f));
        remapData.put(20957, new RemapData(20960, 0.25f));
        //////// Revitalisation potion (4) /////////
        remapData.put(20955, new RemapData(20956, 0.75f));
        remapData.put(20954, new RemapData(20956, 0.5f));
        remapData.put(20953, new RemapData(20956, 0.25f));
        //////// Revitalisation (-)(4) /////////
        remapData.put(20951, new RemapData(20952, 0.75f));
        remapData.put(20950, new RemapData(20952, 0.5f));
        remapData.put(20949, new RemapData(20952, 0.25f));
        //////// Twisted (+)(4) /////////
        remapData.put(20935, new RemapData(20936, 0.75f));
        remapData.put(20934, new RemapData(20936, 0.5f));
        remapData.put(20933, new RemapData(20936, 0.25f));
        //////// Twisted potion (4) /////////
        remapData.put(20931, new RemapData(20932, 0.75f));
        remapData.put(20930, new RemapData(20932, 0.5f));
        remapData.put(20929, new RemapData(20932, 0.25f));
        //////// Twisted (-)(4) /////////
        remapData.put(20927, new RemapData(20928, 0.75f));
        remapData.put(20926, new RemapData(20928, 0.5f));
        remapData.put(20925, new RemapData(20928, 0.25f));
        //////// Kodai (+)(4) /////////
        remapData.put(20947, new RemapData(20948, 0.75f));
        remapData.put(20946, new RemapData(20948, 0.5f));
        remapData.put(20945, new RemapData(20948, 0.25f));
        //////// Kodai potion (4) /////////
        remapData.put(20943, new RemapData(20944, 0.75f));
        remapData.put(20942, new RemapData(20944, 0.5f));
        remapData.put(20941, new RemapData(20944, 0.25f));
        //////// Kodai (-)(4) /////////
        remapData.put(20939, new RemapData(20940, 0.75f));
        remapData.put(20938, new RemapData(20940, 0.5f));
        remapData.put(20937, new RemapData(20940, 0.25f));
        //////// Elder (+)(4) /////////
        remapData.put(20923, new RemapData(20924, 0.75f));
        remapData.put(20922, new RemapData(20924, 0.5f));
        remapData.put(20921, new RemapData(20924, 0.25f));
        //////// Elder potion (4) /////////
        remapData.put(20919, new RemapData(20920, 0.75f));
        remapData.put(20918, new RemapData(20920, 0.5f));
        remapData.put(20917, new RemapData(20920, 0.25f));
        //////// Elder (-)(4) /////////
        remapData.put(20915, new RemapData(20916, 0.75f));
        remapData.put(20914, new RemapData(20916, 0.5f));
        remapData.put(20913, new RemapData(20916, 0.25f));
        //////// Zamorak brew(4) /////////
        remapData.put(189, new RemapData(2450, 0.75f));
        remapData.put(191, new RemapData(2450, 0.5f));
        remapData.put(193, new RemapData(2450, 0.25f));
        //////// Waterskin(4) /////////
        remapData.put(1825, new RemapData(1823, 0.75f));
        remapData.put(1827, new RemapData(1823, 0.5f));
        remapData.put(1829, new RemapData(1823, 0.25f));
        remapData.put(1831, new RemapData(1823, 0.0f));
        //////// Watering can(8) /////////
        remapData.put(5339, new RemapData(5340, 0.875f));
        remapData.put(5338, new RemapData(5340, 0.75f));
        remapData.put(5337, new RemapData(5340, 0.625f));
        remapData.put(5336, new RemapData(5340, 0.5f));
        remapData.put(5335, new RemapData(5340, 0.375f));
        remapData.put(5334, new RemapData(5340, 0.25f));
        remapData.put(5333, new RemapData(5340, 0.125f));
        remapData.put(5331, new RemapData(5340, 0.0f));
        //////// Teleport crystal (5) /////////
        remapData.put(6099, new RemapData(13102, 0.8f));
        remapData.put(6100, new RemapData(13102, 0.6f));
        remapData.put(6101, new RemapData(13102, 0.4f));
        remapData.put(6102, new RemapData(13102, 0.2f));
        //////// Super strength(4) /////////
        remapData.put(157, new RemapData(2440, 0.75f));
        remapData.put(159, new RemapData(2440, 0.5f));
        remapData.put(161, new RemapData(2440, 0.25f));
        //////// Super restore(4) /////////
        remapData.put(3026, new RemapData(3024, 0.75f));
        remapData.put(3028, new RemapData(3024, 0.5f));
        remapData.put(3030, new RemapData(3024, 0.25f));
        //////// Super ranging (4) /////////
        remapData.put(11723, new RemapData(11722, 0.75f));
        remapData.put(11724, new RemapData(11722, 0.5f));
        remapData.put(11725, new RemapData(11722, 0.25f));
        //////// Super magic potion (4) /////////
        remapData.put(11727, new RemapData(11726, 0.75f));
        remapData.put(11728, new RemapData(11726, 0.5f));
        remapData.put(11729, new RemapData(11726, 0.25f));
        //////// Super energy(4) /////////
        remapData.put(3018, new RemapData(3016, 0.75f));
        remapData.put(3020, new RemapData(3016, 0.5f));
        remapData.put(3022, new RemapData(3016, 0.25f));
        //////// Super defence(4) /////////
        remapData.put(163, new RemapData(2442, 0.75f));
        remapData.put(165, new RemapData(2442, 0.5f));
        remapData.put(167, new RemapData(2442, 0.25f));
        //////// Super combat potion(4) /////////
        remapData.put(12697, new RemapData(12695, 0.75f));
        remapData.put(12699, new RemapData(12695, 0.5f));
        remapData.put(12701, new RemapData(12695, 0.25f));
        //////// Super attack(4) /////////
        remapData.put(145, new RemapData(2436, 0.75f));
        remapData.put(147, new RemapData(2436, 0.5f));
        remapData.put(149, new RemapData(2436, 0.25f));
        //////// Super antifire potion(4) /////////
        remapData.put(21981, new RemapData(21978, 0.75f));
        remapData.put(21984, new RemapData(21978, 0.5f));
        remapData.put(21987, new RemapData(21978, 0.25f));
        //////// Superantipoison(4) /////////
        remapData.put(181, new RemapData(2448, 0.75f));
        remapData.put(183, new RemapData(2448, 0.5f));
        remapData.put(185, new RemapData(2448, 0.25f));
        //////// Strength potion(4) /////////
        remapData.put(115, new RemapData(113, 0.75f));
        remapData.put(117, new RemapData(113, 0.5f));
        remapData.put(119, new RemapData(113, 0.25f));
        //////// Stamina potion(4) /////////
        remapData.put(12627, new RemapData(12625, 0.75f));
        remapData.put(12629, new RemapData(12625, 0.5f));
        remapData.put(12631, new RemapData(12625, 0.25f));
        //////// Serum 208 (4) /////////
        remapData.put(3417, new RemapData(3416, 0.75f));
        remapData.put(3418, new RemapData(3416, 0.5f));
        remapData.put(3419, new RemapData(3416, 0.25f));
        //////// Serum 207 (4) /////////
        remapData.put(3410, new RemapData(3408, 0.75f));
        remapData.put(3412, new RemapData(3408, 0.5f));
        remapData.put(3414, new RemapData(3408, 0.25f));
        //////// Saradomin brew(4) /////////
        remapData.put(6687, new RemapData(6685, 0.75f));
        remapData.put(6689, new RemapData(6685, 0.5f));
        remapData.put(6691, new RemapData(6685, 0.25f));
        //////// Sanfew serum(4) /////////
        remapData.put(10927, new RemapData(10925, 0.75f));
        remapData.put(10929, new RemapData(10925, 0.5f));
        remapData.put(10931, new RemapData(10925, 0.25f));
        //////// Potatoes(10) /////////
        remapData.put(5436, new RemapData(5438, 0.9f));
        remapData.put(5434, new RemapData(5438, 0.8f));
        remapData.put(5432, new RemapData(5438, 0.7f));
        remapData.put(5430, new RemapData(5438, 0.6f));
        remapData.put(5428, new RemapData(5438, 0.5f));
        remapData.put(5426, new RemapData(5438, 0.4f));
        remapData.put(5424, new RemapData(5438, 0.3f));
        remapData.put(5422, new RemapData(5438, 0.2f));
        remapData.put(5420, new RemapData(5438, 0.1f));
        //////// Onions(10) /////////
        remapData.put(5456, new RemapData(5458, 0.9f));
        remapData.put(5454, new RemapData(5458, 0.8f));
        remapData.put(5452, new RemapData(5458, 0.7f));
        remapData.put(5450, new RemapData(5458, 0.6f));
        remapData.put(5448, new RemapData(5458, 0.5f));
        remapData.put(5446, new RemapData(5458, 0.4f));
        remapData.put(5444, new RemapData(5458, 0.3f));
        remapData.put(5442, new RemapData(5458, 0.2f));
        remapData.put(5440, new RemapData(5458, 0.1f));
        //////// Cabbages(10) /////////
        remapData.put(5476, new RemapData(5478, 0.9f));
        remapData.put(5474, new RemapData(5478, 0.8f));
        remapData.put(5472, new RemapData(5478, 0.7f));
        remapData.put(5470, new RemapData(5478, 0.6f));
        remapData.put(5468, new RemapData(5478, 0.5f));
        remapData.put(5466, new RemapData(5478, 0.4f));
        remapData.put(5464, new RemapData(5478, 0.3f));
        remapData.put(5462, new RemapData(5478, 0.2f));
        remapData.put(5460, new RemapData(5478, 0.1f));
        //////// Slayer ring (8) /////////
        remapData.put(11867, new RemapData(11866, 0.875f));
        remapData.put(11868, new RemapData(11866, 0.75f));
        remapData.put(11869, new RemapData(11866, 0.625f));
        remapData.put(11870, new RemapData(11866, 0.5f));
        remapData.put(11871, new RemapData(11866, 0.375f));
        remapData.put(11872, new RemapData(11866, 0.25f));
        remapData.put(11873, new RemapData(11866, 0.125f));
        //////// Ring of dueling(8) /////////
        remapData.put(2554, new RemapData(2552, 0.875f));
        remapData.put(2556, new RemapData(2552, 0.75f));
        remapData.put(2558, new RemapData(2552, 0.625f));
        remapData.put(2560, new RemapData(2552, 0.5f));
        remapData.put(2562, new RemapData(2552, 0.375f));
        remapData.put(2564, new RemapData(2552, 0.25f));
        remapData.put(2566, new RemapData(2552, 0.125f));
        //////// Ring of returning(5) /////////
        remapData.put(21132, new RemapData(21129, 0.8f));
        remapData.put(21134, new RemapData(21129, 0.6f));
        remapData.put(21136, new RemapData(21129, 0.4f));
        remapData.put(21138, new RemapData(21129, 0.2f));
        //////// Restore potion(4) /////////
        remapData.put(127, new RemapData(2430, 0.75f));
        remapData.put(129, new RemapData(2430, 0.5f));
        remapData.put(131, new RemapData(2430, 0.25f));
        //////// Relicym's balm(4) /////////
        remapData.put(4844, new RemapData(4842, 0.75f));
        remapData.put(4846, new RemapData(4842, 0.5f));
        remapData.put(4848, new RemapData(4842, 0.25f));
        //////// Ranging potion(4) /////////
        remapData.put(169, new RemapData(2444, 0.75f));
        remapData.put(171, new RemapData(2444, 0.5f));
        remapData.put(173, new RemapData(2444, 0.25f));
        //////// Prayer potion(4) /////////
        remapData.put(139, new RemapData(2434, 0.75f));
        remapData.put(141, new RemapData(2434, 0.5f));
        remapData.put(143, new RemapData(2434, 0.25f));
        //////// Necklace of passage(5) /////////
        remapData.put(21149, new RemapData(21146, 0.8f));
        remapData.put(21151, new RemapData(21146, 0.6f));
        remapData.put(21153, new RemapData(21146, 0.4f));
        remapData.put(21155, new RemapData(21146, 0.2f));
        //////// Overload (4) /////////
        remapData.put(11731, new RemapData(11730, 0.75f));
        remapData.put(11732, new RemapData(11730, 0.5f));
        remapData.put(11733, new RemapData(11730, 0.25f));
        //////// Magic essence(4) /////////
        remapData.put(9022, new RemapData(9021, 0.75f));
        remapData.put(9023, new RemapData(9021, 0.5f));
        remapData.put(9024, new RemapData(9021, 0.25f));
        //////// Magic potion(4) /////////
        remapData.put(3042, new RemapData(3040, 0.75f));
        remapData.put(3044, new RemapData(3040, 0.5f));
        remapData.put(3046, new RemapData(3040, 0.25f));
        //////// Imp-in-a-box(2) /////////
        remapData.put(10028, new RemapData(10027, 0.5f));
        //////// Hunter potion(4) /////////
        remapData.put(10000, new RemapData(9998, 0.75f));
        remapData.put(10002, new RemapData(9998, 0.5f));
        remapData.put(10004, new RemapData(9998, 0.25f));
        //////// Guthix balance(4) /////////
        remapData.put(7662, new RemapData(7660, 0.75f));
        remapData.put(7664, new RemapData(7660, 0.5f));
        remapData.put(7666, new RemapData(7660, 0.25f));
        //////// Guthix rest(4) /////////
        remapData.put(4419, new RemapData(4417, 0.75f));
        remapData.put(4421, new RemapData(4417, 0.5f));
        remapData.put(4423, new RemapData(4417, 0.25f));
        //////// Games necklace(8) /////////
        remapData.put(3855, new RemapData(3853, 0.875f));
        remapData.put(3857, new RemapData(3853, 0.75f));
        remapData.put(3859, new RemapData(3853, 0.625f));
        remapData.put(3861, new RemapData(3853, 0.5f));
        remapData.put(3863, new RemapData(3853, 0.375f));
        remapData.put(3865, new RemapData(3853, 0.25f));
        remapData.put(3867, new RemapData(3853, 0.125f));
        //////// Fungicide spray 10 /////////
        remapData.put(7422, new RemapData(7421, 0.9f));
        remapData.put(7423, new RemapData(7421, 0.8f));
        remapData.put(7424, new RemapData(7421, 0.7f));
        remapData.put(7425, new RemapData(7421, 0.6f));
        remapData.put(7426, new RemapData(7421, 0.5f));
        remapData.put(7427, new RemapData(7421, 0.4f));
        remapData.put(7428, new RemapData(7421, 0.3f));
        remapData.put(7429, new RemapData(7421, 0.2f));
        remapData.put(7430, new RemapData(7421, 0.1f));
        remapData.put(7431, new RemapData(7421, 0.0f));
        //////// Fishing potion(4) /////////
        remapData.put(151, new RemapData(2438, 0.75f));
        remapData.put(153, new RemapData(2438, 0.5f));
        remapData.put(155, new RemapData(2438, 0.25f));
        //////// Extended super antifire(4) /////////
        remapData.put(22212, new RemapData(22209, 0.75f));
        remapData.put(22215, new RemapData(22209, 0.5f));
        remapData.put(22218, new RemapData(22209, 0.25f));
        //////// Extended antifire(4) /////////
        remapData.put(11953, new RemapData(11951, 0.75f));
        remapData.put(11955, new RemapData(11951, 0.5f));
        remapData.put(11957, new RemapData(11951, 0.25f));
        //////// Energy potion(4) /////////
        remapData.put(3010, new RemapData(3008, 0.75f));
        remapData.put(3012, new RemapData(3008, 0.5f));
        remapData.put(3014, new RemapData(3008, 0.25f));
        //////// Enchanted lyre(5) /////////
        remapData.put(6127, new RemapData(13079, 0.8f));
        remapData.put(6126, new RemapData(13079, 0.6f));
        remapData.put(6125, new RemapData(13079, 0.4f));
        remapData.put(3691, new RemapData(13079, 0.2f));
        //////// Divine super strength potion(4) /////////
        remapData.put(23712, new RemapData(23709, 0.75f));
        remapData.put(23715, new RemapData(23709, 0.5f));
        remapData.put(23718, new RemapData(23709, 0.25f));
        //////// Divine super defence potion(4) /////////
        remapData.put(23724, new RemapData(23721, 0.75f));
        remapData.put(23727, new RemapData(23721, 0.5f));
        remapData.put(23730, new RemapData(23721, 0.25f));
        //////// Divine super combat potion(4) /////////
        remapData.put(23688, new RemapData(23685, 0.75f));
        remapData.put(23691, new RemapData(23685, 0.5f));
        remapData.put(23694, new RemapData(23685, 0.25f));
        //////// Divine super attack potion(4) /////////
        remapData.put(23700, new RemapData(23697, 0.75f));
        remapData.put(23703, new RemapData(23697, 0.5f));
        remapData.put(23706, new RemapData(23697, 0.25f));
        //////// Divine ranging potion(4) /////////
        remapData.put(23736, new RemapData(23733, 0.75f));
        remapData.put(23739, new RemapData(23733, 0.5f));
        remapData.put(23742, new RemapData(23733, 0.25f));
        //////// Divine magic potion(4) /////////
        remapData.put(23748, new RemapData(23745, 0.75f));
        remapData.put(23751, new RemapData(23745, 0.5f));
        remapData.put(23754, new RemapData(23745, 0.25f));
        //////// Divine battlemage potion(4) /////////
        remapData.put(24626, new RemapData(24623, 0.75f));
        remapData.put(24629, new RemapData(24623, 0.5f));
        remapData.put(24632, new RemapData(24623, 0.25f));
        //////// Divine bastion potion(4) /////////
        remapData.put(24638, new RemapData(24635, 0.75f));
        remapData.put(24641, new RemapData(24635, 0.5f));
        remapData.put(24644, new RemapData(24635, 0.25f));
        //////// Digsite pendant (5) /////////
        remapData.put(11193, new RemapData(11194, 0.8f));
        remapData.put(11192, new RemapData(11194, 0.6f));
        remapData.put(11191, new RemapData(11194, 0.4f));
        remapData.put(11190, new RemapData(11194, 0.2f));
        //////// Defence potion(4) /////////
        remapData.put(133, new RemapData(2432, 0.75f));
        remapData.put(135, new RemapData(2432, 0.5f));
        remapData.put(137, new RemapData(2432, 0.25f));
        //////// Compost potion(4) /////////
        remapData.put(6472, new RemapData(6470, 0.75f));
        remapData.put(6474, new RemapData(6470, 0.5f));
        remapData.put(6476, new RemapData(6470, 0.25f));
        //////// Combat potion(4) /////////
        remapData.put(9741, new RemapData(9739, 0.75f));
        remapData.put(9743, new RemapData(9739, 0.5f));
        remapData.put(9745, new RemapData(9739, 0.25f));
        //////// Burning amulet(5) /////////
        remapData.put(21169, new RemapData(21166, 0.8f));
        remapData.put(21171, new RemapData(21166, 0.6f));
        remapData.put(21173, new RemapData(21166, 0.4f));
        remapData.put(21175, new RemapData(21166, 0.2f));
        //////// Blighted super restore(4) /////////
        remapData.put(24601, new RemapData(24598, 0.75f));
        remapData.put(24603, new RemapData(24598, 0.5f));
        remapData.put(24605, new RemapData(24598, 0.25f));
        //////// Ogre bellows (3) /////////
        remapData.put(2873, new RemapData(2872, 0.6666667f));
        remapData.put(2874, new RemapData(2872, 0.33333334f));
        remapData.put(2871, new RemapData(2872, 0.0f));
        //////// Battlemage potion(4) /////////
        remapData.put(22452, new RemapData(22449, 0.75f));
        remapData.put(22455, new RemapData(22449, 0.5f));
        remapData.put(22458, new RemapData(22449, 0.25f));
        //////// Bastion potion(4) /////////
        remapData.put(22464, new RemapData(22461, 0.75f));
        remapData.put(22467, new RemapData(22461, 0.5f));
        remapData.put(22470, new RemapData(22461, 0.25f));
        //////// Tomatoes(5) /////////
        remapData.put(5966, new RemapData(5968, 0.8f));
        remapData.put(5964, new RemapData(5968, 0.6f));
        remapData.put(5962, new RemapData(5968, 0.4f));
        remapData.put(5960, new RemapData(5968, 0.2f));
        //////// Strawberries(5) /////////
        remapData.put(5404, new RemapData(5406, 0.8f));
        remapData.put(5402, new RemapData(5406, 0.6f));
        remapData.put(5400, new RemapData(5406, 0.4f));
        remapData.put(5398, new RemapData(5406, 0.2f));
        //////// Oranges(5) /////////
        remapData.put(5394, new RemapData(5396, 0.8f));
        remapData.put(5392, new RemapData(5396, 0.6f));
        remapData.put(5390, new RemapData(5396, 0.4f));
        remapData.put(5388, new RemapData(5396, 0.2f));
        //////// Bananas(5) /////////
        remapData.put(5414, new RemapData(5416, 0.8f));
        remapData.put(5412, new RemapData(5416, 0.6f));
        remapData.put(5410, new RemapData(5416, 0.4f));
        remapData.put(5408, new RemapData(5416, 0.2f));
        //////// Apples(5) /////////
        remapData.put(5384, new RemapData(5386, 0.8f));
        remapData.put(5382, new RemapData(5386, 0.6f));
        remapData.put(5380, new RemapData(5386, 0.4f));
        remapData.put(5378, new RemapData(5386, 0.2f));
        //////// Attack potion(4) /////////
        remapData.put(121, new RemapData(2428, 0.75f));
        remapData.put(123, new RemapData(2428, 0.5f));
        remapData.put(125, new RemapData(2428, 0.25f));
        //////// Anti-venom+(4) /////////
        remapData.put(12915, new RemapData(12913, 0.75f));
        remapData.put(12917, new RemapData(12913, 0.5f));
        remapData.put(12919, new RemapData(12913, 0.25f));
        //////// Anti-venom(4) /////////
        remapData.put(12907, new RemapData(12905, 0.75f));
        remapData.put(12909, new RemapData(12905, 0.5f));
        remapData.put(12911, new RemapData(12905, 0.25f));
        //////// Antifire potion(4) /////////
        remapData.put(2454, new RemapData(2452, 0.75f));
        remapData.put(2456, new RemapData(2452, 0.5f));
        remapData.put(2458, new RemapData(2452, 0.25f));
        //////// Ancient brew(4) /////////
        remapData.put(26342, new RemapData(26340, 0.75f));
        remapData.put(26344, new RemapData(26340, 0.5f));
        remapData.put(26346, new RemapData(26340, 0.25f));
        //////// Antidote++(4) /////////
        remapData.put(5954, new RemapData(5952, 0.75f));
        remapData.put(5956, new RemapData(5952, 0.5f));
        remapData.put(5958, new RemapData(5952, 0.25f));
        //////// Antidote+(4) /////////
        remapData.put(5945, new RemapData(5943, 0.75f));
        remapData.put(5947, new RemapData(5943, 0.5f));
        remapData.put(5949, new RemapData(5943, 0.25f));
        //////// Antipoison(4) /////////
        remapData.put(175, new RemapData(2446, 0.75f));
        remapData.put(177, new RemapData(2446, 0.5f));
        remapData.put(179, new RemapData(2446, 0.25f));
        //////// Agility potion(4) /////////
        remapData.put(3034, new RemapData(3032, 0.75f));
        remapData.put(3036, new RemapData(3032, 0.5f));
        remapData.put(3038, new RemapData(3032, 0.25f));
        //////// Absorption (4) /////////
        remapData.put(11735, new RemapData(11734, 0.75f));
        remapData.put(11736, new RemapData(11734, 0.5f));
        remapData.put(11737, new RemapData(11734, 0.25f));
        //////// Abyssal bracelet(5) /////////
        remapData.put(11097, new RemapData(11095, 0.8f));
        remapData.put(11099, new RemapData(11095, 0.6f));
        remapData.put(11101, new RemapData(11095, 0.4f));
        remapData.put(11103, new RemapData(11095, 0.2f));
    }
}
