package com.gpperhour;

import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GPPerHourPluginTest {

    @Mock
    private ItemManager itemManager;
    @Mock
    private GPPerHourConfig config;
    @Mock
    private Client client; 
    @Mock
    private LootingBagManager lootingBagManager;
    @Mock
    private WeaponChargesManager weaponChargesManager;
    @Mock
    private ChargedItemManager chargedItemManager;

    @Mock
    private ItemContainer mockEquipmentContainer;

    private GPPerHourPlugin plugin;

    // Item IDs
    protected static final int DIZANAS_MAX_CAPE_ID = ItemID.DIZANAS_MAX_CAPE;
    protected static final int RUBY_DRAGON_BOLTS_E_ID = ItemID.RUBY_DRAGON_BOLTS_E;
    protected static final int FIRE_CAPE_ID = ItemID.FIRE_CAPE;
    protected static final int TINDERBOX_ID = ItemID.TINDERBOX; // Example non-ammo
    protected static final int ADAMANT_ARROW_ID = ItemID.ADAMANT_ARROW; // Example standard ammo


    // Helper to mock ItemComposition
    protected ItemComposition mockItemComposition(String name, int id) {
        ItemComposition itemComp = mock(ItemComposition.class);
        when(itemComp.getName()).thenReturn(name);
        lenient().when(itemComp.getId()).thenReturn(id); 
        return itemComp;
    }

    // Helper to mock Item
    protected Item mockItem(int id, int quantity) {
        // If id is -1, it usually means an empty slot in some contexts, 
        // but ItemContainer.getItem() can return null for empty.
        // Let's return null if id is -1 to simulate an empty slot more directly for getItem().
        if (id == -1) return null; 
        Item item = mock(Item.class);
        when(item.getId()).thenReturn(id);
        when(item.getQuantity()).thenReturn(quantity);
        return item;
    }

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        plugin = new GPPerHourPlugin();

        setField(plugin, "itemManager", itemManager);
        setField(plugin, "config", config);
        setField(plugin, "client", client); // Though not directly used by refreshQtyMap, good for completeness
        setField(plugin, "lootingBagManager", lootingBagManager);
        setField(plugin, "weaponChargesManager", weaponChargesManager);
        setField(plugin, "chargedItemManager", chargedItemManager);
        
        // Initialize plugin.ignoredItems HashSet by calling the plugin's method
        when(config.ignoredItems()).thenReturn(""); // Default: no ignored items
        plugin.refreshIgnoredItems();

        // Mock container type for equipment
        when(mockEquipmentContainer.getInventoryID()).thenReturn(InventoryID.EQUIPMENT);

        // Ensure component adders don't interfere with the map for these specific tests
        lenient().doAnswer(invocation -> null).when(lootingBagManager).addLootingBagContents(anyMap());
        lenient().doAnswer(invocation -> null).when(weaponChargesManager).addChargedWeaponComponents(anyMap());
        lenient().doAnswer(invocation -> null).when(chargedItemManager).addChargedItemComponents(anyMap());
    }

    // Reflection helper to set private fields in the plugin instance
    private void setField(Object target, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = GPPerHourPlugin.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
    
    // Helper to set up the item array for mockEquipmentContainer.getItems()
    protected void setupContainerItemsArray(Item... itemsInSlots) {
        Item[] allEquipmentItems = new Item[EquipmentInventorySlot.values().length]; // Sized for all equipment slots
        
        // Initialize all slots to a "default empty" mock if needed, or null
        for (int i = 0; i < allEquipmentItems.length; i++) {
             // We could mock empty items, but null from getItems() is also possible for empty slots.
             // The plugin's loop handles itemId == -1, which our mockItem(-1,X) becomes null.
             // Let's ensure items are explicitly null if not specified.
        }
        
        for (Item item : itemsInSlots) {
            if (item == null) continue;

            // Determine slot based on item ID for simplicity in test setup
            int slotIdx = -1;
            if (item.getId() == DIZANAS_MAX_CAPE_ID || item.getId() == FIRE_CAPE_ID) {
                slotIdx = EquipmentInventorySlot.CAPE.getSlotIdx();
            } else if (item.getId() == RUBY_DRAGON_BOLTS_E_ID || item.getId() == ADAMANT_ARROW_ID || item.getId() == TINDERBOX_ID) {
                 slotIdx = EquipmentInventorySlot.AMMO.getSlotIdx();
            }
            // Add more specific slot assignments if other items are used in tests.
            
            if (slotIdx != -1) {
                 allEquipmentItems[slotIdx] = item;
            }
        }
        when(mockEquipmentContainer.getItems()).thenReturn(allEquipmentItems);
    }

    @Test
    void testDizanasCapeWithTrackableAmmo() {
        Item dizanasCape = mockItem(DIZANAS_MAX_CAPE_ID, 1);
        Item rubyBolts = mockItem(RUBY_DRAGON_BOLTS_E_ID, 50);

        when(mockEquipmentContainer.getItem(EquipmentInventorySlot.CAPE.getSlotIdx())).thenReturn(dizanasCape);
        when(itemManager.getItemComposition(DIZANAS_MAX_CAPE_ID)).thenReturn(mockItemComposition("Dizana's max cape", DIZANAS_MAX_CAPE_ID));
        
        when(mockEquipmentContainer.getItem(EquipmentInventorySlot.AMMO.getSlotIdx())).thenReturn(rubyBolts);
        when(itemManager.getItemComposition(RUBY_DRAGON_BOLTS_E_ID)).thenReturn(mockItemComposition("Ruby dragon bolts (e)", RUBY_DRAGON_BOLTS_E_ID));

        setupContainerItemsArray(dizanasCape, rubyBolts);
        
        Map<Integer, Float> actualQtyMap = new HashMap<>();
        plugin.refreshQtyMap(actualQtyMap, mockEquipmentContainer);

        assertEquals(2, actualQtyMap.size(), "Map should contain two items: cape and bolts.");
        assertEquals(1.0f, actualQtyMap.get(DIZANAS_MAX_CAPE_ID), "Dizana's max cape quantity should be 1.");
        assertEquals(50.0f, actualQtyMap.get(RUBY_DRAGON_BOLTS_E_ID), "Ruby dragon bolts (e) quantity should be 50.");
    }

    @Test
    void testDizanasCapeWithIgnoredAmmo() {
        when(config.ignoredItems()).thenReturn("Ruby dragon bolts (e)"); // Configure bolts to be ignored
        plugin.refreshIgnoredItems(); // Re-initialize plugin's ignoredItems set

        Item dizanasCape = mockItem(DIZANAS_MAX_CAPE_ID, 1);
        Item rubyBolts = mockItem(RUBY_DRAGON_BOLTS_E_ID, 50); // This item's name will be on the ignore list

        when(mockEquipmentContainer.getItem(EquipmentInventorySlot.CAPE.getSlotIdx())).thenReturn(dizanasCape);
        when(itemManager.getItemComposition(DIZANAS_MAX_CAPE_ID)).thenReturn(mockItemComposition("Dizana's max cape", DIZANAS_MAX_CAPE_ID));
        
        when(mockEquipmentContainer.getItem(EquipmentInventorySlot.AMMO.getSlotIdx())).thenReturn(rubyBolts);
        // Crucially, mock the ItemComposition for the bolts so their name can be checked against the ignore list
        when(itemManager.getItemComposition(RUBY_DRAGON_BOLTS_E_ID)).thenReturn(mockItemComposition("Ruby dragon bolts (e)", RUBY_DRAGON_BOLTS_E_ID));

        setupContainerItemsArray(dizanasCape, rubyBolts);

        Map<Integer, Float> actualQtyMap = new HashMap<>();
        plugin.refreshQtyMap(actualQtyMap, mockEquipmentContainer);
        
        assertEquals(1, actualQtyMap.size(), "Map should contain only the cape, as ammo is ignored.");
        assertEquals(1.0f, actualQtyMap.get(DIZANAS_MAX_CAPE_ID), "Dizana's max cape quantity should be 1.");
        assertNull(actualQtyMap.get(RUBY_DRAGON_BOLTS_E_ID), "Ignored Ruby dragon bolts (e) should not be in the map.");
    }

    @Test
    void testDizanasCapeWithNoAmmo() {
        Item dizanasCape = mockItem(DIZANAS_MAX_CAPE_ID, 1);
        // AMMO slot is empty (getItem returns null for this slot)
        when(mockEquipmentContainer.getItem(EquipmentInventorySlot.AMMO.getSlotIdx())).thenReturn(null); 

        when(mockEquipmentContainer.getItem(EquipmentInventorySlot.CAPE.getSlotIdx())).thenReturn(dizanasCape);
        when(itemManager.getItemComposition(DIZANAS_MAX_CAPE_ID)).thenReturn(mockItemComposition("Dizana's max cape", DIZANAS_MAX_CAPE_ID));
        
        setupContainerItemsArray(dizanasCape, null); // Pass null for ammo item

        Map<Integer, Float> actualQtyMap = new HashMap<>();
        plugin.refreshQtyMap(actualQtyMap, mockEquipmentContainer);

        assertEquals(1, actualQtyMap.size(), "Map should contain only the cape.");
        assertEquals(1.0f, actualQtyMap.get(DIZANAS_MAX_CAPE_ID), "Dizana's max cape quantity should be 1.");
    }

    @Test
    void testNonDizanasCapeWithAmmo() {
        // Standard cape (Fire Cape) + Adamant Arrows. Dizana-specific logic should not apply.
        Item fireCape = mockItem(FIRE_CAPE_ID, 1);
        Item adamantArrows = mockItem(ADAMANT_ARROW_ID, 100);

        when(mockEquipmentContainer.getItem(EquipmentInventorySlot.CAPE.getSlotIdx())).thenReturn(fireCape);
        when(itemManager.getItemComposition(FIRE_CAPE_ID)).thenReturn(mockItemComposition("Fire cape", FIRE_CAPE_ID));
        
        when(mockEquipmentContainer.getItem(EquipmentInventorySlot.AMMO.getSlotIdx())).thenReturn(adamantArrows);
        when(itemManager.getItemComposition(ADAMANT_ARROW_ID)).thenReturn(mockItemComposition("Adamant arrow", ADAMANT_ARROW_ID));

        setupContainerItemsArray(fireCape, adamantArrows);

        Map<Integer, Float> actualQtyMap = new HashMap<>();
        plugin.refreshQtyMap(actualQtyMap, mockEquipmentContainer);

        // Expect both items to be tracked by the standard loop in refreshQtyMap
        assertEquals(2, actualQtyMap.size(), "Map should contain Fire cape and Adamant arrows.");
        assertEquals(1.0f, actualQtyMap.get(FIRE_CAPE_ID), "Fire cape quantity should be 1.");
        assertEquals(100.0f, actualQtyMap.get(ADAMANT_ARROW_ID), "Adamant arrow quantity should be 100.");
    }

    @Test
    void testDizanasCapeWithNonAmmoInAmmoSlot() {
        // Dizana's Cape + Tinderbox in AMMO slot (edge case)
        Item dizanasCape = mockItem(DIZANAS_MAX_CAPE_ID, 1);
        Item tinderbox = mockItem(TINDERBOX_ID, 1); 

        when(mockEquipmentContainer.getItem(EquipmentInventorySlot.CAPE.getSlotIdx())).thenReturn(dizanasCape);
        when(itemManager.getItemComposition(DIZANAS_MAX_CAPE_ID)).thenReturn(mockItemComposition("Dizana's max cape", DIZANAS_MAX_CAPE_ID));
        
        when(mockEquipmentContainer.getItem(EquipmentInventorySlot.AMMO.getSlotIdx())).thenReturn(tinderbox);
        when(itemManager.getItemComposition(TINDERBOX_ID)).thenReturn(mockItemComposition("Tinderbox", TINDERBOX_ID));
        
        setupContainerItemsArray(dizanasCape, tinderbox);

        Map<Integer, Float> actualQtyMap = new HashMap<>();
        plugin.refreshQtyMap(actualQtyMap, mockEquipmentContainer);

        // The Dizana-specific logic should add the tinderbox because it's a valid item in the slot.
        // The main loop will then skip this slot due to dizanasCapeHandledAmmo = true.
        assertEquals(2, actualQtyMap.size(), "Map should contain Dizana's cape and Tinderbox.");
        assertEquals(1.0f, actualQtyMap.get(DIZANAS_MAX_CAPE_ID), "Dizana's max cape quantity should be 1.");
        assertEquals(1.0f, actualQtyMap.get(TINDERBOX_ID), "Tinderbox quantity should be 1.");
    }
}
