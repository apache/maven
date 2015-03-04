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
        throws IOException, IllegalArgumentException, IllegalStateException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void cdsect( String text )
        throws IOException, IllegalArgumentException, IllegalStateException
    {
        // TODO Auto-generated method stub

    }

    public void comment( String text )
        throws IOException, IllegalArgumentException, IllegalStateException
    {
        // TODO Auto-generated method stub

    }

    public void docdecl( String text )
        throws IOException, IllegalArgumentException, IllegalStateException
    {
        // TODO Auto-generated method stub

    }

    public void endDocument()
        throws IOException, IllegalArgumentException, IllegalStateException
    {
        // TODO Auto-generated method stub

    }

    public XmlSerializer endTag( String namespace, String name )
        throws IOException, IllegalArgumentException, IllegalStateException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void entityRef( String text )
        throws IOException, IllegalArgumentException, IllegalStateException
    {
        // TODO Auto-generated method stub

    }

    public void flush()
        throws IOException
    {
        // TODO Auto-generated method stub

    }

    public int getDepth()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean getFeature( String name )
    {
        // TODO Auto-generated method stub
        return false;
    }

    public String getName()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getNamespace()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getPrefix( String namespace, boolean generatePrefix )
        throws IllegalArgumentException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getProperty( String name )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void ignorableWhitespace( String text )
        throws IOException, IllegalArgumentException, IllegalStateException
    {
        // TODO Auto-generated method stub

    }

    public void processingInstruction( String text )
        throws IOException, IllegalArgumentException, IllegalStateException
    {
        // TODO Auto-generated method stub

    }

    public void setFeature( String name, boolean state )
        throws IllegalArgumentException, IllegalStateException
    {
        // TODO Auto-generated method stub

    }

    public void setOutput( OutputStream os, String encoding )
        throws IOException, IllegalArgumentException, IllegalStateException
    {
        // TODO Auto-generated method stub

    }

    public void setPrefix( String prefix, String namespace )
        throws IOException, IllegalArgumentException, IllegalStateException
    {
        // TODO Auto-generated method stub

    }

    public void setProperty( String name, Object value )
        throws IllegalArgumentException, IllegalStateException
    {
        // TODO Auto-generated method stub

    }

    public void startDocument( String encoding, Boolean standalone )
        throws IOException, IllegalArgumentException, IllegalStateException
    {
        // TODO Auto-generated method stub

    }

    public XmlSerializer startTag( String namespace, String name )
        throws IOException, IllegalArgumentException, IllegalStateException
    {
        output.write( name );

        return this;
    }

    public XmlSerializer text( String text )
        throws IOException, IllegalArgumentException, IllegalStateException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public XmlSerializer text( char[] buf, int start, int len )
        throws IOException, IllegalArgumentException, IllegalStateException
    {
        // TODO Auto-generated method stub
        return null;
    }
}
