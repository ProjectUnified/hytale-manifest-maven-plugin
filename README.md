# Hytale Manifest Maven Plugin ![Maven Central Version](https://img.shields.io/maven-central/v/io.github.projectunified/hytale-manifest-maven-plugin)

A Maven plugin for Hytale server plugin development. It provides two goals:

- **`generateManifest`** — generates a `manifest.json` for your plugin
- **`updateServerDependency`** — updates the `com.hypixel.hytale:Server`
  dependency version in your POM

## Setup

Add the plugin to your `build` settings:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.projectunified</groupId>
            <artifactId>hytale-manifest-maven-plugin</artifactId>
            <version>VERSION</version>
            <executions>
                <execution>
                    <goals>
                        <goal>generateManifest</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <main>dev.hytalemodding.ExamplePlugin</main>
            </configuration>
        </plugin>
    </plugins>
</build>
```

By default, `generateManifest` runs in the `generate-resources` phase.

> [!NOTE]
> If you want to quickly create your project, you can fork
> [this template](https://github.com/HSGamer/hytale-plugin-template)

---

## Goal: `generateManifest`

Generates a `manifest.json` file and places it in the build output directory so
it is included in the final JAR.

### Parameters

| Name                   | Property                            | Description                                                                     | Default                               |
| ---------------------- | ----------------------------------- | ------------------------------------------------------------------------------- | ------------------------------------- |
| `group`                | `hytale.manifest.group`             | The group ID of the plugin                                                      | `${project.groupId}`                  |
| `name`                 | `hytale.manifest.name`              | The display name of the plugin                                                  | `${project.name}`                     |
| `version`              | `hytale.manifest.version`           | The version of the plugin                                                       | `${project.version}`                  |
| `description`          | `hytale.manifest.description`       | A short description of the plugin                                               | `${project.description}`              |
| `website`              | `hytale.manifest.website`           | The plugin's website URL                                                        | `${project.url}`                      |
| `main`                 | `hytale.manifest.main`              | Fully-qualified entry-point class (**required**)                                |                                       |
| `authors`              |                                     | List of authors (see [Authors](#authors))                                       | Project `developers` + `contributors` |
| `serverVersion`        | `hytale.manifest.serverVersion`     | Server version compatibility (see [Server Version](#server-version-resolution)) | `*`                                   |
| `dependencies`         |                                     | Required plugin dependencies (see [Dependencies](#dependencies))                |                                       |
| `optionalDependencies` |                                     | Optional plugin dependencies                                                    |                                       |
| `loadBefore`           |                                     | Plugins to load before this one                                                 |                                       |
| `disabledByDefault`    | `hytale.manifest.disabledByDefault` | Whether the plugin is disabled by default                                       | `false`                               |
| `includesAssetPack`    | `hytale.manifest.includesAssetPack` | Whether the plugin includes an asset pack                                       | `false`                               |
| `outputDirectory`      | `hytale.manifest.outputDirectory`   | Output directory for the manifest file                                          | `${project.build.directory}/classes`  |

### Authors

If no `authors` are explicitly configured, the Mojo falls back to the project's
`<developers>` and `<contributors>`.

```xml
<authors>
    <author>
        <name>Author Name</name>
        <email>author@email.com</email>
        <url>https://author.website</url>
    </author>
</authors>
```

### Dependencies

The `dependencies`, `optionalDependencies`, and `loadBefore` parameters each
accept a list of entries with:

- `name` — the dependency plugin name (**required**)
- `version` — the required version (**required**, use `*` for any)

```xml
<dependencies>
    <dependency>
        <name>dependency-name</name>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

### Server Version Resolution

If `serverVersion` is set to `LATEST` or `RELEASE` (case-insensitive), the
actual version is resolved at build time from the
[Hytale Maven repository metadata](https://maven.hytale.com/release/com/hypixel/hytale/Server/maven-metadata.xml).

```xml
<configuration>
    <main>com.example.MyPlugin</main>
    <serverVersion>RELEASE</serverVersion>
</configuration>
```

---

## Goal: `updateServerDependency`

Updates the `com.hypixel.hytale:Server` dependency version in a POM file. This
goal is **not bound** to any lifecycle phase — invoke it manually:

```bash
mvn hytale-manifest:updateServerDependency
```

### Parameters

| Name                | Property                     | Description                                                                | Default                      |
| ------------------- | ---------------------------- | -------------------------------------------------------------------------- | ---------------------------- |
| `serverVersionType` | `hytale.server.versionType`  | Which version to fetch: `LATEST` or `RELEASE`                              | `RELEASE`                    |
| `pom`               | `hytale.server.pom`          | Path to the POM file to update                                             | `${project.basedir}/pom.xml` |
| `propertyName`      | `hytale.server.propertyName` | If set, update this `<properties>` entry instead of the dependency version |                              |

### Examples

Update the dependency version directly:

```bash
mvn hytale-manifest:updateServerDependency
```

Update a Maven property instead (e.g. when the dependency uses
`${hytale.server.version}`):

```bash
mvn hytale-manifest:updateServerDependency -Dhytale.server.propertyName=hytale.server.version
```

Fetch the latest version instead of the latest release:

```bash
mvn hytale-manifest:updateServerDependency -Dhytale.server.versionType=LATEST
```
