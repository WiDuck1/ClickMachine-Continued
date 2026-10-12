# ClickMachine Continued

A Minecraft 26.2 mod for NeoForge 26.2 that adds a configurable automatic clicker block. Placebo is no longer required.

## Run in IntelliJ IDEA

1. Open this folder as a Gradle project and let Gradle finish syncing.
2. Set the project SDK and Gradle JVM to **Java 25**. Gradle uses the included wrapper.
3. Select **Click Machine - Client** in the run configuration dropdown and press **Run**.

The configuration runs Gradle's `runClient` task, which launches a development Minecraft client with Click Machine. The game stores settings and worlds in `run-26.2/client/`, which is excluded from Git. The old `run/` folder is preserved separately.

Create a Creative world and find **Click Machine** in the Redstone Blocks tab to try the mod.

## Run from a terminal

With a Java 25 JDK on `PATH` or configured in `JAVA_HOME`:

```sh
./gradlew runClient
```

On Windows, use `gradlew.bat runClient`.

## Build

```sh
./gradlew build
```

The mod JAR is written to `build/libs/`. Install the JAR with Minecraft **26.2**, NeoForge **26.2.0.90 or newer in the 26.2 series**, and Java **25**.

## Configuration and behavior

Settings remain in `config/clickmachine.cfg`; an existing file can be copied unchanged. Changes are loaded when the server starts or runs `/reload`, and are synchronized to clients. The defaults remain:

- Click intervals: 500, 200, 100, 50, 20, 10, 5, 2, and 1 ticks.
- RF disabled; when enabled, the costs are 0, 3, 5, 10, 25, 50, 100, 250, and 500 RF **per tick**.
- Energy capacity and maximum transfer per operation: 50,000 RF.
- One inventory slot, with the existing 64-item limit; bedrock is blacklisted.
- A redstone signal disables clicking and pauses both the timer and energy consumption.

Registry names, the crafting recipe, comparator output, control IDs, and block-entity save keys are retained. Old held-item data is upgraded with Minecraft's item data fixer, including enchantments and durability. The underlying Minecraft version still determines vanilla item and entity behavior.

## Verification

```sh
./gradlew test
./gradlew runGameTestServer
./gradlew runData build
./gradlew runClientSmoke
```

Unit tests cover the existing configuration format and invalid values. The isolated GameTest mod checks gameplay, persistence, menus, automation, and crafting in `run-26.2/gametest/`. Test classes and resources are excluded from the released mod JAR.

`runClientSmoke` prepares a disposable copy of the GameTest world, opens the real client menu, verifies inventory, RF, toggles, and speed synchronization, then closes the client. Use `./gradlew runClientSmoke -PsmokeUsesRF=false` to check the rainbow-power rendering too.

## License

See [LICENSE](LICENSE) for the original project's license and attribution.
