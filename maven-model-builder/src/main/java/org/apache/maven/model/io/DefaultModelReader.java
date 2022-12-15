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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelSourceTransformer;
import org.apache.maven.model.building.TransformerContext;
import org.apache.maven.model.v4.MavenXpp3Reader;
import org.apache.maven.model.v4.MavenXpp3ReaderEx;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.EntityReplacementMap;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Handles deserialization of a model from some kind of textual format like XML.
 *
 * @author Benjamin Bentmann
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

        try (XmlStreamReader in = ReaderFactory.newXmlReader(input)) {
            Model model = read(in, input.toPath(), options);

            model.setPomFile(input);

            return model;
        }
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

        try (XmlStreamReader in = ReaderFactory.newXmlReader(input)) {
            return read(in, null, options);
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

    private TransformerContext getTransformerContext(Map<String, ?> options) {
        Object value = (options != null) ? options.get(TRANSFORMER_CONTEXT) : null;
        return (TransformerContext) value;
    }

    private Model read(Reader reader, Path pomFile, Map<String, ?> options) throws IOException {
        try {
            XmlPullParser parser = new MXParser(EntityReplacementMap.defaultEntityReplacementMap);
            parser.setInput(reader);

            TransformerContext context = getTransformerContext(options);
            XmlPullParser transformingParser =
                    context != null ? transformer.transform(parser, pomFile, context) : parser;

            InputSource source = getSource(options);
            boolean strict = isStrict(options);
            if (source != null) {
                return readModelEx(transformingParser, source, strict);
            } else {
                return readModel(transformingParser, strict);
            }
        } catch (XmlPullParserException e) {
            throw new ModelParseException(e.getMessage(), e.getLineNumber(), e.getColumnNumber(), e);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Unable to transform pom", e);
        }
    }

    private Model readModel(XmlPullParser parser, boolean strict) throws XmlPullParserException, IOException {
        MavenXpp3Reader mr = new MavenXpp3Reader();
        return new Model(mr.read(parser, strict));
    }

    private Model readModelEx(XmlPullParser parser, InputSource source, boolean strict)
            throws XmlPullParserException, IOException {
        MavenXpp3ReaderEx mr = new MavenXpp3ReaderEx();
        return new Model(mr.read(
                parser, strict, new org.apache.maven.api.model.InputSource(source.getModelId(), source.getLocation())));
    }
}
