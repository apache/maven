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
package org.apache.maven.settings.io.xpp3;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.maven.settings.Settings;
import org.apache.maven.settings.v4.SettingsStaxReader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @deprecated Maven 3 compatibility - please use {@code org.apache.maven.api.services.xml.SettingsXmlFactory} from {@code maven-api-core}
 * or {@link SettingsStaxReader}
 */
@Deprecated
public class SettingsXpp3Reader {

    private final SettingsStaxReader delegate;

    public SettingsXpp3Reader() {
        delegate = new SettingsStaxReader();
    }

    public SettingsXpp3Reader(ContentTransformer contentTransformer) {
        delegate = new SettingsStaxReader(contentTransformer::transform);
    }

    /**
     * Returns the state of the "add default entities" flag.
     *
     * @return boolean
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
     * @param reader a reader object.
     * @param strict a strict object.
     * @throws IOException IOException if any.
     * @throws XmlPullParserException XmlPullParserException if
     * any.
     * @return Settings
     */
    public Settings read(Reader reader, boolean strict) throws IOException, XmlPullParserException {
        try {
            return new Settings(delegate.read(reader, strict, null));
        } catch (XMLStreamException e) {
            throw new XmlPullParserException(e.getMessage(), null, e);
        }
    }

    /**
     * @param reader a reader object.
     * @throws IOException IOException if any.
     * @throws XmlPullParserException XmlPullParserException if
     * any.
     * @return Model
     */
    public Settings read(Reader reader) throws IOException, XmlPullParserException {
        try {
            return new Settings(delegate.read(reader));
        } catch (XMLStreamException e) {
            throw new XmlPullParserException(e.getMessage(), null, e);
        }
    }

    /**
     * Method read.
     *
     * @param in a in object.
     * @param strict a strict object.
     * @throws IOException IOException if any.
     * @throws XmlPullParserException XmlPullParserException if
     * any.
     * @return Settings
     */
    public Settings read(InputStream in, boolean strict) throws IOException, XmlPullParserException {
        try {
            return new Settings(delegate.read(in, strict, null));
        } catch (XMLStreamException e) {
            throw new XmlPullParserException(e.getMessage(), null, e);
        }
    }

    /**
     * Method read.
     *
     * @param in a in object.
     * @throws IOException IOException if any.
     * @throws XmlPullParserException XmlPullParserException if
     * any.
     * @return Settings
     */
    public Settings read(InputStream in) throws IOException, XmlPullParserException {
        try {
            return new Settings(delegate.read(in));
        } catch (XMLStreamException e) {
            throw new XmlPullParserException(e.getMessage(), null, e);
        }
    }

    /**
     * Method read.
     *
     * @param parser a parser object.
     * @param strict a strict object.
     * @throws IOException IOException if any.
     * @throws XmlPullParserException XmlPullParserException if
     * any.
     * @return Settings
     */
    public Settings read(XMLStreamReader parser, boolean strict) throws IOException, XmlPullParserException {
        try {
            return new Settings(delegate.read(parser, strict, null));
        } catch (XMLStreamException e) {
            throw new XmlPullParserException(e.getMessage(), null, e);
        }
    }

    public interface ContentTransformer {
        /**
         * Interpolate the value read from the xpp3 document
         * @param source The source value
         * @param fieldName A description of the field being interpolated. The implementation may use this to
         *                           log stuff.
         * @return The interpolated value.
         */
        String transform(String source, String fieldName);
    }
}
