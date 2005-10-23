package org.apache.maven.plugin.assembly;

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

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileWriter;
import java.util.Map;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * Components XML file filter.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ComponentsXmlArchiverFileFilter
{
    private Map components;

    public static final String COMPONENTS_XML_PATH = "META-INF/plexus/components.xml";

    public void addComponentsXml( File componentsXml )
        throws IOException, XmlPullParserException
    {
        FileReader fileReader = null;
        Xpp3Dom newDom;
        try
        {
            fileReader = new FileReader( componentsXml );
            newDom = Xpp3DomBuilder.build( fileReader );
        }
        finally
        {
            IOUtil.close( fileReader );
        }

        if ( newDom != null )
        {
            newDom = newDom.getChild( "components" );
        }
        if ( newDom != null )
        {
            Xpp3Dom[] children = newDom.getChildren();
            for ( int i = 0; i < children.length; i++ )
            {
                Xpp3Dom component = children[i];

                if ( components == null )
                {
                    components = new LinkedHashMap();
                }

                String role = component.getChild( "role" ).getValue();
                Xpp3Dom child = component.getChild( "role-hint" );
                String roleHint = child != null ? child.getValue() : "";
                components.put( role + roleHint, component );
            }
        }
    }

    public void addToArchive( Archiver archiver )
        throws IOException, ArchiverException
    {
        if ( components != null )
        {
            File f = File.createTempFile( "maven-assembly-plugin", "tmp" );
            f.deleteOnExit();

            FileWriter fileWriter = new FileWriter( f );
            try
            {
                Xpp3Dom dom = new Xpp3Dom( "component-set" );
                Xpp3Dom componentDom = new Xpp3Dom( "components" );
                dom.addChild( componentDom );

                for ( Iterator i = components.values().iterator(); i.hasNext(); )
                {
                    Xpp3Dom component = (Xpp3Dom) i.next();
                    componentDom.addChild( component );
                }

                Xpp3DomWriter.write( fileWriter, dom );
            }
            finally
            {
                IOUtil.close( fileWriter );
            }
            archiver.addFile( f, COMPONENTS_XML_PATH );
        }
    }
}
