package io.github.projectunified.hytalemanifestplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Developer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates a Hytale {@code manifest.json} file for the current project.
 * <p>
 * This Mojo is bound to the {@code generate-resources} phase by default
 * and writes the manifest to the build output directory so it is included
 * in the final JAR.
 *
 * <h2>Configuration</h2>
 * Most fields default to the corresponding Maven project properties
 * (groupId, name, version, description, url). The only <b>required</b>
 * field with no default is {@code main} — the fully-qualified entry-point
 * class.
 *
 * <h2>Author resolution</h2>
 * If no {@code <authors>} are explicitly configured, the Mojo falls back
 * to the project's {@code <developers>} and {@code <contributors>}.
 *
 * <h2>Server version resolution</h2>
 * If {@code serverVersion} is set to {@code LATEST} or {@code RELEASE}
 * (case-insensitive), the actual version is resolved from the
 * <a href=
 * "https://maven.hytale.com/release/com/hypixel/hytale/Server/maven-metadata.xml">
 * Hytale Maven repository metadata</a>.
 *
 * <h2>Example usage</h2>
 * 
 * <pre>{@code
 * <plugin>
 *   <groupId>io.github.projectunified</groupId>
 *   <artifactId>hytale-manifest-maven-plugin</artifactId>
 *   <configuration>
 *     <main>com.example.MyPlugin</main>
 *     <serverVersion>RELEASE</serverVersion>
 *   </configuration>
 * </plugin>
 * }</pre>
 */
