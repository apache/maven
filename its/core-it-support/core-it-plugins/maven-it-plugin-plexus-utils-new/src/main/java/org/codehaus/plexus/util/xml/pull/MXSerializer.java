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
public class MXSerializer
    implements XmlSerializer
{
    private Writer output;

    public void setOutput( Writer writer )
    {
        output = writer;
    }

    public XmlSerializer attribute( String namespace, String name, String value )
    {
        return null;
    }

    public void cdsect( String text )
    {
        // ignore
    }

    public void comment( String text )
    {
        // ignore
    }

    public void docdecl( String text )
    {
        // ignore
    }

    public void endDocument()
    {
        // ignore
    }

    public XmlSerializer endTag( String namespace, String name )
    {
        return null;
    }

    public void entityRef( String text )
    {
        // ignore
    }

    public void flush()
    {
        // ignore
    }

    public int getDepth()
    {
        return 0;
    }

    public boolean getFeature( String name )
    {
        return false;
    }

    public String getName()
    {
        return null;
    }

    public String getNamespace()
    {
        return null;
    }

    public String getPrefix( String namespace, boolean generatePrefix )
    {
        return null;
    }

    public Object getProperty( String name )
    {
        return null;
    }

    public void ignorableWhitespace( String text )
    {
        // ignore
    }

    public void processingInstruction( String text )
    {
        // ignore
    }

    public void setFeature( String name, boolean state )
    {
        // ignore
    }

    public void setOutput( OutputStream os, String encoding )
    {
        // ignore
    }

    public void setPrefix( String prefix, String namespace )
    {
        // ignore
    }

    public void setProperty( String name, Object value )
    {
        // ignore
    }

    public void startDocument( String encoding, Boolean standalone )
    {
        // ignore
    }

    public XmlSerializer startTag( String namespace, String name )
        throws IOException
    {
        output.write( name );

        return this;
    }

    public XmlSerializer text( String text )
    {
        return null;
    }

    public XmlSerializer text( char[] buf, int start, int len )
    {
        return null;
    }
}
