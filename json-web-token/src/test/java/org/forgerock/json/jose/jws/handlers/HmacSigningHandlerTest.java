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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2026 Wren Security.
 */

package org.forgerock.json.jose.jws.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class HmacSigningHandlerTest {

    private HmacSigningHandler signingHandler;

    @BeforeMethod
    public void init() {
        signingHandler = new HmacSigningHandler(new byte[32]);
    }

    @Test
    public void shouldVerifyValidHmacSignature() {
        byte[] data = "payload".getBytes();
        byte[] signature = signingHandler.sign(JwsAlgorithm.HS256, data);
        assertThat(signingHandler.verify(JwsAlgorithm.HS256, data, signature)).isTrue();
    }

    @Test
    public void shouldRejectNoneAlgorithmOnVerify() {
        assertThat(signingHandler.verify(JwsAlgorithm.NONE, "payload".getBytes(), new byte[0])).isFalse();
    }

    @Test
    public void shouldRejectNonHmacAlgorithmOnVerify() {
        byte[] data = "payload".getBytes();
        byte[] signature = signingHandler.sign(JwsAlgorithm.HS256, data);
        assertThat(signingHandler.verify(JwsAlgorithm.RS256, data, signature)).isFalse();
    }

    @Test
    public void shouldRefuseToSignWithNonHmacAlgorithm() {
        assertThatThrownBy(() -> signingHandler.sign(JwsAlgorithm.RS256, "payload".getBytes()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldRefuseToSignWithNoneAlgorithm() {
        assertThatThrownBy(() -> signingHandler.sign(JwsAlgorithm.NONE, "payload".getBytes()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
