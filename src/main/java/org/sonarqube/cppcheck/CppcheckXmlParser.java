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
import com.google.common.io.Closeables;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.StaxParser;

import javax.annotation.Nullable;
import javax.annotation.WillClose;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;

public final class CppcheckXmlParser implements StaxParser.XmlStreamHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CppcheckXmlParser.class);

  private CppcheckXmlParser() {
  }

  public static class Message {

    @Nullable
    private final String replacement;

    private final String id;
    private final String severity;
    private final String msg;
    private final String verbose;
    private final String filename;
    private final String line;

    public Message(@Nullable String replacement, String id, String severity, String msg, String verbose) {
      this.replacement = replacement;
      this.id = id;
      this.severity = severity;
      this.msg = msg;
      this.verbose = verbose;
      this.filename = null;
      this.line = null;
    }

    public Message(String id, String severity, String msg, String verbose, @Nullable String filename, @Nullable String line) {
      this.replacement = null;
      this.id = id;
      this.severity = severity;
      this.msg = msg;
      this.verbose = verbose;
      this.filename = filename;
      this.line = line;
    }

    @Nullable
    public String getReplacement() {
      return replacement;
    }

    public String getId() {
      return id;
    }

    public String getSeverity() {
      return severity;
    }

    public String getMsg() {
      return msg;
    }

    public String getVerbose() {
      return verbose;
    }

    @Nullable
    public String getFilename() {
      return filename;
    }

    @Nullable
    public String getLine() {
      return line;
    }

  }

  public static Collection<Message> parse(@WillClose InputStream is) throws XMLStreamException {
    CppcheckXmlParser handler = new CppcheckXmlParser();
    try {
      new StaxParser(handler).parse(is);
    } finally {
      Closeables.closeQuietly(is);
    }
    return handler.result.build();
  }

  public static Collection<Message> parse(File file) {
    CppcheckXmlParser handler = new CppcheckXmlParser();
    try {
      new StaxParser(handler).parse(file);
    } catch (XMLStreamException e) {
      throw new SonarException("Unable to parse file: " + file, e);
    }
    return handler.result.build();
  }

  private final ImmutableList.Builder<Message> result = ImmutableList.builder();

  @Override
  public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
    rootCursor.advance();
    SMInputCursor cppcheckCursor = rootCursor.childElementCursor();
    while (cppcheckCursor.getNext() != null) {
      String localName = cppcheckCursor.getLocalName();
      if ("cppcheck".equals(localName)) {
        String version = cppcheckCursor.getAttrValue("version");
        if (version != null) {
          LOG.info("Cppcheck version: " + version);
        }
      } else if ("errors".equals(localName)) {
        loopOnErrors(cppcheckCursor.childElementCursor("error"));
      } else {
        throw new IllegalStateException("Unexpected cppcheck file format, unexpected xml element: " + localName);
      }
    }

    loopOnErrors(cppcheckCursor);
  }

  private void loopOnErrors(SMInputCursor error) throws XMLStreamException {
    while (error.getNext() != null) {
      String id = error.getAttrValue("id");
      String severity = error.getAttrValue("severity");
      String msg = error.getAttrValue("msg");
      String verbose = error.getAttrValue("verbose");

      String replacement = error.getAttrValue("SonarQube");

      SMInputCursor location = error.childElementCursor("location");
      if (location.getNext() != null) {
        String filename = location.getAttrValue("file");
        String line = location.getAttrValue("line");
        result.add(new Message(id, severity, msg, verbose, filename, line));
      } else {
        result.add(new Message(replacement, id, severity, msg, verbose));
      }
    }
  }

}
