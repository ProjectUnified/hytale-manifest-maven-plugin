package io.github.projectunified.hytalemanifestplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Mojo that updates the version of the {@code com.hypixel.hytale:Server}
 * dependency in the consumer project's {@code pom.xml} to either the latest
 * or release version from the Hytale Maven repository.
 * <p>
 * By default it updates the {@code <version>} element of the dependency
 * directly. If {@code propertyName} is set, it updates the corresponding
 * property in the {@code <properties>} section instead.
 * <p>
 * This Mojo uses DOM parsing to locate the element, then performs a
 * targeted text replacement on the raw file content to preserve the
 * original formatting, comments, and whitespace.
 */
@SuppressWarnings({ "FieldMayBeFinal", "FieldCanBeLocal", "unused" })
@Mojo(name = "updateServerDependency")
public class UpdateServerDependencyMojo extends AbstractMojo {

    /**
     * The version type to fetch from the Hytale Maven metadata.
     * Accepted values: {@code LATEST} or {@code RELEASE} (case-insensitive).
     */
    @Parameter(property = "hytale.server.versionType", defaultValue = "RELEASE")
    private String serverVersionType;

    /**
     * The POM file to update.
     */
    @Parameter(property = "hytale.server.pom", defaultValue = "${project.basedir}/pom.xml")
    private File pom;

    /**
     * If set, update this property in the {@code <properties>} section of the
     * POM instead of the dependency's {@code <version>} element.
     * <p>
     * For example, if the dependency version is defined as
     * {@code ${hytale.server.version}}, set this to
     * {@code hytale.server.version} to update the property.
     */
    @Parameter(property = "hytale.server.propertyName")
    private String propertyName;

    /**
     * The patchline to use when fetching the server version.
     * Accepted values: {@code RELEASE} or {@code PRERELEASE}
     * (case-insensitive). Determines which Maven repository metadata
     * URL is queried.
     */
    @Parameter(property = "hytale.server.patchline", defaultValue = "RELEASE")
    private String patchline;

    private static final String TARGET_GROUP_ID = "com.hypixel.hytale";
    private static final String TARGET_ARTIFACT_ID = "Server";

    @Override
    public void execute() throws MojoExecutionException {
        // 1. Fetch the resolved version
        String resolvedVersion;
        try {
            resolvedVersion = HytaleMetadataUtil.fetchVersion(serverVersionType, patchline);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to fetch server version", e);
        }
        getLog().info("Resolved Hytale Server version (" + serverVersionType + "): " + resolvedVersion);

        // 2. Validate the POM file exists
        if (!pom.exists()) {
            throw new MojoExecutionException("POM file not found: " + pom.getAbsolutePath());
        }

        // 3. Parse with DOM to find the old version
        Document doc;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(pom);
            doc.getDocumentElement().normalize();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new MojoExecutionException("Failed to parse POM file: " + pom.getAbsolutePath(), e);
        }

        String oldVersion;
        String xmlTag;
        String label;

        if (propertyName != null && !propertyName.isEmpty()) {
            // Update a property in <properties>
            oldVersion = findPropertyValue(doc, propertyName);
            if (oldVersion == null) {
                throw new MojoExecutionException(
                        "Property <" + propertyName + "> not found in " + pom.getAbsolutePath());
            }
            xmlTag = propertyName;
            label = "property <" + propertyName + ">";
        } else {
            // Update the dependency version directly
            oldVersion = findDependencyVersion(doc);
            if (oldVersion == null) {
                throw new MojoExecutionException("Dependency " + TARGET_GROUP_ID + ":" + TARGET_ARTIFACT_ID
                        + " not found in " + pom.getAbsolutePath());
            }
            xmlTag = "version";
            label = TARGET_GROUP_ID + ":" + TARGET_ARTIFACT_ID;
        }

        if (oldVersion.equals(resolvedVersion)) {
            getLog().info("Version is already up to date: " + resolvedVersion);
            return;
        }

        // 4. Read raw text and do targeted replacement to preserve formatting
        String content;
        try {
            content = new String(Files.readAllBytes(pom.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to read POM file: " + pom.getAbsolutePath(), e);
        }

        String target = "<" + xmlTag + ">" + oldVersion + "</" + xmlTag + ">";
        int index = content.indexOf(target);
        if (index < 0) {
            throw new MojoExecutionException("Could not locate <" + xmlTag + "> element in raw POM text");
        }

        String replacement = "<" + xmlTag + ">" + resolvedVersion + "</" + xmlTag + ">";
        String updatedContent = content.substring(0, index)
                + replacement
                + content.substring(index + target.length());

        getLog().info("Updated " + label + " from " + oldVersion + " to " + resolvedVersion);

        // 5. Write back, preserving original formatting
        try {
            Files.write(pom.toPath(), updatedContent.getBytes(StandardCharsets.UTF_8));
            getLog().info("Updated POM file: " + pom.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write updated POM file", e);
        }
    }

    /**
     * Uses DOM to find the version of the target dependency.
     *
     * @return the version string, or {@code null} if the dependency is not found
     */
    private String findDependencyVersion(Document doc) {
        NodeList dependencyNodes = doc.getElementsByTagName("dependency");
        for (int i = 0; i < dependencyNodes.getLength(); i++) {
            Element dep = (Element) dependencyNodes.item(i);
            String groupId = getChildText(dep, "groupId");
            String artifactId = getChildText(dep, "artifactId");

            if (TARGET_GROUP_ID.equals(groupId) && TARGET_ARTIFACT_ID.equals(artifactId)) {
                return getChildText(dep, "version");
            }
        }
        return null;
    }

    /**
     * Uses DOM to find the value of a property in the {@code <properties>} section.
     *
     * @param doc          the parsed POM document
     * @param propertyName the property name to look up
     * @return the property value, or {@code null} if not found
     */
    private String findPropertyValue(Document doc, String propertyName) {
        NodeList propertiesNodes = doc.getElementsByTagName("properties");
        for (int i = 0; i < propertiesNodes.getLength(); i++) {
            Element properties = (Element) propertiesNodes.item(i);
            String value = getChildText(properties, propertyName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String getChildText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }
}
