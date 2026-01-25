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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal", "unused"})
@Mojo(name = "generateManifest", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GenerateManifestMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.groupId}")
    private String group;

    @Parameter(defaultValue = "${project.name}")
    private String name;

    @Parameter(defaultValue = "${project.version}")
    private String version;

    @Parameter(defaultValue = "${project.description}")
    private String description;

    @Parameter(defaultValue = "${project.url}")
    private String website;

    @Parameter(required = true)
    private String main;

    @Parameter
    private List<Author> authors;

    @Parameter
    private String serverVersion = "*";

    @Parameter
    private Map<String, String> dependencies = Collections.emptyMap();

    @Parameter
    private Map<String, String> optionalDependencies = Collections.emptyMap();

    @Parameter
    private boolean disabledByDefault = false;

    @Parameter
    private boolean includesAssetPack = false;

    @Parameter(defaultValue = "${project.build.directory}/classes")
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        if (authors == null || authors.isEmpty()) {
            authors = new ArrayList<>();
            if (project.getDevelopers() != null) {
                for (Developer developer : project.getDevelopers()) {
                    authors.add(new Author(developer.getName(), developer.getEmail(), developer.getUrl()));
                }
            }
            if (project.getContributors() != null) {
                for (Contributor contributor : project.getContributors()) {
                    authors.add(new Author(contributor.getName(), contributor.getEmail(), contributor.getUrl()));
                }
            }
        }

        Manifest manifest = new Manifest(
                group,
                name,
                version,
                description,
                authors,
                website,
                serverVersion,
                dependencies,
                optionalDependencies,
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

        try (Writer writer = new FileWriter(manifestFile)) {
            gson.toJson(manifest, writer);
            getLog().info("Generated manifest file at " + manifestFile.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Error writing manifest file", e);
        }
    }

    public static class Author {
        @Parameter
        @SerializedName("Name")
        private String name;

        @Parameter
        @SerializedName("Email")
        private String email;

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
        private final List<Author> authors;
        @SerializedName("Website")
        private final String website;
        @SerializedName("ServerVersion")
        private final String serverVersion;
        @SerializedName("Dependencies")
        private final Map<String, String> dependencies;
        @SerializedName("OptionalDependencies")
        private final Map<String, String> optionalDependencies;
        @SerializedName("DisabledByDefault")
        private final boolean disabledByDefault;
        @SerializedName("IncludesAssetPack")
        private final boolean includesAssetPack;
        @SerializedName("Main")
        private final String main;

        public Manifest(String group, String name, String version, String description, List<Author> authors,
                String website, String serverVersion, Map<String, String> dependencies,
                Map<String, String> optionalDependencies, boolean disabledByDefault, boolean includesAssetPack,
                String main) {
            this.group = group;
            this.name = name;
            this.version = version;
            this.description = description;
            this.authors = authors;
            this.website = website;
            this.serverVersion = serverVersion;
            this.dependencies = dependencies;
            this.optionalDependencies = optionalDependencies;
            this.disabledByDefault = disabledByDefault;
            this.includesAssetPack = includesAssetPack;
            this.main = main;
        }
    }
}
