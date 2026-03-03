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
 * Portions Copyright 2018-2026 Wren Security.
 */

package org.forgerock.http.swagger;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.protocol.Response.newResponsePromise;
import static org.forgerock.json.JsonValue.json;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.forgerock.http.Handler;
import org.forgerock.http.handler.DescribableHandler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.test.assertj.AssertJJsonValueAssert;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;

@SuppressWarnings("javadoc")
public class OpenApiRequestFilterTest {

    @Mock
    DescribableHandler handler;


    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        Paths paths = new Paths();
        paths.addPathItem("test", new PathItem().post(new Operation()));
        OpenAPI openApi = new OpenAPI().paths(paths);
        when(handler.handleApiRequest(any(Context.class), any(Request.class)))
                .thenReturn(openApi);
        when(handler.handle(any(Context.class), any(Request.class)))
                .thenReturn(newResponsePromise(new Response(Status.TEAPOT)));
    }

    @Test
    public void shouldAnswerSwaggerResult() throws Exception {
        OpenApiRequestFilter filter = new OpenApiRequestFilter();
        Request request = new Request()
                .setMethod("GET")
                .setUri(format("http://localhost:%d/test?_api", 8888));

        Response response = filter.filter(new RootContext(), request, handler).get();

        AssertJJsonValueAssert.assertThat(json(response.getEntity().getJson())).isObject()
                .hasObject("paths")
                .hasObject("test")
                .hasObject("post");
    }

    @Test
    public void shouldNotAnswerSwaggerResultBecauseHandlerIsNotDescribable() throws Exception {
        Handler handler = new Handler() {
            @Override
            public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                return newResponsePromise(new Response(Status.TEAPOT));
            }
        };

        OpenApiRequestFilter filter = new OpenApiRequestFilter();
        Request request = new Request()
                .setMethod("GET")
                .setUri(format("http://localhost:%d/test?_api", 8888));

        Response response = filter.filter(new RootContext(), request, handler).get();

        assertThat(response.getStatus()).isEqualTo(Status.TEAPOT);
    }

    @Test
    public void shouldNotAnswerSwaggerResultBecauseQueryParameterIsNotPresent() throws Exception {
        OpenApiRequestFilter filter = new OpenApiRequestFilter();
        Request request = new Request()
                .setMethod("GET")
                .setUri(format("http://localhost:%d/test", 8888));

        Response response = filter.filter(new RootContext(), request, handler).get();

        assertThat(response.getStatus()).isEqualTo(Status.TEAPOT);
    }

}
