import java.io.File;
import java.util.List;

public class SurefirePlugin
{
    public boolean execute( String mavenRepoLocal,
                            String basedir,
                            String classesDirectory,
                            String testClassesDirectory,
                            List includes,
                            List excludes,
                            String[] classpathElements )
        throws Exception
    {
        System.setProperty( "basedir", basedir );

        SurefireBooter surefireBooter = new SurefireBooter();

        surefireBooter.addBattery( "org.codehaus.surefire.battery.DirectoryBattery", new Object[]{basedir, includes, excludes} );

        surefireBooter.addClassPathUrl( new File( mavenRepoLocal, "junit/jars/junit-3.8.1.jar" ).getPath() );

        surefireBooter.addClassPathUrl( new File( mavenRepoLocal, "surefire/jars/surefire-1.2-SNAPSHOT.jar" ).getPath() );

        surefireBooter.addClassPathUrl( new File( classesDirectory ).getPath() );

        surefireBooter.addClassPathUrl( new File( testClassesDirectory ).getPath() );

        for ( int i = 0; i < classpathElements.length; i++ )
        {
            surefireBooter.addClassPathUrl( classpathElements[i] );
        }

        surefireBooter.addReport( "org.codehaus.surefire.report.ConsoleReporter" );

        return surefireBooter.run();
    }
}
