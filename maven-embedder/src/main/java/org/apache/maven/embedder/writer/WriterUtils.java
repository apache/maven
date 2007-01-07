/* ==========================================================================
 * Copyright 2006 Mevenide Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * =========================================================================
 */

package org.apache.maven.embedder.writer;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.jdom.MavenJDOMWriter;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.codehaus.plexus.util.IOUtil;

import java.io.IOException;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.nio.channels.FileLock;

//TODO: turn this into a component

/** @author mkleint@codehaus.org */
public class WriterUtils
{
    public static void write( Writer w,
                              Model newModel )
        throws IOException
    {
        write( w, newModel, false );
    }

    public static void write( Writer w,
                              Model newModel,
                              boolean namespaceDeclaration )
        throws IOException
    {
        Element root = new Element( "project" );

        if ( namespaceDeclaration )
        {
            String modelVersion = newModel.getModelVersion();

            Namespace pomNamespace = Namespace.getNamespace( "", "http://maven.apache.org/POM/" + modelVersion );

            root.setNamespace( pomNamespace );

            Namespace xsiNamespace = Namespace.getNamespace( "xsi", "http://www.w3.org/2001/XMLSchema-instance" );

            root.addNamespaceDeclaration( xsiNamespace );

            if ( root.getAttribute( "schemaLocation", xsiNamespace ) == null )
            {
                root.setAttribute( "schemaLocation", "http://maven.apache.org/POM/" + modelVersion +
                    " http://maven.apache.org/maven-v" + modelVersion.replace( '.', '_' ) + ".xsd", xsiNamespace );
            }
        }

        Document doc = new Document( root );

        MavenJDOMWriter writer = new MavenJDOMWriter();

        String encoding = newModel.getModelEncoding() != null ? newModel.getModelEncoding() : "UTF-8";

        Format format = Format.getPrettyFormat().setEncoding( encoding );

        writer.write( newModel, doc, w, format );
    }

    /*
    public static void writePomModel( FileObject pom,
                                      Model newModel )
        throws IOException
    {
        InputStream inStr = null;

        FileLock lock = null;

        OutputStreamWriter outStr = null;

        try
        {
            inStr = pom.getInputStream();

            SAXBuilder builder = new SAXBuilder();

            Document doc = builder.build( inStr );

            inStr.close();

            inStr = null;

            lock = pom.lock();

            MavenJDOMWriter writer = new MavenJDOMWriter();

            String encoding = newModel.getModelEncoding() != null ? newModel.getModelEncoding() : "UTF-8";

            outStr = new OutputStreamWriter( pom.getOutputStream( lock ), encoding );

            Format form = Format.getRawFormat().setEncoding( encoding );

            writer.write( newModel, doc, outStr, form );

            outStr.close();

            outStr = null;
        }
        catch ( JDOMException exc )
        {
            exc.printStackTrace();
            throw (IOException) new IOException( "Cannot parse the POM by JDOM." ).initCause( exc );
        }
        finally
        {
            IOUtil.close( inStr );

            IOUtil.close( outStr );

            if ( lock != null )
            {
                lock.releaseLock();
            }

        }
    }
    */
}
