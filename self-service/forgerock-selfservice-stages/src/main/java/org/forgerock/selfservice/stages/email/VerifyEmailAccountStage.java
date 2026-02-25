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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.selfservice.stages.email;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.selfservice.core.ServiceUtils.INITIAL_TAG;
import static org.forgerock.selfservice.stages.CommonStateFields.EMAIL_FIELD;
import static org.forgerock.selfservice.stages.utils.LocaleUtils.getTranslationFromLocaleMap;
import static org.wrensecurity.guava.common.base.Strings.isNullOrEmpty;

import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.IllegalStageTagException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.annotations.SelfService;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenCallback;
import org.forgerock.selfservice.core.util.RequirementsBuilder;
import org.forgerock.util.Reject;
import org.forgerock.util.i18n.PreferredLocales;

/**
 * Having retrieved the email address from the context or in response to the initial requirements, verifies the
 * validity of the email address with the user who submitted the requirements via an email flow.
 *
 * @since 0.1.0
 */
public final class VerifyEmailAccountStage implements ProgressStage<VerifyEmailAccountConfig> {

    static final String REQUIREMENT_KEY_CODE = "code";

    private static final String VALIDATE_CODE_TAG = "validateCode";

    private static final String SKIP_VALIDATION = "skipValidation";

    private final ConnectionFactory connectionFactory;

    /**
     * Constructs a new stage.
     *
     * @param connectionFactory
     *         the CREST connection factory
     */
    @Inject
    public VerifyEmailAccountStage(@SelfService ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context, VerifyEmailAccountConfig config)
            throws BadRequestException {
        Reject.ifNull(config.getEmailServiceUrl(), "Email service url should be configured");
        Reject.ifNull(config.getMessageTranslations(), "Email message should be configured");
        Reject.ifNull(config.getSubjectTranslations(), "Email subject should be configured");
        Reject.ifNull(config.getVerificationLink(), "Verification link should be configured");
        Reject.ifNull(config.getVerificationLinkToken(), "Verification link token should be configured");
        Reject.ifNull(config.getIdentityEmailField(), "Identity email field should be configured");

        verifyEmailState(context);
        return RequirementsBuilder.newEmptyRequirements();
    }

    private String verifyEmailState(ProcessContext context) throws BadRequestException {
        final JsonValue emailState = context.getState(EMAIL_FIELD);
        if (!context.containsState(EMAIL_FIELD)
                || emailState == null
                || isNullOrEmpty(emailState.asString())) {
            throw new BadRequestException("Unable to verify email");
        }
        return emailState.asString();
    }

    @Override
    public StageResponse advance(ProcessContext context, VerifyEmailAccountConfig config) throws ResourceException {
        if (context.getState(SKIP_VALIDATION) != null && context.getState(SKIP_VALIDATION).asBoolean()) {
            return StageResponse.newBuilder().build();
        }
        switch (context.getStageTag()) {
        case INITIAL_TAG:
            return sendEmail(context, config);
        case VALIDATE_CODE_TAG:
            return validateCode(context);
        }

        throw new IllegalStageTagException(context.getStageTag());
    }

    private StageResponse sendEmail(ProcessContext context, final VerifyEmailAccountConfig config)
            throws ResourceException {
        final String mail = verifyEmailState(context);
        final String code = UUID.randomUUID().toString();
        context.putState("code", code);

        JsonValue requirements = RequirementsBuilder
                .newInstance("Verify emailed code")
                .addRequireProperty("code", "Enter code emailed")
                .build();

        SnapshotTokenCallback callback = new SnapshotTokenCallback() {

            @Override
            public void snapshotTokenPreview(ProcessContext context, String snapshotToken)
                    throws ResourceException {
                sendEmail(context, snapshotToken, code, mail, config);
            }

        };

        return StageResponse.newBuilder()
                .setStageTag(VALIDATE_CODE_TAG)
                .setRequirements(requirements)
                .setCallback(callback)
                .build();
    }

    private StageResponse validateCode(ProcessContext context) throws ResourceException {
        String originalCode = context
                .getState("code")
                .asString();

        String submittedCode = context
                .getInput()
                .get("code")
                .asString();

        if (isNullOrEmpty(submittedCode)) {
            throw new BadRequestException("Input code is missing");
        }

        if (!originalCode.equals(submittedCode)) {
            throw new BadRequestException("Invalid code");
        }

        return StageResponse
                .newBuilder()
                .build();
    }

    private void sendEmail(ProcessContext processContext, String snapshotToken, String code,
            String email, VerifyEmailAccountConfig config) throws ResourceException {

        String emailUrl = config.getVerificationLink() + "&token=" + snapshotToken + "&code=" + code;

        PreferredLocales preferredLocales = processContext.getRequest().getPreferredLocales();
        String subjectText = getTranslationFromLocaleMap(preferredLocales, config.getSubjectTranslations());
        String bodyText = getTranslationFromLocaleMap(preferredLocales, config.getMessageTranslations());

        bodyText = bodyText.replace(config.getVerificationLinkToken(), emailUrl);

        try (Connection connection = connectionFactory.getConnection()) {
            ActionRequest request = Requests
                    .newActionRequest(config.getEmailServiceUrl(), "send")
                    .setContent(
                            json(
                                    object(
                                            field("to", email),
                                            field("from", config.getFrom()),
                                            field("subject", subjectText),
                                            field("type", config.getMimeType()),
                                            field("body", bodyText))));

            for (Map.Entry<String, String> parameter : config.getEmailServiceParameters().entrySet()) {
                request.setAdditionalParameter(parameter.getKey(), parameter.getValue());
            }

            connection.action(processContext.getRequestContext(), request);
        }
    }

}
