<p align="center">
    <img width="300" src="https://s2.loli.net/2024/04/30/NJrstR1QzpoLyIT.png" alt="title">
</p>
<hr>
<p align="center">Timeless and Classics Guns Zero</p>
<p align="center">
    <a href="https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero">
        <img src="http://cf.way2muchnoise.eu/full_timeless-and-classics-zero.svg" alt="CurseForge Download">
    </a>
    <img src="https://img.shields.io/badge/license-GNU GPL 3.0 | CC%20BY--NC--ND%204.0-green" alt="License">
    <br>
    <a href="https://jitpack.io/#MCModderAnchor/TACZ">
        <img src="https://jitpack.io/v/MCModderAnchor/TACZ.svg" alt="jitpack build">
    </a>
    <a href="https://crowdin.com/project/tacz">
        <img src="https://badges.crowdin.net/tacz/localized.svg" alt="crowdin">
    </a>
</p>
<p align="center">
    <a href="https://github.com/MCModderAnchor/TACZ/issues">Report Bug</a>    ·
    <a href="https://github.com/MCModderAnchor/TACZ/releases">View Release</a>    ·
    <a href="https://tacwiki.mcma.club/zh/">Wiki</a>
</p>

Timeless and Classics Guns Zero is a gun mod for Minecraft with Forge and NeoForge builds.

## Maintenance Status

| Minecraft Version | Mod Loader | Version Series | Latest Version | Maintenance Status |
| --- | --- | --- | --- | --- |
| 1.18.2 | Forge | All versions | 1.1.4-hotfix | End of Support |
| 1.19.2 | Forge | All versions | 1.1.4-hotfix | End of Support |
| 1.20.1 | Forge | 1.x | 1.2.0 | Long-term Support |
| 1.21.1 | NeoForge | 1.x | 1.2.0 | Long-term Support |
| 1.20.1 | Forge | 2.x | TBA | In Development |
| 1.21.1 | NeoForge | 2.x | TBA | In Development |

- **End of Support**: These Minecraft versions are no longer maintained. `1.1.4-hotfix` is their final mod version.
- **Long-term Support**: The 1.x series continues to receive maintenance for Minecraft 1.20.1 and 1.21.1. However, there will be no more major updates.
- **In Development**: The 2.x series is under development. There are currently no builds available. Its release version is to be announced (TBA).

There are currently no plans to support other mod loaders or Minecraft versions.

## Notice

- If you have any bugs, you can visit [Issues](https://github.com/MCModderAnchor/TACZ/issues) to
  submit issues.

## Authors

- Programmer: `286799714`, `TartaricAcid`, `F1zeiL`, `xjqsh`, `ClumsyAlien`
- Artist: `NekoCrane`, `Receke`, `Pos_2333`

## Credits

- Other players who have helped me in any ways, and you

## License

- Code: [GNU GPL 3.0](https://www.gnu.org/licenses/gpl-3.0.txt)
- Assets: [CC BY-NC-ND 4.0](https://creativecommons.org/licenses/by-nc-nd/4.0/)

## Maven

```groovy
repositories {
    maven {
        // Add curse maven to repositories
        name = "Curse Maven"
        url = "https://www.cursemaven.com"
        content {
            includeGroup "curse.maven"
        }
    }
}

dependencies {
    // You can see the https://www.cursemaven.com/
    // Choose one of the following three

    // If you want to use version tacz-1.20.1-1.1.6-release
    implementation fg.deobf("curse.maven:timeless-and-classics-zero-1028108:6632240-sources-6633203")
}
```
