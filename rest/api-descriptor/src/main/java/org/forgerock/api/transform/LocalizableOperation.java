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
import io.swagger.v3.oas.models.Operation;
import java.util.ArrayList;
import java.util.List;
import org.forgerock.util.i18n.LocalizableString;

/**
 * Localizable {@link Operation}.
 */
public class LocalizableOperation extends Operation implements LocalizableDescription<Operation> {

    private LocalizableString description;
    private List<LocalizableString> localizableTags;

    @Override
    public LocalizableOperation description(LocalizableString desc) {
        this.description = desc;
        return this;
    }

    @Override
    public LocalizableOperation description(String description) {
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

    @Override
    public Operation addTagsItem(String tag) {
        super.addTagsItem(tag);
        addTag(new LocalizableString(tag));
        return this;
    }

    /**
     * Adds a Tag, a String value that is metadata related to the Operation.
     * @param tag a LocalizableString
     */
    public void addTag(LocalizableString tag) {
        if (localizableTags == null) {
            localizableTags = new ArrayList<>();
        }
        localizableTags.add(tag);
    }

    @Override
    public void setTags(List<String> tags) {
        super.setTags(tags);
        localizableTags = new ArrayList<>();
        if (tags != null) {
            for (String tag : tags) {
                addTag(new LocalizableString(tag));
            }
        }
    }

    /**
     * Returns the localizable tags for this operation.
     *
     * @return the localizable tags for this operation
     */
    @JsonProperty("tags")
    public List<LocalizableString> getLocalizableTags() {
        return localizableTags;
    }

    @Override
    @JsonIgnore
    public List<String> getTags() {
        return super.getTags();
    }

    /**
     * Sets a vendor extension on this operation.
     *
     * @param name Extension name (must start with "x-").
     * @param value Extension value.
     */
    public void setVendorExtension(String name, Object value) {
        addExtension(name, value);
    }

}
