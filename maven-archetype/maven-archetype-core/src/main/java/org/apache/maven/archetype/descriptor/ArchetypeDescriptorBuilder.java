package org.apache.maven.archetype.descriptor;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import java.io.Reader;

import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ArchetypeDescriptorBuilder
{
    public ArchetypeDescriptor build( Reader reader )
        throws Exception
    {
        ArchetypeDescriptor descriptor = new ArchetypeDescriptor();

        Xpp3Dom dom = Xpp3DomBuilder.build( reader );

        descriptor.setId( dom.getChild( "id" ).getValue() );

        Xpp3Dom sources = dom.getChild( "sources" );

        if ( sources != null )
        {
            Xpp3Dom[] sourceList = sources.getChildren( "source" );

            for ( int i = 0; i < sourceList.length; i++ )
            {
                descriptor.addSource( sourceList[i].getValue() );
            }
        }

        Xpp3Dom resources = dom.getChild( "resources" );

        if ( resources != null )
        {
            Xpp3Dom[] resourceList = resources.getChildren( "resource" );

            for ( int i = 0; i < resourceList.length; i++ )
            {
                descriptor.addResource( resourceList[i].getValue() );
            }
        }

        Xpp3Dom testSources = dom.getChild( "testSources" );

        if ( testSources != null )
        {
            Xpp3Dom[] testSourceList = testSources.getChildren( "source" );

            for ( int i = 0; i < testSourceList.length; i++ )
            {
                descriptor.addTestSource( testSourceList[i].getValue() );
            }
        }

        Xpp3Dom testResources = dom.getChild( "testResources" );

        if ( testResources != null )
        {
            Xpp3Dom[] testResourceList = testResources.getChildren( "resource" );

            for ( int i = 0; i < testResourceList.length; i++ )
            {
                descriptor.addTestResource( testResourceList[i].getValue() );
            }
        }

        return descriptor;
    }
}
