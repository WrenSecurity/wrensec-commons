/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 * Portions Copyright 2026 Wren Security.
 */
package org.forgerock.http.swagger;

import static java.util.Collections.singletonList;
import static org.forgerock.http.util.Paths.addLeadingSlash;
import static org.forgerock.http.util.Paths.removeTrailingSlash;
import static org.wrensecurity.guava.common.base.Strings.isNullOrEmpty;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.forgerock.http.ApiProducer;
import org.forgerock.http.header.AcceptApiVersionHeader;
import org.forgerock.http.routing.Version;
import org.wrensecurity.guava.common.base.Function;

/**
 * An API Producer for APIs that use the OpenAPI 3.0 model implementation of the OpenAPI specification.
 */
public class SwaggerApiProducer implements ApiProducer<OpenAPI> {

    private final String basePath;
    private final Info info;
    private final String host;
    private final boolean secure;

    /**
     * Create a new API Description Producer with {@literal null} as basePath, host and no scheme.
     *
     * @param info The {@code Info} instance to add to all OpenAPI descriptors.
     */
    public SwaggerApiProducer(Info info) {
        this(info, null, null, false);
    }

    /**
     * Create a new API Description Producer.
     *
     * @param info The {@code Info} instance to add to all OpenAPI descriptors.
     * @param basePath The base path.
     * @param host The host, if known at construction time, otherwise null.
     * @param secure Whether to use HTTPS ({@code true}) or HTTP ({@code false}).
     */
    public SwaggerApiProducer(Info info, String basePath, String host, boolean secure) {
        this.info = info;
        this.basePath = basePath;
        this.host = host;
        this.secure = secure;
    }

    @Override
    public OpenAPI withPath(OpenAPI descriptor, String parentPath) {
        return transform(descriptor, new PathTransformer(parentPath));
    }

    private static class PathTransformer implements Function<Map<String, PathItem>, Map<String, PathItem>> {

        private final String parentPath;

        PathTransformer(String parentPath) {
            this.parentPath = addLeadingSlash(removeTrailingSlash(parentPath));
        }

        @Override
        public Map<String, PathItem> apply(Map<String, PathItem> pathMap) {
            Map<String, PathItem> result = new HashMap<>(pathMap.size());
            for (Map.Entry<String, PathItem> entry : pathMap.entrySet()) {
                String key = entry.getKey();
                result.put(parentPath + addLeadingSlash(key), entry.getValue());
            }
            return result;
        }

    }

    @Override
    public OpenAPI withVersion(OpenAPI descriptor, Version version) {
        return transform(descriptor, new VersionTransformer(version));
    }

    private static class VersionTransformer implements Function<Map<String, PathItem>, Map<String, PathItem>> {

        public static final String PATH_FRAGMENT_MARKER = "#";
        public static final String PATH_FRAGMENT_COMPONENT_SEPARATOR = "_";
        private final Version version;

        VersionTransformer(Version version) {
            this.version = version;
        }

        @Override
        public Map<String, PathItem> apply(Map<String, PathItem> pathMap) {
            Map<String, PathItem> result = new HashMap<>(pathMap.size());
            for (Map.Entry<String, PathItem> entry : pathMap.entrySet()) {
                String key = entry.getKey();
                PathItem pathItem = entry.getValue();
                Parameter acceptVersionHeader = new Parameter()
                        .in("header")
                        .name(AcceptApiVersionHeader.NAME)
                        .schema(new Schema<String>().type("string")
                                ._enum(singletonList(AcceptApiVersionHeader.RESOURCE + "=" + version)));
                pathItem.addParametersItem(acceptVersionHeader);
                if (key.contains(PATH_FRAGMENT_MARKER)) {
                    result.put(key + PATH_FRAGMENT_COMPONENT_SEPARATOR + version, pathItem);
                } else {
                    result.put(key + PATH_FRAGMENT_MARKER + version, pathItem);
                }
            }
            return result;
        }

    }

    private OpenAPI transform(OpenAPI descriptor, Function<Map<String, PathItem>,
            Map<String, PathItem>> transformer) {
        OpenAPI openApi = addApiInfo(SwaggerUtils.clone(descriptor));
        Paths paths = new Paths();
        paths.putAll(transformer.apply(descriptor.getPaths()));
        openApi.setPaths(paths);
        return openApi;
    }

