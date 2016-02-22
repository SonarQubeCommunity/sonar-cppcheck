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

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;

import java.io.File;
import java.util.Collection;
import java.util.Set;

public class CppcheckSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(CppcheckSensor.class);

  private final CppcheckConfiguration configuration;
  private final RulesProfile profile;
  private final RuleFinder ruleFinder;
  private final FileSystem fileSystem;
  private final ResourcePerspectives resourcePerspectives;

  public CppcheckSensor(CppcheckConfiguration configuration, RulesProfile profile, RuleFinder ruleFinder, FileSystem fileSystem, ResourcePerspectives resourcePerspectives) {
    this.configuration = configuration;
    this.profile = profile;
    this.ruleFinder = ruleFinder;
    this.fileSystem = fileSystem;
    this.resourcePerspectives = resourcePerspectives;
  }

  private boolean hasRulesEnabledFor(String key) {
    return !profile.getActiveRulesByRepository(CppcheckRuleRepository.repositoryKeyForLanguage(key)).isEmpty();
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return (fileSystem.hasFiles(fileSystem.predicates().hasLanguages("c")) && hasRulesEnabledFor("c")) ||
      (fileSystem.hasFiles(fileSystem.predicates().hasLanguages("cpp")) && hasRulesEnabledFor("cpp"));
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    File report = configuration.getCppcheckReportFile();
    Collection<CppcheckXmlParser.Message> messages = CppcheckXmlParser.parse(report);
    saveAll(project, context, "c", messages);
    saveAll(project, context, "cpp", messages);
  }

  private void saveAll(Project project, SensorContext context, String language, Collection<CppcheckXmlParser.Message> messages) {
    if (hasRulesEnabledFor(language)) {
      Set<File> indexedFiles = Sets.newHashSet(fileSystem.files(fileSystem.predicates().hasLanguage(language)));
      for (CppcheckXmlParser.Message message : messages) {
        save(project, context, language, indexedFiles, message);
      }
    }
  }

  private Rule getRule(String key, String id) {
    return ruleFinder.findByKey(CppcheckRuleRepository.repositoryKeyForLanguage(key), id);
  }

  private void save(Project project, SensorContext context, String language, Set<File> indexedFiles, CppcheckXmlParser.Message message) {
    Rule rule = getRule(language, message.getId());
    if (rule == null) {
      LOG.warn("No such rule in Sonar, so issue from Cppcheck will be ignored: {}", message.getId());
      return;
    }
    final Resource resource;
    if (message.getFilename() == null) {
      resource = project;
    } else {
      File file = new File(message.getFilename()).getAbsoluteFile();
      if (!indexedFiles.contains(file)) {
        return;
      }
      resource = org.sonar.api.resources.File.fromIOFile(file, project);
      if (!context.isIndexed(resource, false)) {
        LOG.warn("File not analysed by Sonar, so issue from Cppcheck will be ignored: {}", file);
        return;
      }
    }
    save(resource, rule, message);
  }

  void save(Resource resource, Rule rule, CppcheckXmlParser.Message message) {
    Issuable issuable = resourcePerspectives.as(Issuable.class, resource);
    if (issuable == null) {
      return;
    }
    Issuable.IssueBuilder issue = issuable.newIssueBuilder()
      .ruleKey(rule.ruleKey())
      .message(message.getMsg());
    if (message.getLine() != null) {
      int line = Integer.parseInt(message.getLine());
      if (line != 0) {
        issue.line(line);
      }
    }
    issuable.addIssue(issue.build());
  }

  @Override
  public String toString() {
    return "Cppcheck";
  }

}
