package org.apache.maven.it;

import org.apache.maven.it.cli.CommandLineUtils;
import org.apache.maven.it.cli.Commandline;
import org.apache.maven.it.cli.StreamConsumer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class Verifier
{
    private static final String LOG_FILENAME = "log.txt";

    private static String localRepo;

    private final String basedir;

    private final ByteArrayOutputStream outStream = new ByteArrayOutputStream();

    private final ByteArrayOutputStream errStream = new ByteArrayOutputStream();

    private final PrintStream originalOut;

    private final PrintStream originalErr;

    // TODO: needs to be configurable
    private static String localRepoLayout = "default";

    public Verifier( String basedir )
    {
        this.basedir = basedir;

        originalOut = System.out;

        System.setOut( new PrintStream( outStream ) );

        originalErr = System.err;

        System.setErr( new PrintStream( errStream ) );
    }

    public void resetStreams()
    {
        System.setOut( originalOut );

        System.setErr( originalErr );
    }

    public void displayStreamBuffers()
    {
        String out = outStream.toString();

        if ( out != null && out.trim().length() > 0 )
        {
            System.out.println( "----- Standard Out -----" );

            System.out.println( out );
        }

        String err = errStream.toString();

        if ( err != null && err.trim().length() > 0 )
        {
            System.err.println( "----- Standard Error -----" );

            System.err.println( err );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void verify( boolean chokeOnErrorOutput )
        throws VerificationException
    {
        List lines = loadFile( basedir, "expected-results.txt" );

        for ( Iterator i = lines.iterator(); i.hasNext(); )
        {
            String line = (String) i.next();

            verifyExpectedResult( line );
        }

        if ( chokeOnErrorOutput )
        {
            lines = loadFile( basedir, LOG_FILENAME );

            for ( Iterator i = lines.iterator(); i.hasNext(); )
            {
                String line = (String) i.next();

                if ( line.indexOf( "[ERROR]" ) >= 0 )
                {
                    throw new VerificationException( "Error in execution." );
                }
            }
        }
    }

    private Properties loadProperties( String filename )
        throws VerificationException
    {
        Properties properties = new Properties();

        FileInputStream fis;
        try
        {
            File propertiesFile = new File( basedir, filename );
            if ( propertiesFile.exists() )
            {
                fis = new FileInputStream( propertiesFile );
                properties.load( fis );
            }
        }
        catch ( IOException e )
        {
            throw new VerificationException( "Error reading properties file", e );
        }

        return properties;
    }

    private static List loadFile( String basedir, String filename )
        throws VerificationException
    {
        return loadFile( new File( basedir, filename ) );
    }

    private static List loadFile( File file )
        throws VerificationException
    {
        List lines = new ArrayList();

        if ( file.exists() )
        {
            try
            {
                BufferedReader reader = new BufferedReader( new FileReader( file ) );

                String line = "";

                while ( ( line = reader.readLine() ) != null )
                {
                    line = line.trim();

                    if ( line.startsWith( "#" ) || line.length() == 0 )
                    {
                        continue;
                    }

                    lines.addAll( replaceArtifacts( line ) );
                }

                reader.close();
            }
            catch ( Exception e )
            {
                throw new VerificationException( e );
            }
        }

        return lines;
    }

    private static List replaceArtifacts( String line )
    {
        String MARKER = "${artifact:";
        int index = line.indexOf( MARKER );
        if ( index >= 0 )
        {
            String newLine = line.substring( 0, index );
            index = line.indexOf( "}", index );
            if ( index < 0 )
            {
                throw new IllegalArgumentException( "line does not contain ending artifact marker: '" + line + "'" );
            }
            String artifact = line.substring( newLine.length() + MARKER.length(), index );

            newLine += convertArtifact( artifact );
            newLine += line.substring( index + 1 );

            index = newLine.lastIndexOf( "SNAPSHOT" );
            if ( index >= 0 )
            {
                List l = new ArrayList();
                l.add( newLine );
                l.add( newLine.substring( 0, index ) + "SNAPSHOT.version.txt" );
                return l;
            }
            else
            {
                return Collections.singletonList( newLine );
            }
        }
        else
        {
            return Collections.singletonList( line );
        }
    }

    private static String convertArtifact( String artifact )
    {
        StringTokenizer tok = new StringTokenizer( artifact, ":" );
        if ( tok.countTokens() != 4 )
        {
            throw new IllegalArgumentException( "Artifact must have 4 tokens: '" + artifact + "'" );
        }

        String[] a = new String[4];
        for ( int i = 0; i < 4; i++ )
        {
            a[i] = tok.nextToken();
        }

        String ext = a[3];
        if ( "maven-plugin".equals( a[3] ) )
        {
            ext = "jar";
        }

        String repositoryPath;
        if ( "legacy".equals( localRepoLayout ) )
        {
            repositoryPath = a[0] + "/" + a[3] + "s/" + a[1] + "-" + a[2] + "." + ext;
        }
        else if ( "default".equals( localRepoLayout ) )
        {
            repositoryPath = a[0].replace( '.', '/' );
//            if ( !a[3].equals( "pom" ) )
//            {
            repositoryPath = repositoryPath + "/" + a[1] + "/" + a[2];
//            }
            repositoryPath = repositoryPath + "/" + a[1] + "-" + a[2] + "." + ext;
        }
        else
        {
            throw new IllegalStateException( "Unknown layout: " + localRepoLayout );
        }

        return localRepo + "/" + repositoryPath;
    }

    public void executeHook( String filename )
        throws VerificationException
    {
        try
        {
            File f = new File( basedir, filename );

            if ( !f.exists() )
            {
                return;
            }

            List lines = loadFile( f );

            for ( Iterator i = lines.iterator(); i.hasNext(); )
            {
                String line = (String) i.next();

                executeCommand( line );
            }
        }
        catch ( VerificationException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new VerificationException( e );
        }
    }

    private static void executeCommand( String line )
        throws VerificationException
    {
        int index = line.indexOf( " " );

        String cmd;

        String args = null;

        if ( index >= 0 )
        {
            cmd = line.substring( 0, index );

            args = line.substring( index + 1 );
        }
        else
        {
            cmd = line;
        }

        if ( "rm".equals( cmd ) )
        {
            System.out.println( "Removing file: " + args );

            File f = new File( args );

            if ( f.exists() && !f.delete() )
            {
                throw new VerificationException( "Error removing file - delete failed" );
            }
        }
        else
        {
            throw new VerificationException( "unknown command: " + cmd );
        }
    }

    private static String retrieveLocalRepo()
    {
        String repo = System.getProperty( "maven.repo.local" );

        if ( repo == null )
        {
            UserModelReader userModelReader = new UserModelReader();

            try
            {
                String userHome = System.getProperty( "user.home" );

                File userXml = new File( userHome, ".m2/settings.xml" );

                if ( userXml.exists() )
                {
                    userModelReader.parse( userXml );

                    String localRepository = userModelReader.getLocalRepository();
                    if ( localRepository != null )
                    {
                        repo = new File( localRepository ).getAbsolutePath();
                    }
                }
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }

        if ( repo == null )
        {
            String userHome = System.getProperty( "user.home" );
            String m2LocalRepoPath = "/.m2/repository";

            File repoDir = new File( userHome, m2LocalRepoPath );
            if ( !repoDir.exists() )
            {
                repoDir.mkdirs();
            }

            repo = repoDir.getAbsolutePath();

            System.out.println( "Using default local repository: " + repoDir.getAbsolutePath() );
        }

        return repo;
    }

    private void verifyExpectedResult( String line )
        throws VerificationException
    {
        boolean wanted = true;
        if ( line.startsWith( "!" ) )
        {
            line = line.substring( 1 );
            wanted = false;
        }

        if ( line.indexOf( "!/" ) > 0 )
        {
            String urlString = "jar:file:" + basedir + "/" + line;

            InputStream is = null;
            try
            {
                URL url = new URL( urlString );

                is = url.openStream();

                if ( is == null )
                {
                    if ( wanted )
                    {
                        throw new VerificationException( "Expected JAR resource was not found: " + line );
                    }
                }
                else
                {
                    if ( !wanted )
                    {
                        throw new VerificationException( "Unwanted JAR resource was found: " + line );
                    }
                }
            }
            catch ( Exception e )
            {
                throw new VerificationException( "Error looking for JAR resource", e );
            }
            finally
            {
                if ( is != null )
                {
                    try
                    {
                        is.close();
                    }
                    catch ( IOException e )
                    {
                    }
                }
            }
        }
        else
        {
            File expectedFile = new File( line );

            if ( !expectedFile.isAbsolute() && !line.startsWith( "/" ) )
            {
                expectedFile = new File( basedir, line );
            }

            if ( !expectedFile.exists() )
            {
                if ( wanted )
                {
                    throw new VerificationException( "Expected file was not found: " + expectedFile.getPath() );
                }
            }
            else
            {
                if ( !wanted )
                {
                    throw new VerificationException( "Unwanted file was found: " + expectedFile.getPath() );
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void executeGoals( Properties properties, String filename )
        throws VerificationException
    {
        String mavenHome = System.getProperty( "maven.home" );

        List goals = loadFile( basedir, filename );

        List cliOptions = loadFile( basedir, "cli-options.txt" );

        if ( goals.size() == 0 )
        {
            throw new VerificationException( "No goals specified" );
        }

        List allGoals = new ArrayList();

        allGoals.add( "clean:clean" );

        allGoals.addAll( goals );

        int ret;

        try
        {
            Commandline cli = new Commandline();

            cli.setWorkingDirectory( basedir );

            String executable;
            if ( mavenHome != null )
            {
                executable = mavenHome + "/bin/m2";
            }
            else
            {
                executable = "m2";
            }

            cli.setExecutable( executable );

            for ( Iterator it = cliOptions.iterator(); it.hasNext(); )
            {
                String key = (String) it.next();
                cli.createArgument().setLine( key );
            }

            cli.createArgument().setValue( "-e" );

            cli.createArgument().setValue( "--no-plugin-registry" );

            cli.createArgument().setValue( "--check-plugin-latest" );

            cli.createArgument().setValue( "--batch-mode" );

            for ( Iterator i = properties.keySet().iterator(); i.hasNext(); )
            {
                String key = (String) i.next();
                cli.createArgument().setLine( "-D" + key + "=" + properties.getProperty( key ) );
            }

            // Note: Make sure that the repo is surrounded by quotes as it can possibly have
            // spaces in its path.
            cli.createArgument().setLine( "-Dmaven.repo.local=" + "\"" + localRepo + "\"" );

            for ( Iterator i = allGoals.iterator(); i.hasNext(); )
            {
                cli.createArgument().setValue( (String) i.next() );
            }

            Writer logWriter = new FileWriter( new File( basedir, LOG_FILENAME ) );

            StreamConsumer out = new WriterStreamConsumer( logWriter );

            StreamConsumer err = new WriterStreamConsumer( logWriter );

            System.out.println( "Command: " + Commandline.toString( cli.getCommandline() ) );

            ret = CommandLineUtils.executeCommandLine( cli, out, err );

            logWriter.close();
        }
        catch ( Exception e )
        {
            throw new VerificationException( e );
        }

        if ( ret > 0 )
        {
            System.err.println( "Exit code: " + ret );

            throw new VerificationException();
        }
    }

    private void displayLogFile()
    {
        System.out.println( "Log file contents:" );
        try
        {
            BufferedReader reader = new BufferedReader( new FileReader( new File( basedir, LOG_FILENAME ) ) );
            String line = reader.readLine();
            while ( line != null )
            {
                System.out.println( line );
                line = reader.readLine();
            }
            reader.close();
        }
        catch ( IOException e )
        {
            System.err.println( "Error: " + e );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public static void main( String args[] )
        throws VerificationException
    {
        String basedir = System.getProperty( "user.dir" );

        localRepo = retrieveLocalRepo();

        List tests = null;

        if ( args.length == 0 )
        {
            try
            {
                tests = loadFile( basedir, "integration-tests.txt" );
            }
            catch ( VerificationException e )
            {
                System.err.println( "Unable to load integration tests file" );

                System.err.println( e.getMessage() );

                System.exit( 2 );
            }
        }
        else
        {
            tests = new ArrayList( args.length );
            NumberFormat fmt = new DecimalFormat( "0000" );
            for ( int i = 0; i < args.length; i++ )
            {
                String test = args[i];
                if ( !test.startsWith( "it" ) )
                {
                    test = "it" + fmt.format( Integer.valueOf( test ) );
                }
                tests.add( test );
            }
        }

        if ( tests.size() == 0 )
        {
            System.out.println( "No tests to run" );
        }

        int exitCode = 0;

        for ( Iterator i = tests.iterator(); i.hasNext(); )
        {
            String test = (String) i.next();

            System.out.print( test + "... " );

            String dir = basedir + "/" + test;

            if ( !new File( dir, "pom.xml" ).exists() )
            {
                System.err.println( "Test " + test + " in " + dir + " does not exist" );

                System.exit( 2 );
            }

            Verifier verifier = new Verifier( dir );

            try
            {
                verifier.executeHook( "prebuild-hook.txt" );

                Properties properties = verifier.loadProperties( "system.properties" );

                Properties controlProperties = verifier.loadProperties( "verifier.properties" );

                boolean chokeOnErrorOutput = Boolean.valueOf(
                    controlProperties.getProperty( "failOnErrorOutput", "true" ) ).booleanValue();

                verifier.executeGoals( properties, "goals.txt" );

                verifier.executeHook( "postbuild-hook.txt" );

                System.out.println( "*** Verifying: fail when [ERROR] detected? " + chokeOnErrorOutput + " ***" );

                verifier.verify( chokeOnErrorOutput );

                verifier.resetStreams();

                System.out.println( "OK" );
            }
            catch ( VerificationException e )
            {
                verifier.resetStreams();

                System.out.println( "FAILED" );

                verifier.displayStreamBuffers();

                System.out.println( ">>>>>> Error Stacktrace:" );
                e.printStackTrace( System.out );
                System.out.println( "<<<<<< Error Stacktrace" );

                verifier.displayLogFile();

                exitCode = 1;
            }
        }

        System.exit( exitCode );
    }

    static class UserModelReader
        extends DefaultHandler
    {
        private SAXParserFactory saxFactory;

        private String localRepository;

        private StringBuffer currentBody = new StringBuffer();

        public boolean parse( File file )
        {
            try
            {
                saxFactory = SAXParserFactory.newInstance();

                SAXParser parser = saxFactory.newSAXParser();

                InputSource is = new InputSource( new FileInputStream( file ) );

                parser.parse( is, this );

                return true;
            }
            catch ( Exception e )
            {
                e.printStackTrace();

                return false;
            }
        }

        public void warning( SAXParseException spe )
        {
            printParseError( "Warning", spe );
        }

        public void error( SAXParseException spe )
        {
            printParseError( "Error", spe );
        }

        public void fatalError( SAXParseException spe )
        {
            printParseError( "Fatal Error", spe );
        }

        private final void printParseError( String type, SAXParseException spe )
        {
            System.err.println(
                type + " [line " + spe.getLineNumber() + ", row " + spe.getColumnNumber() + "]: " + spe.getMessage() );
        }

        public String getLocalRepository()
        {
            return localRepository;
        }

        public void characters( char[] ch, int start, int length )
            throws SAXException
        {
            currentBody.append( ch, start, length );
        }

        public void endElement( String uri, String localName, String rawName )
            throws SAXException
        {
            if ( "localRepository".equals( rawName ) )
            {
                if ( notEmpty( currentBody.toString() ) )
                {
                    localRepository = currentBody.toString().trim();
                }
                else
                {
                    throw new SAXException(
                        "Invalid mavenProfile entry. Missing one or more " + "fields: {localRepository}." );
                }
            }

            currentBody = new StringBuffer();
        }

        private boolean notEmpty( String test )
        {
            return test != null && test.trim().length() > 0;
        }

        public void reset()
        {
            this.currentBody = null;
            this.localRepository = null;
        }
    }

}

