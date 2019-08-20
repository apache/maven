package org.apache.maven.xml.filter;

import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * XML Filter to transform pom.xml to consumer pom.
 * This often means stripping of build-specific information.
 * When extra information is required during filtering it is probably a member of the BuildPomXMLFilter
 * 
 * This filter is used at 2 locations:
 * - {@link org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory} when publishing pom files.
 * - TODO ???Class when a reactor module is used as dependency. This ensures consistency of dependency handling
 * 
 * @author Robert Scholte
 * @since 3.7.0
 */
public class ConsumerPomXMLFilter extends XMLFilterImpl
{
    ConsumerPomXMLFilter( XMLReader filter )
    {
        super( filter );
    }
    
    /**
     * Don't allow overwriting parent
     */
    @Override
    public final void setParent( XMLReader parent )
    {
        if ( getParent() == null )
        {
            super.setParent( parent );
        }
    }
}
