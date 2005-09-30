import org.apache.maven.embedder.*;
import org.apache.maven.project.*;
import org.apache.maven.monitor.event.*;
import java.io.*;
import java.util.*;

public class Plugin
{
    public Plugin()
        throws Exception
    {
        MavenEmbedder maven = new MavenEmbedder();
        
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        maven.setClassLoader( classLoader );
        
        maven.start();
        
        System.out.println( "Happy happy joy joy!" );

        System.out.println( "Now build a project" );

        File targetDirectory = new File( System.getProperty( "user.dir" ), "target/embedder-test-project" );

        System.out.println( ">> " + targetDirectory );
        
        File pomFile = new File( targetDirectory, "pom.xml" );

        MavenProject pom = maven.readProjectWithDependencies( pomFile );

        EventDispatcher eventDispatcher = new DefaultEventDispatcher();

        maven.execute( pom, Collections.singletonList( "package" ), eventDispatcher, null, targetDirectory );
    }
    
    public static void main( String[] args )
        throws Exception
    {
        Plugin plugin = new Plugin();
    }
}
