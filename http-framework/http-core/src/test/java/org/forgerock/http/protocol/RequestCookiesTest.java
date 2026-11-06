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
 * Copyright 2026 Wren Security
 */

package org.forgerock.http.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class RequestCookiesTest {

    @Test
    public void shouldMatchCookieNamesCaseSensitively() {
        final Request request = new Request();
        request.getHeaders().put("Cookie", "SESSION=evil; session=good");

        final RequestCookies cookies = request.getCookies();

        assertThat(cookies.get("session")).extracting(Cookie::getValue).containsExactly("good");
        assertThat(cookies.get("SESSION")).extracting(Cookie::getValue).containsExactly("evil");
        assertThat(cookies.get("Session")).isNull();
        assertThat(cookies.containsKey("Session")).isFalse();
        assertThat(cookies.keySet()).containsExactly("SESSION", "session");
        assertThat(cookies.size()).isEqualTo(2);
    }

    @Test
    public void shouldGroupCookiesWithSameName() {
        final Request request = new Request();
        request.getHeaders().put("Cookie", "a=1; a=2; b=3");

        final RequestCookies cookies = request.getCookies();

        assertThat(cookies.get("a")).extracting(Cookie::getValue).containsExactly("1", "2");
        assertThat(cookies.keySet()).containsExactly("a", "b");
    }

}
