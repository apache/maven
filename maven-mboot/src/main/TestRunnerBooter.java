import org.apache.maven.test.TestRunner;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TestRunnerBooter
{
    public static void main(String[] args) throws Exception
    {
        TestRunnerBooter booter = new TestRunnerBooter();
        booter.execute(args);
    }
    
    public void execute(String[] args) throws Exception
    {
        File basedir = new File(args[0]);
        System.setProperty( "basedir", basedir.getPath() );

        String mavenRepoLocal = args[1];

        IsolatedClassLoader classLoader = new IsolatedClassLoader();

        Thread.currentThread().setContextClassLoader( classLoader );

        classLoader.addURL( new File ( mavenRepoLocal, "junit/jars/junit-3.8.1.jar" ).toURL() );
        classLoader.addURL( new File ( mavenRepoLocal, "maven/jars/surefire-runner-1.0.jar" ).toURL() );
        classLoader.addURL( new File( basedir, "target/classes/" ).toURL() );
        classLoader.addURL( new File( basedir, "target/test-classes/" ).toURL() );
        
        File dependenciesFile = new File(args[2]);
        
        processDependencies( dependencies, classLoader );

        File includesFile = new File(args[3]);

        File excludesFile = new File(args[4]);
        
        List includes = new ArrayList();

        List excludes = new ArrayList();
        
        String[] tests = collectTests( basedir,
                                       includes,
                                       excludes );

        Class testRunnerClass = classLoader.loadClass( "org.apache.maven.test.TestRunner" );

        Object testRunner = testRunnerClass.newInstance();

        Method m = testRunnerClass.getMethod( "runTestClasses", new Class[] {
            ClassLoader.class,
            String[].class
        } );

        m.invoke( testRunner, new Object[]{
            classLoader,
            tests
        } );
    }
    
    private void processDependencies(List dependencies, IsolatedClassLoader classLoader)
    {
        for (Iterator i=dependencies.iterator(); i.hasNext(); )
        {
            String dep = (String)i.next();
            classLoader.addURL( new File( dep ).toURL() );
        }
    }
    
    public String[] collectTests( File basedir, List includes, List excludes )
        throws Exception
    {
        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir( new File( basedir, "target/test-classes" ) );

        String[] incs = new String[includes.size()];

        for ( int i = 0; i < incs.length; i++ )
        {
            incs[i] = StringUtils.replace( (String) includes.get( i ), "java", "class" );
        }

        scanner.setIncludes( incs );

        String[] excls = new String[excludes.size() + 1];

        for ( int i = 0; i < excls.length - 1; i++ )
        {
            excls[i] = StringUtils.replace( (String) excludes.get( i ), "java", "class" );
        }

        // Exclude inner classes

        excls[excludes.size()] = "**/*$*";

        scanner.setExcludes( excls );

        scanner.scan();

        return scanner.getIncludedFiles();
    }
}