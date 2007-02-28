package org.apache.maven.embedder;

import java.io.File;
import java.util.Properties;

/** @author Jason van Zyl */
public class EmbedderUsingScmPluginTest
    extends AbstractEmbedderExecutionTestCase
{
    protected String getId()
    {
        return "scm-plugin-from-embedder";
    }

    public void testRunningScmPlugin()
        throws Exception
    {
        Properties p = new Properties();

        File outputDirectory = new File( getBasedir(), "target/scm.diff" );

        p.setProperty( "outputDirectory", outputDirectory.getCanonicalPath() );

        p.setProperty( "connectionUrl", "scm:svn:http://svn.apache.org/repos/asf/maven/components/trunk/maven-embedder" );

        File basedir = runWithProject( "scm:diff", p );
    }
}
