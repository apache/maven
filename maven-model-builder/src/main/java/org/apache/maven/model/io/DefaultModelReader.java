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
package org.apache.maven.model.io;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelSourceTransformer;
import org.apache.maven.model.v4.MavenStaxReader;
import org.apache.maven.stax.xinclude.XInclude;
import org.codehaus.stax2.io.Stax2FileSource;

/**
 * Handles deserialization of a model from some kind of textual format like XML.
 *
 */
@Named
@Singleton
public class DefaultModelReader implements ModelReader {
    private final ModelSourceTransformer transformer;

    @Inject
    public DefaultModelReader(ModelSourceTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public Model read(File input, Map<String, ?> options) throws IOException {
        Objects.requireNonNull(input, "input cannot be null");

        Model model = read(null, input, options);

        model.setPomFile(input);

        return model;
    }

    @Override
    public Model read(Reader input, Map<String, ?> options) throws IOException {
        Objects.requireNonNull(input, "input cannot be null");

        try (Reader in = input) {
            return read(in, null, options);
        }
    }

    @Override
    public Model read(InputStream input, Map<String, ?> options) throws IOException {
        Objects.requireNonNull(input, "input cannot be null");

        try (InputStream in = input) {
            return read(input, null, options);
        }
    }

    private boolean isStrict(Map<String, ?> options) {
        Object value = (options != null) ? options.get(IS_STRICT) : null;
        return value == null || Boolean.parseBoolean(value.toString());
    }

    private InputSource getSource(Map<String, ?> options) {
        Object value = (options != null) ? options.get(INPUT_SOURCE) : null;
        return (InputSource) value;
    }

    private Path getRootDirectory(Map<String, ?> options) {
        Object value = (options != null) ? options.get(ROOT_DIRECTORY) : null;
        return (Path) value;
    }

    private boolean getXInclude(Map<String, ?> options) {
        Object value = (options != null) ? options.get(XINCLUDE) : null;
        return value instanceof Boolean && (Boolean) value;
    }

    private Model read(InputStream input, File pomFile, Map<String, ?> options) throws IOException {
        try {
            InputSource source = getSource(options);
            boolean strict = isStrict(options);
            Path rootDirectory = getRootDirectory(options);

            Source xmlSource;
            if (pomFile != null) {
                xmlSource = new Stax2FileSource(pomFile);
            } else {
                xmlSource = new StreamSource(input);
            }

            XMLStreamReader parser;
            // We only support general external entities and XInclude when reading a file in strict mode
            if (pomFile != null && strict && getXInclude(options)) {
                parser = XInclude.xinclude(xmlSource, new LocalXmlResolver(rootDirectory));
            } else {
                XMLInputFactory factory = new com.ctc.wstx.stax.WstxInputFactory();
                factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
                factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
                parser = factory.createXMLStreamReader(xmlSource);
            }

            MavenStaxReader mr = new MavenStaxReader();
            mr.setAddLocationInformation(source != null);
            Model model = new Model(mr.read(parser, strict, source != null ? source.toApiSource() : null));
            return model;
        } catch (XMLStreamException e) {
            Location location = e.getLocation();
            throw new ModelParseException(
                    e.getMessage(),
                    location != null ? location.getLineNumber() : -1,
                    location != null ? location.getColumnNumber() : -1,
                    e);
        } catch (Exception e) {
            throw new IOException("Unable to transform pom", e);
        }
    }

    private Model read(Reader reader, Path pomFile, Map<String, ?> options) throws IOException {
        try {
            XMLInputFactory factory = new com.ctc.wstx.stax.WstxInputFactory();
            XMLStreamReader parser = factory.createXMLStreamReader(reader);

            InputSource source = getSource(options);
            boolean strict = isStrict(options);
            MavenStaxReader mr = new MavenStaxReader();
            mr.setAddLocationInformation(source != null);
            Model model = new Model(mr.read(parser, strict, source != null ? source.toApiSource() : null));
            return model;
        } catch (XMLStreamException e) {
            Location location = e.getLocation();
            throw new ModelParseException(
                    e.getMessage(),
                    location != null ? location.getLineNumber() : -1,
                    location != null ? location.getColumnNumber() : -1,
                    e);
        } catch (Exception e) {
            throw new IOException("Unable to transform pom", e);
        }
    }
}
