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
package org.apache.maven.cling.invoker.mvnup.jdom;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Objects;

import org.jdom2.CDATA;
import org.jdom2.Comment;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.filter.ContentFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.LocatedJDOMFactory;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import static java.util.Objects.requireNonNull;

/**
 * Class that parses and writes possibly manipulated JDOM to file.
 */
public final class JDomDocumentIO implements Closeable {
    private final IOConsumer<String> consumer;
    private final String lineSeparator;
    private final String body;
    private final String head;
    private final String tail;
    private final Document document;

    public JDomDocumentIO(Path file) throws IOException {
        this(
                () -> Files.readString(file, StandardCharsets.UTF_8),
                s -> Files.writeString(file, s, StandardCharsets.UTF_8));
    }

    public JDomDocumentIO(IOSupplier<String> supplier, IOConsumer<String> consumer) throws IOException {
        this(supplier, consumer, System.lineSeparator());
    }

    public JDomDocumentIO(IOSupplier<String> supplier, IOConsumer<String> consumer, String lineSeparator)
            throws IOException {
        requireNonNull(supplier, "supplier");
        this.consumer = requireNonNull(consumer, "consumer");
        this.lineSeparator = requireNonNull(lineSeparator, "lineSeparator");

        this.body = normalizeLineEndings(supplier.get(), lineSeparator);
        SAXBuilder builder = new SAXBuilder();
        builder.setJDOMFactory(new LocatedJDOMFactory());
        try {
            this.document = builder.build(new StringReader(body));
        } catch (JDOMException e) {
            throw new IOException(e);
        }
        normaliseLineEndings(document, lineSeparator);

        int headIndex = body.indexOf("<" + document.getRootElement().getName());
        if (headIndex >= 0) {
            this.head = body.substring(0, headIndex);
        } else {
            this.head = null;
        }
        String lastTag = "</" + document.getRootElement().getName() + ">";
        int tailIndex = body.lastIndexOf(lastTag);
        if (tailIndex >= 0) {
            this.tail = body.substring(tailIndex + lastTag.length());
        } else {
            this.tail = null;
        }
    }

    public Document getDocument() {
        return document;
    }

    @Override
    public void close() throws IOException {
        Format format = Format.getRawFormat();
        format.setLineSeparator(lineSeparator);
        XMLOutputter out = new XMLOutputter(format);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (OutputStream outputStream = output) {
            if (head != null) {
                outputStream.write(head.getBytes(StandardCharsets.UTF_8));
            }
            out.output(document.getRootElement(), outputStream);
            if (tail != null) {
                outputStream.write(tail.getBytes(StandardCharsets.UTF_8));
            }
        }
        String newBody = output.toString(StandardCharsets.UTF_8);
        if (!Objects.equals(body, newBody)) {
            consumer.accept(newBody);
        }
    }

    private static void normaliseLineEndings(Document document, String lineSeparator) {
        for (Iterator<?> i = document.getDescendants(new ContentFilter(ContentFilter.COMMENT)); i.hasNext(); ) {
            Comment c = (Comment) i.next();
            c.setText(normalizeLineEndings(c.getText(), lineSeparator));
        }
        for (Iterator<?> i = document.getDescendants(new ContentFilter(ContentFilter.CDATA)); i.hasNext(); ) {
            CDATA c = (CDATA) i.next();
            c.setText(normalizeLineEndings(c.getText(), lineSeparator));
        }
    }

    private static String normalizeLineEndings(String text, String separator) {
        String norm = text;
        if (text != null) {
            norm = text.replaceAll("(\r\n)|(\n)|(\r)", separator);
        }
        return norm;
    }
}
