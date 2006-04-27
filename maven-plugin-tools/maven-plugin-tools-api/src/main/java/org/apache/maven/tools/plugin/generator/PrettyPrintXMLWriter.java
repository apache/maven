package org.apache.maven.tools.plugin.generator;

import java.io.PrintWriter;
import java.io.Writer;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Copied from plexus-utils 1.3-SNAPSHOT as we can't upgrade it yet.
 * This class can be removed when a newer version of plexus-utils is included with Maven
 * 
 * @see org.codehaus.plexus.util.xml.PrettyPrintXMLWriter
 */
public class PrettyPrintXMLWriter
    extends org.codehaus.plexus.util.xml.PrettyPrintXMLWriter
{

    private static final String LS = System.getProperty("line.separator");

    private PrintWriter writer;

    private String lineIndenter;

    private int depth;

    public PrettyPrintXMLWriter( PrintWriter writer, String lineIndenter )
    {
        this( writer, lineIndenter, null, null );
    }

    public PrettyPrintXMLWriter( Writer writer, String lineIndenter )
    {
        this( new PrintWriter( writer ), lineIndenter );
    }

    public PrettyPrintXMLWriter( PrintWriter writer )
    {
        this( writer, null, null );
    }

    public PrettyPrintXMLWriter( Writer writer )
    {
        this( new PrintWriter( writer ) );
    }

    public PrettyPrintXMLWriter( PrintWriter writer, String lineIndenter, String encoding, String doctype )
    {
        super( writer, lineIndenter, encoding, doctype );

        setWriter( writer );

        setLineIndenter( lineIndenter );
    }

    public PrettyPrintXMLWriter( Writer writer, String lineIndenter, String encoding, String doctype )
    {
        this( new PrintWriter( writer ), lineIndenter, encoding, doctype );
    }

    public PrettyPrintXMLWriter( PrintWriter writer, String encoding, String doctype )
    {
        this( writer, "  ", encoding, doctype );
    }

    public PrettyPrintXMLWriter( Writer writer, String encoding, String doctype )
    {
        this( new PrintWriter( writer ), encoding, doctype );
    }

    /**
     * Write a string to the underlying writer
     * @param str
     */
    private void write( String str )
    {
        getWriter().write( str );
    }

    /**
     * Get the string used as line indenter
     * @return the line indenter
     */
    protected String getLineIndenter(){
        return lineIndenter;
    }

    /**
     * Set the string used as line indenter 
     * @param lineIndenter
     */
    protected void setLineIndenter( String lineIndenter ){
        this.lineIndenter = lineIndenter;
    }

    /**
     * Write the end of line character (using system line separator)
     * and start new line with indentation 
     */
    protected void endOfLine()
    {
        write( LS );

        for ( int i = 0; i < getDepth(); i++ )
        {
            write( getLineIndenter() );
        }
    }

    /**
     * Set the underlying writer
     * @param writer
     */
    protected void setWriter( PrintWriter writer )
    {
        this.writer = writer;
    }

    /**
     * Get the underlying writer
     * @return the underlying writer
     */
    protected PrintWriter getWriter()
    {
        return writer;
    }

    /**
     * Set the current depth in the xml indentation
     * @param depth
     */
    protected void setDepth( int depth )
    {
        this.depth = depth;
    }

    /**
     * Get the current depth in the xml indentation
     * @return
     */
    protected int getDepth()
    {
        return depth;
    }

    public void startElement( String name )
    {
        super.startElement( name );

        setDepth( getDepth() + 1 );
    }

    public void endElement()
    {
        super.endElement();

        setDepth( getDepth() - 1 );
    }

}
