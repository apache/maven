package org.apache.maven.tools.plugin.util;

import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.util.xml.CompactXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.StringWriter;
import java.util.Collections;

import junit.framework.TestCase;

/**
 * @author jdcasey
 */
public class PluginUtilsTest
    extends TestCase
{

    public void testShouldTrimArtifactIdToFindPluginId()
    {
        assertEquals( "artifactId", PluginDescriptor.getGoalPrefixFromArtifactId( "maven-artifactId-plugin" ) );
        assertEquals( "artifactId", PluginDescriptor.getGoalPrefixFromArtifactId( "maven-plugin-artifactId" ) );
        assertEquals( "artifactId", PluginDescriptor.getGoalPrefixFromArtifactId( "artifactId-maven-plugin" ) );
        assertEquals( "artifactId", PluginDescriptor.getGoalPrefixFromArtifactId( "artifactId" ) );
        assertEquals( "artifactId", PluginDescriptor.getGoalPrefixFromArtifactId( "artifactId-plugin" ) );
        assertEquals( "plugin", PluginDescriptor.getGoalPrefixFromArtifactId( "maven-plugin-plugin" ) );
    }

    public void testShouldWriteDependencies()
        throws Exception
    {
        ComponentDependency dependency = new ComponentDependency();
        dependency.setArtifactId( "testArtifactId" );
        dependency.setGroupId( "testGroupId" );
        dependency.setType( "pom" );
        dependency.setVersion( "0.0.0" );
        
        PluginDescriptor descriptor = new PluginDescriptor();
        descriptor.setDependencies( Collections.singletonList( dependency ) );

        StringWriter sWriter = new StringWriter();
        XMLWriter writer = new CompactXMLWriter( sWriter );

        PluginUtils.writeDependencies( writer, descriptor );

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