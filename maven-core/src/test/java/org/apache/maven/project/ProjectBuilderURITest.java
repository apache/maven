package org.apache.maven.project;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import junit.framework.TestCase;

public class ProjectBuilderURITest
    extends TestCase
{

    /**
     * MNG-3272:
     * See {@link DefaultMavenProjectBuilder#readModel(String, URL, boolean)}
     * for where this fix is implemented.
     */
    public void testURL_to_URI_forSuperPom_WhenMavenHasSpaceInPath()
        throws URISyntaxException, MalformedURLException, UnsupportedEncodingException
    {
        String url = "jar:file:/c:/Program Files/maven2.1/bin/../lib/maven-project-2.1-SNAPSHOT.jar!/org/apache/maven/project/pom-4.0.0.xml";
        System.out.println( "Original URL String:\n" + url );

        URL urlInst = new URL( url );

        URI uUri = new URI( urlInst.toExternalForm().replaceAll( " ", "%20" ) );
        System.out.println( "URI result:\n" + uUri );
    }

}
