package com.shopsphere.util;

/**
 * Resolves product image references so that bundled images keep working
 * no matter which context path the application is deployed under.
 *
 * <p>Supported values:</p>
 * <ul>
 *   <li>{@code https://…}, {@code http://…}, {@code //…} or {@code data:…}
 *       — used as-is (external images entered by an administrator)</li>
 *   <li>{@code /images/foo.svg} — prefixed with the context path</li>
 *   <li>{@code images/foo.svg} — prefixed with the context path</li>
 *   <li>empty / null — the bundled placeholder</li>
 * </ul>
 */
public final class Images {

    private static final String PLACEHOLDER = "images/placeholder.svg";

    private Images() {
    }

    public static String resolve(String contextPath, String imageUrl) {

        String base = (contextPath == null || contextPath.isBlank())
                ? ""
                : contextPath;

        if (imageUrl == null || imageUrl.isBlank()) {
            return base + "/" + PLACEHOLDER;
        }

        String url = imageUrl.trim();

        if (url.startsWith("http://") || url.startsWith("https://")
                || url.startsWith("data:") || url.startsWith("//")) {
            return url;
        }

        if (url.startsWith("/")) {
            return base + url;
        }

        return base + "/" + url;
    }
}
