package org.apache.maven.it;

import org.apache.maven.it.cli.CommandLineUtils;
import org.apache.maven.it.cli.Commandline;
import org.apache.maven.it.cli.StreamConsumer;
import org.xml.sax.Attributes;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

    public void verify() throws VerificationException
    {
        List lines = loadFile( basedir, "expected-results.txt" );

        for ( Iterator i = lines.iterator(); i.hasNext(); )
        {
            String line = (String) i.next();

            verifyExpectedResult( line );
        }

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

    private static List loadFile( String basedir, String filename ) throws VerificationException
    {
        return loadFile( new File( basedir, filename ) );
    }

    private static List loadFile( File file ) throws VerificationException
    {
        List lines = new ArrayList();

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

                line = replace( line, "${localRepository}", localRepo );

                lines.add( line );
            }

            reader.close();
        }
        catch ( Exception e )
        {
            throw new VerificationException( e );
        }
        return lines;
    }

    public void executeHook( String filename ) throws VerificationException
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

    private static void executeCommand( String line ) throws VerificationException
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

        if ( cmd.equals( "rm" ) )
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

                File userXml = new File( userHome, ".m2/user.xml" );

                if ( userXml.exists() )
                {
                    userModelReader.parse( userXml );

                    MavenProfile activeProfile = userModelReader.getActiveMavenProfile();

                    repo = new File( activeProfile.getLocalRepo() ).getAbsolutePath();
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

    private void verifyExpectedResult( String line ) throws VerificationException
    {
        if ( line.indexOf( "!/" ) > 0 )
        {
            String urlString = "jar:file:" + basedir + "/" + line;

            try
            {
                URL url = new URL( urlString );

                InputStream is = url.openStream();

                if ( is == null )
                {
                    throw new VerificationException( "Expected JAR resource was not found: " + line );
                }

                is.close();
            }
            catch ( Exception e )
            {
                throw new VerificationException( "Expected JAR resource was not found: " + line );
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
                throw new VerificationException( "Expected file was not found: " + expectedFile.getPath() );
            }
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public static String replaceOnce( String text, String repl, String with )
    {
        return replace( text, repl, with, 1 );
    }

    public static String replace( String text, String repl, String with )
    {
        return replace( text, repl, with, -1 );
    }

    public static String replace( String text, String repl, String with, int max )
    {
        if ( text == null || repl == null || with == null || repl.length() == 0 )
        {
            return text;
        }

        StringBuffer buf = new StringBuffer( text.length() );

        int start = 0, end = 0;

        while ( ( end = text.indexOf( repl, start ) ) != -1 )
        {
            buf.append( text.substring( start, end ) ).append( with );

            start = end + repl.length();

            if ( --max == 0 )
            {
                break;
            }
        }

        buf.append( text.substring( start ) );

        return buf.toString();
    }

    public void executeGoals( String filename ) throws VerificationException
    {
        String mavenHome = System.getProperty( "maven.home" );

        if ( mavenHome == null )
        {
            throw new VerificationException( "maven.home has not been specified" );
        }

        List goals = loadFile( basedir, filename );

        if ( goals.size() == 0 )
        {
            throw new VerificationException( "No goals specified" );
        }

        List allGoals = new ArrayList();

        allGoals.add( "clean:clean" );

        allGoals.addAll( goals );

        int ret = 0;

        try
        {
            Commandline cli = new Commandline();

            cli.setWorkingDirectory( basedir );

            cli.setExecutable( "m2" );

            for ( Iterator i = allGoals.iterator(); i.hasNext(); )
            {
                cli.createArgument().setValue( (String) i.next() );
            }

            Writer logWriter = new FileWriter( new File( basedir, LOG_FILENAME ) );

            StreamConsumer out = new WriterStreamConsumer( logWriter );

            StreamConsumer err = new WriterStreamConsumer( logWriter );

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
    {
        String basedir = System.getProperty( "user.dir" );

        localRepo = retrieveLocalRepo();

        List tests = null;

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

        if ( tests.size() == 0 )
        {
            System.out.println( "No tests to run" );
        }

        int exitCode = 0;

        for ( Iterator i = tests.iterator(); i.hasNext(); )
        {
            String test = (String) i.next();

            System.out.print( test + "... " );

            Verifier verifier = new Verifier( basedir + "/" + test );

            try
            {
                verifier.executeHook( "prebuild-hook.txt" );

                verifier.executeGoals( "goals.txt" );

                verifier.executeHook( "postbuild-hook.txt" );

                verifier.verify();

                verifier.resetStreams();

                System.out.println( "OK" );
            }
            catch ( VerificationException e )
            {
                verifier.resetStreams();

                System.out.println( "FAILED" );

                verifier.displayStreamBuffers();

                e.printStackTrace();

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

        private Map mavenProfiles = new TreeMap();

        private MavenProfile currentProfile = null;

        private StringBuffer currentBody = new StringBuffer();

        private String activeProfileId = null;

        private MavenProfile activeMavenProfile = null;

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
            System.err.println( type + " [line " + spe.getLineNumber() + ", row " + spe.getColumnNumber() + "]: "
                + spe.getMessage() );
        }

        public MavenProfile getActiveMavenProfile()
        {
            return activeMavenProfile;
        }

        public void characters( char[] ch, int start, int length ) throws SAXException
        {
            currentBody.append( ch, start, length );
        }

        public void endElement( String uri, String localName, String rawName ) throws SAXException
        {
            if ( "mavenProfile".equals( rawName ) )
            {
                if ( notEmpty( currentProfile.getId() ) && notEmpty( currentProfile.getLocalRepo() ) )
                {
                    mavenProfiles.put( currentProfile.getId(), currentProfile );
                    currentProfile = null;
                }
                else
                {
                    throw new SAXException( "Invalid mavenProfile entry. Missing one or more "
                        + "fields: {id,localRepository}." );
                }
            }
            else if ( currentProfile != null )
            {
                if ( "id".equals( rawName ) )
                {
                    currentProfile.setId( currentBody.toString().trim() );
                }
                else if ( "localRepository".equals( rawName ) )
                {
                    currentProfile.setLocalRepo( currentBody.toString().trim() );
                }
                else
                {
                    throw new SAXException( "Illegal element inside mavenProfile: \'" + rawName + "\'" );
                }
            }
            else if ( "userModel".equals( rawName ) )
            {
                this.activeMavenProfile = (MavenProfile) mavenProfiles.get( activeProfileId );
            }
            else if ( "mavenProfileId".equals( rawName ) )
            {
                this.activeProfileId = currentBody.toString().trim();
            }

            currentBody = new StringBuffer();
        }

        private boolean notEmpty( String test )
        {
            return test != null && test.trim().length() > 0;
        }

        public void startElement( String uri, String localName, String rawName, Attributes attributes )
            throws SAXException
        {
            if ( "mavenProfile".equals( rawName ) )
            {
                currentProfile = new MavenProfile();
            }
        }

        public void reset()
        {
            this.currentBody = null;
            this.activeMavenProfile = null;
            this.activeProfileId = null;
            this.currentProfile = null;
            this.mavenProfiles.clear();
        }
    }

    public static class MavenProfile
    {

        private String localRepository;

        private String id;

        public void setLocalRepo( String localRepo )
        {
            this.localRepository = localRepo;
        }

        public String getLocalRepo()
        {
            return localRepository;
        }

        public void setId( String id )
        {
            this.id = id;
        }

        public String getId()
        {
            return id;
        }

    }

}

