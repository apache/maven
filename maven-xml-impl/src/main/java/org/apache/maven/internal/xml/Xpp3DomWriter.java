/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.internal.xml;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;
import org.apache.maven.api.xml.Dom;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 *
 */
public class Xpp3DomWriter {
    public static void write(Writer writer, Dom dom) {
        write(new PrettyPrintXMLWriter(writer), dom);
    }

    public static void write(PrintWriter writer, Dom dom) {
        write(new PrettyPrintXMLWriter(writer), dom);
    }

    public static void write(XMLWriter xmlWriter, Dom dom) {
        write(xmlWriter, dom, true);
    }

    public static void write(XMLWriter xmlWriter, Dom dom, boolean escape) {
        // TODO: move to XMLWriter?
        xmlWriter.startElement(dom.getName());
        for (Map.Entry<String, String> attr : dom.getAttributes().entrySet()) {
            xmlWriter.addAttribute(attr.getKey(), attr.getValue());
        }
        for (Dom aChildren : dom.getChildren()) {
            write(xmlWriter, aChildren, escape);
        }

        String value = dom.getValue();
        if (value != null) {
            if (escape) {
                xmlWriter.writeText(value);
            } else {
                xmlWriter.writeMarkup(value);
            }
        }

        xmlWriter.endElement();
    }
}