    @Override
    public OpenAPI merge(List<OpenAPI> descriptors) {
        descriptors = new ArrayList<>(descriptors);
        descriptors.removeAll(Collections.<OpenAPI>singletonList(null));
        if (descriptors.isEmpty()) {
            return null;
        }

        OpenAPI openApi = addApiInfo(new SwaggerExtended());
        Components components = openApi.getComponents();
        if (components == null) {
            components = new Components();
            openApi.setComponents(components);
        }
        for (OpenAPI descriptor : descriptors) {
            for (Tag tag : ensureNotNull(descriptor.getTags())) {
                openApi.addTagsItem(tag);
            }
            Components srcComponents = descriptor.getComponents();
            if (srcComponents != null) {
                for (Map.Entry<String, ApiResponse> response
                        : ensureNotNull(srcComponents.getResponses()).entrySet()) {
                    if (isUndefinedEntry("response", response, components.getResponses())) {
                        components.addResponses(response.getKey(), response.getValue());
                    }
                }
                for (Map.Entry<String, Parameter> parameter
                        : ensureNotNull(srcComponents.getParameters()).entrySet()) {
                    if (isUndefinedEntry("parameter", parameter, components.getParameters())) {
                        components.addParameters(parameter.getKey(), parameter.getValue());
                    }
                }
                for (Map.Entry<String, Object> extension
                        : ensureNotNull(srcComponents.getExtensions()).entrySet()) {
                    if (isUndefinedEntry("extension", extension, openApi.getExtensions())) {
                        openApi.addExtension(extension.getKey(), extension.getValue());
                    }
                }
                for (Map.Entry<String, Schema> definition
                        : ensureNotNull(srcComponents.getSchemas()).entrySet()) {
                    if (isUndefinedEntry("definition", definition, components.getSchemas())) {
                        components.addSchemas(definition.getKey(), definition.getValue());
                    }
                }
                for (Map.Entry<String, SecurityScheme> secDef
                        : ensureNotNull(srcComponents.getSecuritySchemes()).entrySet()) {
                    if (isUndefinedEntry("security definition", secDef, components.getSecuritySchemes())) {
                        components.addSecuritySchemes(secDef.getKey(), secDef.getValue());
                    }
                }
            }
            if (descriptor.getExtensions() != null) {
                for (Map.Entry<String, Object> extension : descriptor.getExtensions().entrySet()) {
                    if (isUndefinedEntry("extension", extension, openApi.getExtensions())) {
                        openApi.addExtension(extension.getKey(), extension.getValue());
                    }
                }
            }
            Paths descriptorPaths = descriptor.getPaths();
            if (descriptorPaths != null) {
                Paths openApiPaths = openApi.getPaths();
                if (openApiPaths == null) {
                    openApiPaths = new Paths();
                    openApi.setPaths(openApiPaths);
                }
                for (Map.Entry<String, PathItem> path : descriptorPaths.entrySet()) {
                    validatePathNotDefined(path.getKey(),
                            openApiPaths.keySet());
                    openApiPaths.addPathItem(path.getKey(), path.getValue());
                }
            }
            for (SecurityRequirement security : ensureNotNull(descriptor.getSecurity())) {
                openApi.addSecurityItem(security);
            }
        }
        return openApi;
    }

    private <T> Map<String, T> ensureNotNull(Map<String, T> map) {
        return map == null ? Collections.<String, T>emptyMap() : map;
    }

    private <T> List<T> ensureNotNull(List<T> list) {
        return list == null ? Collections.<T>emptyList() : list;
    }

    @Override
    public OpenAPI addApiInfo(OpenAPI openApi) {
        if (info != null) {
            Info existingInfo = openApi.getInfo();
            if (existingInfo == null) {
                openApi.setInfo(info);
            } else {
                // Merge: prefer existing values, fill in from this.info
                if (existingInfo.getTitle() == null) {
                    existingInfo.setTitle(info.getTitle());
                }
                if (existingInfo.getDescription() == null) {
                    existingInfo.setDescription(info.getDescription());
                }
                if (existingInfo.getVersion() == null) {
                    existingInfo.setVersion(info.getVersion());
                }
            }
        }
        if (host != null || basePath != null) {
            String scheme = secure ? "https" : "http";
            String serverUrl = scheme + "://" + (host != null ? host : "localhost")
                    + (basePath != null ? basePath : "");
            if (openApi.getServers() == null || openApi.getServers().isEmpty()) {
                openApi.setServers(List.of(new Server().url(serverUrl)));
            }
        }
        return openApi;
    }

    private <V> boolean isUndefinedEntry(String entryType, Map.Entry<String, V> entry, Map<String, V> existing) {
        V value = existing == null ? null : existing.get(entry.getKey());
        if (value == null) {
            return true;
        }
        if (value.equals(entry.getValue())) {
            return false;
        }
        throw new IllegalArgumentException("Duplicated key for " + entryType + " but different value. Already got "
                + value);
    }

    private void validatePathNotDefined(String path, Set<String> paths) {
        if (paths.contains(path)) {
            throw new IllegalArgumentException("Duplicated path");
        }
    }

    @Override
    public ApiProducer<OpenAPI> newChildProducer(String subPath) {
        return new SwaggerApiProducer(info, isNullOrEmpty(basePath) ? subPath : basePath + subPath, host, secure);
    }
}
