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
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;

import java.io.File;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CppcheckSensorTest {

  private RuleFinder ruleFinder;
  private RulesProfile profile;
  private CppcheckConfiguration configuration;
  private CppcheckSensor sensor;
  private DefaultFileSystem fileSystem;

  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() {
    ruleFinder = mock(RuleFinder.class);
    profile = mock(RulesProfile.class);
    configuration = mock(CppcheckConfiguration.class);
    fileSystem = spy(new DefaultFileSystem(new File("src/test/resources/cppcheck")));

    sensor = spy(new CppcheckSensor(configuration, profile, ruleFinder, fileSystem, null));
    Mockito.doNothing().when(sensor).save(any(Resource.class), Mockito.any(Rule.class), any(CppcheckXmlParser.Message.class));
  }

  @Test
  public void shouldExecuteOnProject() {
    Project project = mock(Project.class);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    when(profile.getActiveRulesByRepository("cpp-cppcheck"))
      .thenReturn(Collections.singletonList(mock(ActiveRule.class)));
    when(fileSystem.hasFiles(any(FilePredicate.class))).thenReturn(false);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    when(fileSystem.hasFiles(any(FilePredicate.class))).thenReturn(true);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();

    // no active rules:
    when(profile.getActiveRulesByRepository("cpp-cppcheck"))
        .thenReturn(Collections.<ActiveRule>emptyList());
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void should_parse() {
    when(ruleFinder.findByKey(anyString(), anyString()))
      .thenReturn(null)
      .thenReturn(Rule.create());

    Project project = mock(Project.class);

    SensorContext context = mock(SensorContext.class);
    when(context.isIndexed(Mockito.eq((Resource) null), Mockito.eq(false)))
      .thenReturn(false)
      .thenReturn(true);

    when(profile.getActiveRulesByRepository(anyString())).thenReturn(ImmutableList.of(mock(ActiveRule.class)));

    when(configuration.getCppcheckReportFile()).thenReturn(new File("src/test/resources/cppcheck2.xml"));
    sensor.analyse(project, context);

    verify(sensor, times(2)).save(any(Resource.class), Mockito.any(Rule.class), any(CppcheckXmlParser.Message.class));
  }

  @Test
  public void test_toString() {
    assertThat(sensor.toString()).isEqualTo("Cppcheck");
  }

}
