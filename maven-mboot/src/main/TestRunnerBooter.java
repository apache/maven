import org.apache.maven.test.TestRunner;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

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
        
        List dependencies = new ArrayList();
        BufferedReader buf = new BufferedReader(new FileReader(dependenciesFile));
        String line;
        while ((line = buf.readLine()) != null)
        {
            dependencies.add(line);
        }
        buf.close();
        
        processDependencies( dependencies, classLoader );

        File includesFile = new File(args[3]);
        List includes = new ArrayList();
        buf = new BufferedReader(new FileReader(includesFile));
        line = buf.readLine();
        String includesStr = line.substring(line.indexOf("@")+1);
        StringTokenizer st = new StringTokenizer( includesStr, "," );
        while ( st.hasMoreTokens() )
        {
            includes.add( st.nextToken().trim() );
        }
        buf.close();

        File excludesFile = new File(args[4]);
        List excludes = new ArrayList();
        buf = new BufferedReader(new FileReader(excludesFile));
        line = buf.readLine();
        String excludesStr = line.substring(line.indexOf("@")+1);
        st = new StringTokenizer( excludesStr, "," );
        while ( st.hasMoreTokens() )
        {
            excludes.add( st.nextToken().trim() );
        }
        buf.close();
        
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
        throws Exception
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