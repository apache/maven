package test;

import util.IsolatedClassLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class
    SurefireBooter
{
    private List batteries = new ArrayList();

    private List reports = new ArrayList();

    private List classpathUrls = new ArrayList();
    
    private String reportsDir;

    public SurefireBooter()
    {
    }
    
    public void setReportsDirectory( String reportsDirectory )
    {
        this.reportsDir = reportsDirectory;
    }

    public void addBattery( String battery, Object[] params )
    {
        batteries.add( new Object[]{ battery, params } );
    }

    public void addBattery( String battery )
    {
        batteries.add( new Object[]{ battery, null } );
    }

    public void addReport( String report )
    {
        reports.add( report );
    }

    public void addClassPathUrl( String path )
    {
        if ( !classpathUrls.contains( path ) )
        {
            classpathUrls.add( path );
        }
    }

    public boolean run()
        throws Exception
    {
        IsolatedClassLoader surefireClassLoader = new IsolatedClassLoader();

        for ( Iterator i = classpathUrls.iterator(); i.hasNext(); )
        {
            String url = (String) i.next();

            if ( url != null )
            {
                surefireClassLoader.addURL( new File( url ).toURL() );
            }
        }

        Class batteryExecutorClass = surefireClassLoader.loadClass( "org.codehaus.surefire.Surefire" );

        Object batteryExecutor = batteryExecutorClass.newInstance();

        Method run = batteryExecutorClass.getMethod( "run", new Class[] { List.class, List.class, ClassLoader.class, String.class } );

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader( surefireClassLoader );

        Boolean result = (Boolean) run.invoke( batteryExecutor, new Object[]{ reports, batteries, surefireClassLoader, reportsDir } );

        Thread.currentThread().setContextClassLoader( oldContextClassLoader );

        return result.booleanValue();
    }

    public void reset()
    {
        batteries.clear();

        reports.clear();

        classpathUrls.clear();
    }

    // ----------------------------------------------------------------------
    // Main
    // ----------------------------------------------------------------------

    public static void main( String[] args )
        throws Exception
    {
        String basedir = args[0];

        System.setProperty( "basedir", basedir );

        String mavenRepoLocal = args[1];

        File dependenciesFile = new File( args[2] );

        List dependencies = new ArrayList();

        BufferedReader buf = new BufferedReader( new FileReader( dependenciesFile ) );

        String line;

        while ( ( line = buf.readLine() ) != null )
        {
            dependencies.add( line );
        }

        buf.close();

        File includesFile = new File( args[3] );

        List includes = new ArrayList();

        buf = new BufferedReader( new FileReader( includesFile ) );

        line = buf.readLine();

        String includesStr = line.substring( line.indexOf( "@" ) + 1 );

        StringTokenizer st = new StringTokenizer( includesStr, "," );

        while ( st.hasMoreTokens() )
        {
            String inc = st.nextToken().trim();

            includes.add( inc );
        }

        buf.close();

        File excludesFile = new File( args[4] );

        List excludes = new ArrayList();

        buf = new BufferedReader( new FileReader( excludesFile ) );

        line = buf.readLine();

        String excludesStr = line.substring( line.indexOf( "@" ) + 1 );

        st = new StringTokenizer( excludesStr, "," );

        while ( st.hasMoreTokens() )
        {
            excludes.add( st.nextToken().trim() );
        }

        buf.close();

        SurefireBooter surefireBooter = new SurefireBooter();

        surefireBooter.addBattery( "org.codehaus.surefire.battery.DirectoryBattery", new Object[]{ basedir, includes, excludes } );

        surefireBooter.addClassPathUrl( new File( mavenRepoLocal, "junit/jars/junit-3.8.1.jar" ).getPath() );

        surefireBooter.addClassPathUrl( new File( mavenRepoLocal, "surefire/jars/surefire-1.2-SNAPSHOT.jar" ).getPath() );

        surefireBooter.addClassPathUrl( new File( basedir, "target/classes/" ).getPath() );

        surefireBooter.addClassPathUrl( new File( basedir, "target/test-classes/" ).getPath() );

        processDependencies( dependencies, surefireBooter );

        surefireBooter.addReport( "org.codehaus.surefire.report.ConsoleReport" );

        surefireBooter.run();
    }

    private static void processDependencies( List dependencies, SurefireBooter sureFire )
        throws Exception
    {
        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            String dep = (String) i.next();

            sureFire.addClassPathUrl( new File( dep ).getPath() );
        }
    }
}
