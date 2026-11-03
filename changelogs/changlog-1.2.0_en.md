# TACZ 1.2.0-neoforge-mc1.21.1

Content is synchronized with 1.2.0-forge-mc1.20.1.

## Additional Changes and Content

- Added a NeoForge version for Minecraft 1.21.1, requiring Java 21.
- Adapted guns, ammo, attachments, and other items to data components. Custom NBT data is now stored in `minecraft:custom_data`.
- Adapted the script NBT read/write API to the new item data format while preserving existing calls.
- Updated gun pack recipes, loot tables, advancements, and tags to the 1.21.1 data format.
- Gun packs now support overlays selected by Minecraft version, allowing one pack to contain resources for multiple versions.
- The default gun pack is now exported as a shared 1.20.1 base pack with a 1.21.1 overlay.
- Added the `/tacz convert universal` tool to generate 1.21.1 overlays for older gun packs, with checks and exports for individual packs or batches.
  - Converted gun packs support both 1.20.1 and 1.21.1.
  - Converted packs and reports are saved separately in the `tacz-converted` directory.
  - Custom rules can override item tag conversion mappings.
- Shader compatibility now uses Iris.
- Added support for Player Animator 2.x while preserving the interpolation behavior of older player animations.
- Dedicated KubeJS integration is not currently available in the 1.21.1 version.

---

# TACZ 1.2.0-forge-mc1.20.1

## New Content

### Guns

- Assault rifles: AK-12, AK-74M, H.C.A.R., MCX Spear LT, MK18, QR-24, and URGI.
- Pistols and SMGs: TEC-9 and MP7A1.
- Marksman rifles: SCAR-MK20 and Winchester M1866.
- Sniper rifles: SVD, MRAD, and HM50B.
- Shotguns and machine guns: M1897 "Trench Gun" and PKM.

### Attachments

- Added APM4s and 1P87 reflex sights.
- Added PSO-1, C79, Mark 4 ER/T, AMG 10, and Vintage Winchester Sharpshooter scopes.
- Added the XM157 Fire Control Optic.
- Added REAP-IR, FMP13, and Pulsar XL50 thermal sights.
- Added Magpul AFG and TROY angled grips.
- Added the NGAL laser.
- Added the SOCOM RC1 quick-detach suppressor.
- Added the CTR Heavy Stock and PSG Marksman Stock.
- Added low, mid, and high pistol risers, mid and high rail risers, and the Unity Riser.
- Added factory tactical, combat, HK, and AUG iron sights.
- Added two factory handguard covers.
- Added the M16A1 Assault Handguard.
- Added the M1897 Factory Bayonet.
- Added the M1866 Factory Artillery Barrel.
- Added options to unfold or remove factory stocks as built-in attachments.
- Added the Overpressure Recoil Shell ammo modification attachment.

### Ammo

- Added .300 BLK rifle ammo.

### Blocks

- Added Pegboards and Glow Pegboards with connections, corners, double-sided displays, dyeing, and placement previews.
- Added connectable, lockable Gun Display Cases.
- Added Iron, Gold, and Diamond Ammo Storage with large capacities, upgrades, portability, and quick transfers to and from ammo boxes.

## New Features

- Updated SnowStorm-based muzzle flash and smoke effects, with support for muzzle attachments to adjust their appearance.
- Firing now briefly illuminates the first-person hand model.
- Added new scope lens effects:
  - Configurable edge distortion, chromatic aberration, tinting, and vignetting.
  - Optional night vision and white-hot, black-hot, and ironbow thermal imaging modes.
- Added support for rangefinder displays on sights.
- Added ballistic impact prediction for fire-control optics.
- Added support for ammo counters on sights. The UH-1 is now a Smart Digital Sight.
- Built-in factory attachments can now be switched in the customization screen without consuming attachment items.
- Riser adapters can now be selected in the customization screen.
- Added ammo unloading, bound to `Alt + R` by default, along with related scripting APIs.
- Added configurable aiming transitions with custom ADS paths and FOV presets.
- Added recoil push with separate settings for standing, airborne, crouching, and prone positions.
- The M1866 now supports different action cycling speeds for hip fire and aiming.
- Added configurable surface impact effects and melee particles.
- Restored gunshot aggro: firing alerts nearby neutral mobs and attracts hostile mobs unless a suppressor is installed. This can be disabled in the config.
- Going prone no longer requires holding a gun.
- Prone pitch limits can now be configured.
- Gun data can now specify whether a weapon continues cooling while stored in the inventory, disabled by default. Enabled for the Minigun.
- Added a gun pack filter sidebar to Creative mode, with icons configurable in gun pack metadata.
- Added JEI queries for compatible gun attachments and ammo.
- Added JEI recipe transfer support, allowing selection of the corresponding workbench recipe.
- Added a TaCZ advancement tree covering crafting, customization, collecting, and shooting challenges.

