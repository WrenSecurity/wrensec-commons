/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal/CDDLv1_0.txt or
 * http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal/CDDLv1_0.txt.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *      Copyright 2011 ForgeRock AS
 */
package org.forgerock.i18n.maven;



import static org.fest.assertions.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;



/**
 * Tests the MessagePropertyKey class.
 */
@Test
public class MessagePropertyKeyTest
{

  /**
   * Data provider for {@link #testValueOfInvalidString(String)}.
   * 
   * @return Test data.
   */
  @DataProvider(parallel = true)
  public Object[][] invalidMessagePropertyKeyStrings()
  {
    return new Object[][] { { "" }, { "1" }, { "1A" }, { "message" },
        { "ANOTHER_message" }, { "ANOTHER-MESSAGE" },
        { "A MESSAGE" }, };
  }



  /**
   * Tests the {@code valueOf(String s)} method with invalid key strings.
   * 
   * @param s
   *          The key string.
   * @throws IllegalArgumentException
   *           The expected exception.
   */
  @Test(dataProvider = "invalidMessagePropertyKeyStrings", expectedExceptions = IllegalArgumentException.class)
  public void testValueOfInvalidString(final String s)
      throws IllegalArgumentException
  {
    MessagePropertyKey.valueOf(s);
  }



  /**
   * Tests the {@code valueOf(String s)} method with valid key strings.
   * 
   * @param s
   *          The key string.
   * @param name
   *          The expected name.
   * @param ordinal
   *          The expected ordinal.
   */
  @Test(dataProvider = "validMessagePropertyKeyStrings")
  public void testValueOfValidString(final String s,
      final String name, final int ordinal)
  {
    final MessagePropertyKey key = MessagePropertyKey.valueOf(s);
    assertThat(key.toString()).isEqualTo(s);
    assertThat(key.getName()).isEqualTo(name);
    assertThat(key.getOrdinal()).isEqualTo(ordinal);
  }



  /**
   * Data provider for {@link #testValueOfValidString(String, String, int)}.
   * 
   * @return Test data.
   */
  @DataProvider(parallel = true)
  public Object[][] validMessagePropertyKeyStrings()
  {
    return new Object[][] { { "MESSAGE", "MESSAGE", -1 },
        { "ANOTHER_MESSAGE", "ANOTHER_MESSAGE", -1 },
        { "YET_ANOTHER_MESSAGE", "YET_ANOTHER_MESSAGE", -1 },
        { "M111ABC_ABC_111_XYZ", "M111ABC_ABC_111_XYZ", -1 },
        { "MESSAGE_1", "MESSAGE", 1 },
        { "MESSAGE_123", "MESSAGE", 123 },
        { "ANOTHER_MESSAGE_1", "ANOTHER_MESSAGE", 1 },
        { "ANOTHER_MESSAGE_123", "ANOTHER_MESSAGE", 123 },
        { "YET_ANOTHER_MESSAGE_1", "YET_ANOTHER_MESSAGE", 1 },
        { "YET_ANOTHER_MESSAGE_123", "YET_ANOTHER_MESSAGE", 123 },
        { "M111ABC_ABC_111_XYZ_1", "M111ABC_ABC_111_XYZ", 1 },
        { "M111ABC_ABC_111_XYZ_123", "M111ABC_ABC_111_XYZ", 123 }, };
  }

}
