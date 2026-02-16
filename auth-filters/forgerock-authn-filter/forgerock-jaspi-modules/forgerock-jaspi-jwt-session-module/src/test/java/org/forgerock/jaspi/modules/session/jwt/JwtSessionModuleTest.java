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
 * Portions Copyright 2026 Wren Security
 */
package org.forgerock.jaspi.modules.session.jwt;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import org.forgerock.http.protocol.Cookie;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class JwtSessionModuleTest {

    JwtSessionModule jwtSessionModule;

    @BeforeMethod
    public void setUp() {
        jwtSessionModule = new JwtSessionModule();
        jwtSessionModule.cookieDomains = Collections.singleton("example.com");
    }

    @Test
    public void shouldCreateSessionCookieWithMaxAge() {
        Collection<Cookie> cookies = jwtSessionModule.createCookies("foo", 7, "/");
        assertEquals(cookies.size(), 1);
        Cookie cookie = cookies.iterator().next();
        assertEquals(cookie.getMaxAge(), Integer.valueOf(7));
        assertNull(cookie.getExpires());
    }

    @Test
    public void shouldCreateSessionCookieWithoutMaxAge() {
        Collection<Cookie> cookies = jwtSessionModule.createCookies("foo", -1, "/");
        assertEquals(cookies.size(), 1);
        Cookie cookie = cookies.iterator().next();
        assertNull(cookie.getMaxAge());
        assertNull(cookie.getExpires());
    }

    @Test
    public void shouldCreateSessionExpiredCookie() {
        Collection<Cookie> cookies = jwtSessionModule.createCookies("foo", 0, "/");
        assertEquals(cookies.size(), 1);
        Cookie cookie = cookies.iterator().next();
        assertTrue(cookie.getMaxAge() <= 0);
    }

}
