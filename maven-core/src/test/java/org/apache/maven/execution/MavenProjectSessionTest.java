package org.apache.maven.execution;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.easymock.MockControl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MavenProjectSessionTest
    extends PlexusTestCase
{

    private Set mockControls = new HashSet();

    private void replay()
    {
        for ( Iterator it = mockControls.iterator(); it.hasNext(); )
        {
            MockControl ctl = (MockControl) it.next();
            ctl.replay();
        }
    }

    private void verify()
    {
        for ( Iterator it = mockControls.iterator(); it.hasNext(); )
        {
            MockControl ctl = (MockControl) it.next();
            ctl.verify();
        }
    }

    public void testAddExtensionRealmThenContainsExtensionRealm_ReturnTrue()
        throws PlexusContainerException
    {
        String projectId = "org.test:test-project1:1";

        MavenProjectSession session = new MavenProjectSession( projectId, getContainer() );

        ArtifactMock extMock = new ArtifactMock( "org.test.dep", "ext1", "2" );

        replay();

        try
        {
            session.createExtensionRealm( extMock.artifact );
        }
        catch ( DuplicateRealmException e )
        {
            e.printStackTrace();

            fail( "ClassRealm for extension should not exist yet." );
        }

        assertTrue( "Should return true for containsExtensionRealm after extension is added.", session.containsExtensionRealm( extMock.artifact ) );

        verify();
    }

    public void testConstructDisposeConstruct_OneExtension_NoDuplicateRealmException()
        throws PlexusContainerException
    {
        String projectId = "org.test:test-project1:1";

        MavenProjectSession session = new MavenProjectSession( projectId, getContainer() );

        ArtifactMock extMock = new ArtifactMock( "org.test.dep", "ext1", "2" );

        replay();

        try
        {
            session.createExtensionRealm( extMock.artifact );
        }
        catch ( DuplicateRealmException e )
        {
            e.printStackTrace();

            fail( "ClassRealm for extension should not exist yet." );
        }

        session.dispose();

        session = new MavenProjectSession( projectId, getContainer() );

        try
        {
            session.createExtensionRealm( extMock.artifact );
        }
        catch ( DuplicateRealmException e )
        {
            e.printStackTrace();

            fail( "Should have disposed ClassRealm for extension." );
        }

        verify();
    }

    public void testAddSameExtensionTwice_DuplicateRealmException()
        throws PlexusContainerException
    {
        String projectId = "org.test:test-project1:1";

        MavenProjectSession session = new MavenProjectSession( projectId, getContainer() );

        ArtifactMock extMock = new ArtifactMock( "org.test.dep", "ext1", "2" );

        replay();

        try
        {
            session.createExtensionRealm( extMock.artifact );
        }
        catch ( DuplicateRealmException e )
        {
            e.printStackTrace();

            fail( "ClassRealm for extension should not exist yet." );
        }

        try
        {
            session.createExtensionRealm( extMock.artifact );
            fail( "Should not allow same extension to be added twice." );
        }
        catch ( DuplicateRealmException e )
        {
        }

        verify();
    }

    private class ArtifactMock
    {
        private MockControl ctl;

        private Artifact artifact;

        public ArtifactMock( String groupId,
                             String artifactId,
                             String version )
        {
            ctl = MockControl.createControl( Artifact.class );
            artifact = (Artifact) ctl.getMock();

            artifact.getGroupId();
            ctl.setReturnValue( groupId, MockControl.ZERO_OR_MORE );

            artifact.getArtifactId();
            ctl.setReturnValue( artifactId, MockControl.ZERO_OR_MORE );

            artifact.getVersion();
            ctl.setReturnValue( version, MockControl.ZERO_OR_MORE );

            mockControls.add( ctl );
        }
    }

}
