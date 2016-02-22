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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import static org.fest.assertions.Assertions.assertThat;

public class CppcheckRuleRepositoryTest {

  @Test
  public void test() throws Exception {
    CppcheckRuleRepository repository = new CppcheckRuleRepository("cpp");
    List<Rule> rules = repository.createRules();
    assertThat(rules).onProperty("name").excludes(null, "");
    assertThat(rules).onProperty("description").excludes(null, "");
    assertThat(rules).onProperty("severity").containsOnly(RulePriority.MAJOR);

    int numberOfDeprecated = 0;
    for (Rule rule : rules) {
      if (Rule.STATUS_DEPRECATED.equals(rule.getStatus())) {
        numberOfDeprecated++;
      }
    }

    assertThat(rules.size()).as("total").isEqualTo(350);
    System.out.println("Number of deprecated rules: " + numberOfDeprecated);
    assertThat(numberOfDeprecated).as("deprecated").isGreaterThan(90);
  }

  @Test
  public void update() throws Exception {
    generate();
    Diff diff = XMLUnit.compareXML(
      new FileReader(new File("target/generated-cppcheck.xml")),
      new FileReader(new File("src/main/resources/org/sonarqube/cppcheck.xml"))
    );
    assertThat(diff.identical()).as("Generated cppcheck.xml identical to currently used").isTrue();
  }

  public static void generate() throws Exception {
    PrintWriter writer = new PrintWriter(new FileWriter("target/generated-cppcheck.xml"));
    TreeMap<String, RuleDescription> map = Maps.newTreeMap();
    String prevVersion = "";
    for (String version : listVersions()) {
      for (CppcheckXmlParser.Message msg : CppcheckXmlParser.parse(new File("src/main/files/cppcheck-" + version + ".xml"))) {
        RuleDescription ruleDescription = map.get(msg.getId());
        if (ruleDescription == null) {
          // new rule
          ruleDescription = new RuleDescription();
          map.put(msg.getId(), ruleDescription);
          ruleDescription.minVersion = version;
        } else {
          // existing rule
          if (!prevVersion.equals(ruleDescription.maxVersion)) {
            System.err.println("Merge of rule, which appear and disappear from version to version: " + msg.getId());
          }
        }
        ruleDescription.maxVersion = version;
        // Supposed that message from newer version is better
        ruleDescription.msg = msg;
      }
      prevVersion = version;
    }

    Properties replacements = loadReplacements();

    writer.println("<results version=\"2\">");
    writer.println("<errors>");
    for (Map.Entry<String, RuleDescription> entry : map.entrySet()) {
      RuleDescription description = entry.getValue();
      final String versions;
      if (description.maxVersion.equals(prevVersion)) {
        versions = " (since Cppcheck " + description.minVersion + ")";
      } else if (description.minVersion.equals(description.maxVersion)) {
        versions = " (Cppcheck " + description.minVersion + ")";
      } else {
        versions = " (Cppcheck " + description.minVersion + "-" + description.maxVersion + ")";
      }
      writer.append("<error ");
      if (replacements.containsKey(entry.getKey())) {
        writer.append("SonarQube=\"").append(replacements.getProperty(entry.getKey())).append("\"\n    ");
      }
      writer.append("id=\"").append(entry.getKey())
        .append("\" msg=\"").append(StringEscapeUtils.escapeXml(description.msg.getMsg() + versions))
        .append("\" verbose=\"").append(StringEscapeUtils.escapeXml(description.msg.getVerbose()))
        .append("\" />")
        .println();
    }
    writer.println("</errors>");
    writer.println("</results>");
    writer.close();
  }

  private static List<String> listVersions() {
    Collection<File> files = FileUtils.listFiles(new File("src/main/files"), new String[]{"xml"}, false);
    List<String> names = Lists.newArrayList();
    for (File file : files) {
      names.add(StringUtils.substringBetween(file.getName(), "cppcheck-", ".xml"));
    }
    Collections.sort(names);
    return names;
  }

  private static class RuleDescription {
    String minVersion, maxVersion;
    CppcheckXmlParser.Message msg;
  }

  @VisibleForTesting
  static Properties loadReplacements() {
    Properties properties = new Properties();
    FileReader reader = null;
    try {
      reader = new FileReader("src/main/files/cppcheck_replacements.properties");
      properties.load(reader);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    } finally {
      Closeables.closeQuietly(reader);
    }
    return properties;
  }

}
