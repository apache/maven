import org.apache.maven.cli.ConsoleDownloadMonitor;
import org.apache.maven.embedder.*;
import org.apache.maven.project.*;
import org.apache.maven.monitor.event.*;
import java.io.*;
import java.util.*;
import org.codehaus.plexus.logging.*;
import org.codehaus.plexus.logging.console.*;

public class Plugin
{
    public Plugin()
        throws Exception
    {
        MavenEmbedder maven = new MavenEmbedder();
        
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        maven.setClassLoader( classLoader );

        maven.setLogger( new MavenEmbedderConsoleLogger() );                
        
        maven.start();
        
        System.out.println( "Happy happy joy joy!" );

        System.out.println( "Now build a project" );

        File targetDirectory = new File( System.getProperty( "user.dir" ), "target/embedder-test-project" );

        System.out.println( ">> " + targetDirectory );
        
        File pomFile = new File( targetDirectory, "pom.xml" );

        MavenProject pom = maven.readProjectWithDependencies( pomFile );

        EventMonitor eventMonitor = new DefaultEventMonitor( new PlexusLoggerAdapter( new MavenEmbedderConsoleLogger() ) );
        
        System.out.println( "<<<<<<<<<<<<<<<<<<<<<<<<<");        
        
        maven.execute( pom, Collections.singletonList( "package" ), eventMonitor, new ConsoleDownloadMonitor(), null, targetDirectory );
        
        System.out.println( "<<<<<<<<<<<<<<<<<<<<<<<<<");        
    }
    
    public static void main( String[] args )
        throws Exception
    {
        Plugin plugin = new Plugin();
    }
}
