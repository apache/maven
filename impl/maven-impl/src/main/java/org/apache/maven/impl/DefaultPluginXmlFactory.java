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
package org.apache.maven.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.plugin.descriptor.PluginDescriptor;
import org.apache.maven.api.services.xml.PluginXmlFactory;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.services.xml.XmlWriterException;
import org.apache.maven.api.services.xml.XmlWriterRequest;
import org.apache.maven.plugin.descriptor.io.PluginDescriptorStaxReader;
import org.apache.maven.plugin.descriptor.io.PluginDescriptorStaxWriter;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.impl.StaxLocation.getLocation;
import static org.apache.maven.impl.StaxLocation.getMessage;

@Named
@Singleton
public class DefaultPluginXmlFactory implements PluginXmlFactory {

    @Override
    public PluginDescriptor read(@Nonnull XmlReaderRequest request) throws XmlReaderException {
        return read(request, requireNonNull(request).getPath(), request.getURL(), request.getReader(), request.getInputStream());
    }

    private static PluginDescriptor read(XmlReaderRequest request, Path path, URL url, Reader reader, InputStream inputStream) {
        if (path == null && url == null && reader == null && inputStream == null) {
            throw new IllegalArgumentException("path, url, reader or inputStream must be non null");
        }
        try {
            PluginDescriptorStaxReader xml = new PluginDescriptorStaxReader();
            xml.setAddDefaultEntities(request.isAddDefaultEntities());
            if (inputStream != null) {
                return xml.read(inputStream, request.isStrict());
            } else if (reader != null) {
                return xml.read(reader, request.isStrict());
            }
            try (InputStream is = Files.newInputStream(requireNonNull(path))) {
                return xml.read(is, request.isStrict());
            }
        } catch (Exception e) {
            throw new XmlReaderException("Unable to read plugin: " + getMessage(e), getLocation(e), e);
        }
    }

    @Override
    public void write(XmlWriterRequest<PluginDescriptor> request) throws XmlWriterException {
        write(request.getWriter(), request.getOutputStream(), request.getPath(), requireNonNull(requireNonNull(request, "request").getContent(), "content"));
    }

    private static void write(Writer writer, OutputStream outputStream, Path path, PluginDescriptor content) {
        if (writer == null && outputStream == null && path == null) {
            throw new IllegalArgumentException("writer, outputStream or path must be non null");
        }
        try {
            if (writer != null) {
                new PluginDescriptorStaxWriter().write(writer, content);
            } else if (outputStream != null) {
                new PluginDescriptorStaxWriter().write(outputStream, content);
            } else {
                try (OutputStream os = Files.newOutputStream(path)) {
                    new PluginDescriptorStaxWriter().write(os, content);
                }
            }
        } catch (Exception e) {
            throw new XmlWriterException("Unable to write plugin: " + getMessage(e), getLocation(e), e);
        }
    }

    /**
     * Parse the given XML string.
     *
     * @param xml the input XML string
     * @return the parsed object
     * @throws XmlReaderException if an error occurs during the parsing
     * @see #toXmlString(Object)
     */
    public static PluginDescriptor fromXml(@Nonnull String xml) throws XmlReaderException {
        return new DefaultPluginXmlFactory().fromXmlString(xml);
    }

    /**
     * Convert the given content to an XML string.
     *
     * @param content the object to convert
     * @return the XML string representation
     * @throws XmlWriterException if an error occurs during the transformation
     * @see #fromXmlString(String)
     */
    public static String toXml(@Nonnull PluginDescriptor content) throws XmlWriterException {
        return new DefaultPluginXmlFactory().toXmlString(content);
    }
}
