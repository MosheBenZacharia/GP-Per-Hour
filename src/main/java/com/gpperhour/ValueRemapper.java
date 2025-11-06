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
package com.gpperhour;

//import static net.runelite.api.gameval.ItemID.*;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
        import net.runelite.api.gameval.ItemID;

//Remaps the value of untradeable items to their commonly traded counterpart based on user config.
//Also remaps value of certain containers (hallowed sack, brimstone key, etc.) to their average value.
public class ValueRemapper {

    //Used for when a value maps 1:1 with another value
    private static final Map<Integer, Integer> directValueRemaps;

    static
    {
        directValueRemaps = new HashMap<>();

        //Seedling to sapling
        directValueRemaps.put(ItemID.PLANTPOT_ACORN, ItemID.PLANTPOT_OAK_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_WILLOW_SEED, ItemID.PLANTPOT_WILLOW_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_MAPLE_SEED, ItemID.PLANTPOT_MAPLE_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_YEW_SEED, ItemID.PLANTPOT_YEW_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_MAGIC_TREE_SEED, ItemID.PLANTPOT_MAGIC_TREE_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_REDWOOD_TREE_SEED, ItemID.PLANTPOT_REDWOOD_TREE_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_SPIRIT_TREE_SEED, ItemID.PLANTPOT_SPIRIT_TREE_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_CRYSTAL_TREE_SEED, ItemID.PLANTPOT_CRYSTAL_TREE_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_APPLE_SEED, ItemID.PLANTPOT_APPLE_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_BANANA_SEED, ItemID.PLANTPOT_BANANA_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_ORANGE_SEED, ItemID.PLANTPOT_ORANGE_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_CURRY_SEED, ItemID.PLANTPOT_CURRY_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_PINEAPPLE_SEED, ItemID.PLANTPOT_PINEAPPLE_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_PAPAYA_SEED, ItemID.PLANTPOT_PAPAYA_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_PALM_SEED, ItemID.PLANTPOT_PALM_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_DRAGONFRUIT_SEED, ItemID.PLANTPOT_DRAGONFRUIT_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_TEAK_SEED, ItemID.PLANTPOT_TEAK_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_MAHOGANY_SEED, ItemID.PLANTPOT_MAHOGANY_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_CALQUAT_SEED, ItemID.PLANTPOT_CALQUAT_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_CELASTRUS_TREE_SEED, ItemID.PLANTPOT_CELASTRUS_TREE_SAPLING);
        //Watered seedling to sapling
        directValueRemaps.put(ItemID.PLANTPOT_ACORN_WATERED, ItemID.PLANTPOT_OAK_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_WILLOW_SEED_WATERED, ItemID.PLANTPOT_WILLOW_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_MAPLE_SEED_WATERED, ItemID.PLANTPOT_MAPLE_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_YEW_SEED_WATERED, ItemID.PLANTPOT_YEW_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_MAGIC_TREE_SEED_WATERED, ItemID.PLANTPOT_MAGIC_TREE_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_REDWOOD_TREE_SEED_WATERED, ItemID.PLANTPOT_REDWOOD_TREE_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_SPIRIT_TREE_SEED_WATERED, ItemID.PLANTPOT_SPIRIT_TREE_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_CRYSTAL_TREE_SEED_WATERED, ItemID.PLANTPOT_CRYSTAL_TREE_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_APPLE_SEED_WATERED, ItemID.PLANTPOT_APPLE_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_BANANA_SEED_WATERED, ItemID.PLANTPOT_BANANA_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_ORANGE_SEED_WATERED, ItemID.PLANTPOT_ORANGE_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_CURRY_SEED_WATERED, ItemID.PLANTPOT_CURRY_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_PINEAPPLE_SEED_WATERED, ItemID.PLANTPOT_PINEAPPLE_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_PAPAYA_SEED_WATERED, ItemID.PLANTPOT_PAPAYA_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_PALM_SEED_WATERED, ItemID.PLANTPOT_PALM_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_DRAGONFRUIT_SEED_WATERED, ItemID.PLANTPOT_DRAGONFRUIT_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_TEAK_SEED_WATERED, ItemID.PLANTPOT_TEAK_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_MAHOGANY_SEED_WATERED, ItemID.PLANTPOT_MAHOGANY_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_CALQUAT_SEED_WATERED, ItemID.PLANTPOT_CALQUAT_SAPLING);
        directValueRemaps.put(ItemID.PLANTPOT_CELASTRUS_TREE_SEED_WATERED, ItemID.PLANTPOT_CELASTRUS_TREE_SAPLING);
    }

