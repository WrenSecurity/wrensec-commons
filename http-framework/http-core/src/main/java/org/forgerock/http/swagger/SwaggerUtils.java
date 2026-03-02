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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Swagger utility. */
public final class SwaggerUtils {

    /**
     * Clone an {@code OpenAPI} instance.
     * @param descriptor The instance to clone.
     * @return The newly cloned instance.
     */
    public static OpenAPI clone(OpenAPI descriptor) {
        OpenAPI openApi = new SwaggerExtended();
        openApi.setInfo(descriptor.getInfo());
        openApi.setTags(descriptor.getTags() != null ? new ArrayList<>(descriptor.getTags()) : null);
        openApi.setExtensions(descriptor.getExtensions());
        if (descriptor.getServers() != null) {
            openApi.setServers(new ArrayList<>(descriptor.getServers()));
        }
        if (descriptor.getPaths() != null) {
            Paths paths = new Paths();
            paths.putAll(descriptor.getPaths());
            openApi.setPaths(paths);
        }
        if (descriptor.getSecurity() != null) {
            openApi.setSecurity(new ArrayList<>(descriptor.getSecurity()));
        }
        if (descriptor.getComponents() != null) {
            Components src = descriptor.getComponents();
            Components dest = new Components();
            if (src.getSchemas() != null) {
                dest.setSchemas(new LinkedHashMap<>(src.getSchemas()));
            }
            if (src.getParameters() != null) {
                dest.setParameters(new LinkedHashMap<>(src.getParameters()));
            }
            if (src.getResponses() != null) {
                dest.setResponses(new LinkedHashMap<>(src.getResponses()));
            }
            if (src.getSecuritySchemes() != null) {
                dest.setSecuritySchemes(new LinkedHashMap<>(src.getSecuritySchemes()));
            }
            openApi.setComponents(dest);
        }
        return openApi;
    }

    private SwaggerUtils() {
        // utility class
    }
}
