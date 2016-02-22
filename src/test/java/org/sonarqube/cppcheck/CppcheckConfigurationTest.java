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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class CppcheckConfigurationTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private Settings settings;
  private CppcheckConfiguration configuration;

  @Before
  public void setUp() {
    settings = new Settings();
    configuration = new CppcheckConfiguration(settings);
  }

  @Test
  public void should_check_for_missing_property() {
    thrown.expect(SonarException.class);
    thrown.expectMessage("Cppcheck report property found empty, you should run cppcheck externally and import its report using sonar.cppcheck.reportPath property, for more information see the plugin documentation.");
    configuration.getCppcheckReportFile();
  }

  @Test
  public void should_check_correctness_of_path_to_cppcheck_report() {
    String reportPath = "not-existing";
    settings.setProperty(CppcheckConfiguration.CPPCHECK_REPORT_PATH_PROPERTY, reportPath);
    thrown.expect(SonarException.class);
    thrown.expectMessage("Cppcheck report is not found, please check property 'sonar.cppcheck.reportPath': not-existing");
    configuration.getCppcheckReportFile();
  }

  @Test
  public void should_return_path_to_cppcheck_report() {
    String reportPath = "src/test/resources/cppcheck2.xml";
    settings.setProperty(CppcheckConfiguration.CPPCHECK_REPORT_PATH_PROPERTY, reportPath);
    assertThat(configuration.getCppcheckReportFile()).isEqualTo(new File(reportPath));
  }

}
