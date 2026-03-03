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
 * Copyright 2016-2017 ForgeRock AS.
 * Portions Copyright 2026 Wren Security.
 */
package org.forgerock.http.swagger;

import static org.forgerock.http.protocol.Entity.APPLICATION_JSON_CHARSET_UTF_8;
import static org.forgerock.http.protocol.Response.newResponsePromise;
import static org.forgerock.http.protocol.Responses.newInternalServerError;
import static org.forgerock.http.protocol.Responses.newNotFound;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import java.net.URI;
import java.util.List;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.header.MalformedHeaderException;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.http.util.Json;
import org.forgerock.http.util.Paths;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This filter looks for the query parameter {code _api} : if present then it returns the API description of the
 * downstream handlers, otherwise the request is processed as expected.
 */
public class OpenApiRequestFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiRequestFilter.class);

    /** Request parameter for the OpenAPI API Descriptor. */
    public static final String API_PARAMETER = "_api";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(new Json.LocalizableStringModule());

    @Override
    public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
        if (!(next instanceof Describable && request.getForm().containsKey(API_PARAMETER))) {
            return next.handle(context, request);
        }

        try {
            OpenAPI result = ((Describable<OpenAPI, Request>) next).handleApiRequest(context, request);
            if (result == null) {
                return newResponsePromise(new Response(Status.NOT_IMPLEMENTED));
            }

            result = setUriDetailsIfNotPresent(request, context, result);
            ObjectWriter writer = Json.makeLocalizingObjectWriter(OBJECT_MAPPER, request);
            Response chfResponse = new Response(Status.OK).setEntity(writer.writeValueAsBytes(result));
            chfResponse.getHeaders().put(ContentTypeHeader.NAME, APPLICATION_JSON_CHARSET_UTF_8);
            return newResponsePromise(chfResponse);
        } catch (IllegalStateException e) {
            // This exception marks that the request couldn't be routed to an acceptable handler
            logger.trace("Cannot route {} to an acceptable handler", request.getUri() , e);
            return newResponsePromise(newNotFound());
        } catch (RuntimeException | JsonProcessingException | MalformedHeaderException e) {
            logger.error("Exception caught while generating OpenAPI descriptor", e);
            return newResponsePromise(newInternalServerError(e));
        }
    }

    /**
     * Deduce and set the server URL of the request for the OpenAPI descriptor from the request context. This method
     * should set the {@code servers} list on the descriptor.
     *
     * @param request The CHF request.
     * @param context The CHF request context.
     * @param descriptor The descriptor object.
     * @return The updated descriptor object.
     */
    protected OpenAPI setUriDetailsIfNotPresent(Request request, Context context, OpenAPI descriptor) {
        if (context.containsContext(UriRouterContext.class)) {
            final UriRouterContext uriRouterContext = context.asContext(UriRouterContext.class);
            final URI originalUri = uriRouterContext.getOriginalUri();

            if (descriptor.getServers() == null || descriptor.getServers().isEmpty()) {
                String scheme = originalUri.getScheme();
                String host = originalUri.getHost();
                if (originalUri.getPort() != -1 && originalUri.getPort() != 80 && originalUri.getPort() != 443) {
                    host += ":" + originalUri.getPort();
                }
                String basePath = Paths.addLeadingSlash(uriRouterContext.getBaseUri());
                String serverUrl = scheme + "://" + host + (basePath != null ? basePath : "");
                descriptor.setServers(List.of(new Server().url(serverUrl)));
            }
        }
        return descriptor;
    }
}
