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

package org.forgerock.http.protocol;

import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.forgerock.http.header.HeaderUtil;
import org.wrensecurity.guava.common.base.Objects;

/**
 * An HTTP cookie.
 *
 * <p>
 * For more information see <a href="https://www.rfc-editor.org/rfc/rfc6265.txt">RFC 6265</a>.
 */
public class Cookie {

    /** Non-alphanumeric characters allowed in a token (RFC 9110, section 5.6.2). */
    private static final String TCHAR_SPECIALS = "!#$%&'*+-.^_`|~";

    private static final String MAX_AGE_ATTR_NAME = "Max-Age";

    private static final String EXPIRES_ATTR_NAME = "Expires";

    private static final String DOMAIN_ATTR_NAME = "Domain";

    private static final String PATH_ATTR_NAME = "Path";

    private static final String SECURE_ATTR_NAME = "Secure";

    private static final String HTTPONLY_ATTR_NAME = "HttpOnly";

    /** The name of the cookie. */
    private String name;

    /** The value of the cookie. */
    private String value;

    /** Additional cookie attribute-value pairs. */
    private Map<String, String> attributes;

    /**
     * Check whether the given string is a valid cookie name.
     *
     * @param name cookie name or attribute name
     * @return {@code true} if the name is a non-empty token
     */
    private static boolean isValidName(String name) {
        return name != null && !name.isEmpty()
                && name.chars().allMatch(c -> (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z')
                        || (c >= 'a' && c <= 'z') || TCHAR_SPECIALS.indexOf(c) >= 0);
    }

    /**
     * Check whether the given string is a valid cookie value ({@code cookie-value} as per RFC 6265).
     *
     * @param value cookie value to check
     * @return {@code true} if the value consists of cookie-octets, optionally enclosed in double quotes
     */
    private static boolean isValidValue(String value) {
        if (value == null) {
            return false;
        }
        if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            value = value.substring(1, value.length() - 1);
        }
        return value.chars().allMatch(c -> c > 0x20 && c < 0x7F && c != '"' && c != ',' && c != ';' && c != '\\');
    }

    /**
     * Check whether the given string is a valid cookie attribute value ({@code av-value} as per RFC 6265).
     *
     * @param value attribute value to check
     * @return {@code true} if the value contains no CTLs and no semicolon
     */
    private static boolean isValidAttributeValue(String value) {
        return value != null && value.chars().allMatch(c -> c >= 0x20 && c < 0x7F && c != ';');
    }

    /**
     * Create a new uninitialized cookie.
     */
    public Cookie() {
        // Empty cookie.
    }

