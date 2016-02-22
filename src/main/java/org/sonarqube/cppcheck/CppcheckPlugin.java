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

import com.google.common.collect.ImmutableList;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.List;

public class CppcheckPlugin extends SonarPlugin {

  private static final String CPPCHECK_SUBCATEGORY = "Cppcheck";

  @Override
  public List getExtensions() {
    ImmutableList.Builder<Object> builder = ImmutableList.builder();
    builder.add(CppcheckSensor.class);
    builder.add(CppcheckConfiguration.class);
    builder.add(new CppcheckRuleRepository("c"));
    builder.add(new CppcheckRuleRepository("cpp"));
    builder.add(PropertyDefinition.builder(CppcheckConfiguration.CPPCHECK_REPORT_PATH_PROPERTY)
      .subCategory(CPPCHECK_SUBCATEGORY)
      .name("Cppcheck Report Path")
      .description("Path to the CppCheck XML report, ex: report/cppcheck.xml")
      .onQualifiers(Qualifiers.PROJECT)
      .build());
    return builder.build();
  }

}
