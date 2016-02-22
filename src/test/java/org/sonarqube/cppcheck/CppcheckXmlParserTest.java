/*
 * SonarQube :: Cppcheck Plugin
 * Copyright (C) 2016-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarqube.cppcheck;

import com.google.common.collect.Iterables;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;

public class CppcheckXmlParserTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test2() {
    File file = FileUtils.toFile(getClass().getResource("/cppcheck2.xml"));

    Collection<CppcheckXmlParser.Message> messages = CppcheckXmlParser.parse(file);
    assertThat(messages.size()).isEqualTo(9);

    CppcheckXmlParser.Message message = messages.iterator().next();
    assertThat(message.getId()).isEqualTo("autoVariables");
    assertThat(message.getMsg()).isEqualTo("Assigning address of local auto-variable to a function parameter.");
    assertThat(message.getVerbose()).startsWith("Dangerous assignment");
    assertThat(message.getSeverity()).isEqualTo("error");
    assertThat(message.getLine()).isEqualTo("4");
    assertThat(message.getFilename()).isEqualTo("src/autoVariables/bad.c");
  }

  @Test
  public void test3() {
    File file = FileUtils.toFile(getClass().getResource("/no-location.xml"));
    Collection<CppcheckXmlParser.Message> messages = CppcheckXmlParser.parse(file);
    CppcheckXmlParser.Message message = Iterables.getOnlyElement(messages);
    assertThat(message.getMsg()).isNotNull();
    assertThat(message.getLine()).isNull();
    assertThat(message.getFilename()).isNull();
  }

  @Test
  public void should_throw_exception() throws Exception {
    thrown.expect(SonarException.class);
    thrown.expectMessage("Unable to parse file: notfound.xml");
    CppcheckXmlParser.parse(new File("notfound.xml"));
  }

}
