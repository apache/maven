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
package org.apache.maven.artifact.repository.metadata.io.xpp3;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.maven.artifact.repository.metadata.Metadata;

/**
 * Provide public methods from {@link org.apache.maven.artifact.repository.metadata.io.MetadataXpp3Reader}
 *
 * @deprecated Maven 3 compatability
 */
@Deprecated
public class MetadataXpp3Reader {

    private final org.apache.maven.artifact.repository.metadata.io.MetadataXpp3Reader delegate;

    /**
     * Default constructor
     */
    public MetadataXpp3Reader() {
        delegate = new org.apache.maven.artifact.repository.metadata.io.MetadataXpp3Reader();
    }

    /**
     * Constructor with ContentTransformer
     *
     * @param contentTransformer a transformer
     */
    public MetadataXpp3Reader(ContentTransformer contentTransformer) {
        delegate =
                new org.apache.maven.artifact.repository.metadata.io.MetadataXpp3Reader(contentTransformer::transform);
    }

    /**
     * Returns the state of the "add default entities" flag.
     *
     * @return boolean a field value
     */
    public boolean getAddDefaultEntities() {
        return delegate.getAddDefaultEntities();
    }

    /**
     * Sets the state of the "add default entities" flag.
     *
     * @param addDefaultEntities a addDefaultEntities object.
     */
    public void setAddDefaultEntities(boolean addDefaultEntities) {
        delegate.setAddDefaultEntities(addDefaultEntities);
    }

    /**
     * Method read.
     *
     * @param reader a reader object.
     * @param strict a strict object.
     * @return Metadata
     * @throws IOException            IOException if any.
     * @throws XMLStreamException XmlPullParserException if
     *                                any.
     */
    public Metadata read(Reader reader, boolean strict) throws IOException, XMLStreamException {
        return new Metadata(delegate.read(reader, strict));
    }

    /**
     * Method read.
     *
     * @param reader a reader object.
     * @return Metadata
     * @throws IOException            IOException if any.
     * @throws XMLStreamException XmlPullParserException if
     *                                any.
     */
    public Metadata read(Reader reader) throws IOException, XMLStreamException {
        return new Metadata(delegate.read(reader));
    }

    /**
     * Method read.
     *
     * @param in     a in object.
     * @param strict a strict object.
     * @return Metadata
     * @throws IOException            IOException if any.
     * @throws XMLStreamException XmlPullParserException if
     *                                any.
     */
    public Metadata read(InputStream in, boolean strict) throws IOException, XMLStreamException {
        return new Metadata(delegate.read(in, strict));
    }

    /**
     * Method read.
     *
     * @param in a in object.
     * @return Metadata
     * @throws IOException            IOException if any.
     * @throws XMLStreamException XmlPullParserException if
     *                                any.
     */
    public Metadata read(InputStream in) throws IOException, XMLStreamException {
        return new Metadata(delegate.read(in));
    }

    /**
     * Method read.
     *
     * @param parser a parser object.
     * @param strict a strict object.
     * @return Metadata
     * @throws IOException            IOException if any.
     * @throws XMLStreamException XmlPullParserException if
     *                                any.
     */
    public Metadata read(XMLStreamReader parser, boolean strict) throws IOException, XMLStreamException {
        return new Metadata(delegate.read(parser, strict));
    }

    /**
     * {@link org.apache.maven.artifact.repository.metadata.io.MetadataXpp3Reader.ContentTransformer}
     */
    public interface ContentTransformer {
        /**
         * Interpolate the value read from the xpp3 document
         *
         * @param source    The source value
         * @param fieldName A description of the field being interpolated. The implementation may use this to
         *                  log stuff.
         * @return The interpolated value.
         */
        String transform(String source, String fieldName);
    }
}
