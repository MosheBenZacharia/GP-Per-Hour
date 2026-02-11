# GP Per Hour - RuneLite Plugin

## Overview

A RuneLite plugin that tracks GP/hr (gold per hour) in Old School RuneScape. It monitors inventory, equipment, and container items across bank-to-bank "trips," calculating profit/loss in real time. Sessions (groups of trips) can be saved and reviewed later.

**Author:** Moshe Ben-Zacharia
**License:** BSD 2-Clause
**Current Version:** 1.16
**Java Target:** 11
**Package:** `com.gpperhour`

## Build & Run

```bash
# Build
./gradlew build

# Run (launches RuneLite with plugin in dev mode)
./gradlew run

# Build shadow JAR
./gradlew shadowJar
```

Uses `latest.release` of the RuneLite client. Lombok is used for boilerplate reduction (`@Getter`, `@Setter`, `@Slf4j`, `@AllArgsConstructor`, etc.).

## Architecture

### Core Concepts

- **Trip**: A bank-to-bank run. Starts when the player closes the bank, ends when they open it again. Tracks initial vs. current inventory/equipment to compute profit/loss.
- **Session**: A collection of trips with aggregated stats (GP/hr, net total, gains, losses). Can be saved to persistent storage and viewed in the session history.
- **RunState**: `NONE` → `BANK` → `RUN` → `BANK` → ... The plugin alternates between BANK (bank UI open) and RUN (out in the world).
- **TrackingMode**: `TOTAL` (raw inventory value) or `PROFIT_LOSS` (difference from trip start).

### Key Files

| File | Purpose |
|------|---------|
| `GPPerHourPlugin.java` | Main plugin class. Handles game events, state machine (BANK/RUN), inventory scanning, price lookups, and data persistence. Entry point for the plugin. |
| `GPPerHourConfig.java` | Config interface with all user-facing settings. Config group: `"gpperhour"`. Sections: Shared, Inventory Overlay, Session Panel, Untradeable Values. |
| `SessionManager.java` | Business logic for trips and sessions. Manages active trips map, session start/end boundaries, session save/load/delete, and session history. |
| `TripData.java` | Data model for a single trip. Stores `initialItemQtys`, current `itemQtys`, `bankedItemQtys`, runtime, and timestamps. Uses `transient` for non-serialized fields. |
| `SessionStats.java` | Data model for a saved session. Immutable stats (runtime, gains, losses, net total, trip count) plus item quantity maps. |
| `ActiveTripOverlay.java` | Renders the in-game overlay above the inventory showing GP/hr, profit, or total. Also renders the hover ledger tooltip. |
| `GoldDropManager.java` | Implements gold drops (XP-drop-style profit/loss animations). Two modes: Vanilla (hijacks fake XP drops) and Static (custom overlay rendering). |
| `LootingBagManager.java` | Tracks looting bag contents by monitoring inventory changes, item despawns, and the bag's item container widget. |
| `ValueRemapper.java` | Maps untradeable items to tradeable equivalents based on user config. Also handles seedling→sapling, brimstone key average value, minnow→shark, etc. |
| `FractionalRemapper.java` | Normalizes dosed/charged items to their full variant. E.g., Super restore(3) → 0.75x Super restore(4). Large static map of ~200 item remappings. |
| `UI.java` | Shared UI utilities: icon loading, GP/time formatting, loot grid rendering, ledger comparison, icon button factory. All static methods. |
| `LedgerItem.java` | Represents a processed item for display: description, quantity, price, combined value. |

### UI Panels

| File | Purpose |
|------|---------|
| `GPPerHourPanel.java` | Root side panel with two tabs: "Active Session" and "Session History". Extends `PluginPanel`. |
| `ActiveSessionPanel.java` | Shows active session stats (GP/hr, net total, gains/losses, trip count) and per-trip cards with Set Start/Set End/Delete buttons and loot grids. |
| `SessionHistoryPanel.java` | Searchable list of saved sessions with expandable details, editable names, and delete functionality. |
| `EditableNameField.java` | Inline-editable text field with Save/Cancel/Edit actions. Used for session names. |
| `RoundedPanel.java` | Custom `JPanel` with rounded top corners for visual styling. |

