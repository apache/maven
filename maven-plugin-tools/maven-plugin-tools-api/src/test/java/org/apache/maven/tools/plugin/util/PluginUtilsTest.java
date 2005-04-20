package org.apache.maven.tools.plugin.util;

import junit.framework.TestCase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.CompactXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.StringWriter;

/**
 * @author jdcasey
 */
public class PluginUtilsTest
    extends TestCase
{

    public void testShouldTrimArtifactIdToFindPluginId()
    {
        Model model = new Model();
        model.setArtifactId( "maven-artifactId-plugin" );

        MavenProject project = new MavenProject( model );

        String pluginId = PluginDescriptor.getPluginIdFromArtifactId( project.getArtifactId() );

        assertEquals( "artifactId", pluginId );
    }

    public void testShouldWriteDependencies()
        throws Exception
    {
        Dependency dependency = new Dependency();
        dependency.setArtifactId( "testArtifactId" );
        dependency.setGroupId( "testGroupId" );
        dependency.setType( "pom" );
        dependency.setVersion( "0.0.0" );

        Model model = new Model();
        model.addDependency( dependency );

        MavenProject project = new MavenProject( model );

        StringWriter sWriter = new StringWriter();
        XMLWriter writer = new CompactXMLWriter( sWriter );

        PluginUtils.writeDependencies( writer, project );

        String output = sWriter.toString();

        String pattern = "<dependencies>" + "<dependency>" + "<groupId>testGroupId</groupId>" +
            "<artifactId>testArtifactId</artifactId>" + "<type>pom</type>" + "<version>0.0.0</version>" +
            "</dependency>" + "</dependencies>";

        assertEquals( pattern, output );
    }

    public void testShouldFindTwoScriptsWhenNoExcludesAreGiven()
    {
        String testScript = "test.txt";

        String basedir = TestUtils.dirname( testScript );

        String includes = "**/*.txt";

        String[] files = PluginUtils.findSources( basedir, includes );
        assertEquals( 2, files.length );
    }

    public void testShouldFindOneScriptsWhenAnExcludeIsGiven()
    {
        String testScript = "test.txt";

        String basedir = TestUtils.dirname( testScript );

        String includes = "**/*.txt";
        String excludes = "**/*Excludes.txt";

        String[] files = PluginUtils.findSources( basedir, includes, excludes );
        assertEquals( 1, files.length );
    }

}