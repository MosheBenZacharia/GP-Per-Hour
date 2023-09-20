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

import static net.runelite.api.ItemID.*;

import lombok.AllArgsConstructor;

//Remaps the value of untradeable items to their commonly traded counterpart based on user config.
//Also remaps value of certain containers (hallowed sack, brimstone key, etc.) to their average value.
public class ValueRemapper {

    public static Float remapPrice(int itemId, GPPerHourPlugin plugin, GPPerHourConfig config)
    {
        if (itemId == BRIMSTONE_KEY)
        {
            //doesn't include fish because of how complex it is
            float value = 
                    (5f/60f)*100000f +// coins
                    (5f/60f)*plugin.getPrice(UNCUT_DIAMOND)*30F +
                    (5f/60f)*plugin.getPrice(UNCUT_RUBY)*30F +
                    (5f/60f)*plugin.getPrice(COAL)*400F +
                    (4f/60f)*plugin.getPrice(GOLD_ORE)*150F +
                    (4f/60f)*plugin.getPrice(DRAGON_ARROWTIPS)*125F +
                    (3f/60f)*plugin.getPrice(IRON_ORE)*425F +
                    (3f/60f)*plugin.getPrice(RUNE_FULL_HELM)*3F +
                    (3f/60f)*plugin.getPrice(RUNE_PLATELEGS)*1.5F +
                    (3f/60f)*plugin.getPrice(RUNE_PLATEBODY)*1.5F +
                    (2f/60f)*plugin.getPrice(RUNITE_ORE)*12.5F +
                    (2f/60f)*plugin.getPrice(STEEL_BAR)*400F +
                    (2f/60f)*plugin.getPrice(DRAGON_DART_TIP)*100F +
                    (2f/60f)*plugin.getPrice(MAGIC_LOGS)*140F +
                    (1f/60f)*plugin.getPrice(PALM_TREE_SEED)*3F +
                    (1f/60f)*plugin.getPrice(MAGIC_SEED)*2.5F +
                    (1f/60f)*plugin.getPrice(CELASTRUS_SEED)*3F +
                    (1f/60f)*plugin.getPrice(DRAGONFRUIT_TREE_SEED)*3F +
                    (1f/60f)*plugin.getPrice(REDWOOD_TREE_SEED)*1F +
                    (1f/60f)*plugin.getPrice(TORSTOL_SEED)*4F +
                    (1f/60f)*plugin.getPrice(SNAPDRAGON_SEED)*4F +
                    (1f/60f)*plugin.getPrice(RANARR_SEED)*4F +
                    (1f/60f)*plugin.getPrice(PURE_ESSENCE)*4500F +
                    (1f/200f)*plugin.getPrice(DRAGON_HASTA)*1F +
                    (1f/1000f)*plugin.getPrice(MYSTIC_ROBE_TOP_DUSK)*1f +
                    (1f/1000f)*plugin.getPrice(MYSTIC_ROBE_BOTTOM_DUSK)*1f +
                    (1f/1000f)*plugin.getPrice(MYSTIC_GLOVES_DUSK)*1f +
                    (1f/1000f)*plugin.getPrice(MYSTIC_BOOTS_DUSK)*1f +
                    (1f/1000f)*plugin.getPrice(MYSTIC_HAT_DUSK)*1f;
            return value;
        }
        else if (itemId == TOKKUL)
        {
            switch(config.tokkulValue())
            {
                case NO_VALUE:
                default:
                    return 0f;
                //overstock price for runes since they hit overstock quickly. overstock price same with/without gloves.
                case BUY_CHAOS_RUNE:
                    return plugin.getPrice(CHAOS_RUNE) / 9f;
                case BUY_DEATH_RUNE:
                    return plugin.getPrice(DEATH_RUNE) / 18f;
                case SELL_OBBY_CAPE:
                    return plugin.getPrice(OBSIDIAN_CAPE) / (config.tokkulKaramjaGloves() ? 78000f : 90000f);
                case SELL_TOKTZ_KET_XIL:
                    return plugin.getPrice(TOKTZKETXIL) / (config.tokkulKaramjaGloves() ? 58500f : 67500f);
                case SELL_TOKTZ_MEJ_TAL:
                    return plugin.getPrice(TOKTZMEJTAL) / (config.tokkulKaramjaGloves() ? 45500f : 52500f);
                case SELL_UNCUT_ONYX:
                    return plugin.getPrice(UNCUT_ONYX) / (config.tokkulKaramjaGloves() ? 260000f : 300000f);
            }
        }
        else if (itemId == CRYSTAL_SHARD)
        {
            switch(config.crystalShardValue())
            {
                case NO_VALUE:
                default:
                    return 0f;
                case BUY_TELEPORT_SEED:
                    return plugin.getPrice(ENHANCED_CRYSTAL_TELEPORT_SEED) / 150f;
                case SELL_BASTION:
                    return (plugin.getPrice(DIVINE_BASTION_POTION4) - plugin.getPrice(BASTION_POTION4))/0.4f;
                case SELL_BATTLEMAGE:
                    return (plugin.getPrice(DIVINE_BATTLEMAGE_POTION4) - plugin.getPrice(BATTLEMAGE_POTION4))/0.4f;
                case SELL_MAGIC:
                    return (plugin.getPrice(DIVINE_MAGIC_POTION4) - plugin.getPrice(MAGIC_POTION4))/0.4f;
                case SELL_RANGING:
                    return (plugin.getPrice(DIVINE_RANGING_POTION4) - plugin.getPrice(RANGING_POTION4))/0.4f;
                case SELL_SUPER_ATTACK:
                    return (plugin.getPrice(DIVINE_SUPER_ATTACK_POTION4) - plugin.getPrice(SUPER_ATTACK4))/0.4f;
                case SELL_SUPER_COMBAT:
                    return (plugin.getPrice(DIVINE_SUPER_COMBAT_POTION4) - plugin.getPrice(SUPER_COMBAT_POTION4))/0.4f;
                case SELL_SUPER_DEFENCE:
                    return (plugin.getPrice(DIVINE_SUPER_DEFENCE_POTION4) - plugin.getPrice(SUPER_DEFENCE4))/0.4f;
                case SELL_SUPER_STRENGTH:
                    return (plugin.getPrice(DIVINE_SUPER_STRENGTH_POTION4) - plugin.getPrice(SUPER_STRENGTH4))/0.4f;
            }
        }
        else if (itemId == CRYSTAL_DUST_23964)
        {
            switch(config.crystalDustValue())
            {
                case NO_VALUE:
                default:
                    return 0f;
                case SELL_BASTION:
                    return (plugin.getPrice(DIVINE_BASTION_POTION4) - plugin.getPrice(BASTION_POTION4))/4f;
                case SELL_BATTLEMAGE:
                    return (plugin.getPrice(DIVINE_BATTLEMAGE_POTION4) - plugin.getPrice(BATTLEMAGE_POTION4))/4f;
                case SELL_MAGIC:
                    return (plugin.getPrice(DIVINE_MAGIC_POTION4) - plugin.getPrice(MAGIC_POTION4))/4f;
                case SELL_RANGING:
                    return (plugin.getPrice(DIVINE_RANGING_POTION4) - plugin.getPrice(RANGING_POTION4))/4f;
                case SELL_SUPER_ATTACK:
                    return (plugin.getPrice(DIVINE_SUPER_ATTACK_POTION4) - plugin.getPrice(SUPER_ATTACK4))/4f;
                case SELL_SUPER_COMBAT:
                    return (plugin.getPrice(DIVINE_SUPER_COMBAT_POTION4) - plugin.getPrice(SUPER_COMBAT_POTION4))/4f;
                case SELL_SUPER_DEFENCE:
                    return (plugin.getPrice(DIVINE_SUPER_DEFENCE_POTION4) - plugin.getPrice(SUPER_DEFENCE4))/4f;
                case SELL_SUPER_STRENGTH:
                    return (plugin.getPrice(DIVINE_SUPER_STRENGTH_POTION4) - plugin.getPrice(SUPER_STRENGTH4))/4f;
            }
        }
        else if (itemId == MERMAIDS_TEAR)
        {
            switch(config.mermaidsTearValue())
            {
                case NO_VALUE:
                default:
                    return 0f;
                case SELL_MERFOLK_TRIDENT:
                    return plugin.getPrice(MERFOLK_TRIDENT)/400f;
            }
        }
        else if (itemId == STARDUST)
        {
            switch(config.stardustValue())
            {
                case NO_VALUE:
                default:
                    return 0f;
                case SELL_SOFT_CLAY_PACK:
                    return plugin.getPrice(SOFT_CLAY_PACK)/150f;
                case SELL_BAG_FULL_OF_GEMS:
                    return plugin.getPrice(BAG_FULL_OF_GEMS)/300f;
            }
        }
        else if (itemId == UNIDENTIFIED_MINERALS)
        {
            switch(config.unidentifiedMineralsValue())
            {
                case NO_VALUE:
                default:
                    return 0f;
                case SELL_SOFT_CLAY_PACK:
                    return plugin.getPrice(SOFT_CLAY_PACK)/10f;
                case SELL_BAG_FULL_OF_GEMS:
                    return plugin.getPrice(BAG_FULL_OF_GEMS)/20f;
            }
        }
        else if (itemId == GOLDEN_NUGGET)
        {
            switch(config.goldenNuggetValue())
            {
                case NO_VALUE:
                default:
                    return 0f;
                case SELL_SOFT_CLAY_PACK:
                    return plugin.getPrice(SOFT_CLAY_PACK)/10f;
                case SELL_BAG_FULL_OF_GEMS:
                    return plugin.getPrice(BAG_FULL_OF_GEMS)/40f;
            }
        }
        else if (itemId == SOFT_CLAY_PACK || itemId == SOFT_CLAY_PACK_12010 || itemId == SOFT_CLAY_PACK_24851 || itemId == SOFT_CLAY_PACK_25533)
        {
            return plugin.getPrice(SOFT_CLAY) * 100f;
        }
        else if (itemId == BAG_FULL_OF_GEMS || itemId == BAG_FULL_OF_GEMS_24853 || itemId == BAG_FULL_OF_GEMS_25537)
        {
            return  plugin.getPrice(UNCUT_SAPPHIRE)*(40f / 2.003f) + 
                    plugin.getPrice(UNCUT_EMERALD)*(40f / 2.884f) + 
                    plugin.getPrice(UNCUT_RUBY)*(40f / 8.475f) + 
                    plugin.getPrice(UNCUT_DIAMOND)*(40f / 32.36f) + 
                    plugin.getPrice(UNCUT_DRAGONSTONE)*(40f / 161.3f) + 
                    plugin.getPrice(UNCUT_ONYX)*(40f / 100000000f);
        }
        else if (itemId == ABYSSAL_PEARLS)
        {
            switch(config.abyssalPearlsValue())
            {
                case NO_VALUE:
                default:
                    return 0f;
                case SELL_RING_OF_THE_ELEMENTS:
                    return plugin.getPrice(RING_OF_THE_ELEMENTS)/400f;
            }
        }
        else if (itemId == HALLOWED_MARK)
        {
            switch(config.hallowedMarkValue())
            {
                case NO_VALUE:
                default:
                    return 0f;
                case SELL_HALLOWED_SACK:
                    return plugin.getPrice(HALLOWED_SACK)/100f;
            }
        }
        else if (itemId == HALLOWED_SACK)
        {
            return plugin.getPrice(MONKS_ROBE_TOP)*1f*.5f+
            plugin.getPrice(MONKS_ROBE)*1f*.5f+
            plugin.getPrice(HOLY_SYMBOL)*1f*.5f+
            plugin.getPrice(AIR_RUNE)*625f*.5f+
            plugin.getPrice(FIRE_RUNE)*625f*.5f+
            plugin.getPrice(CHAOS_RUNE)*37.5f*.5f+
            plugin.getPrice(MITHRIL_BOLTS)*100f*.5f+
            plugin.getPrice(PRAYER_POTION2)*1f*.5f+
            plugin.getPrice(WHITE_LILY)*1f*.5f+
            1f*2250f*.5f+//coins
            plugin.getPrice(ADAMANT_2H_SWORD)*1f*.1f+
            plugin.getPrice(ADAMANT_PLATEBODY)*1f*.1f+
            plugin.getPrice(COSMIC_RUNE)*80f*.1f+
            plugin.getPrice(DEATH_RUNE)*80f*.1f+
            plugin.getPrice(NATURE_RUNE)*80f*.1f+
            plugin.getPrice(ADAMANT_BOLTS)*125f*.1f+
            plugin.getPrice(MONKFISH)*2f*.1f+
            plugin.getPrice(PRAYER_POTION4)*1f*.1f+
            plugin.getPrice(GRIMY_RANARR_WEED)*1.5f*.1f+
            1f*10000f*.1f+//coins
            plugin.getPrice(RUNE_2H_SWORD)*1f*.2f+
            plugin.getPrice(RUNE_PLATEBODY)*1f*.2f+
            plugin.getPrice(LAW_RUNE)*200f*.2f+
            plugin.getPrice(BLOOD_RUNE)*200f*.2f+
            plugin.getPrice(SOUL_RUNE)*200f*.2f+
            plugin.getPrice(RUNITE_BOLTS)*200f*.2f+
            plugin.getPrice(MONKFISH)*4f*.2f+
            plugin.getPrice(SANFEW_SERUM4)*1.5f*.2f+
            plugin.getPrice(RANARR_SEED)*1.5f*.2f+
            1f*20000f*.2f;//coins
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

}