    public static Float remapPrice(int itemId, GPPerHourPlugin plugin, GPPerHourConfig config)
    {
        if (directValueRemaps.containsKey(itemId))
        {
            return plugin.getPrice(directValueRemaps.get(itemId));
        }
        if (itemId == ItemID.KONAR_KEY)
        {
            if (config.brimstoneKeyValue() == BrimstoneKeyOverride.NO_VALUE)
            {
                return 0f;
            }
            //doesn't include fish because of how complex it is
            float value = 
                    (5f/60f)*100000f +// coins
                    (5f/60f)*plugin.getPrice(ItemID.UNCUT_DIAMOND)*30F +
                    (5f/60f)*plugin.getPrice(ItemID.UNCUT_RUBY)*30F +
                    (5f/60f)*plugin.getPrice(ItemID.COAL)*400F +
                    (4f/60f)*plugin.getPrice(ItemID.GOLD_ORE)*150F +
                    (4f/60f)*plugin.getPrice(ItemID.DRAGON_ARROWHEADS)*125F +
                    (3f/60f)*plugin.getPrice(ItemID.IRON_ORE)*425F +
                    (3f/60f)*plugin.getPrice(ItemID.RUNE_FULL_HELM)*3F +
                    (3f/60f)*plugin.getPrice(ItemID.RUNE_PLATELEGS)*1.5F +
                    (3f/60f)*plugin.getPrice(ItemID.RUNE_PLATEBODY)*1.5F +
                    (2f/60f)*plugin.getPrice(ItemID.RUNITE_ORE)*12.5F +
                    (2f/60f)*plugin.getPrice(ItemID.STEEL_BAR)*400F +
                    (2f/60f)*plugin.getPrice(ItemID.DRAGON_DART_TIP)*100F +
                    (2f/60f)*plugin.getPrice(ItemID.MAGIC_LOGS)*140F +
                    (1f/60f)*plugin.getPrice(ItemID.PALM_TREE_SEED)*3F +
                    (1f/60f)*plugin.getPrice(ItemID.MAGIC_TREE_SEED)*2.5F +
                    (1f/60f)*plugin.getPrice(ItemID.CELASTRUS_TREE_SEED)*3F +
                    (1f/60f)*plugin.getPrice(ItemID.DRAGONFRUIT_TREE_SEED)*3F +
                    (1f/60f)*plugin.getPrice(ItemID.REDWOOD_TREE_SEED)*1F +
                    (1f/60f)*plugin.getPrice(ItemID.TORSTOL_SEED)*4F +
                    (1f/60f)*plugin.getPrice(ItemID.SNAPDRAGON_SEED)*4F +
                    (1f/60f)*plugin.getPrice(ItemID.RANARR_SEED)*4F +
                    (1f/60f)*plugin.getPrice(ItemID.BLANKRUNE_HIGH)*4500F +
                    (1f/200f)*plugin.getPrice(ItemID.BRUT_DRAGON_SPEAR)*1F +
                    (1f/1000f)*plugin.getPrice(ItemID.MYSTIC_ROBE_TOP_DUSK)*1f +
                    (1f/1000f)*plugin.getPrice(ItemID.MYSTIC_ROBE_BOTTOM_DUSK)*1f +
                    (1f/1000f)*plugin.getPrice(ItemID.MYSTIC_GLOVES_DUSK)*1f +
                    (1f/1000f)*plugin.getPrice(ItemID.MYSTIC_BOOTS_DUSK)*1f +
                    (1f/1000f)*plugin.getPrice(ItemID.MYSTIC_HAT_DUSK)*1f;
            return value;
        }
        else if (itemId == ItemID.TZHAAR_TOKEN)
        {
            switch(config.tokkulValue())
            {
                case NO_VALUE:
                default:
                    return 0f;
                //overstock price for runes since they hit overstock quickly. overstock price same with/without gloves.
                case BUY_CHAOS_RUNE:
                    return plugin.getPrice(ItemID.CHAOSRUNE) / 9f;
                case BUY_DEATH_RUNE:
                    return plugin.getPrice(ItemID.DEATHRUNE) / 18f;
                case SELL_OBBY_CAPE:
                    return plugin.getPrice(ItemID.TZHAAR_CAPE_OBSIDIAN) / (config.tokkulKaramjaGloves() ? 78000f : 90000f);
                case SELL_TOKTZ_KET_XIL:
                    return plugin.getPrice(ItemID.TZHAAR_SPIKESHIELD) / (config.tokkulKaramjaGloves() ? 58500f : 67500f);
                case SELL_TOKTZ_MEJ_TAL:
                    return plugin.getPrice(ItemID.TZHAAR_STAFF) / (config.tokkulKaramjaGloves() ? 45500f : 52500f);
                case SELL_UNCUT_ONYX:
                    return plugin.getPrice(ItemID.UNCUT_ONYX) / (config.tokkulKaramjaGloves() ? 260000f : 300000f);
            }
        }
        else if (itemId == ItemID.PRIF_CRYSTAL_SHARD)
        {
            switch(config.crystalShardValue())
            {
                case NO_VALUE:
                default:
                    return 0f;
                case BUY_TELEPORT_SEED:
                    return plugin.getPrice(ItemID.PRIF_TELEPORT_SEED) / 150f;
                case SELL_BASTION:
                    return (plugin.getPrice(ItemID._4DOSEDIVINEBASTION) - plugin.getPrice(ItemID._4DOSEBASTION))/0.4f;
                case SELL_BATTLEMAGE:
                    return (plugin.getPrice(ItemID._4DOSEDIVINEBATTLEMAGE) - plugin.getPrice(ItemID._4DOSEBATTLEMAGE))/0.4f;
                case SELL_MAGIC:
                    return (plugin.getPrice(ItemID._4DOSEDIVINEMAGIC) - plugin.getPrice(ItemID._4DOSE1MAGIC))/0.4f;
                case SELL_RANGING:
                    return (plugin.getPrice(ItemID._4DOSEDIVINERANGE) - plugin.getPrice(ItemID._4DOSERANGERSPOTION))/0.4f;
                case SELL_SUPER_ATTACK:
                    return (plugin.getPrice(ItemID._4DOSEDIVINEATTACK) - plugin.getPrice(ItemID._4DOSE2ATTACK))/0.4f;
                case SELL_SUPER_COMBAT:
                    return (plugin.getPrice(ItemID._4DOSEDIVINECOMBAT) - plugin.getPrice(ItemID._4DOSE2COMBAT))/0.4f;
                case SELL_SUPER_DEFENCE:
                    return (plugin.getPrice(ItemID._4DOSEDIVINEDEFENCE) - plugin.getPrice(ItemID._4DOSE2DEFENSE))/0.4f;
                case SELL_SUPER_STRENGTH:
                    return (plugin.getPrice(ItemID._4DOSEDIVINESTRENGTH) - plugin.getPrice(ItemID._4DOSE2STRENGTH))/0.4f;
            }
        }
        else if (itemId == ItemID.PRIF_CRYSTAL_SHARD_CRUSHED)
        {
            switch(config.crystalDustValue())
            {
                case NO_VALUE:
                default:
                    return 0f;
                case SELL_BASTION:
                    return (plugin.getPrice(ItemID._4DOSEDIVINEBASTION) - plugin.getPrice(ItemID._4DOSEBASTION))/4f;
                case SELL_BATTLEMAGE:
                    return (plugin.getPrice(ItemID._4DOSEDIVINEBATTLEMAGE) - plugin.getPrice(ItemID._4DOSEBATTLEMAGE))/4f;
                case SELL_MAGIC:
                    return (plugin.getPrice(ItemID._4DOSEDIVINEMAGIC) - plugin.getPrice(ItemID._4DOSE1MAGIC))/4f;
                case SELL_RANGING:
                    return (plugin.getPrice(ItemID._4DOSEDIVINERANGE) - plugin.getPrice(ItemID._4DOSERANGERSPOTION))/4f;
                case SELL_SUPER_ATTACK:
                    return (plugin.getPrice(ItemID._4DOSEDIVINEATTACK) - plugin.getPrice(ItemID._4DOSE2ATTACK))/4f;
                case SELL_SUPER_COMBAT:
                    return (plugin.getPrice(ItemID._4DOSEDIVINECOMBAT) - plugin.getPrice(ItemID._4DOSE2COMBAT))/4f;
                case SELL_SUPER_DEFENCE:
                    return (plugin.getPrice(ItemID._4DOSEDIVINEDEFENCE) - plugin.getPrice(ItemID._4DOSE2DEFENSE))/4f;
                case SELL_SUPER_STRENGTH:
                    return (plugin.getPrice(ItemID._4DOSEDIVINESTRENGTH) - plugin.getPrice(ItemID._4DOSE2STRENGTH))/4f;
            }
        }
        else if (itemId == ItemID.FOSSIL_MERMAID_TEAR)
        {
            switch(config.mermaidsTearValue())
            {
                case NO_VALUE:
                default:
                    return 0f;
                case SELL_MERFOLK_TRIDENT:
                    return plugin.getPrice(ItemID.MERFOLK_TRIDENT)/400f;
            }
        }
        else if (itemId == ItemID.STAR_DUST)
        {
            switch(config.stardustValue())
            {
                case NO_VALUE:
                default:
                    return 0f;
                case SELL_SOFT_CLAY_PACK:
                    return plugin.getPrice(ItemID.PACK_SOFTCLAY)/150f;
                case SELL_BAG_FULL_OF_GEMS:
                    return plugin.getPrice(ItemID.REWARD_GEM_BAG)/300f;
            }
        }
        else if (itemId == ItemID.MGUILD_MINERALS)
        {
            switch(config.unidentifiedMineralsValue())
            {
                case NO_VALUE:
                default:
                    return 0f;
                case SELL_SOFT_CLAY_PACK:
                    return plugin.getPrice(ItemID.PACK_SOFTCLAY)/10f;
                case SELL_BAG_FULL_OF_GEMS:
                    return plugin.getPrice(ItemID.REWARD_GEM_BAG)/20f;
            }
        }
        else if (itemId == ItemID.MOTHERLODE_NUGGET)
        {
            switch(config.goldenNuggetValue())
            {
                case NO_VALUE:
                default:
                    return 0f;
                case SELL_SOFT_CLAY_PACK:
                    return plugin.getPrice(ItemID.PACK_SOFTCLAY)/10f;
                case SELL_BAG_FULL_OF_GEMS:
                    return plugin.getPrice(ItemID.REWARD_GEM_BAG)/40f;
            }
        }
        else if (itemId == ItemID.PACK_SOFTCLAY || itemId == ItemID.CERT_PACK_SOFTCLAY || itemId == ItemID.PACK_SOFTCLAY_GUILD || itemId == ItemID.STAR_PACK_SOFTCLAY)
        {
            return plugin.getPrice(ItemID.SOFTCLAY) * 100f;
        }
        else if (itemId == ItemID.REWARD_GEM_BAG || itemId == ItemID.REWARD_GEM_BAG_GUILD || itemId == ItemID.STAR_REWARD_GEM_BAG)
        {
            return  plugin.getPrice(ItemID.UNCUT_SAPPHIRE)*(40f / 2.003f) +
                    plugin.getPrice(ItemID.UNCUT_EMERALD)*(40f / 2.884f) +
                    plugin.getPrice(ItemID.UNCUT_RUBY)*(40f / 8.475f) +
                    plugin.getPrice(ItemID.UNCUT_DIAMOND)*(40f / 32.36f) +
                    plugin.getPrice(ItemID.UNCUT_DRAGONSTONE)*(40f / 161.3f) +
                    plugin.getPrice(ItemID.UNCUT_ONYX)*(40f / 100000000f);
        }
        else if (itemId == ItemID.ABYSSAL_PEARL)
        {
            switch(config.abyssalPearlsValue())
            {
                case NO_VALUE:
                default:
                    return 0f;
                case SELL_RING_OF_THE_ELEMENTS:
                    return plugin.getPrice(ItemID.RING_OF_ELEMENTS)/400f;
            }
        }
        else if (itemId == ItemID.HALLOWED_MARK)
        {
            switch(config.hallowedMarkValue())
            {
                case NO_VALUE:
                default:
                    return 0f;
                case SELL_HALLOWED_SACK:
                    return plugin.getPrice(ItemID.HALLOWED_SACK)/100f;
            }
        }
        else if (itemId == ItemID.HALLOWED_SACK)
        {
            return plugin.getPrice(ItemID.MONKROBETOP)*1f*.5f+
            plugin.getPrice(ItemID.MONKROBEBOTTOM)*1f*.5f+
            plugin.getPrice(ItemID.BLESSEDSTAR)*1f*.5f+
            plugin.getPrice(ItemID.AIRRUNE)*625f*.5f+
            plugin.getPrice(ItemID.FIRERUNE)*625f*.5f+
            plugin.getPrice(ItemID.CHAOSRUNE)*37.5f*.5f+
            plugin.getPrice(ItemID.XBOWS_CROSSBOW_BOLTS_MITHRIL)*100f*.5f+
            plugin.getPrice(ItemID._2DOSEPRAYERRESTORE)*1f*.5f+
            plugin.getPrice(ItemID.WHITELILLY)*1f*.5f+
            1f*2250f*.5f+//coins
            plugin.getPrice(ItemID.ADAMANT_2H_SWORD)*1f*.1f+
            plugin.getPrice(ItemID.ADAMANT_PLATEBODY)*1f*.1f+
            plugin.getPrice(ItemID.COSMICRUNE)*80f*.1f+
            plugin.getPrice(ItemID.DEATHRUNE)*80f*.1f+
            plugin.getPrice(ItemID.NATURERUNE)*80f*.1f+
            plugin.getPrice(ItemID.XBOWS_CROSSBOW_BOLTS_ADAMANTITE)*125f*.1f+
            plugin.getPrice(ItemID.MONKFISH)*2f*.1f+
            plugin.getPrice(ItemID._4DOSEPRAYERRESTORE)*1f*.1f+
            plugin.getPrice(ItemID.UNIDENTIFIED_RANARR)*1.5f*.1f+
            1f*10000f*.1f+//coins
            plugin.getPrice(ItemID.RUNE_2H_SWORD)*1f*.2f+
            plugin.getPrice(ItemID.RUNE_PLATEBODY)*1f*.2f+
            plugin.getPrice(ItemID.LAWRUNE)*200f*.2f+
            plugin.getPrice(ItemID.BLOODRUNE)*200f*.2f+
            plugin.getPrice(ItemID.SOULRUNE)*200f*.2f+
            plugin.getPrice(ItemID.XBOWS_CROSSBOW_BOLTS_RUNITE)*200f*.2f+
            plugin.getPrice(ItemID.MONKFISH)*4f*.2f+
            plugin.getPrice(ItemID.SANFEW_SALVE_4_DOSE)*1.5f*.2f+
            plugin.getPrice(ItemID.RANARR_SEED)*1.5f*.2f+
            1f*20000f*.2f;//coins
        }
        else if(itemId == ItemID.GRANITE_CANNONBALL)
        {
            return plugin.getPrice(ItemID.MCANNONBALL);
        }
        else if (itemId == ItemID.MINNOW)
        {
            return plugin.getPrice(ItemID.RAW_SHARK)/40f;
        }

        return null;
    }

