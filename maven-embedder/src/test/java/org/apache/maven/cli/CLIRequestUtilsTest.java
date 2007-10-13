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

}
