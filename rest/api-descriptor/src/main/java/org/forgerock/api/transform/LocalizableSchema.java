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
 * Portions Copyright 2026 Wren Security.
 */
package org.forgerock.api.transform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Objects;
import org.forgerock.util.i18n.LocalizableString;

/**
 * Localizable {@link Schema} that replaces all previous Localizable property and model types.
 * In OpenAPI 3.0, the unified Schema class replaces the separate Property and Model hierarchies.
 */
@SuppressWarnings("rawtypes")
class LocalizableSchema extends Schema implements LocalizableTitleAndDescription<Schema> {

    private LocalizableString locTitle;

    private LocalizableString locDescription;

    @Override
    public Schema title(LocalizableString title) {
        this.locTitle = title;
        return this;
    }

    @Override
    public Schema description(LocalizableString desc) {
        this.locDescription = desc;
        return this;
    }

    @Override
    public Schema title(String title) {
        setTitle(title);
        return this;
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        this.locTitle = new LocalizableString(title);
    }

    @Override
    public Schema description(String description) {
        setDescription(description);
        return this;
    }

    @Override
    public void setDescription(String description) {
        super.setDescription(description);
        this.locDescription = new LocalizableString(description);
    }

    @Override
    @JsonProperty("title")
    public LocalizableString getLocalizableTitle() {
        return locTitle;
    }

    @Override
    @JsonProperty("description")
    public LocalizableString getLocalizableDescription() {
        return locDescription;
    }

    @Override
    @JsonIgnore
    public String getTitle() {
        return super.getTitle();
    }

    @Override
    @JsonIgnore
    public String getDescription() {
        return super.getDescription();
    }

    @Override
    public boolean equals(final Object o) {
        if (!super.equals(o)) {
            return false;
        }
        if (!(o instanceof LocalizableSchema)) {
            return false;
        }
        final LocalizableSchema other = (LocalizableSchema) o;
        if (!Objects.equals(locTitle, other.locTitle)) {
            return false;
        }
        if (!Objects.equals(locDescription, other.locDescription)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), locTitle, locDescription);
    }

}
