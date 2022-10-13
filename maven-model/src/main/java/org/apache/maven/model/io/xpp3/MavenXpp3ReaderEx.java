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
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.EntityReplacementMap;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class MavenXpp3ReaderEx
{
    private boolean addDefaultEntities = true;

    private final ContentTransformer contentTransformer;

    public MavenXpp3ReaderEx()
    {
        this( ( source, fieldName ) -> source );
    }

    public MavenXpp3ReaderEx( ContentTransformer contentTransformer )
    {
        this.contentTransformer = contentTransformer;
    }

    /**
     * Returns the state of the "add default entities" flag.
     *
     * @return boolean
     */
    public boolean getAddDefaultEntities()
    {
        return addDefaultEntities;
    } // -- boolean getAddDefaultEntities()

    /**
     * @see ReaderFactory#newXmlReader
     * @param reader a reader object.
     * @param strict a strict object.
     * @throws IOException IOException if any.
     * @throws XmlPullParserException XmlPullParserException if any.
     * @return Model
     */
    public Model read( Reader reader, boolean strict, InputSource source )
        throws IOException, XmlPullParserException
    {
        XmlPullParser parser =
            addDefaultEntities ? new MXParser( EntityReplacementMap.defaultEntityReplacementMap ) : new MXParser();
        parser.setInput( reader );
        return read( parser, strict, source );
    } // -- Model read( Reader, boolean )

    /**
     * @see ReaderFactory#newXmlReader
     * @param reader a reader object.
     * @throws IOException IOException if any.
     * @throws XmlPullParserException XmlPullParserException if any.
     * @return Model
     */
    public Model read( Reader reader, InputSource source )
        throws IOException, XmlPullParserException
    {
        return read( reader, true, source );
    } // -- Model read( Reader )

    /**
     * Method read.
     *
     * @param in a in object.
     * @param strict a strict object.
     * @throws IOException IOException if any.
     * @throws XmlPullParserException XmlPullParserException if any.
     * @return Model
     */
    public Model read( InputStream in, boolean strict, InputSource source )
        throws IOException, XmlPullParserException
    {
        return read( ReaderFactory.newXmlReader( in ), strict, source );
    } // -- Model read( InputStream, boolean )

    /**
     * Method read.
     *
     * @param in a in object.
     * @throws IOException IOException if any.
     * @throws XmlPullParserException XmlPullParserException if any.
     * @return Model
     */
    public Model read( InputStream in, InputSource source )
        throws IOException, XmlPullParserException
    {
        return read( ReaderFactory.newXmlReader( in ), source );
    } // -- Model read( InputStream )

    /**
     * Method read.
     *
     * @param parser a parser object.
     * @param strict a strict object.
     * @throws IOException IOException if any.
     * @throws XmlPullParserException XmlPullParserException if any.
     * @return Model
     */
    public Model read( XmlPullParser parser, boolean strict, InputSource source )
        throws IOException, XmlPullParserException
    {
        org.apache.maven.model.v4.MavenXpp3ReaderEx reader = contentTransformer != null
                        ? new org.apache.maven.model.v4.MavenXpp3ReaderEx( contentTransformer::transform )
                        : new org.apache.maven.model.v4.MavenXpp3ReaderEx();
        reader.setAddDefaultEntities( addDefaultEntities );
        org.apache.maven.api.model.Model model =
            reader.read( parser, strict,
                         new org.apache.maven.api.model.InputSource( source.getModelId(), source.getLocation() ) );
        return new Model( model );
    }

    /**
     * Sets the state of the "add default entities" flag.
     *
     * @param addDefaultEntities a addDefaultEntities object.
     */
    public void setAddDefaultEntities( boolean addDefaultEntities )
    {
        this.addDefaultEntities = addDefaultEntities;
    } // -- void setAddDefaultEntities( boolean )

    public interface ContentTransformer
    {
        /**
         * Interpolate the value read from the xpp3 document
         * 
         * @param source The source value
         * @param fieldName A description of the field being interpolated. The implementation may use this to log stuff.
         * @return The interpolated value.
         */
        String transform( String source, String fieldName );
    }

}
