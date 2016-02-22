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

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.utils.SonarException;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

public class CppcheckRuleRepository extends RuleRepository {

  static final String REPOSITORY_KEY = "cppcheck";
  private static final String REPOSITORY_NAME = "Cppcheck";

  public CppcheckRuleRepository(String language) {
    super(repositoryKeyForLanguage(language), language);
    setName(REPOSITORY_NAME);
  }

  @Override
  public List<Rule> createRules() {
    List<Rule> rules = Lists.newArrayList();
    for (CppcheckXmlParser.Message message : load()) {
      Rule rule = Rule.create()
        .setKey(message.getId())
        .setConfigKey(message.getId())
        .setSeverity(RulePriority.MAJOR)
        .setName(message.getMsg())
        .setDescription("<p>" + message.getVerbose() + "</p>");

      String replacement = message.getReplacement();
      if (replacement != null) {
        rule.setStatus(Rule.STATUS_DEPRECATED);
        String [] pieces = StringUtils.split(replacement, ':');
        if (pieces.length == 1 || pieces[1].equalsIgnoreCase(getLanguage())) {
          rule.setDescription(rule.getDescription() + "<h2>Deprecated</h2><p>This rule is deprecated, use {rule:" + getLanguage() + ":" + pieces[0] + "} instead.</p>");
          rules.add(rule);
        }
      } else {
        rules.add(rule);
      }
    }
    return rules;
  }

  private Collection<CppcheckXmlParser.Message> load() {
    InputStream is = getClass().getResourceAsStream("/org/sonarqube/cppcheck.xml");
    try {
      return CppcheckXmlParser.parse(is);
    } catch (XMLStreamException e) {
      throw new SonarException("Unable to load descriptions of rules for Cppcheck", e);
    }
  }

  public static String repositoryKeyForLanguage(String language) {
    return language + "-" + REPOSITORY_KEY;
  }

}
