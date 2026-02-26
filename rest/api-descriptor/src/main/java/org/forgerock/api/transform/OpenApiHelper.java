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
package org.forgerock.api.transform;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;

/**
 * Helper methods for applying commonly needed changes to the {@link OpenAPI} model.
 */
public final class OpenApiHelper {

    private OpenApiHelper() {
        // hidden
    }

    /**
     * Adds a header to all operations. For example, one may need to add authentication headers for
     * username and password.
     *
     * @param header Header parameter model (must have {@code in} set to "header")
     * @param openApi OpenAPI model
     */
    public static void addHeaderToAllOperations(final Parameter header, final OpenAPI openApi) {
        final String headerKey = header.getName() + "_header";
        Components components = openApi.getComponents();
        if (components == null) {
            components = new Components();
            openApi.setComponents(components);
        }
        if (components.getParameters() != null && components.getParameters().get(headerKey) != null) {
            throw new IllegalStateException("Header already exists with name: " + header.getName());
        }

        components.addParameters(headerKey, header);
        final Parameter refParameter = new Parameter().$ref("#/components/parameters/" + headerKey);

        if (openApi.getPaths() != null) {
            for (final PathItem pathItem : openApi.getPaths().values()) {
                for (final Operation operation : pathItem.readOperations()) {
                    operation.addParametersItem(refParameter);
                }
            }
        }
    }

    /**
     * Visits all operations.
     *
     * @param visitor Operation visitor
     * @param openApi OpenAPI model
     */
    public static void visitAllOperations(final OperationVisitor visitor, final OpenAPI openApi) {
        if (openApi.getPaths() != null) {
            for (final PathItem pathItem : openApi.getPaths().values()) {
                for (final Operation operation : pathItem.readOperations()) {
                    visitor.visit(operation);
                }
            }
        }
    }

    /**
     * Visits an OpenAPI {@code Operation}.
     */
    public interface OperationVisitor {
        /**
         * Visits an OpenAPI {@code Operation}.
         *
         * @param operation Operation
         */
        void visit(Operation operation);
    }

}
