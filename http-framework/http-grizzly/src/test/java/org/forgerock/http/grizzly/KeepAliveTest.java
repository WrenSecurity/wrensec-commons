/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.1.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.1.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 Wren Security
 */
package org.forgerock.http.grizzly;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.forgerock.http.Applications;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.handler.Handlers;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Responses;
import org.forgerock.http.protocol.Status;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.PromiseImpl;
import org.glassfish.grizzly.PortRange;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.testng.annotations.Test;

public class KeepAliveTest {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private HttpServer createHttpServer() {
        NetworkListener listener = new NetworkListener("HTTP", NetworkListener.DEFAULT_NETWORK_HOST,
                new PortRange(6000, 7000));

        HttpServer server = new HttpServer();
        server.addListener(listener);

        final TCPNIOTransport transport = listener.getTransport();
        transport.setReuseAddress(true);
        transport.setKeepAlive(true);
        transport.setTcpNoDelay(true);
        transport.setWriteTimeout(30_000, TimeUnit.MILLISECONDS);

        transport.setReadBufferSize(4096);
        transport.setWriteBufferSize(4096);
        transport.setIOStrategy(SameThreadIOStrategy.getInstance());

        transport.setSelectorRunnersCount(1);
        transport.setServerConnectionBackLog(2);

        return server;
    }

    @Test
    @SuppressWarnings("resource")
    public void testAsyncProcessing() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Create CHF application that asynchronously reflects request body size
        HttpApplication httpApplication = Applications.simpleHttpApplication(
                Handlers.chainOf(
                    (Context context, Request request) -> {
                        try {
                            byte[] data = request.getEntity()
                                    .getRawContentInputStream()
                                    .readAllBytes();
                            return Response.newResponsePromise(new Response(Status.OK)
                                    .setEntity("body=" + data.length));
                        } catch (Exception e) {
                            return Response.newResponsePromise(Responses.newInternalServerError(e));
                        }
                    },
                    (Context context, Request request, Handler next) -> {
                        final PromiseImpl<Response, NeverThrowsException> promise = PromiseImpl.create();
                        executor.submit(() -> {
                            next.handle(context, request).thenOnResult(promise).thenOnRuntimeException(promise);
                        });
                        return promise;
                    }), null);

        // Create Grizzly HTTP handler
        HttpHandler httpHandler = GrizzlySupport.newGrizzlyHttpHandler(httpApplication);

        // Create Grizzly HTTP server
        HttpServer httpServer = createHttpServer();
        httpServer.getServerConfiguration().addHttpHandler(httpHandler);
        httpServer.start();

        // Create HTTP client (reuses HTTP/1.1 connections by default)
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // Create test resource URI
        URI resourceUri = URI.create("http://localhost:" + httpServer.getListeners().iterator().next().getPort()
                + "/test");

        // Generate fixed long body (has to span multiple packets / chunks)
        byte[] body = new byte[10 * 1024];
        java.util.Arrays.fill(body, (byte) 'A');

        try {
            HttpRequest firstRequest = HttpRequest.newBuilder()
                    .uri(resourceUri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/octet-stream")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> firstResponse = httpClient.send(firstRequest, BodyHandlers.ofString());
            assertThat(firstResponse.statusCode()).isEqualTo(200);
            assertThat(firstResponse.body()).isEqualTo("body=" + body.length);

            HttpRequest secondRequest = HttpRequest.newBuilder()
                    .uri(resourceUri)
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> secondResponse = httpClient.send(secondRequest, BodyHandlers.ofString());
            assertThat(secondResponse.statusCode()).isEqualTo(200);
        } finally {
            httpServer.shutdownNow();
            executor.shutdownNow();
        }
    }

}
