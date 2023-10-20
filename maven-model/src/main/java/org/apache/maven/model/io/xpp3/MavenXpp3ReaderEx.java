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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @deprecated Use MavenStaxReader instead
 */
@Deprecated
public class MavenXpp3ReaderEx extends MavenXpp3Reader {

    public MavenXpp3ReaderEx() {
        this(null);
    }

    public MavenXpp3ReaderEx(ContentTransformer contentTransformer) {
        super(contentTransformer != null ? contentTransformer::transform : null, true);
    }

    @Override
    public Model read(Reader reader, boolean strict, InputSource source) throws IOException, XmlPullParserException {
        return super.read(reader, strict, source);
    }

    @Override
    public Model read(Reader reader, boolean strict) throws IOException, XmlPullParserException {
        return super.read(reader, strict);
    }

    @Override
    public Model read(Reader reader) throws IOException, XmlPullParserException {
        return super.read(reader);
    }

    @Override
    public Model read(InputStream in, boolean strict, InputSource source) throws IOException, XmlPullParserException {
        return super.read(in, strict, source);
    }

    @Override
    public Model read(InputStream in, boolean strict) throws IOException, XmlPullParserException {
        return super.read(in, strict);
    }

    @Override
    public Model read(InputStream in) throws IOException, XmlPullParserException {
        return super.read(in);
    }

    public interface ContentTransformer {
        /**
         * Interpolate the value read from the xpp3 document
         *
         * @param source    the source value
         * @param fieldName a description of the field being interpolated. The implementation may use this to
         *                  log stuff
         * @return the interpolated value
         */
        String transform(String source, String fieldName);
    }
}
