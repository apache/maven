package org.apache.maven.archetype.descriptor;

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
            Xpp3Dom[] resourceList = resources.getChildren( "source" );

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

        Xpp3Dom testResources = dom.getChild( "sources" );

        if ( testResources != null )
        {
            Xpp3Dom[] testResourceList = sources.getChildren( "source" );

            for ( int i = 0; i < testResourceList.length; i++ )
            {
                descriptor.addTestResource( testResourceList[i].getValue() );
            }
        }

        return descriptor;
    }
}
