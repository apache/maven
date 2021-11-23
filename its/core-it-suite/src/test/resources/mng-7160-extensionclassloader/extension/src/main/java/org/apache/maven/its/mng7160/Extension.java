package org.apache.maven.its.mng7160;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.util.Base64;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.apache.maven.AbstractMavenLifecycleParticipant;

@Singleton
@Named
public class Extension extends AbstractMavenLifecycleParticipant
{

    @Inject
    public Extension()
    {
        ClassLoader ext = getClass().getClassLoader();

        testClass( "xpp3", Xpp3Dom.class.getName() );
        testClass( "base64", Base64.class.getName() );
    }

    private void testClass( String shortName, String className )
    {
        try
        {
            ClassLoader mvn = AbstractMavenLifecycleParticipant.class.getClassLoader();
            ClassLoader ext = getClass().getClassLoader();
            Class<?> clsMvn = mvn.loadClass( className );
            Class<?> clsExt = ext.loadClass( className );
            if ( clsMvn != clsExt )
            {
                System.out.println( shortName + " -> ext" );
            }
            else
            {
                System.out.println( shortName + " -> mvn" );
            }
        }
        catch ( Throwable t )
        {
            System.out.println( shortName + " -> " + t );
        }
    }

}