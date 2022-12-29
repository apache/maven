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
package org.apache.maven.internal.xml;

import java.io.Reader;

import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class DomBuilder {

    public static Xpp3Dom build(Reader reader) throws MavenXmlException {
        return build(reader, true);
    }

    public static Xpp3Dom build(Reader reader, boolean trim) throws MavenXmlException {
        try {
            MXParser parser = new MXParser();
            parser.setInput(reader);
            return build(parser, trim);
        } catch (XmlPullParserException e) {
            throw new MavenXmlException("Unable to build DOM", e);
        }
    }

    public static Xpp3Dom build(XmlPullParser parser) {
        return build(parser, true, null);
    }

    public static Xpp3Dom build(XmlPullParser parser, boolean trim) {
        return build(parser, trim, null);
    }

    public static Xpp3Dom build(XmlPullParser parser, boolean trim, LocationBuilder locationBuilder) {
        try {
            Xpp3DomBuilder.InputLocationBuilder ilb =
                    locationBuilder != null ? (p -> locationBuilder.getLocation()) : null;
            return Xpp3DomBuilder.build(parser, trim, ilb);
        } catch (Exception e) {
            throw new MavenXmlException("Unable to build DOM", e);
        }
    }

    public static class LocationBuilder {

        private final Object location;

        public LocationBuilder(Object location) {
            this.location = location;
        }

        public Object getLocation() {
            return location;
        }
    }
}
