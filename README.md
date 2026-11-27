# ServerHub (Fabric, Minecraft 26.3)

Server-side Fabric mod: **money, auction house, admin shop, teleport requests.**
Players do **not** need to install anything. All menus are vanilla chest GUIs and all
icons are built-in Minecraft items.

> Not compiled or tested by the author (the sandbox it was written in had no internet).
> If the build reports an error, paste it back and it can be fixed quickly - Minecraft's
> internal names move around between versions.

## Requirements
- Minecraft 26.3, Fabric Loader 0.19.5+, **Fabric API 0.160.5+26.3** (install it on the server too)
- Java 25

## Build
1. Install JDK 25 and Gradle 9.x (or copy `gradlew`, `gradlew.bat` and the `gradle/` folder from the
   official Fabric example mod into this folder).
2. In this folder run: `gradle build` (or `./gradlew build`)
3. Take `build/libs/serverhub-1.0.0.jar` (NOT the `-sources` jar).
4. Upload it to your server's `mods` folder (with Fabric API), then restart.

Versions are set in `gradle.properties`; check https://fabricmc.net/develop/ if one fails to resolve.

## Commands
**Money**
- `/balance [player]` (`/bal`) - show balance
- `/pay <player> <amount>`
- `/baltop`
- `/eco give|take|set <player> <amount>` - ops only

**Auction house**
- `/ah` (or `/auction`) - open the auction house; click a listing to buy it, click your own to cancel it
- `/ah sell <price> [amount]` - sell the item in your main hand
- `/ah claim` - collect items from expired listings

**Admin shop** (spawn eggs etc.)
- `/shop` (or `/adminshop`) - open; click = buy 1, shift-click = buy a full stack
- `/shopadmin add <item> <price>` - add an item or change its price (tab-completes every item)
- `/shopadmin price <item> <price>`
- `/shopadmin remove <item>`
- `/shopadmin list`
- Ops (permission level 2) only. You can also edit `config/serverhub/shop.json` while the server is stopped.

**Teleport requests**
- `/tpa <player>` - ask to teleport to them
- `/tpahere <player>` - ask them to teleport to you
- `/tpaccept [player]`, `/tpdeny [player]`, `/tpcancel` (the request message also has clickable buttons)

## Config & data (in `config/serverhub/`)
- `config.json` - currency symbol, starting balance, auction duration/tax/limits, TPA timeout/cooldown
- `balances.json`, `auction.json`, `shop.json` - data (autosaved every 30s and on shutdown)
