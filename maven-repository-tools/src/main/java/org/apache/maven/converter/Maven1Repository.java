package org.apache.maven.converter;

/*
 * LICENSE
 */

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.model.Model;

import org.codehaus.plexus.util.FileUtils;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class Maven1Repository
    extends AbstractMavenRepository
{
    public Iterator getArtifactsByType( String type )
        throws Exception
    {
        List files = FileUtils.getFiles( getRepository(), "*/" + type + "s/*." + type, "" );

        Collections.sort( files );

        return files.iterator();
    }

    public String getPomForArtifact( String artifactPath )
        throws Exception
    {
        int i = artifactPath.indexOf( '/' );

        String groupId = artifactPath.substring( 0, i );

        i = artifactPath.indexOf( '/', groupId.length() + 1 );

        String artifactId = artifactPath.substring( i + 1 );

        artifactId = artifactId.substring( 0, artifactId.length() - 4 );

        System.out.println( "groupId: " + groupId );

        System.out.println( "artifactId: " + artifactId );

        String pomPath = groupId + "/poms/" + artifactId + ".pom";

        File pom = new File( getRepository(), pomPath );

        if ( !pom.exists() )
        {
            return null;
        }

        return pomPath;
    }

    public void installArtifact( File artifact, Model model )
        throws Exception
    {
        throw new Exception( "Not implemented." );
    }
}
