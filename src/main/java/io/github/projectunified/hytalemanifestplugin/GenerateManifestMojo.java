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

@SuppressWarnings({ "FieldMayBeFinal", "FieldCanBeLocal", "unused" })
@Mojo(name = "generateManifest", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GenerateManifestMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The group ID of the plugin.
     */
    @Parameter(required = true, defaultValue = "${project.groupId}", property = "hytale.manifest.group")
    private String group;

    /**
     * The name of the plugin.
     */
    @Parameter(readonly = true, defaultValue = "${project.name}", property = "hytale.manifest.name")
    private String name;

    /**
     * The version of the plugin.
     */
    @Parameter(required = true, defaultValue = "${project.version}", property = "hytale.manifest.version")
    private String version;

    /**
     * The description of the plugin.
     */
    @Parameter(defaultValue = "${project.description}", property = "hytale.manifest.description")
    private String description;

    /**
     * The website of the plugin.
     */
    @Parameter(defaultValue = "${project.url}", property = "hytale.manifest.website")
    private String website;

    /**
     * The main class of the plugin.
     */
    @Parameter(required = true, property = "hytale.manifest.main")
    private String main;

    /**
     * The authors of the plugin.
     * If not specified, it will use the developers and contributors from the
     * project.
     */
    @Parameter
    private Author[] authors;

    /**
     * The supported server version of the plugin.
     */
    @Parameter(property = "hytale.manifest.serverVersion")
    private String serverVersion = "*";

    /**
     * The dependencies of the plugin.
     */
    @Parameter
    private Dependency[] dependencies;

    /**
     * The optional dependencies of the plugin.
     */
    @Parameter
    private Dependency[] optionalDependencies;

    /**
     * The list of plugins to load before the plugin.
     */
    @Parameter
    private Dependency[] loadBefore;

    /**
     * Whether the plugin is disabled by default.
     */
    @Parameter(property = "hytale.manifest.disabledByDefault")
    private boolean disabledByDefault = false;

    /**
     * Whether the plugin includes an asset pack.
     */
    @Parameter(property = "hytale.manifest.includesAssetPack")
    private boolean includesAssetPack = false;

    /**
     * The output directory for the generated manifest file.
     */
    @Parameter(defaultValue = "${project.build.directory}/classes", property = "hytale.manifest.outputDirectory")
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        if (authors == null || authors.length == 0) {
            List<Author> authorList = new ArrayList<>();
            if (project.getDevelopers() != null) {
                for (Developer developer : project.getDevelopers()) {
                    authorList.add(new Author(developer.getName(), developer.getEmail(), developer.getUrl()));
                }
            }
            if (project.getContributors() != null) {
                for (Contributor contributor : project.getContributors()) {
                    authorList.add(new Author(contributor.getName(), contributor.getEmail(), contributor.getUrl()));
                }
            }
            authors = authorList.toArray(new Author[0]);
        }

        Manifest manifest = new Manifest(
                group,
                name,
                version,
                description,
                authors,
                website,
                serverVersion,
                processDependencies(dependencies),
                processDependencies(optionalDependencies),
                processDependencies(loadBefore),
                disabledByDefault,
                includesAssetPack,
                main);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File manifestFile = new File(outputDirectory, "manifest.json");

        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                throw new MojoExecutionException("Could not create output directory: " + outputDirectory);
            }
        }

        try (Writer writer = new FileWriter(manifestFile, StandardCharsets.UTF_8)) {
            gson.toJson(manifest, writer);
            getLog().info("Generated manifest file at " + manifestFile.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Error writing manifest file", e);
        }
    }

    private Map<String, String> processDependencies(Dependency[] dependencies) {
        if (dependencies == null || dependencies.length == 0) {
            return null;
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (Dependency dependency : dependencies) {
            map.put(dependency.getName(), dependency.getVersion());
        }
        return map;
    }

    public static class Dependency {
        /**
         * The name of the dependency.
         */
        @Parameter(required = true)
        private String name;

        /**
         * The version of the dependency.
         */
        @Parameter(required = true)
        private String version;

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }
    }

    public static class Author {
        /**
         * The name of the author.
         */
        @Parameter
        @SerializedName("Name")
        private String name;

        /**
         * The email of the author.
         */
        @Parameter
        @SerializedName("Email")
        private String email;

        /**
         * The website of the author.
         */
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

        public Manifest(String group, String name, String version, String description, Author[] authors,
                String website, String serverVersion, Map<String, String> dependencies,
                Map<String, String> optionalDependencies, Map<String, String> loadBefore,
                Boolean disabledByDefault, Boolean includesAssetPack,
                String main) {
            this.group = emptyToNull(group);
            this.name = emptyToNull(name);
            this.version = emptyToNull(version);
            this.description = emptyToNull(description);
            this.authors = authors != null && authors.length == 0 ? null : authors;
            this.website = emptyToNull(website);
            this.serverVersion = emptyToNull(serverVersion);
            this.dependencies = dependencies;
            this.optionalDependencies = optionalDependencies;
            this.loadBefore = loadBefore;
            this.disabledByDefault = disabledByDefault;
            this.includesAssetPack = includesAssetPack;
            this.main = emptyToNull(main);
        }

        private String emptyToNull(String s) {
            if (s == null || s.isEmpty()) {
                return null;
            }
            return s;
        }
    }
}