@SuppressWarnings({ "FieldMayBeFinal", "FieldCanBeLocal", "unused" })
@Mojo(name = "generateManifest", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GenerateManifestMojo extends AbstractMojo {

    /** Injected Maven project — used to read developers/contributors. */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    // ---------------------------------------------------------------
    // Plugin identity
    // ---------------------------------------------------------------

    /**
     * The group identifier of the plugin (maps to the {@code "Group"} field).
     * Defaults to {@code ${project.groupId}}.
     */
    @Parameter(required = true, defaultValue = "${project.groupId}", property = "hytale.manifest.group")
    private String group;

    /**
     * The display name of the plugin (maps to {@code "Name"}).
     * Defaults to {@code ${project.name}}.
     */
    @Parameter(readonly = true, defaultValue = "${project.name}", property = "hytale.manifest.name")
    private String name;

    /**
     * The version of the plugin (maps to {@code "Version"}).
     * Defaults to {@code ${project.version}}.
     */
    @Parameter(required = true, defaultValue = "${project.version}", property = "hytale.manifest.version")
    private String version;

    /**
     * A short description of the plugin (maps to {@code "Description"}).
     * Defaults to {@code ${project.description}}.
     */
    @Parameter(defaultValue = "${project.description}", property = "hytale.manifest.description")
    private String description;

    /**
     * The plugin's website URL (maps to {@code "Website"}).
     * Defaults to {@code ${project.url}}.
     */
    @Parameter(defaultValue = "${project.url}", property = "hytale.manifest.website")
    private String website;

    /**
     * The fully-qualified name of the plugin's entry-point class
     * (maps to {@code "Main"}). <b>Required — no default.</b>
     */
    @Parameter(required = true, property = "hytale.manifest.main")
    private String main;

    // ---------------------------------------------------------------
    // Authors
    // ---------------------------------------------------------------

    /**
     * Explicit list of authors. If omitted the Mojo falls back to the
     * project's {@code <developers>} and {@code <contributors>}.
     *
     * <pre>{@code
     * <authors>
     *   <author>
     *     <name>Jane</name>
     *     <email>jane@example.com</email>
     *     <url>https://jane.dev</url>
     *   </author>
     * </authors>
     * }</pre>
     */
    @Parameter
    private Author[] authors;

    // ---------------------------------------------------------------
    // Server compatibility
    // ---------------------------------------------------------------

    /**
     * The server version this plugin is compatible with (maps to
     * {@code "ServerVersion"}). Use {@code "*"} for any version, or set
     * to {@code LATEST} / {@code RELEASE} to resolve the version from
     * the Hytale Maven repository at build time.
     */
    @Parameter(property = "hytale.manifest.serverVersion")
    private String serverVersion = "*";

    // ---------------------------------------------------------------
    // Dependencies
    // ---------------------------------------------------------------

    /**
     * Required plugin dependencies (maps to {@code "Dependencies"}).
     *
     * <pre>{@code
     * <dependencies>
     *   <dependency>
     *     <name>SomePlugin</name>
     *     <version>*</version>
     *   </dependency>
     * </dependencies>
     * }</pre>
     */
    @Parameter
    private Dependency[] dependencies;

    /**
     * Optional plugin dependencies (maps to {@code "OptionalDependencies"}).
     * Same format as {@code dependencies}.
     */
    @Parameter
    private Dependency[] optionalDependencies;

    /**
     * Plugins that should be loaded <em>before</em> this one
     * (maps to {@code "LoadBefore"}). Same format as {@code dependencies}.
     */
    @Parameter
    private Dependency[] loadBefore;

    // ---------------------------------------------------------------
    // Flags
    // ---------------------------------------------------------------

    /**
     * Whether the plugin should be disabled by default
     * (maps to {@code "DisabledByDefault"}).
     */
    @Parameter(property = "hytale.manifest.disabledByDefault")
    private boolean disabledByDefault = false;

    /**
     * Whether the plugin includes an asset pack
     * (maps to {@code "IncludesAssetPack"}).
     */
    @Parameter(property = "hytale.manifest.includesAssetPack")
    private boolean includesAssetPack = false;

    // ---------------------------------------------------------------
    // Output
    // ---------------------------------------------------------------

    /**
     * Directory where {@code manifest.json} will be written.
     * Defaults to {@code ${project.build.directory}/classes} so the
     * file is included in the packaged JAR.
     */
    @Parameter(defaultValue = "${project.build.directory}/classes", property = "hytale.manifest.outputDirectory")
    private File outputDirectory;

    // ===============================================================
    // Execution
    // ===============================================================

    @Override
    public void execute() throws MojoExecutionException {
        resolveAuthors();
        resolveServerVersion();

        Manifest manifest = buildManifest();
        writeManifest(manifest);
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    /**
     * Falls back to project developers/contributors when no authors are configured.
     */
    private void resolveAuthors() {
        if (authors != null && authors.length > 0) {
            return;
        }
        List<Author> list = new ArrayList<>();
        if (project.getDevelopers() != null) {
            for (Developer d : project.getDevelopers()) {
                list.add(new Author(d.getName(), d.getEmail(), d.getUrl()));
            }
        }
        if (project.getContributors() != null) {
            for (Contributor c : project.getContributors()) {
                list.add(new Author(c.getName(), c.getEmail(), c.getUrl()));
            }
        }
        authors = list.toArray(new Author[0]);
    }

    /** Resolves {@code LATEST} / {@code RELEASE} to a concrete version. */
    private void resolveServerVersion() throws MojoExecutionException {
        if (!"LATEST".equalsIgnoreCase(serverVersion) && !"RELEASE".equalsIgnoreCase(serverVersion)) {
            return;
        }
        try {
            String resolved = HytaleMetadataUtil.fetchVersion(serverVersion);
            getLog().info("Resolved serverVersion '" + serverVersion + "' to '" + resolved + "'");
            serverVersion = resolved;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to resolve server version '" + serverVersion + "'", e);
        }
    }

    /** Assembles the manifest data object from the configured parameters. */
    private Manifest buildManifest() {
        return new Manifest(
                group, name, version, description,
                authors, website, serverVersion,
                toDependencyMap(dependencies),
                toDependencyMap(optionalDependencies),
                toDependencyMap(loadBefore),
                disabledByDefault, includesAssetPack, main);
    }

    /** Writes the manifest to {@code manifest.json} inside the output directory. */
    private void writeManifest(Manifest manifest) throws MojoExecutionException {
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new MojoExecutionException("Could not create output directory: " + outputDirectory);
        }

        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        File manifestFile = new File(outputDirectory, "manifest.json");

        try (Writer writer = new FileWriter(manifestFile, StandardCharsets.UTF_8)) {
            gson.toJson(manifest, writer);
            getLog().info("Generated manifest file at " + manifestFile.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Error writing manifest file", e);
        }
    }

    /**
     * Converts the dependency array to an ordered map ({@code name → version}).
     *
     * @return the map, or {@code null} if the array is empty (so Gson omits the
     *         field)
     */
    private static Map<String, String> toDependencyMap(Dependency[] deps) {
        if (deps == null || deps.length == 0) {
            return null;
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (Dependency d : deps) {
            map.put(d.name, d.version);
        }
        return map;
    }

    /**
     * Returns {@code null} when the string is null or empty so that Gson
     * omits the field from the JSON output.
     */
    private static String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    // ===============================================================
    // Inner data classes
    // ===============================================================

    /**
     * A Hytale plugin dependency entry ({@code name → version}).
     */
    public static class Dependency {
        /** The name of the dependency plugin. */
        @Parameter(required = true)
        private String name;

        /** The required version (use {@code "*"} for any). */
        @Parameter(required = true)
        private String version;
    }

    /**
     * Represents a plugin author with optional contact information.
     */
    public static class Author {
        /** Author's display name. */
        @Parameter
        @SerializedName("Name")
        private String name;

        /** Author's email address (optional). */
        @Parameter
        @SerializedName("Email")
        private String email;

        /** Author's website URL (optional). */
        @Parameter
        @SerializedName("Url")
        private String url;

        public Author() {
        }

        public Author(String name, String email, String url) {
            this.name = name;
            this.email = email;
            this.url = url;
        }
    }

    /**
     * Internal data class serialized to JSON by Gson.
     * Field names are mapped via {@link SerializedName}.
     */
    private static class Manifest {
        @SerializedName("Group")
        private final String group;
        @SerializedName("Name")
        private final String name;
        @SerializedName("Version")
        private final String version;
        @SerializedName("Description")
        private final String description;
        @SerializedName("Authors")
        private final Author[] authors;
        @SerializedName("Website")
        private final String website;
        @SerializedName("ServerVersion")
        private final String serverVersion;
        @SerializedName("Dependencies")
        private final Map<String, String> dependencies;
        @SerializedName("OptionalDependencies")
        private final Map<String, String> optionalDependencies;
        @SerializedName("LoadBefore")
        private final Map<String, String> loadBefore;
        @SerializedName("DisabledByDefault")
        private final Boolean disabledByDefault;
        @SerializedName("IncludesAssetPack")
        private final Boolean includesAssetPack;
        @SerializedName("Main")
        private final String main;

        Manifest(String group, String name, String version, String description,
                Author[] authors, String website, String serverVersion,
                Map<String, String> dependencies, Map<String, String> optionalDependencies,
                Map<String, String> loadBefore, Boolean disabledByDefault,
                Boolean includesAssetPack, String main) {
            this.group = emptyToNull(group);
            this.name = emptyToNull(name);
            this.version = emptyToNull(version);
            this.description = emptyToNull(description);
            this.authors = (authors != null && authors.length == 0) ? null : authors;
            this.website = emptyToNull(website);
            this.serverVersion = emptyToNull(serverVersion);
            this.dependencies = dependencies;
            this.optionalDependencies = optionalDependencies;
            this.loadBefore = loadBefore;
            this.disabledByDefault = disabledByDefault;
            this.includesAssetPack = includesAssetPack;
            this.main = emptyToNull(main);
        }
    }
}
