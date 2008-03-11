package org.apache.maven.project.workspace;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.build.model.ModelAndFile;
import org.apache.maven.workspace.DefaultMavenWorkspaceStore;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;

import junit.framework.TestCase;

public class DefaultProjectWorkspaceTest
    extends TestCase
{

    public void testStoreAndRetrieveModelAndFile()
    {
        ModelAndFile maf = newModelAndFile( "group", "artifact", "1" );

        DefaultProjectWorkspace ws = newWorkspace();
        ws.storeModelAndFile( maf );

        ModelAndFile r1 = ws.getModelAndFile( maf.getFile() );

        assertSame( maf, r1 );

        ModelAndFile r2 = ws.getModelAndFile( maf.getModel().getGroupId(), maf.getModel().getArtifactId(), maf.getModel().getVersion() );

        assertSame( maf, r2 );
    }

    private DefaultProjectWorkspace newWorkspace()
    {
        DefaultProjectWorkspace ws = new DefaultProjectWorkspace( new DefaultMavenWorkspaceStore(), new ConsoleLogger( Logger.LEVEL_INFO, "test" ) );
        return ws;
    }

    public void testStoreAndRetrieveProjectByFile_CoordinateRetrievalReturnsNull()
    {
        MavenProject project = newProject( "group", "artifact", "1" );

        DefaultProjectWorkspace ws = newWorkspace();
        ws.storeProjectByFile( project );

        assertSame( project, ws.getProject( project.getFile() ) );
        assertNull( ws.getProject( project.getGroupId(), project.getArtifactId(), project.getVersion() ) );
    }

    public void testStoreAndRetrieveProjectByCoordinate_FileRetrievalReturnsNull()
    {
        MavenProject project = newProject( "group", "artifact", "1" );

        DefaultProjectWorkspace ws = newWorkspace();
        ws.storeProjectByCoordinate( project );

        assertNull( ws.getProject( project.getFile() ) );
        assertSame( project, ws.getProject( project.getGroupId(), project.getArtifactId(), project.getVersion() ) );
    }

    private MavenProject newProject( String gid,
                                     String aid,
                                     String ver )
    {
        File f = new File( "test-project" );
        Model model = new Model();
        model.setGroupId( gid );
        model.setArtifactId( aid );
        model.setVersion( ver );

        MavenProject project = new MavenProject( model );
        project.setFile( f );
        return project;
    }

    private ModelAndFile newModelAndFile( String gid,
                                          String aid,
                                          String ver )
    {
        File f = new File( "test-modelAndFile" );
        Model model = new Model();
        model.setGroupId( gid );
        model.setArtifactId( aid );
        model.setVersion( ver );

        ModelAndFile maf = new ModelAndFile( model, f, false );
        return maf;
    }

}