    @AllArgsConstructor
    public enum TokkulOverride {
        NO_VALUE("No Value (Default)"),
        BUY_CHAOS_RUNE      ("Chaos Rune (Buy)"),
        BUY_DEATH_RUNE      ("Death Rune (Buy)"),
        SELL_UNCUT_ONYX     ("Uncut Onyx (Sell)"),
        SELL_TOKTZ_MEJ_TAL  ("Toktz-mej-tal (Sell)"),
        SELL_TOKTZ_KET_XIL  ("Toktz-ket-xil (Sell)"),
        SELL_OBBY_CAPE      ("Obsidian cape (Sell)");
    
        private final String configName;
        @Override
        public String toString() { return configName; }
    }

    @AllArgsConstructor
    public enum CrystalShardOverride {
        NO_VALUE("No Value (Default)"),
        BUY_TELEPORT_SEED       ("Teleport Seed (Buy)"),
        SELL_SUPER_ATTACK       ("Super Attack (Sell)"),
        SELL_SUPER_STRENGTH     ("Super Strength (Sell)"),
        SELL_SUPER_DEFENCE      ("Super Defence (Sell)"),
        SELL_RANGING            ("Ranging (Sell)"),
        SELL_MAGIC              ("Magic (Sell)"),
        SELL_BASTION            ("Bastion (Sell)"),
        SELL_BATTLEMAGE         ("Battlemage (Sell)"),
        SELL_SUPER_COMBAT       ("Super Combat (Sell)");
    
