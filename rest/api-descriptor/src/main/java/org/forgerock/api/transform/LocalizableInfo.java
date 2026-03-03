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
import io.swagger.v3.oas.models.info.Info;
import org.forgerock.util.i18n.LocalizableString;

/**
 * A localizable {@code Info}.
 */
class LocalizableInfo extends Info implements LocalizableTitleAndDescription<Info> {

    private LocalizableString locTitle;
    private LocalizableString locDescription;

    @Override
    public LocalizableInfo title(LocalizableString title) {
        this.locTitle = title;
        return this;
    }

    @Override
    public LocalizableInfo description(LocalizableString description) {
        this.locDescription = description;
        return this;
    }

    @Override
    public LocalizableInfo title(String title) {
        setTitle(title);
        return this;
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        this.locTitle = new LocalizableString(title);
    }

    @Override
    public LocalizableInfo description(String description) {
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

    /**
     * Merge another Info's values into this one (existing values take precedence).
     *
     * @param info The info to merge.
     * @return This info.
     */
    public LocalizableInfo mergeWith(Info info) {
        if (info == null) {
            return this;
        }
        if (info instanceof LocalizableInfo) {
            LocalizableInfo localizableInfo = (LocalizableInfo) info;
            if (localizableInfo.locDescription != null && this.locDescription == null) {
                this.locDescription = localizableInfo.locDescription;
                super.setDescription(localizableInfo.getDescription());
            }
            if (localizableInfo.locTitle != null && this.locTitle == null) {
                this.locTitle = localizableInfo.locTitle;
                super.setTitle(localizableInfo.getTitle());
            }
        } else {
            if (info.getDescription() != null && getDescription() == null) {
                description(info.getDescription());
            }
            if (info.getTitle() != null && getTitle() == null) {
                title(info.getTitle());
            }
        }
        if (info.getVersion() != null && getVersion() == null) {
            setVersion(info.getVersion());
        }
        return this;
    }
}
