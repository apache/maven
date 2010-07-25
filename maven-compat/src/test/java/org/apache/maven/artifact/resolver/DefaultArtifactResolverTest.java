package org.apache.maven.artifact.resolver;

import java.util.Collections;

import org.apache.maven.artifact.AbstractArtifactComponentTestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.DefaultArtifactResolver.DaemonThreadCreator;

public class DefaultArtifactResolverTest
    extends AbstractArtifactComponentTestCase
{
    private DefaultArtifactResolver artifactResolver;

    private Artifact projectArtifact;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        artifactResolver = (DefaultArtifactResolver) lookup( ArtifactResolver.class );

        projectArtifact = createLocalArtifact( "project", "3.0" );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        artifactFactory = null;
        projectArtifact = null;
        super.tearDown();
    }

    @Override
    protected String component()
    {
        return "resolver";
    }

    public void testMNG4738()
        throws Exception
    {
        Artifact g = createLocalArtifact( "g", "1.0" );
        createLocalArtifact( "h", "1.0" );
        artifactResolver.resolveTransitively( Collections.singleton( g ), projectArtifact, remoteRepositories(),
                                              localRepository(), null );

        // we want to see all top-level thread groups
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        while ( !( tg.getParent() != null ) )
        {
            tg = tg.getParent();
        }

        ThreadGroup[] tgList = new ThreadGroup[tg.activeGroupCount()];
        tg.enumerate( tgList );

        boolean seen = false;

        for ( int i = 0; i < tgList.length; i++ )
        {
            if ( !tgList[i].getName().equals( DaemonThreadCreator.THREADGROUP_NAME ) )
            {
                continue;
            }

            seen = true;

            tg = tgList[i];
            Thread[] ts = new Thread[tg.activeCount()];
            tg.enumerate( ts );

            for ( Thread active : ts )
            {
                String name = active.getName();
                boolean daemon = active.isDaemon();
                assertTrue( name + " is no daemon Thread.", daemon );
            }

        }

        assertTrue( "Could not find ThreadGroup: " + DaemonThreadCreator.THREADGROUP_NAME, seen );
    }

    public void testLookup()
        throws Exception
    {
        ArtifactResolver resolver = lookup( ArtifactResolver.class, "default" );
    }
}