        private final String configName;
        @Override
        public String toString() { return configName; }
    }

    @AllArgsConstructor
    public enum CrystalDustOverride {
        NO_VALUE("No Value (Default)"),
        SELL_SUPER_ATTACK       ("Super Attack (Sell)"),
        SELL_SUPER_STRENGTH     ("Super Strength (Sell)"),
        SELL_SUPER_DEFENCE      ("Super Defence (Sell)"),
        SELL_RANGING            ("Ranging (Sell)"),
        SELL_MAGIC              ("Magic (Sell)"),
        SELL_BASTION            ("Bastion (Sell)"),
        SELL_BATTLEMAGE         ("Battlemage (Sell)"),
        SELL_SUPER_COMBAT       ("Super Combat (Sell)");
    
        private final String configName;
        @Override
        public String toString() { return configName; }
    }

    @AllArgsConstructor
    public enum MermaidsTearOverride {
        NO_VALUE                    ("No Value (Default)"),
        SELL_MERFOLK_TRIDENT        ("Merfolk Trident");
    
        private final String configName;
        @Override
        public String toString() { return configName; }
    }

    @AllArgsConstructor
    public enum UnidentifiedMineralsOverride {
        NO_VALUE                        ("No Value (Default)"),
        SELL_SOFT_CLAY_PACK             ("Soft Clay Pack"),
        SELL_BAG_FULL_OF_GEMS           ("Bag Full of Gems");
    
