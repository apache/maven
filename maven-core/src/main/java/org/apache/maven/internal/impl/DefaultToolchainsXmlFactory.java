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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Objects;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.services.xml.ToolchainsXmlFactory;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.services.xml.XmlWriterException;
import org.apache.maven.api.services.xml.XmlWriterRequest;
import org.apache.maven.api.toolchain.PersistedToolchains;
import org.apache.maven.toolchain.v4.MavenToolchainsXpp3Reader;
import org.apache.maven.toolchain.v4.MavenToolchainsXpp3Writer;

@Named
@Singleton
public class DefaultToolchainsXmlFactory implements ToolchainsXmlFactory {
    @Override
    public PersistedToolchains read(@Nonnull XmlReaderRequest request) throws XmlReaderException {
        Objects.requireNonNull(request, "request can not be null");
        Reader reader = request.getReader();
        InputStream inputStream = request.getInputStream();
        if (reader == null && inputStream == null) {
            throw new IllegalArgumentException("reader or inputStream must be non null");
        }
        try {
            InputSource source = null;
            if (request.getModelId() != null || request.getLocation() != null) {
                source = new InputSource(request.getModelId(), request.getLocation());
            }
            MavenToolchainsXpp3Reader xml = new MavenToolchainsXpp3Reader();
            xml.setAddDefaultEntities(request.isAddDefaultEntities());
            if (reader != null) {
                return xml.read(reader, request.isStrict());
            } else {
                return xml.read(inputStream, request.isStrict());
            }
        } catch (Exception e) {
            throw new XmlReaderException("Unable to read toolchains", e);
        }
    }

    @Override
    public void write(XmlWriterRequest<PersistedToolchains> request) throws XmlWriterException {
        Objects.requireNonNull(request, "request can not be null");
        PersistedToolchains content = Objects.requireNonNull(request.getContent(), "content can not be null");
        OutputStream outputStream = request.getOutputStream();
        Writer writer = request.getWriter();
        if (writer == null && outputStream == null) {
            throw new IllegalArgumentException("writer or outputStream must be non null");
        }
        try {
            if (writer != null) {
                new MavenToolchainsXpp3Writer().write(writer, content);
            } else {
                new MavenToolchainsXpp3Writer().write(outputStream, content);
            }
        } catch (Exception e) {
            throw new XmlWriterException("Unable to write toolchains", e);
        }
    }
}
