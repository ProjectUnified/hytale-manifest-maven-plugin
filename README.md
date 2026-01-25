# Hytale Manifest Maven Plugin ![Maven Central Version](https://img.shields.io/maven-central/v/io.github.projectunified/hytale-manifest-maven-plugin)

A Maven plugin to generate manifest file (manifest.json) for Hytale plugins

## Usage

Add the plugin to your `build` settings

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

By default, it will be integrated in the `generate-resources` phase.

> [!NOTE]
> If you want to quickly create your project, you can fork [this template](https://github.com/HSGamer/hytale-plugin-template)

## Parameters

Here is a list of available parameters in the `configuration` of the plugin

| Name                   | Description                                   | Default Value                        |
|------------------------|-----------------------------------------------|--------------------------------------|
| `group`                | The group ID of the plugin                    | `${project.groupId}`                 |
| `name`                 | The name of the plugin                        | `${project.name}`                    |
| `version`              | The version of the plugin                     | `${project.version}`                 |
| `description`          | The description of the plugin                 | `${project.description}`             |
| `website`              | The website of the plugin                     | `${project.url}`                     |
| `main`                 | The main class of the plugin (**Required**)   |                                      |
| `authors`              | The authors of the plugin                     | `developers` and `contributors` from the `pom.xml`    |
| `serverVersion`        | The supported server version                  | `*`                                  |
| `dependencies`         | The dependencies of the plugin                |                                      |
| `optionalDependencies` | The optional dependencies of the plugin       |                                      |
| `loadBefore`           | The list of plugins to load before the plugin |                                      |
| `disabledByDefault`    | Whether the plugin is disabled by default     | `false`                              |
| `includesAssetPack`    | Whether the plugin includes an asset pack     | `false`                              |
| `outputDirectory`      | The output directory for the manifest file    | `${project.build.directory}/classes` |

### Authors

The `authors` parameter is a list of objects with the following properties:

- `name`: The name of the author
- `email`: The email of the author
- `url`: The website of the author

If `authors` is not specified, it will use the `developers` and `contributors` from
the project (`pom.xml`).

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

The `dependencies`, `optionalDependencies` and `loadBefore` parameters are lists
of objects with the following properties:

- `name`: The name of the dependency (**Required**)
- `version`: The version of the dependency (**Required**)

```xml
<dependencies>
    <dependency>
        <name>dependency-name</name>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```