        private final String configName;
        @Override
        public String toString() { return configName; }
    }

    @AllArgsConstructor
    public enum StardustOverride {
        NO_VALUE                        ("No Value (Default)"),
        SELL_SOFT_CLAY_PACK             ("Soft Clay Pack"),
        SELL_BAG_FULL_OF_GEMS           ("Bag Full of Gems");
    
        private final String configName;
        @Override
        public String toString() { return configName; }
    }

    @AllArgsConstructor
    public enum GoldenNuggetOverride {
        NO_VALUE                        ("No Value (Default)"),
        SELL_SOFT_CLAY_PACK             ("Soft Clay Pack"),
        SELL_BAG_FULL_OF_GEMS           ("Bag Full of Gems");
    
        private final String configName;
        @Override
        public String toString() { return configName; }
    }

    @AllArgsConstructor
    public enum HallowedMarkOverride {
        NO_VALUE                        ("No Value (Default)"),
        SELL_HALLOWED_SACK              ("Hallowed Sack");
    
        private final String configName;
        @Override
        public String toString() { return configName; }
    }

    @AllArgsConstructor
    public enum AbyssalPearlsOverride {
        NO_VALUE                        ("No Value (Default)"),
        SELL_RING_OF_THE_ELEMENTS       ("Ring of the Elements");
    
        private final String configName;
        @Override
        public String toString() { return configName; }
    }

    @AllArgsConstructor
    public enum BrimstoneKeyOverride {
        NO_VALUE                        ("No Value"),
        AVERAGE_VALUE                   ("Avg Value (Default)");
    
        private final String configName;
        @Override
        public String toString() { return configName; }
    }

}
