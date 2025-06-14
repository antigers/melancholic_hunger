
# Melancholic Hunger

**Melancholic Hunger & HUD** is a [Fabric](https://github.com/FabricMC/fabric) Minecraft mod that aims to seamlessly integrate the classic health regeneration system from before Beta 1.8 versions into the modern game.
It also auto-hides the experience bar from the HUD to match the look of those old versions while keeping the exp bar accessible.

## Main Features

1. **Hunger system is removed** — Health regenerates directly from eating food
2. **Gradual health regeneration** — Compensates for food stacking in modern versions. Regen speed depends on what types of food you eat.
Eating various types of food gives a cumulative effect that greatly increases the speed of regeneration (similar to the great mod [BTA!](https://www.betterthanadventure.net/))
3. **Smaller food stack sizes** — Prevents players from carrying an infinite supply of health in their inventory
4. **Quality-of-life improvements** — The HUD displays the number of hearts that can be restored by the currently held food item, as well as the number of hearts that are already regenerating
5. **Health-limited sprinting** — Sprinting is only available when the player's health is higher than the set minimum limit
6. **Removed hunger effect** — The hunger effect is replaced with the poison effect with a shorter duration
7. **The armor bar is on the right side, and it's drawn right-to-left, the experience bar is hidden** — Brings back the nostalgic, clean HUD appearance
8. **Smart experience bar** — The experience bar only appears when exp is being gained. Also when the player opens their inventory, or when interacting with workstations that use experience points
9. **Smooth animation** — Clean appearance and disappearance of the experience bar
10. **Server support** — You can run the mod on a modded server instance (clients would have to have the mod as well)

All features are toggleable through the GUI config, allowing you to customize the mod to your liking (requires [Mod Menu](https://modrinth.com/mod/modmenu) installed to access the GUI).

### Server Features

1. Some options (those that only affect the appearance of the HUD) are client-side, so each player can set and change them how they like
2. Other options (those that affect game mechanics) are server-side, so the server enforces them to all players.
Server operators can use the mod's GUI to update the configuration on the server.
The updated config will be sent to all players automatically

## Compatibility

This mod is compatible with [Nostalgic Tweaks](https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks) by Adrenix.
Overlapping config settings will be synchronized automatically.

This mod should be compatible with most resourcepacks, although there may be some small issues with the HUD.

**Warning:** Most mods that modify the hunger system or the HUD are very likely to be incompatible with this mod.

## Dependencies

1. [Cardinal Components API](https://github.com/Ladysnake/Cardinal-Components-API) by Ladysnake
2. [YetAnotherConfigLib](https://github.com/isXander/YetAnotherConfigLib) by isXander