    /**
     * Create a new cookie with the given name and value.
     *
     * @param name the name of the cookie
     * @param value the value of the cookie
     *
     * @throws IllegalArgumentException in case the name is not a valid token or the value contains invalid characters
     */
    public Cookie(String name, String value) {
        setName(name);
        setValue(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Cookie)) {
            return false;
        }
        final Cookie other = (Cookie) obj;
        return Objects.equal(name, other.name)
                && Objects.equal(value, other.value)
                && getAttributes().equals(other.getAttributes());
    }

    @Override
    public int hashCode() {
        int attributesHash = 0;
        for (Map.Entry<String, String> attribute : getAttributes().entrySet()) {
            // attribute names are case-insensitive
            attributesHash += attribute.getKey().toLowerCase(Locale.ROOT).hashCode() ^ attribute.getValue().hashCode();
        }
        return Objects.hashCode(name, value, attributesHash);
    }

    /**
     * Get all cookie attributes.
     *
     * @return cookie attributes or empty map if none defined
     */
    public Map<String, String> getAttributes() {
        return attributes == null ? Collections.emptyMap() : Collections.unmodifiableMap(attributes);
    }

    /**
     * Get cookie attribute value.
     *
     * @param name cookie attribute name
     * @return cookie attribute value or <code>null</code> if no such attribute has been set
     */
    public String getAttribute(String name) {
        return attributes != null ? attributes.get(name) : null;
    }

    /**
     * Set cookie attribute value or remove existing value by setting <code>null</code>.
     *
     * @param name cookie attribute name
     * @param value cookie attribute value to set or <code>null</code> to remove any previously set value
     * @return this cookie
     *
     * @throws IllegalArgumentException in case the attribute name is not a valid token or the value contains
     *         invalid characters
     */
    public Cookie setAttribute(String name, String value) {
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Invalid cookie attribute name: " + name);
        }

        if (EXPIRES_ATTR_NAME.equalsIgnoreCase(name)) {
            return setExpires(HeaderUtil.parseDate(value));
        } else if (MAX_AGE_ATTR_NAME.equalsIgnoreCase(name)) {
            return setMaxAge(value != null ? parseInteger(value) : null);
        } else {
            return putAttribute(name, value);
        }
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Cookie putAttribute(String name, String value) {
        if (value != null && !isValidAttributeValue(value)) {
            throw new IllegalArgumentException("Invalid cookie attribute value: " + value);
        }

        if (attributes == null) {
            attributes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        }

        if (value != null) {
            attributes.put(name, value);
        } else {
            attributes.remove(name);
        }
        return this;
    }

    /**
     * Get name of the cookie.
     *
     * @return the name of the cookie
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the cookie.
     *
     * @param name the name of the cookie
     * @return this cookie
     *
     * @throws IllegalArgumentException in case the cookie name is not a valid token
     */
    public Cookie setName(String name) {
        if (name != null && !isValidName(name)) {
            throw new IllegalArgumentException("Invalid cookie name: " + name);
        }

        this.name = name;
        return this;
    }

    /**
     * Get the value of the cookie.
     *
     * @return the value of the cookie
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the cookie.
     *
     * @param value the value of the cookie
     * @return this cookie
     *
     * @throws IllegalArgumentException in case the value contains invalid characters
     */
    public Cookie setValue(String value) {
        if (value != null && !isValidValue(value)) {
            throw new IllegalArgumentException("Invalid cookie value: " + value);
        }

        this.value = value;
        return this;
    }

    /**
     * Get the domain for which the cookie is valid.
     *
     * @return the domain for which the cookie is valid
     */
    public String getDomain() {
        return getAttribute(DOMAIN_ATTR_NAME);
    }

    /**
     * Set the domain for which the cookie is valid.
     *
     * @param domain the domain for which the cookie is valid
     * @return this cookie
     *
     * @throws IllegalArgumentException in case the domain contains invalid characters
     */
    public Cookie setDomain(String domain) {
        return putAttribute(DOMAIN_ATTR_NAME, domain);
    }

    /**
     * Get {@code true} if the user agent should make the cookie inaccessible to client side script.
     *
     * @return {@code true} if the user agent should make the cookie inaccessible to client side script.
     */
    public Boolean isHttpOnly() {
        return getAttribute(HTTPONLY_ATTR_NAME) != null;
    }

    /**
     * Set the value indicating whether the user agent should make the cookie inaccessible to client side script.
     *
     * @param httpOnly {@code true} if the user agent should make the cookie inaccessible to client side script
     * @return this cookie
     */
    public Cookie setHttpOnly(boolean httpOnly) {
        return putAttribute(HTTPONLY_ATTR_NAME, httpOnly ? "" : null);
    }

    /**
     * Get the lifetime of the cookie, expressed as the date and time of expiration.
     *
     * @return The lifetime of the cookie, expressed as the date and time of expiration.
     */
    public Date getExpires() {
        return HeaderUtil.parseDate(getAttribute(EXPIRES_ATTR_NAME));
    }

    /**
     * Set the lifetime of the cookie, expressed as the date and time of expiration.
     *
     * <p>
     * The value is stored as HTTP-date, therefore any sub-second precision is lost.
     *
     * @param expires the lifetime of the cookie, expressed as the date and time of expiration
     * @return this cookie
     */
    public Cookie setExpires(Date expires) {
        return putAttribute(EXPIRES_ATTR_NAME, expires != null ? HeaderUtil.formatDate(expires) : null);
    }

    /**
     * Get the lifetime of the cookie, expressed in seconds.
     *
     * @return the lifetime of the cookie, expressed in seconds
     */
    public Integer getMaxAge() {
        String maxAge = getAttribute(MAX_AGE_ATTR_NAME);
        return maxAge != null ? Integer.parseInt(maxAge) : null;
    }

    /**
     * Set the lifetime of the cookie, expressed in seconds.
     *
     * @param maxAge the lifetime of the cookie, expressed in seconds
     * @return this cookie
     */
    public Cookie setMaxAge(Integer maxAge) {
        return putAttribute(MAX_AGE_ATTR_NAME, maxAge != null ? maxAge.toString() : null);
    }

    /**
     * Get the subset of URLs on the origin server to which this cookie applies.
     *
     * @return the subset of URLs on the origin server to which this cookie applies
     */
    public String getPath() {
        return getAttribute(PATH_ATTR_NAME);
    }

    /**
     * Set the subset of URLs on the origin server to which this cookie applies.
     *
     * @param path the subset of URLs on the origin server to which this cookie applies
     * @return this cookie
     *
     * @throws IllegalArgumentException in case the path contains invalid characters
     */
    public Cookie setPath(String path) {
        return putAttribute(PATH_ATTR_NAME, path);
    }

    /**
     * Get flag indicating if the user agent should use only secure means to send back this cookie.
     *
     * @return {@code true} if the user agent should use only secure means to send back this cookie
     */
    public Boolean isSecure() {
        return getAttribute(SECURE_ATTR_NAME) != null;
    }

    /**
     * Set the value indicating whether the user agent should use only secure means to send back this cookie.
     *
     * @param secure {@code true} if the user agent should use only secure means to send back this cookie
     * @return this cookie
     */
    public Cookie setSecure(boolean secure) {
        return putAttribute(SECURE_ATTR_NAME, secure ? "" : null);
    }

    @Override
    public String toString() {
        return String.format("Cookie[%s=%s,%s]", name, value, attributes);
    }

}
