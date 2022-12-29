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
package org.codehaus.plexus.util.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @version $Id$
 */
public class Xpp3DomBuilder {
    private static final boolean DEFAULT_TRIM = true;

    public static Xpp3Dom build(Reader reader) throws XmlPullParserException, IOException {
        return build(reader, null);
    }

    /**
     * @since 3.2.0
     */
    public static Xpp3Dom build(Reader reader, InputLocationBuilder locationBuilder)
            throws XmlPullParserException, IOException {
        return build(reader, DEFAULT_TRIM, locationBuilder);
    }

    public static Xpp3Dom build(InputStream is, String encoding) throws XmlPullParserException, IOException {
        return build(is, encoding, DEFAULT_TRIM);
    }

    public static Xpp3Dom build(InputStream is, String encoding, boolean trim)
            throws XmlPullParserException, IOException {
        return new Xpp3Dom(org.apache.maven.internal.xml.Xpp3DomBuilder.build(is, encoding, trim));
    }

    public static Xpp3Dom build(Reader reader, boolean trim) throws XmlPullParserException, IOException {
        return build(reader, trim, null);
    }

    /**
     * @since 3.2.0
     */
    public static Xpp3Dom build(Reader reader, boolean trim, InputLocationBuilder locationBuilder)
            throws XmlPullParserException, IOException {
        return new Xpp3Dom(org.apache.maven.internal.xml.Xpp3DomBuilder.build(
                reader, trim, locationBuilder != null ? locationBuilder::toInputLocation : null));
    }

    public static Xpp3Dom build(XmlPullParser parser) throws XmlPullParserException, IOException {
        return build(parser, DEFAULT_TRIM);
    }

    public static Xpp3Dom build(XmlPullParser parser, boolean trim) throws XmlPullParserException, IOException {
        return build(parser, trim, null);
    }

    /**
     * @since 3.2.0
     */
    public static Xpp3Dom build(XmlPullParser parser, boolean trim, InputLocationBuilder locationBuilder)
            throws XmlPullParserException, IOException {
        return new Xpp3Dom(org.apache.maven.internal.xml.Xpp3DomBuilder.build(
                parser, trim, locationBuilder != null ? locationBuilder::toInputLocation : null));
    }

    /**
     * Input location builder interface, to be implemented to choose how to store data.
     *
     * @since 3.2.0
     */
    public interface InputLocationBuilder {
        Object toInputLocation(XmlPullParser parser);
    }
}
