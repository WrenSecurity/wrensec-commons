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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import org.forgerock.util.i18n.LocalizableString;

/**
 * Localizable {@link Parameter} base class.
 *
 * <p>In OpenAPI 3.0, all parameter types (header, query, path) are represented by a single
 * {@code Parameter} class with an {@code in} field.
 */
class LocalizableParameter extends Parameter implements LocalizableDescription<Parameter> {

    private LocalizableString description;

    @Override
    public LocalizableParameter description(LocalizableString desc) {
        this.description = desc;
        return this;
    }

    @Override
    public LocalizableParameter description(String description) {
        setDescription(description);
        return this;
    }

    @Override
    public void setDescription(String description) {
        super.setDescription(description);
        this.description = new LocalizableString(description);
    }

    @Override
    @JsonProperty("description")
    public LocalizableString getLocalizableDescription() {
        return description;
    }

    @Override
    @JsonIgnore
    public String getDescription() {
        return super.getDescription();
    }

    /**
     * Convenience method to set the type on the parameter's schema.
     * In OpenAPI 3.0, parameter type is expressed through the schema.
     *
     * @param type The type string (e.g., "string", "integer", "boolean").
     */
    public void setType(String type) {
        ensureSchema().setType(type);
    }

    /**
     * Convenience method to set enum values on the parameter's schema.
     *
     * @param enumValues The enum values.
     */
    @SuppressWarnings({ "unchecked" })
    public void setEnum(List<?> enumValues) {
        ensureSchema().setEnum(enumValues);
    }

    /**
     * Convenience method to set the collection format equivalent.
     * In OpenAPI 3.0, "csv" collection format maps to style=form, explode=false for query parameters.
     *
     * @param collectionFormat The collection format (e.g., "csv").
     */
    public void setCollectionFormat(String collectionFormat) {
        if ("csv".equals(collectionFormat)) {
            setStyle(StyleEnum.FORM);
            setExplode(false);
        }
    }

    /**
     * Convenience method to set default value on the parameter's schema.
     *
     * @param defaultValue The default value.
     */
    public void setDefault(String defaultValue) {
        ensureSchema().setDefault(defaultValue);
    }

    @SuppressWarnings("rawtypes")
    private Schema ensureSchema() {
        Schema schema = getSchema();
        if (schema == null) {
            schema = new Schema<>();
            setSchema(schema);
        }
        return schema;
    }

}
