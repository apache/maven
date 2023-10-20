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
package org.apache.maven.model.io.xpp3;

import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.v4.MavenStaxReader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @deprecated Use MavenStaxReader instead
 */
@Deprecated
public class MavenXpp3Reader {
    private MavenStaxReader delegate;

    public MavenXpp3Reader() {
        this(null, false);
    }

    public MavenXpp3Reader(ContentTransformer contentTransformer) {
        this(contentTransformer, false);
    }

    protected MavenXpp3Reader(ContentTransformer contentTransformer, boolean addLocationInformation) {
        delegate =
                contentTransformer != null ? new MavenStaxReader(contentTransformer::transform) : new MavenStaxReader();
        delegate.setAddLocationInformation(addLocationInformation);
    }

    /**
     * Returns the state of the "add default entities" flag.
     *
     * @return boolean
     */
    public boolean getAddDefaultEntities() {
        return delegate.getAddDefaultEntities();
    } // -- boolean getAddDefaultEntities()

    /**
     * Sets the state of the "add default entities" flag.
     *
     * @param addDefaultEntities a addDefaultEntities object.
     */
    public void setAddDefaultEntities(boolean addDefaultEntities) {
        delegate.setAddLocationInformation(addDefaultEntities);
    } // -- void setAddDefaultEntities( boolean )

    protected Model read(Reader reader, boolean strict, InputSource source) throws IOException, XmlPullParserException {
        try {
            org.apache.maven.api.model.Model model =
                    delegate.read(reader, strict, source != null ? source.toApiSource() : null);
            return new Model(model);
        } catch (XMLStreamException e) {
            throw new XmlPullParserException(e.getMessage(), null, e);
        }
    }

    /**
     *
     * @param reader a reader object.
     * @param strict a strict object.
     * @throws IOException IOException if any.
     * @throws XmlPullParserException XmlPullParserException if
     * any.
     * @return Model
     */
    public Model read(Reader reader, boolean strict) throws IOException, XmlPullParserException {
        return read(reader, strict, null);
    } // -- Model read( Reader, boolean )

    /**
     *
     * @param reader a reader object.
     * @throws IOException IOException if any.
     * @throws XmlPullParserException XmlPullParserException if
     * any.
     * @return Model
     */
    public Model read(Reader reader) throws IOException, XmlPullParserException {
        return read(reader, true);
    } // -- Model read( Reader )

    protected Model read(InputStream is, boolean strict, InputSource source)
            throws IOException, XmlPullParserException {
        try {
            org.apache.maven.api.model.Model model =
                    delegate.read(is, strict, source != null ? source.toApiSource() : null);
            return new Model(model);
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
     * @return Model
     */
    public Model read(InputStream in, boolean strict) throws IOException, XmlPullParserException {
        return read(in, strict, null);
    } // -- Model read( InputStream, boolean )

    /**
     * Method read.
     *
     * @param in a in object.
     * @throws IOException IOException if any.
     * @throws XmlPullParserException XmlPullParserException if
     * any.
     * @return Model
     */
    public Model read(InputStream in) throws IOException, XmlPullParserException {
        return read(in, true);
    } // -- Model read( InputStream )

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
