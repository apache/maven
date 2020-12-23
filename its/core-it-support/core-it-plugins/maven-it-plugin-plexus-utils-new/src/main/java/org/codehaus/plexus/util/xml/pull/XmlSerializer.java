package org.codehaus.plexus.util.xml.pull;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 *
 */
public interface XmlSerializer
{

    void setFeature( String name, boolean state )
        throws IllegalArgumentException, IllegalStateException;

    boolean getFeature( String name );

    void setProperty( String name, Object value )
        throws IllegalArgumentException, IllegalStateException;

    Object getProperty( String name );

    void setOutput( OutputStream os, String encoding )
        throws IOException, IllegalArgumentException, IllegalStateException;

    void setOutput( Writer writer )
        throws IOException, IllegalArgumentException, IllegalStateException;

    void startDocument( String encoding, Boolean standalone )
        throws IOException, IllegalArgumentException, IllegalStateException;

    void endDocument()
        throws IOException, IllegalArgumentException, IllegalStateException;

    void setPrefix( String prefix, String namespace )
        throws IOException, IllegalArgumentException, IllegalStateException;

    String getPrefix( String namespace, boolean generatePrefix )
        throws IllegalArgumentException;

    int getDepth();

    String getNamespace();

    String getName();

    XmlSerializer startTag( String namespace, String name )
        throws IOException, IllegalArgumentException, IllegalStateException;

    XmlSerializer attribute( String namespace, String name, String value )
        throws IOException, IllegalArgumentException, IllegalStateException;

    XmlSerializer endTag( String namespace, String name )
        throws IOException, IllegalArgumentException, IllegalStateException;

    XmlSerializer text( String text )
        throws IOException, IllegalArgumentException, IllegalStateException;

    XmlSerializer text( char[] buf, int start, int len )
        throws IOException, IllegalArgumentException, IllegalStateException;

    void cdsect( String text )
        throws IOException, IllegalArgumentException, IllegalStateException;

    void entityRef( String text )
        throws IOException, IllegalArgumentException, IllegalStateException;

    void processingInstruction( String text )
        throws IOException, IllegalArgumentException, IllegalStateException;

    void comment( String text )
        throws IOException, IllegalArgumentException, IllegalStateException;

    void docdecl( String text )
        throws IOException, IllegalArgumentException, IllegalStateException;

    void ignorableWhitespace( String text )
        throws IOException, IllegalArgumentException, IllegalStateException;

    void flush()
        throws IOException;
}
