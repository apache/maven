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
package org.apache.maven.internal.impl;

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.services.xml.XmlWriterException;
import org.apache.maven.api.services.xml.XmlWriterRequest;
import org.apache.maven.model.v4.MavenStaxReader;
import org.apache.maven.model.v4.MavenStaxWriter;

import static org.apache.maven.internal.impl.Utils.nonNull;

@Named
@Singleton
public class DefaultModelXmlFactory implements ModelXmlFactory {
    @Override
    public Model read(@Nonnull XmlReaderRequest request) throws XmlReaderException {
        nonNull(request, "request");
        Path path = request.getPath();
        URL url = request.getURL();
        Reader reader = request.getReader();
        InputStream inputStream = request.getInputStream();
        if (path == null && url == null && reader == null && inputStream == null) {
            throw new IllegalArgumentException("path, url, reader or inputStream must be non null");
        }
        try {
            InputSource source = null;
            if (request.getModelId() != null || request.getLocation() != null) {
                source = new InputSource(request.getModelId(), request.getLocation());
            }
            MavenStaxReader xml = new MavenStaxReader();
            xml.setAddDefaultEntities(request.isAddDefaultEntities());
            if (inputStream != null) {
                return xml.read(inputStream, request.isStrict(), source);
            } else if (reader != null) {
                return xml.read(reader, request.isStrict(), source);
            } else if (path != null) {
                try (InputStream is = Files.newInputStream(path)) {
                    return xml.read(is, request.isStrict(), source);
                }
            } else {
                try (InputStream is = url.openStream()) {
                    return xml.read(is, request.isStrict(), source);
                }
            }
        } catch (Exception e) {
            throw new XmlReaderException("Unable to read model", e);
        }
    }

    @Override
    public void write(XmlWriterRequest<Model> request) throws XmlWriterException {
        nonNull(request, "request");
        Model content = nonNull(request.getContent(), "content");
        Path path = request.getPath();
        OutputStream outputStream = request.getOutputStream();
        Writer writer = request.getWriter();
        if (writer == null && outputStream == null && path == null) {
            throw new IllegalArgumentException("writer, outputStream or path must be non null");
        }
        try {
            if (writer != null) {
                new MavenStaxWriter().write(writer, content);
            } else if (outputStream != null) {
                new MavenStaxWriter().write(outputStream, content);
            } else {
                try (OutputStream os = Files.newOutputStream(path)) {
                    new MavenStaxWriter().write(outputStream, content);
                }
            }
        } catch (Exception e) {
            throw new XmlWriterException("Unable to write model", e);
        }
    }

    /**
     * Simply parse the given xml string.
     *
     * @param xml the input xml string
     * @return the parsed object
     * @throws XmlReaderException if an error occurs during the parsing
     * @see #toXmlString(Object)
     */
    public static Model fromXml(@Nonnull String xml) throws XmlReaderException {
        return new DefaultModelXmlFactory().fromXmlString(xml);
    }

    /**
     * Simply converts the given content to an xml string.
     *
     * @param content the object to convert
     * @return the xml string representation
     * @throws XmlWriterException if an error occurs during the transformation
     * @see #fromXmlString(String)
     */
    public static String toXml(@Nonnull Model content) throws XmlWriterException {
        return new DefaultModelXmlFactory().toXmlString(content);
    }
}
