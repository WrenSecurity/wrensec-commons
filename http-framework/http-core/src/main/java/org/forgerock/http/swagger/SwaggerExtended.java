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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * Extension of {@link OpenAPI} to override some of its behaviors.
 */
public class SwaggerExtended extends OpenAPI {

    /**
     * Adds a tag item, de-duplicating by tag name.
     *
     * @param tagsItem The tag to add.
     * @return This instance.
     */
    @Override
    public OpenAPI addTagsItem(Tag tagsItem) {
        if (getTags() != null) {
            for (Tag existing : getTags()) {
                if (existing.getName() != null && existing.getName().equals(tagsItem.getName())) {
                    return this;
                }
            }
        }
        return super.addTagsItem(tagsItem);
    }
}
