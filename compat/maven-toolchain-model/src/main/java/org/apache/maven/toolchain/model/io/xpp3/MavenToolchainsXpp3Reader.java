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
package org.apache.maven.toolchain.model.io.xpp3;

import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.v4.MavenToolchainsStaxReader;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 *
 * @deprecated use MavenToolchainsStaxReader.
 */
@Deprecated(since = "4.0.0")
public class MavenToolchainsXpp3Reader {

    private final MavenToolchainsStaxReader delegate;

    public MavenToolchainsXpp3Reader() {
        delegate = new MavenToolchainsStaxReader();
    }

    public MavenToolchainsXpp3Reader(ContentTransformer contentTransformer) {
        delegate = contentTransformer != null
                ? new MavenToolchainsStaxReader(contentTransformer::transform)
                : new MavenToolchainsStaxReader();
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
     * Returns the state of the "add default entities" flag.
     *
     * @return boolean
     */
    public boolean getAddDefaultEntities() {
        return delegate.getAddDefaultEntities();
    }

    /**
     * Method read.
     *
     * @param parser a parser object.
     * @param strict a strict object.
     * @return PersistedToolchains
     * @throws IOException IOException if any.
     * @throws XmlPullParserException XmlPullParserException if
     *         any.
     */
    public PersistedToolchains read(XmlPullParser parser, boolean strict) throws IOException, XmlPullParserException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * @param reader a reader object.
     * @param strict a strict object.
     * @return PersistedToolchains
     * @throws IOException IOException if any.
     * @throws XmlPullParserException XmlPullParserException if
     *         any.
     * @see XmlStreamReader
     */
    public PersistedToolchains read(Reader reader, boolean strict) throws IOException, XmlPullParserException {
        try {
            return new PersistedToolchains(delegate.read(reader, strict, null));
        } catch (XMLStreamException e) {
            throw new XmlPullParserException(e.getMessage(), null, e);
        }
    }

    /**
     * @param reader a reader object.
     * @return PersistedToolchains
     * @throws IOException IOException if any.
     * @throws XmlPullParserException XmlPullParserException if
     *         any.
     * @see XmlStreamReader
     */
    public PersistedToolchains read(Reader reader) throws IOException, XmlPullParserException {
        return read(reader, true);
    }

    /**
     * Method read.
     *
     * @param in a in object.
     * @param strict a strict object.
     * @return PersistedToolchains
     * @throws IOException IOException if any.
     * @throws XmlPullParserException XmlPullParserException if
     *         any.
     */
    public PersistedToolchains read(InputStream in, boolean strict) throws IOException, XmlPullParserException {
        return read(new XmlStreamReader(in), strict);
    }

    /**
     * Method read.
     *
     * @param in a in object.
     * @return PersistedToolchains
     * @throws IOException IOException if any.
     * @throws XmlPullParserException XmlPullParserException if
     *         any.
     */
    public PersistedToolchains read(InputStream in) throws IOException, XmlPullParserException {
        return read(new XmlStreamReader(in));
    }

    public interface ContentTransformer {
        /**
         * Interpolate the value read from the xpp3 document
         *
         * @param source The source value
         * @param fieldName A description of the field being interpolated. The implementation may use this to
         *         log stuff.
         * @return The interpolated value.
         */
        String transform(String source, String fieldName);
    }
}
