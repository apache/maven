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
package org.apache.maven.model.transform;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.maven.model.transform.pull.XmlUtils;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public abstract class AbstractXMLFilterTests
{
    protected XmlPullParser getFilter( XmlPullParser parser )
    {
        throw new UnsupportedOperationException( "Override one of the getFilter() methods" );
    }

    protected String transform( String input )
        throws XmlPullParserException, IOException
    {
        return transform( new StringReader( input ) );
    }

    protected String transform( Reader input )
        throws XmlPullParserException, IOException
    {

        MXParser parser = new MXParser();
        parser.setFeature( XmlPullParser.FEATURE_PROCESS_NAMESPACES, true );
        parser.setInput( input );
        XmlPullParser filter = getFilter( parser );

        StringWriter writer = new StringWriter();
        XmlUtils.writeDocument( filter, writer );
        return writer.toString();
    }

}
