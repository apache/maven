package org.apache.maven.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.maven.execution.MavenExecutionRequest;

import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

public class CLIRequestUtilsTest
    extends TestCase
{

    public void test_buildRequest_ParseCommandLineProperty()
        throws ParseException
    {
        String key = "key";
        String value = "value";

        CLIManager cliManager = new CLIManager();

        String[] args = {
            "-D" + key + "=" + value
        };

        CommandLine commandLine = cliManager.parse( args );

        assertTrue( commandLine.hasOption( CLIManager.SET_SYSTEM_PROPERTY ) );

        System.out.println( commandLine.getOptionValue( CLIManager.SET_SYSTEM_PROPERTY ) );
        System.out.println( commandLine.getArgList() );

        assertEquals( 1, commandLine.getOptionValues( CLIManager.SET_SYSTEM_PROPERTY ).length );

        MavenExecutionRequest request = CLIRequestUtils.buildRequest( commandLine, false, false, false );

        Properties execProperties = request.getProperties();

        assertEquals( value, execProperties.getProperty( key ) );

        List goals = request.getGoals();
        assertTrue( ( goals == null ) || goals.isEmpty() );
    }

    public void testGetExecutionProperties()
        throws Exception
    {
        System.setProperty( "test.property.1", "1.0" );
        System.setProperty( "test.property.2", "2.0" );
        Properties p = CLIRequestUtils.getExecutionProperties( new CLIManager().parse( new String[] {
            "-Dtest.property.2=2.1",
            "-Dtest.property.3=3.0" } ) );

        // assume that everybody has a PATH env var
        String envPath = p.getProperty( "env.PATH" );
        if ( envPath == null )
        {
            envPath = p.getProperty( "env.Path" );
        }
        assertNotNull( envPath );

        assertEquals( "1.0", p.getProperty( "test.property.1" ) );
        assertEquals( "3.0", p.getProperty( "test.property.3" ) );

        // sys props should override cmdline props
        assertEquals( "2.0", p.getProperty( "test.property.2" ) );
    }
}
