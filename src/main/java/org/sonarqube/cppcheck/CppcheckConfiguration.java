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

import com.google.common.base.Strings;
import org.sonar.api.BatchExtension;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;

import java.io.File;

public class CppcheckConfiguration implements BatchExtension {

  public static final String CPPCHECK_REPORT_PATH_PROPERTY = "sonar.cppcheck.reportPath";

  private final Settings settings;

  public CppcheckConfiguration(Settings settings) {
    this.settings = settings;
  }

  public File getCppcheckReportFile() {
    String cppcheckReportPath = settings.getString(CPPCHECK_REPORT_PATH_PROPERTY);
    if (Strings.isNullOrEmpty(cppcheckReportPath)) {
      throw new SonarException("Cppcheck report property found empty, you should run cppcheck externally and " +
        "import its report using " + CppcheckConfiguration.CPPCHECK_REPORT_PATH_PROPERTY + " property, for more information see the plugin documentation.");
    }
    File executable = new File(cppcheckReportPath);
    if (!executable.isFile()) {
      throw new SonarException("Cppcheck report is not found, please check property '" + CPPCHECK_REPORT_PATH_PROPERTY + "': " + cppcheckReportPath);
    }
    return executable;
  }

}
