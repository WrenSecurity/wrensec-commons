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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit tests for the cookie class.
 *
 * <p>
 * See <link>https://www.rfc-editor.org/rfc/rfc6265.txt</link>
 */
@SuppressWarnings("javadoc")
public class CookieTest {

    @DataProvider
    public Object[][] validNames() {
        return new Object[][] { { "NAME" }, { "a1" }, { "!#$%&'*+-.^_`|~" } };
    }

    @Test(dataProvider = "validNames")
    public void shouldAcceptValidName(String name) {
        assertThat(new Cookie().setName(name).getName()).isEqualTo(name);
    }

    @DataProvider
    public Object[][] invalidNames() {
        return new Object[][] { { "" }, { "NA ME" }, { "NA=ME" }, { "NA;ME" }, { "NA\"ME" }, { "NA\tME" },
            { "NÁME" } };
    }

    @Test(dataProvider = "invalidNames")
    public void shouldRejectInvalidName(String name) {
        assertThatThrownBy(() -> new Cookie().setName(name)).isInstanceOf(IllegalArgumentException.class);
    }

    @DataProvider
    public Object[][] validValues() {
        return new Object[][] { { "" }, { "VALUE" }, { "a=b&c" }, { "\"VALUE\"" }, { "\"\"" } };
    }

    @Test(dataProvider = "validValues")
    public void shouldAcceptValidValue(String value) {
        assertThat(new Cookie().setValue(value).getValue()).isEqualTo(value);
    }

    @DataProvider
    public Object[][] invalidValues() {
        return new Object[][] { { "VA LUE" }, { "VA,LUE" }, { "VA;LUE" }, { "VA\\LUE" }, { "VA\"LUE" }, { "\"" },
            { "\"VA\"LUE\"" }, { "VÁLUE" } };
    }

    @Test(dataProvider = "invalidValues")
    public void shouldRejectInvalidValue(String value) {
        assertThatThrownBy(() -> new Cookie().setValue(value)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldRejectInvalidNameInConstructor() {
        assertThatThrownBy(() -> new Cookie("NA ME", "VALUE")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldAcceptValidAttributeValue() {
        Cookie cookie = new Cookie("NAME", "VALUE").setPath("/a b,\"c\"").setAttribute("Flag", "");
        assertThat(cookie.getPath()).isEqualTo("/a b,\"c\"");
        assertThat(cookie.getAttribute("Flag")).isEmpty();
    }

    @Test
    public void shouldRejectInvalidAttributeName() {
        assertThatThrownBy(() -> new Cookie().setAttribute("At tr", "VALUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldRejectInvalidAttributeValue() {
        assertThatThrownBy(() -> new Cookie().setAttribute("Attr", "a;b"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Cookie().setPath("/a;b")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Cookie().setDomain("exa\nmple.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldStoreFlagsAsEmptyValue() {
        Cookie cookie = new Cookie("NAME", "VALUE").setSecure(true).setHttpOnly(true);
        assertThat(cookie.getAttribute("Secure")).isEmpty();
        assertThat(cookie.getAttribute("HttpOnly")).isEmpty();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.isHttpOnly()).isTrue();

        cookie.setSecure(false).setHttpOnly(false);
        assertThat(cookie.getAttributes()).isEmpty();
        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.isHttpOnly()).isFalse();
    }

    @Test
    public void shouldBeEqualWithRemovedAttributes() {
        Cookie cookie = new Cookie("NAME", "VALUE").setPath("/path").setPath(null);
        assertThat(cookie).isEqualTo(new Cookie("NAME", "VALUE"));
        assertThat(cookie.hashCode()).isEqualTo(new Cookie("NAME", "VALUE").hashCode());
    }

    @Test
    public void shouldBeEqualWithDifferentAttributeNameCase() {
        Cookie cookie = new Cookie("NAME", "VALUE").setAttribute("path", "/path");
        assertThat(cookie).isEqualTo(new Cookie("NAME", "VALUE").setPath("/path"));
        assertThat(cookie.hashCode()).isEqualTo(new Cookie("NAME", "VALUE").setPath("/path").hashCode());
    }

    @Test
    public void shouldNotBeEqualWithDifferentAttributes() {
        assertThat(new Cookie("NAME", "VALUE").setPath("/a")).isNotEqualTo(new Cookie("NAME", "VALUE").setPath("/b"));
        assertThat(new Cookie("NAME", "VALUE").setPath("/a")).isNotEqualTo(new Cookie("NAME", "VALUE"));
    }

    @Test
    public void shouldUnsetWithNull() {
        Cookie cookie = new Cookie("NAME", "VALUE").setPath("/path").setAttribute("SameSite", "Lax");
        cookie.setName(null).setValue(null).setPath(null).setAttribute("SameSite", null);
        assertThat(cookie.getName()).isNull();
        assertThat(cookie.getValue()).isNull();
        assertThat(cookie.getAttributes()).isEmpty();
    }

}
