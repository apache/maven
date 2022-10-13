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
package org.apache.maven.model.transform.pull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.MXSerializer;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.util.xml.pull.XmlSerializer;

public class XmlUtils
{

    public static ByteArrayInputStream writeDocument( XmlStreamReader reader, XmlPullParser parser )
        throws IOException, XmlPullParserException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer writer = newWriter( reader, baos );
        writeDocument( parser, writer );
        return new ByteArrayInputStream( baos.toByteArray() );
    }

    public static void writeDocument( XmlPullParser parser, Writer writer )
        throws IOException, XmlPullParserException
    {
        XmlSerializer serializer = new MXSerializer();
        serializer.setOutput( writer );

        while ( parser.nextToken() != XmlPullParser.END_DOCUMENT )
        {
            switch ( parser.getEventType() )
            {
                case XmlPullParser.START_DOCUMENT:
                    serializer.startDocument( parser.getInputEncoding(), true );
                    break;
                case XmlPullParser.END_DOCUMENT:
                    serializer.endDocument();
                case XmlPullParser.START_TAG:
                    int nsStart = parser.getNamespaceCount( parser.getDepth() - 1 );
                    int nsEnd = parser.getNamespaceCount( parser.getDepth() );
                    for ( int i = nsStart; i < nsEnd; i++ )
                    {
                        String prefix = parser.getNamespacePrefix( i );
                        String ns = parser.getNamespaceUri( i );
                        serializer.setPrefix( prefix, ns );
                    }
                    serializer.startTag( parser.getNamespace(), parser.getName() );
                    for ( int i = 0; i < parser.getAttributeCount(); i++ )
                    {
                        serializer.attribute( parser.getAttributeNamespace( i ), parser.getAttributeName( i ),
                                              parser.getAttributeValue( i ) );
                    }
                    break;
                case XmlPullParser.END_TAG:
                    serializer.endTag( parser.getNamespace(), parser.getName() );
                    break;
                case XmlPullParser.TEXT:
                    serializer.text( normalize( parser.getText() ) );
                    break;
                case XmlPullParser.CDSECT:
                    serializer.cdsect( parser.getText() );
                    break;
                case XmlPullParser.ENTITY_REF:
                    serializer.entityRef( parser.getName() );
                    break;
                case XmlPullParser.IGNORABLE_WHITESPACE:
                    serializer.ignorableWhitespace( normalize( parser.getText() ) );
                    break;
                case XmlPullParser.PROCESSING_INSTRUCTION:
                    serializer.processingInstruction( parser.getText() );
                    break;
                case XmlPullParser.COMMENT:
                    serializer.comment( normalize( parser.getText() ) );
                    break;
                case XmlPullParser.DOCDECL:
                    serializer.docdecl( normalize( parser.getText() ) );
                    break;
                default:
                    break;
            }
        }

        serializer.endDocument();
    }

    private static OutputStreamWriter newWriter( XmlStreamReader reader, ByteArrayOutputStream baos )
        throws UnsupportedEncodingException
    {
        if ( reader.getEncoding() != null )
        {
            return new OutputStreamWriter( baos, reader.getEncoding() );
        }
        else
        {
            return new OutputStreamWriter( baos );
        }
    }

    private static String normalize( String input )
    {
        if ( input.indexOf( '\n' ) >= 0 && !"\n".equals( System.lineSeparator() ) )
        {
            return input.replace( "\n", System.lineSeparator() );
        }
        return input;
    }
}
