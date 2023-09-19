# GP Per Hour

_Track your gp/hr across various trips and save your sessions for later viewing._

GP Per Hour is a comprehensive money making tracker, that will accurately calculate your gp/hr by combining your profits and losses over time.

What makes this more accurate than any other profit tracker? 
- Tracks loss of charge components from weapons, armor, and utility items (Trident, blowpipe, crystal armor, blood essence etc.)
- Keeps track of items in containers (Looting bag, herb sack, etc.)
- Ability to assign value to untradeable items (Tokkul, crystal shards, etc.)

## UI

### Trip Overlay

See your gp/hr at glance, and hover your mouse over the overlay to see more details about the current trip.

<img width="203" alt="overlay" src="https://github.com/MosheBenZacharia/GP-Per-Hour/assets/12495920/7d0a27ca-fb38-4f9e-8f46-5496f801114c">


<img width="368" alt="overlay_ledger" src="https://github.com/MosheBenZacharia/GP-Per-Hour/assets/12495920/eb98a0fb-2683-4ea2-bc18-f16b57f658a9">


### Session Panel
See how your gp/hr is looking across various trips, and save a group of trips to the session history.

[Screenshot of active session panel]

[Screenshot of session history]

### Gold drops
- Configure the minimum amount needed to display a gold drop
- Configure the color of profit/loss text

<img width="341" alt="Screenshot 2023-09-18 at 7 43 16 PM 1" src="https://github.com/MosheBenZacharia/GP-Per-Hour/assets/12495920/e7d6f834-07ed-4d24-9a78-810018532ffe">


## Tracking Features

### Weapons and Armor Tracking
- Tracks charges used across the most popular weapons and automatically subtracts resources when you use your weapon.
- List of supported weapons/armor:
    - Warped Sceptre
    - Trident of the seas
    - Trident of the swamp
    - Abyssal tentacle
    - Crystal halberd
    - Crystal bow
    - Crystal helm, crystal body, and crystal legs
    - Tome of fire
    - Tome of water
    - Scythe of vitur
    - Sanguinesti staff
    - Arclight
    - Craw’s Bow / Webweaver Bow
    - Viggora’s Chainmace / Ursine Chainmace
    - Thammaron’s Sceptre / Accursed Sceptre
    - Bow of faerdhinen
    - Serpentine helm
    - Tumeken's shadow
    - Toxic blowpipe
- List of unsupported weapons/armor:
    - Barrows armor 
    - Iban’s staff (ambiguity on charge cost)
    - Blade of saeldor (needs testing)

[Screenshot of active session panel with trident & blowpipe charges missing]

### Utility Charge Tracking

Tracks components from common utility items, and tracks fractional components of items with charges.

- List of support items
    - Kharedst memoirs
    - Ash sanctifier
    - Blood essence
    - Bottomless compost bucket

<img width="447" alt="blood" src="https://github.com/MosheBenZacharia/GP-Per-Hour/assets/12495920/5b0babf3-3c5a-49f3-bd94-8a82f8ab21b3">


### Container Support

Automatically tracks items stored inside containers.

- List of support items:
    - Looting bag
    - Fish barrel
    - Gem bag
    - Herb sack
    - Log basket
    - Seed box
- Unsupported items:
    - Forestry kit (Widget check)
    - Plank sack (Lack of game messages)

[Screenshot of overlay + inventory with fish barrel in inventory but no other fish at karambwans]

### Untradeable Item Support

Provides config options to assign value to various untradeable items using their commonly traded counterparts.

- Supported items:
    - Tokkul
    - Crystal shards and crystal dust
    - Mermaid’s tears
    - Stardust
    - Unidentified minerals
    - Golden nuggets
    - Abyssal pearls
    - Hallowed marks

[Screenshot of fishing infernal eels]
