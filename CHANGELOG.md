# Changelog

The newest `## ` section is used as the release notes when publishing (see PUBLISHING.md).

## 1.4.0

- Added support for Minecraft 1.21.10 (Fabric and NeoForge)
- Added support for Forge 1.20.1 through Sinytra Connector, using the Fabric 1.20.1 jar (with Forgified Fabric API and the Fabric build of owo-lib)
- Fixed pets turning into pigs when summoned. Pets collared while riding something (a boat, a minecart, a seat, another mob) were saved without their type in 1.3.x. Pets that haven't turned into pigs yet are repaired automatically
- Pets that already turned into pigs, and pets of unknown types (e.g. from a removed mod), are untracked when the world loads, and their leftover pigs are removed
- Fixed pets sometimes being duplicated when summoned, e.g. on the first summon after a restart while the pet was in an unloaded chunk, or after another mod moved the pet. Leftover copies are removed when their chunk loads, including duplicates made by earlier versions once the pet has been summoned again
- Fixed collared pets staying invulnerable after the mod is removed. Pets are only invulnerable while the mod is installed (pets saved by 1.3.x are updated the next time they're saved). If the mod was already removed, use `/kill` or `/data merge entity <pet> {Invulnerable:0b}`
