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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2010–2011 ApexIdentity Inc.
 * Portions Copyright 2011-2015 ForgeRock AS.
 * Portions Copyright 2026 Wren Security
 */

package org.forgerock.http.header;

import static java.util.Collections.singletonList;
import static org.forgerock.http.header.HeaderUtil.parseMultiValuedHeader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.forgerock.http.protocol.Cookie;
import org.forgerock.http.protocol.Header;
import org.forgerock.http.protocol.Request;

/**
 * Processes the <strong>{@code Cookie}</strong> request message header.
 *
 * <p>
 * For more information see <a href="https://www.rfc-editor.org/rfc/rfc6265.txt">RFC 6265</a>.
 * <p>
 * Note: This implementation is designed to be forgiving when parsing malformed cookies.
 */
public class CookieHeader extends Header {
    private static CookieHeader valueOf(final List<String> values) {
        List<Cookie> cookies = new ArrayList<>(values.size());
        for (String s1 : values) {
            for (String s2 : HeaderUtil.split(s1, ';')) {
                String[] nvp = HeaderUtil.parseParameter(s2);
                if (nvp[0].isEmpty()) {
                    continue; // ignore empty cookie pair
                } else if (nvp[0].startsWith("$")) {
                    continue; // ignore legacy cookie attributes
                } else if (nvp.length > 1){
                    cookies.add(new Cookie(nvp[0], nvp[1]));
                }
            }
        }
        return new CookieHeader(cookies);
    }

    /**
     * Constructs a new header, initialized from the specified request message.
     *
     * @param message
     *            The request message to initialize the header from.
     * @return The parsed header.
     */
    public static CookieHeader valueOf(final Request message) {
        return valueOf(parseMultiValuedHeader(message, NAME));
    }

    /**
     * Constructs a new header, initialized from the specified string value.
     *
     * @param string
     *            The value to initialize the header from.
     * @return The parsed header.
     */
    public static CookieHeader valueOf(final String string) {
        return valueOf(parseMultiValuedHeader(string));
    }

    /** The name of this header. */
    public static final String NAME = "Cookie";

    /** Request message cookies. */
    private final List<Cookie> cookies;

    /**
     * Constructs a new empty header.
     */
    public CookieHeader() {
        this(new ArrayList<Cookie>(1));
    }

    /**
     * Constructs a new header with the provided cookies.
     *
     * @param cookies
     *            The cookies.
     */
    public CookieHeader(List<Cookie> cookies) {
        this.cookies = cookies;
    }

    /**
     * Returns the cookies' request list.
     *
     * @return The cookies' request list.
     */
    public List<Cookie> getCookies() {
        return cookies;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<String> getValues() {
        StringBuilder sb = new StringBuilder();
        for (Cookie cookie : cookies) {
            if (cookie.getName() != null) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(cookie.getName()).append('=');
                sb.append(HeaderUtil.quote(cookie.getValue()));
            }
        }
        return sb.length() > 0 ? singletonList(sb.toString()) : Collections.emptyList();
    }

    static class Factory extends HeaderFactory<CookieHeader> {

        @Override
        public CookieHeader parse(String value) {
            return valueOf(value);
        }

        @Override
        public CookieHeader parse(List<String> values) {
            return valueOf(values);
        }
    }
}
