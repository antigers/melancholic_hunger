
# Melancholic Hunger

**Melancholic Hunger & HUD** is a [Fabric](https://github.com/FabricMC/fabric) Minecraft mod that aims to seamlessly integrate the classic health regeneration system from pre-Beta 1.8 versions of the game.
It also auto-hides the experience bar from the HUD to match those old versions while still making the exp bar accessible.

## Main Features

1. **Hunger system is removed** — Health regenerates directly from eating food
2. **Gradual health regeneration based on different food types** — Compensates for food stacking in modern versions (similar to the great mod [BTA!](https://www.betterthanadventure.net/))
3. **Smaller food stack sizes** — Prevents players from carrying an infinite supply of health in their inventory
4. **Quality-of-life improvements** — Shows the number of hearts that can be restored by the currently held food item, as well as the number of hearts already regenerating
5. **Health-limited sprinting** — Sprinting is only available when the player's health is at a certain level
6. **Removed hunger effect** — The hunger effect is replaced with the poison effect of shorter duration
7. **Armor bar is drawn right-to-left, experience bar is hidden** — Brings back the nostalgic, clean HUD appearance
8. **Smart experience bar** — The experience bar only appears when exp is being gained, when opening inventory, or when interacting with workstations that use experience points
9. **Smooth animation** — Clean appearance and disappearance of the experience bar

All features are toggleable through the GUI config, allowing you to customize the mod to your liking.

## Compatibility

This mod is compatible with [Nostalgic Tweaks](https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks) by Adrenix.
Overlapping config settings will be synchronized automatically.

This mod should be compatible with most resourcepacks, although there may be some issues with the HUD.

**Note:** Most mods that modify the hunger system or the HUD are very likely incompatible with this mod.

## Dependencies

1. [Cardinal Components API](https://github.com/Ladysnake/Cardinal-Components-API) by Ladysnake
2. [YetAnotherConfigLib](https://github.com/isXander/YetAnotherConfigLib) by isXander