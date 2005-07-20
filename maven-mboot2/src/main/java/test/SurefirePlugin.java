package test;

import java.io.File;
import java.util.Iterator;
import java.util.List;

public class SurefirePlugin
{
    public boolean execute( String basedir, List includes, List excludes, List classpathElements,
                            String reportsDirectory )
        throws Exception
    {
        System.setProperty( "basedir", basedir );

        SurefireBooter surefireBooter = new SurefireBooter();

        surefireBooter.addBattery( "org.codehaus.surefire.battery.DirectoryBattery",
                                   new Object[]{new File( basedir, "target/test-classes" ), includes, excludes} );

        for ( Iterator i = classpathElements.iterator(); i.hasNext(); )
        {
            surefireBooter.addClassPathUrl( (String) i.next() );
        }

        surefireBooter.setReportsDirectory( reportsDirectory );

        surefireBooter.addReport( "org.codehaus.surefire.report.ConsoleReporter" );

        surefireBooter.addReport( "org.codehaus.surefire.report.FileReporter" );

        return surefireBooter.run();
    }
}
