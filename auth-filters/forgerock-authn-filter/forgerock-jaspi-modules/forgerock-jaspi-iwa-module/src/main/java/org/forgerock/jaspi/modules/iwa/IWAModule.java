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
 * Copyright 2013-2016 ForgeRock AS.
 * Portions Copyright 2024 Wren Security
 */

package org.forgerock.jaspi.modules.iwa;

import static jakarta.security.auth.message.AuthStatus.*;
import static org.forgerock.caf.authentication.framework.AuthenticationFramework.LOG;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import jakarta.security.auth.message.AuthStatus;
import jakarta.security.auth.message.MessagePolicy;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.jaspi.modules.iwa.wdsso.Base64;
import org.forgerock.jaspi.modules.iwa.wdsso.WDSSO;
import org.forgerock.util.promise.Promise;

/**
 * Authentication module that uses IWA for authentication.
 *
 * @since 1.0.0
 */
public class IWAModule implements AsyncServerAuthModule {

    private static final String IWA_FAILED = "iwa-failed";
    private static final String NEGOTIATE_AUTH_SCHEME = "Negotiate";

    private Map<String, Object> options;

    @Override
    public String getModuleId() {
        return "IWA";
    }

    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler,
            Map<String, Object> options) throws AuthenticationException {
        this.options = options;
    }

    @Override
    public Collection<Class<?>> getSupportedMessageTypes() {
        return Arrays.asList(new Class<?>[]{ Request.class, Response.class });
    }

    /**
     * Validates the request by checking the Authorization header in the request for a IWA token and processes that
     * for authentication.
     */
    @Override
    public Promise<AuthStatus, AuthenticationException> validateRequest(MessageInfoContext messageInfo,
            Subject clientSubject, Subject serviceSubject) {
        Request request = messageInfo.getRequest();
        Response response = messageInfo.getResponse();

        String authorizationHeader = request.getHeaders().getFirst("Authorization");

        if (authorizationHeader == null || authorizationHeader.isEmpty()) {
            LOG.debug("IWAModule: Authorization Header NOT set in request.");

            response.getHeaders().put("WWW-Authenticate", NEGOTIATE_AUTH_SCHEME);
            response.setStatus(Status.UNAUTHORIZED);
            response.setEntity(Map.of("failure", true, "recason", IWA_FAILED));
            return newResultPromise(SEND_CONTINUE);
        }

        // Handle only negotiate authentication requests
        if (!authorizationHeader.startsWith(NEGOTIATE_AUTH_SCHEME)) {
            return newResultPromise(SEND_FAILURE);
        }

        LOG.debug("IWAModule: Negotiate authorization header set in request.");

        // Check SPNEGO token
        byte[] spnegoToken = extractSpnegoToken(authorizationHeader);
        if (spnegoToken == null) {
            return newExceptionPromise(new AuthenticationException("Invalid SPNEGO token"));
        }

        // Ignore NTLM over SPNEGO
        if (spnegoToken[0] == 'N' && spnegoToken[1] == 'T' && spnegoToken[2] == 'L' && spnegoToken[3] == 'M') {
            return newResultPromise(SEND_FAILURE);
        }

        // Perform Kerberos authentication
        try {
            final String username = new WDSSO().process(options, messageInfo, spnegoToken);
            LOG.debug("IWAModule: IWA successful with username, {}", username);

            clientSubject.getPrincipals().add(new Principal() {
                @Override
                public String getName() {
                    return username;
                }
            });
            return newResultPromise(SUCCESS);
        } catch (Exception e) {
            LOG.debug("IWAModule: IWA has failed. {}", e.getMessage());
            return newExceptionPromise(new AuthenticationException("IWA has failed"));
        }
    }

    @Override
    public Promise<AuthStatus, AuthenticationException> secureResponse(MessageInfoContext messageInfo, Subject serviceSubject) {
        return newResultPromise(SEND_SUCCESS);
    }

    @Override
    public Promise<Void, AuthenticationException> cleanSubject(MessageInfoContext messageInfo, Subject subject) {
        return newResultPromise(null);
    }

    /**
     * Extract SPNEGO token from the specified negotiate authorization header.
     * @param header Authorization header to extract SPNEGO token.
     * @return Extracted token or null when extraction fails.
     */
    private byte[] extractSpnegoToken(String header) {
        try {
            return Base64.decode(header.substring(NEGOTIATE_AUTH_SCHEME.length()).trim());
        } catch (Exception e) {
            LOG.error("IWAModule: Failed to extract SPNEGO token from authorization header");
            return null;
        }
    }

}
