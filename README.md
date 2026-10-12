# ClickMachine Continued

A Minecraft 1.21.1 mod for NeoForge that adds a configurable automatic clicker block.

## Run in IntelliJ IDEA

1. Open this folder as a Gradle project and let Gradle finish syncing.
2. Set the project SDK and Gradle JVM to **Java 21**. Gradle uses the included wrapper.
3. Select **Click Machine - Client** in the run configuration dropdown and press **Run**.

The configuration runs Gradle's `runClient` task, which launches a development Minecraft client with Click Machine and its dependencies. The game stores settings and worlds in `run/`, which is excluded from Git.

Create a Creative world and find **Click Machine** in the Redstone Blocks tab to try the mod.

## Run from a terminal

With a Java 21 JDK on `PATH` or configured in `JAVA_HOME`:

```sh
./gradlew runClient
```

On Windows, use `gradlew.bat runClient`.

## Build

```sh
./gradlew build
```

The mod JAR is written to `build/libs/`. For a normal Minecraft installation, use Minecraft 1.21.1 with NeoForge 21.1.187 or newer and Placebo 9.9.0 or newer for Minecraft 1.21.1.

## License

See [LICENSE](LICENSE) for the original project's license and attribution.
