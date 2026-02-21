package io.github.projectunified.hytalemanifestplugin;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility class for fetching version information from the Hytale Maven
 * repository.
 */
public final class HytaleMetadataUtil {

    /** Metadata URL for the stable release patchline. */
    private static final String RELEASE_METADATA_URL = "https://maven.hytale.com/release/com/hypixel/hytale/Server/maven-metadata.xml";

    /** Metadata URL for the pre-release patchline. */
    private static final String PRERELEASE_METADATA_URL = "https://maven.hytale.com/pre-release/com/hypixel/hytale/Server/maven-metadata.xml";

    private HytaleMetadataUtil() {
    }

    /**
     * Returns the metadata URL for the given patchline.
     *
     * @param patchline the patchline — {@code "RELEASE"} or {@code "PRERELEASE"}
     *                  (case-insensitive)
     * @return the metadata URL
     * @throws IOException if the patchline value is not recognized
     */
    public static String getMetadataUrl(String patchline) throws IOException {
        if ("RELEASE".equalsIgnoreCase(patchline)) {
            return RELEASE_METADATA_URL;
        } else if ("PRERELEASE".equalsIgnoreCase(patchline)) {
            return PRERELEASE_METADATA_URL;
        } else {
            throw new IOException("Unknown patchline: " + patchline
                    + ". Expected 'RELEASE' or 'PRERELEASE'.");
        }
    }

    /**
     * Fetches a version string from the Hytale Server maven-metadata.xml
     * using the default {@code RELEASE} patchline.
     *
     * @param type the version type to fetch — {@code "latest"} or
     *             {@code "release"} (case-insensitive)
     * @return the resolved version string
     * @throws IOException if the metadata cannot be fetched or parsed
     */
    public static String fetchVersion(String type) throws IOException {
        return fetchVersion(type, "RELEASE");
    }

    /**
     * Fetches a version string from the Hytale Server maven-metadata.xml
     * for the specified patchline.
     *
     * @param type      the version type to fetch — {@code "latest"} or
     *                  {@code "release"} (case-insensitive)
     * @param patchline the patchline — {@code "RELEASE"} or
     *                  {@code "PRERELEASE"} (case-insensitive)
     * @return the resolved version string
     * @throws IOException if the metadata cannot be fetched or parsed
     */
    public static String fetchVersion(String type, String patchline) throws IOException {
        String metadataUrl = getMetadataUrl(patchline);
        HttpURLConnection connection = null;
        try {
            URL url = new URL(metadataUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Failed to fetch metadata from " + metadataUrl
                        + ": HTTP " + responseCode);
            }

            try (InputStream is = connection.getInputStream()) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(is);
                doc.getDocumentElement().normalize();

                String tagName;
                if ("latest".equalsIgnoreCase(type)) {
                    tagName = "latest";
                } else if ("release".equalsIgnoreCase(type)) {
                    tagName = "release";
                } else {
                    throw new IOException("Unknown version type: " + type
                            + ". Expected 'latest' or 'release'.");
                }

                var nodes = doc.getElementsByTagName(tagName);
                if (nodes.getLength() == 0) {
                    throw new IOException("No <" + tagName + "> element found in maven-metadata.xml");
                }
                return nodes.item(0).getTextContent().trim();
            } catch (ParserConfigurationException | SAXException e) {
                throw new IOException("Failed to parse maven-metadata.xml", e);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
