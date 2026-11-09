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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.selfservice.stages.user;

import java.util.Objects;
import java.util.Set;

import org.forgerock.selfservice.core.config.StageConfig;

/**
 * Configuration for the user details stage.
 *
 * @since 0.2.0
 */
public final class UserDetailsConfig implements StageConfig {

    /**
     * Name of the stage configuration.
     */
    public static final String NAME = "userDetails";

    private String identityEmailField;

    private Set<String> allowedFields;

    /**
     * Gets the field name for the identity email address.
     *
     * @return the identity email address field name
     */
    public String getIdentityEmailField() {
        return identityEmailField;
    }

    /**
     * Sets the field name for the identity email address.
     *
     * @param identityEmailField
     *         the identity email address field name
     *
     * @return this config instance
     */
    public UserDetailsConfig setIdentityEmailField(String identityEmailField) {
        this.identityEmailField = identityEmailField;
        return this;
    }

    /**
     * Gets the names of the top-level user fields that may be supplied in the stage input.
     * When {@code null}, any field is accepted.
     *
     * @return the allowed field names, or {@code null} if not configured
     */
    public Set<String> getAllowedFields() {
        return allowedFields;
    }

    /**
     * Sets the names of the top-level user fields that may be supplied in the stage input.
     * Any other field submitted in the user object is rejected.
     *
     * @param allowedFields the allowed field names, or {@code null} to accept any field
     * @return this config instance
     */
    public UserDetailsConfig setAllowedFields(Set<String> allowedFields) {
        this.allowedFields = allowedFields == null ? null : Set.copyOf(allowedFields);
        return this;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getProgressStageClassName() {
        return UserDetailsStage.class.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof UserDetailsConfig)) {
            return false;
        }

        UserDetailsConfig that = (UserDetailsConfig) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getProgressStageClassName(), that.getProgressStageClassName())
                && Objects.equals(identityEmailField, that.identityEmailField)
                && Objects.equals(allowedFields, that.allowedFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getProgressStageClassName(), identityEmailField, allowedFields);
    }

}
