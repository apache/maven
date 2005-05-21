package org.apache.maven.plugin.ear;

import org.apache.maven.plugin.ear.module.EarModule;
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

    private final String version;

    public ApplicationXmlWriter( String version )
    {
        this.version = version;
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

        // @todo Add DTD or XSchema reference based on version attribute

        XMLWriter writer = new PrettyPrintXMLWriter( w );
        writer.startElement( "application" );

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
}
