package org.apache.maven.artifact.handler;

import java.io.File;
import java.util.List;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

public class ArtifactHandlerTest
    extends PlexusTestCase
{
    public void testAptConsistency()
        throws Exception
    {
        File apt = getTestFile( "src/site/apt/artifact-handlers.apt" );

        @SuppressWarnings( "unchecked" )
        List<String> lines = FileUtils.loadFile( apt );

        for ( String line : lines )
        {
            if ( line.startsWith( "||" ) )
            {
                String[] cols = line.split( "\\|\\|" );
                String[] expected =
                    new String[] { "", "type", "extension", "packaging", "classifier", "language", "added to classpath",
                        "includesDependencies", "" };

                int i = 0;
                for ( String col : cols )
                {
                    assertEquals( "Wrong column header", expected[i++], col.trim() );
                }
            }
            else if ( line.startsWith( "|" ) )
            {
                String[] cols = line.split( "\\|" );

                String type = trimApt( cols[1] );
                String extension = trimApt( cols[2], type );
                String packaging = trimApt( cols[3], type );
                String classifier = trimApt( cols[4] );
                String language = trimApt( cols[5] );
                String addedToClasspath = trimApt( cols[6] );
                String includesDependencies = trimApt( cols[7] );

                ArtifactHandler handler = lookup( ArtifactHandler.class, type );
                assertEquals( type + " extension", handler.getExtension(), extension );
                assertEquals( type + " packaging", handler.getPackaging(), packaging );
                assertEquals( type + " classifier", handler.getClassifier(), classifier );
                assertEquals( type + " language", handler.getLanguage(), language );
                assertEquals( type + " addedToClasspath", handler.isAddedToClasspath() ? "true" : null, addedToClasspath );
                assertEquals( type + " includesDependencies", handler.isIncludesDependencies() ? "true" : null, includesDependencies );
            }
        }
    }

    private String trimApt( String content, String type )
    {
        String value = trimApt( content );
        return "= type".equals( value ) ? type : value;
    }

    private String trimApt( String content )
    {
        content = content.replace( '<', ' ' ).replace( '>', ' ' ).trim();

        return ( content.length() == 0 ) ? null : content;
    }
}