## Optimizations and Fixes

- Added VBO acceleration, significantly improving rendering performance for third-person, dropped, and displayed guns.
- Optimized gun pack network synchronization with compression and chunking, fixing failures to join worlds with large numbers of gun packs.
- Optimized JEI initialization.
- Optimized inventory ammo counting and fixed the resulting issues with Minigun automatic fire.
- Updated models, textures, animations, and sounds for existing guns such as the M4A1 and AWP, as well as several sights.
- Added PBR textures for more guns, attachments, and shell casings.
- Added LOD models for more guns.
- Improved the default animation state machine and transitions, retaining the old state machine for third-party gun pack compatibility.
- Improved deployment and action cycling animations for built-in bipods.
- Reduced view obstruction by the gun when large sights are installed.
- Fixed incorrect bullet visibility in empty magazines and animations for guns such as the M700 and P320.
- Fixed the HK416 iron sight position.
- Fixed Vector model issues.
- Fixed RPG-7 animation issues.
- Attempted to fix cases where guns cannot fire after respawning or joining a server.
- Unified input handling so actions can be correctly bound to mouse buttons or keyboard keys and used with modifier keys.
- Fixed prone state being forcibly maintained and the "Hold to Crawl" option not working correctly.
- Improved Player Animator compatibility so mods that display a player model, such as paper doll mods, should now render correctly.
- Fixed misaligned rendering of armor on the hands from some mods under certain conditions.
- Fixed first-person sound positions drifting with player movement in some cases.
- Fixed offhand guns causing the main-hand walking bob to disappear.
- Fixed lighting on some item models in the inventory screen.
- Fixed emissive models rendering incorrectly with accelerated rendering.
- Fixed charging preventing interruption of round-by-round reloads.
- Unified gun animation and cooldown timing during game pauses. Animations and timers now stop advancing while the game is paused.
- Fixed workbenches producing item stacks above their stack limits.
- Fixed overlapping hide-chart buttons in the customization screen.
- Fixed the gun HUD appearing in Spectator mode.
- Fixed headshot hitboxes appearing when reduced debug information is enabled.
- Improved server-side script error handling to prevent script errors from crashing the server.
- Fixed invalid fire modes throwing exceptions that could crash the client.
- Fixed rare client crashes caused by projectiles spawned by other sources with null resource IDs.
- Fixed empty sound or particle keyframes causing animation loading to fail.
- Improved animation constraint handling by taking absolute values first, ensuring compatibility with Blockbench 5 keyframes.
- Corrected event triggering for firing, reload completion, and drawing guns.

## Other Changes

- Rebalanced various existing guns and attachments.
- Added a Marksman Rifle category and moved the MK14, Tactical SKS, and SPR-15HB into it.
- Adjusted slide restrictions for some guns.
- Guns can now configure whether reloading or cycling the action forces the player out of aiming.
- Workbench and Creative mode items are now grouped and sorted by namespace.
- Adjusted the display order of ammo and attachments.
- Built-in factory attachments are now labeled and pinned to the top of the customization list.
- Added sight tooltips for rangefinding, fire control, thermal imaging, and ammo counters.
- Added ingredient item tooltips to workbenches.
- Improved workbench hints for filtering by the held item.
- Updated some attachment icons.
- Added a crafting recipe for the Taurus 943.
- Removed recipes for four older raised pistol sights.
- Added an option to use hip-fire spread while airborne.
- Added an option for bullets to pass through teammates when friendly fire is disabled.
- Added a configurable list of entity types ignored by bullet damage.
- The list of blocks bullets can destroy is now customizable, with a hardness limit.
- Added a muzzle smoke toggle.
- Added hit particle toggles and a display distance setting.
- Added toggles for scope lens effects and thermal imaging.
- Added an option to hide third-person guns in the offhand and unselected hotbar slots.
- Added a held-gun damage tilt strength setting.
- Sounds now support custom volume, pitch, random variation, and references to vanilla sounds.
- Some animations now support particle keyframes, limited to the muzzle position; locators are not supported.
- Improved default gun pack update detection.
- Clarified the default gun pack debug setting description.
- Updated the default gun pack icon.
- Updated translations.
