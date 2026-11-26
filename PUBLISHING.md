# Building and publishing

## Build every version

```sh
./gradlew chiseledBuild
```

Jars land in `versions/<target>/build/libs/friendsforlife-<version>+<target>.jar`
(ignore the `-dev.jar` files in the Fabric targets).

## One-time setup

1. In `gradle.properties`, fill in:
   - `publish.modrinth_id`: the project slug or ID from the Modrinth URL
   - `publish.curseforge_id`: the numeric **Project ID** from the CurseForge project page sidebar
2. Create API tokens:
   - Modrinth: Settings → PATs, with the *Create versions* scope
   - CurseForge: https://legacy.curseforge.com/account/api-tokens
3. Keep the tokens out of the repo. Export them in your shell, e.g. in `~/.bashrc` or a sourced file outside the project:
   ```sh
   export MODRINTH_TOKEN=...
   export CURSEFORGE_TOKEN=...
   ```

## Releasing

1. Bump `releaseVersion` at the top of `build.gradle.kts`.
2. Add a `## <version>` section at the top of `CHANGELOG.md`. The newest section becomes the release notes.
3. Preview what will be uploaded. This builds everything and prints each upload without sending it:
   ```sh
   ./gradlew chiseledPublish -Ppublish.dry_run=true
   ```
4. Publish all 5 targets to both sites:
   ```sh
   ./gradlew chiseledPublish
   ```
   Uploads go out as **beta** by default. Override with `-Ppublish.release_type=release` (or `alpha`),
   or change `publish.release_type` in `gradle.properties`.

To publish a single target, run e.g. `./gradlew :1.21.10-neoforge:publishMods`. To publish to one site,
run e.g. `./gradlew :1.21.10-neoforge:publishModrinth`.

If an ID or token is missing, the upload task fails before anything is sent.

## Per-target settings

Each `versions/<target>/gradle.properties` has `publish.minecraft_versions`, the game versions that
jar is listed under (comma-separated). For example, set it to `1.21.9,1.21.10` if you've verified
the 1.21.10 jar also works on 1.21.9.

Every upload declares owo-lib as a required dependency; Fabric uploads also require Fabric API.