### Enums

| Enum | Values | Purpose |
|------|--------|---------|
| `RunState` | `NONE`, `BANK`, `RUN` | Current plugin state |
| `TrackingMode` | `TOTAL`, `PROFIT_LOSS` | What the overlay displays |
| `TripOverlayAlignment` | `CENTER`, `LEFT`, `RIGHT` | Overlay horizontal alignment |
| `InventoryOverlayDisplayMode` | `TRIP_GP_PER_HOUR`, `TRIP_PROFIT`, `SESSION_GP_PER_HOUR`, `SESSION_PROFIT`, `INVENTORY_TOTAL` | What data the overlay shows |
| `ValueMode` | `RUNELITE_VALUE`, `LOW_ALCHEMY_VALUE`, `HIGH_ALCHEMY_VALUE` | Price source selection |
| `GoldDropDisplayMode` | `DISABLED`, `VANILLA`, `STATIC` | Gold drop rendering mode |

### Weapon Charges Subsystem (`weaponcharges/`)

Tracks charge consumption on weapons like tridents, blowpipe, scythe, crystal gear, etc.

| File | Purpose |
|------|---------|
| `ChargedWeapon.java` | Large enum (~1300 lines) defining every trackable weapon with item IDs, animation IDs, charge components, regex patterns for chat/dialog messages, and recharge amounts. |
| `WeaponChargesManager.java` | Event handler that monitors animations, chat messages, hitsplats, and dialogs to track weapon charges. Stores charge counts in RuneLite config. |
| `DialogTracker.java` | Monitors game widget state to detect NPC/player dialogs. Notifies listeners on dialog transitions and option selections. |
| `DialogStateMatcher.java` | Regex-based matching of dialog content (sprite text, NPC text, input text). Builder pattern for creating matchers. |
| `ChargesDialogHandler.java` | Predefined handlers for common dialog patterns (charge checking, adding charges, uncharging). |
| `ChargesMessage.java` | Encapsulates chat message patterns with update/check timing semantics. |

**Charge detection methods:** animation-based (1 charge per attack), chat message regex, dialog input parsing, hitsplat counting (for degradable armor).

### Item Charges Subsystem (`itemcharges/`)

Tracks contents of container items (herb sack, fish barrel, etc.) and utility item charges.

| File | Purpose |
|------|---------|
| `ChargedItemManager.java` | Factory that creates all `ChargedItem` instances and dispatches RuneLite events to them. |
| `ChargedItem.java` | Abstract base class (~940 lines) with trigger-based architecture. Each item defines arrays of triggers that specify when charges change. |
| `ChargesItem.java` | Enum listing all supported charged items. |

**Trigger types** (`triggers/`): `TriggerAnimation`, `TriggerChatMessage`, `TriggerGraphic`, `TriggerHitsplat`, `TriggerItem`, `TriggerItemContainer`, `TriggerItemDespawn`, `TriggerMenuOption`, `TriggerReset`, `TriggerWidget`, `TriggerXPDrop`. Each uses a builder/fluent pattern.

**Supported items** (`items/`): `U_FishBarrel`, `U_LogBasket`, `U_HerbSack`, `U_CoalBag`, `U_GemBag`, `U_SeedBox`, `U_BloodEssence`, `U_AshSanctifier`, `U_BottomlessCompostBucket`, `S_KharedstMemoirs`.

### Reward Chest Support

The plugin tracks items banked via reward interfaces (COX, TOA, TOB, Drift Net, Fortis Colosseum, Lunar Chest, Fishing Trawler, Wilderness Loot Chest, Doom of Mokhaiotl). Handled in `onMenuOptionClicked` and `onWidgetLoaded` by reading reward item containers and merging into `bankedItemQtys`.

