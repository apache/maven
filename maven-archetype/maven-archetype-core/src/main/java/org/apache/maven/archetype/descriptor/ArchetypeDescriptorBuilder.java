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

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.Reader;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ArchetypeDescriptorBuilder
{
    public ArchetypeDescriptor build( Reader reader )
        throws IOException, XmlPullParserException
    {
        ArchetypeDescriptor descriptor = new ArchetypeDescriptor();

        Xpp3Dom dom = Xpp3DomBuilder.build( reader );

        descriptor.setId( dom.getChild( "id" ).getValue() );

        Xpp3Dom allowPartialDom = dom.getChild( "allowPartial" );

        if ( allowPartialDom != null )
        {
            String allowPartial = allowPartialDom.getValue();

            if ( "true".equals( allowPartial ) || "1".equals( allowPartial ) || "on".equals( allowPartial ) )
            {
                descriptor.setAllowPartial( true );
            }
        }

        // ----------------------------------------------------------------------
        // Main
        // ----------------------------------------------------------------------

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

        // ----------------------------------------------------------------------
        // Test
        // ----------------------------------------------------------------------

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

        // ----------------------------------------------------------------------
        // Site
        // ----------------------------------------------------------------------

        Xpp3Dom siteResources = dom.getChild( "siteResources" );

        if ( siteResources != null )
        {
            Xpp3Dom[] siteResourceList = siteResources.getChildren( "resource" );

            for ( int i = 0; i < siteResourceList.length; i++ )
            {
                descriptor.addSiteResource( siteResourceList[i].getValue() );
            }
        }

        return descriptor;
    }
}
