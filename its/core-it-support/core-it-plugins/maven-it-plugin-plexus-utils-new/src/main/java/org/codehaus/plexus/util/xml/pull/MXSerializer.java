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
package org.codehaus.plexus.util.xml.pull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 *
 */
public class MXSerializer implements XmlSerializer {
    private Writer output;

    @Override
    public void setOutput(Writer writer) {
        output = writer;
    }

    @Override
    public XmlSerializer attribute(String namespace, String name, String value) {
        return null;
    }

    @Override
    public void cdsect(String text) {
        // ignore
    }

    @Override
    public void comment(String text) {
        // ignore
    }

    @Override
    public void docdecl(String text) {
        // ignore
    }

    @Override
    public void endDocument() {
        // ignore
    }

    @Override
    public XmlSerializer endTag(String namespace, String name) {
        return null;
    }

    @Override
    public void entityRef(String text) {
        // ignore
    }

    @Override
    public void flush() {
        // ignore
    }

    @Override
    public int getDepth() {
        return 0;
    }

    @Override
    public boolean getFeature(String name) {
        return false;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    public String getPrefix(String namespace, boolean generatePrefix) {
        return null;
    }

    @Override
    public Object getProperty(String name) {
        return null;
    }

    @Override
    public void ignorableWhitespace(String text) {
        // ignore
    }

    @Override
    public void processingInstruction(String text) {
        // ignore
    }

    @Override
    public void setFeature(String name, boolean state) {
        // ignore
    }

    @Override
    public void setOutput(OutputStream os, String encoding) {
        // ignore
    }

    @Override
    public void setPrefix(String prefix, String namespace) {
        // ignore
    }

    @Override
    public void setProperty(String name, Object value) {
        // ignore
    }

    @Override
    public void startDocument(String encoding, Boolean standalone) {
        // ignore
    }

    @Override
    public XmlSerializer startTag(String namespace, String name) throws IOException {
        output.write(name);

        return this;
    }

    @Override
    public XmlSerializer text(String text) {
        return null;
    }

    @Override
    public XmlSerializer text(char[] buf, int start, int len) {
        return null;
    }
}