## Data Flow

1. **Bank opens** → `RunState.BANK`. Current trip ends. If trip had changes, it's kept in session history.
2. **Bank closes** → `RunState.RUN`. New trip starts. `initialItemQtys` snapshot taken from inventory + equipment.
3. **Each game tick** → Inventory/equipment scanned. Item quantities merged with weapon charge components, item container contents (looting bag, fish barrel, etc.), and rune pouch contents. Fractional remapping applied. Total GP calculated.
4. **Profit change detected** → Gold drop animation triggered (if enabled). Auto-resume if trip was paused.
5. **Bank opens again** → Trip completed. Trip data added to session manager. Cycle repeats.

## Data Persistence

- **Trip data**: Serialized as JSON via `Gson` to RuneLite's `ConfigManager` with key `"inventory_total_data"`. Per-profile (per RS account).
- **Session history**: Each session stored with key `"session_stats_{uuid}"`. Session ID list stored at `"session_ids"`.
- **Looting bag contents**: Stored at config key `"looting_bag"`.
- **Item charges**: Each charged item has its own config key (e.g., `"fish_barrel"`, `"herb_sack"`).
- **Weapon charges**: Stored per-weapon via `ConfigManager`.

## Price System

Prices are cached in a static `itemPrices` map to prevent fluctuation during a trip. Cleared and repopulated at trip start.

**Price resolution order:**
1. Coins → 1gp, Platinum tokens → 1000gp
2. Cached price (from current trip)
3. `ValueRemapper.remapPrice()` for untradeables and special items
4. RuneLite value (GE/Wiki price), Low Alchemy (0.4x store), or High Alchemy (0.6x store) based on config

## Conventions

- **Logging**: Slf4j via `@Slf4j` annotation
- **DI**: Guice `@Inject` for RuneLite services, manual construction for `SessionManager` and panels
- **Threading**: Game events on client thread, UI updates on Swing EDT via `SwingUtilities.invokeLater()`, data persistence on `ScheduledExecutorService`
- **Item IDs**: Use `net.runelite.api.gameval.ItemID` constants. Some legacy raw integer IDs exist in `FractionalRemapper`.
- **Config keys**: String constants defined in `GPPerHourConfig` (e.g., `showTripOverlayKeyName`, `goldDropDisplayModeKey`)
- **Changelog**: Version bumps update `plugin_version` and `plugin_message` in `GPPerHourPlugin.java`. Shown once per version via chat message on login.

## Git Commits

- **NEVER** include "Co-Authored-By: Claude" or any Claude/AI attribution in commit messages, descriptions, or author fields.
- Keep commit messages short and lowercase (e.g., `version bump 1.17 (container tracking)`)

### Version Bump Commits

A version bump touches 2 files with 3 changes:

1. `build.gradle` — update `version = '1.XX'`
2. `GPPerHourPlugin.java` — update `plugin_version = "1.XX"`
3. `GPPerHourPlugin.java` — update `plugin_message` with a changelog bullet point:
   ```java
   private static final String plugin_message = "" +
       "GP Per Hour 1.XX:<br>" +
           "* Description of the change.";
   ```

## Testing

Single test file `GPPerHourPluginTest.java` bootstraps RuneLite with the plugin as an external plugin for manual testing:
```bash
./gradlew run
```

## Logs

Logs are streamed to `logs/deploy.log`. To filter for game logs, grep for `[Client] DEBUG`:
```bash
grep "\[Client\] DEBUG" logs/deploy.log
```

## Resources

PNG icons in `src/main/resources/` prefixed with `gpperhour-`:
- `gpperhour-icon.png` - Plugin nav button icon
- `gpperhour-pause.png` / `gpperhour-play.png` - Trip pause/resume
- `gpperhour-session-*.png` - Session panel toolbar icons (gear, refresh, trash, save, stop, play, info, grid, plus, wrench)
