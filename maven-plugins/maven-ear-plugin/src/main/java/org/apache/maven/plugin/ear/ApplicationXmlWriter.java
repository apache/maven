package org.apache.maven.plugin.ear;

import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
 * An <tt>XmlWriter</tt> based implementation used to generate an
 * <tt>application.xml</tt> file
 *
 * @author Stephane Nicoll <stephane.nicoll@gmail.com>
 * @author $Author: sni $ (last edit)
 * @version $Id$
 */
public final class ApplicationXmlWriter
{
    public static final String DOCTYPE_1_3 = "application PUBLIC\n" +
        "\t\"-//Sun Microsystems, Inc.//DTD J2EE Application 1.3//EN\"\n" +
        "\t\"http://java.sun.com/dtd/application_1_3.dtd\"";

    private static final String APPLICATION_ELEMENT = "application";


    private final String version;

    private final String encoding;

    public ApplicationXmlWriter( String version, String encoding )
    {
        this.version = version;
        this.encoding = encoding;
    }

    public void write( File destinationFile, List earModules, String displayName, String description )
        throws EarPluginException
    {
        FileWriter w;
        try
        {
            w = new FileWriter( destinationFile );
        }
        catch ( IOException ex )
        {
            throw new EarPluginException( "Exception while opening file[" + destinationFile.getAbsolutePath() + "]",
                                          ex );
        }

        XMLWriter writer = null;
        if ( GenerateApplicationXmlMojo.VERSION_1_3.equals( version ) )
        {
            writer = initializeRootElementOneDotThree( w );
        }
        else if ( GenerateApplicationXmlMojo.VERSION_1_4.equals( version ) )
        {
            writer = initializeRootElementOneDotFour( w );
        }

        if ( displayName != null )
        {
            writer.startElement( "display-name" );
            writer.writeText( displayName );
            writer.endElement();
        }

        if ( description != null )
        {
            writer.startElement( "description" );
            writer.writeText( description );
            writer.endElement();
        }

        Iterator i = earModules.iterator();
        while ( i.hasNext() )
        {
            EarModule module = (EarModule) i.next();
            module.appendModule( writer, version );
        }
        writer.endElement();

        close( w );
    }

    private void close( Writer closeable )
    {
        if ( closeable == null )
        {
            return;
        }

        try
        {
            closeable.close();
        }
        catch ( Exception e )
        {
            // TODO: warn
        }
    }

    private XMLWriter initializeRootElementOneDotThree( FileWriter w )
    {
        XMLWriter writer = new PrettyPrintXMLWriter( w, encoding, DOCTYPE_1_3 );
        writer.startElement( APPLICATION_ELEMENT );
        return writer;
    }

    private XMLWriter initializeRootElementOneDotFour( FileWriter w )
    {
        XMLWriter writer = new PrettyPrintXMLWriter( w, encoding, null );
        writer.startElement( APPLICATION_ELEMENT );
        writer.addAttribute( "xmlns", "http://java.sun.com/xml/ns/j2ee" );
        writer.addAttribute( "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance" );
        writer.addAttribute( "xsi:schemaLocation",
                             "http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/application_1_4.xsd" );
        writer.addAttribute( "version", "1.4" );
        return writer;
    }
}
