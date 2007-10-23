package org.apache.maven.cli;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.BuildFailureException;
import org.apache.maven.ProjectBuildFailureException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.InvalidProjectModelException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.validation.ModelValidationResult;
import org.codehaus.plexus.util.dag.CycleDetectedException;

public class CLIReportingUtilsTest
    extends TestCase
{

    private TestEmbedderLogger logger = new TestEmbedderLogger();

    public void test_logResult_ShowLongMessageForMojoFailureException()
        throws CycleDetectedException, DuplicateProjectException
    {
        String longMessage = "This is a longer message.";

        MojoFailureException e = new MojoFailureException( "test-id", "Short Message", longMessage );

        MojoBinding binding = new MojoBinding();
        binding.setGroupId( "plugin.group" );
        binding.setArtifactId( "plugin-artifact" );
        binding.setVersion( "10" );

        BuildFailureException buildError = new ProjectBuildFailureException( "test:project:1", binding, e );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        MavenExecutionResult result = new DefaultMavenExecutionResult();

        request.setStartTime( new Date() );

        MavenProject project = createProject( "test", "project", "1" );

        ReactorManager reactorManager = new ReactorManager( Collections.singletonList( project ),
                                                            ReactorManager.FAIL_FAST );
        reactorManager.registerBuildFailure( project, e, "task", 0 );

        result.setReactorManager( reactorManager );
        result.addBuildFailureException( buildError );

        CLIReportingUtils.logResult( request, result, logger );
        assertPresent( longMessage, logger.getErrorMessages() );
    }

    public void test_logResult_ShowReasonableMessageForInvalidProject()
        throws CycleDetectedException, DuplicateProjectException
    {
        String validationMessage = "dependencies.dependency.version is required";

        ModelValidationResult results = new ModelValidationResult();
        results.addMessage( validationMessage );

        String projectId = "test:project";
        File projectPath = new File( "/path/to/somewhere" );
        String message = "message";

        InvalidProjectModelException e = new InvalidProjectModelException( projectId, message, projectPath, results );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        MavenExecutionResult result = new DefaultMavenExecutionResult();

        request.setStartTime( new Date() );

        MavenProject project = createProject( "test", "project", "1" );

        Dependency dep = new Dependency();
        dep.setGroupId( "test" );
        dep.setArtifactId( "dep" );

        project.getModel().addDependency( dep );

        ReactorManager reactorManager = new ReactorManager( Collections.singletonList( project ),
                                                            ReactorManager.FAIL_FAST );
        reactorManager.registerBuildFailure( project, e, "task", 0 );

        result.setReactorManager( reactorManager );
        result.addProjectBuildingException( e );

        CLIReportingUtils.logResult( request, result, logger );
        assertPresent( projectId, logger.getErrorMessages() );
        assertPresent( projectPath.getPath(), logger.getErrorMessages() );
        assertPresent( message, logger.getErrorMessages() );
        assertPresent( validationMessage, logger.getErrorMessages() );
    }

    private MavenProject createProject( String groupId,
                                        String artifactId,
                                        String version )
    {
        Model model = new Model();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );

        return new MavenProject( model );
    }

    private void assertPresent( String message,
                                List messages )
    {
        for ( Iterator it = messages.iterator(); it.hasNext(); )
        {
            String entry = (String) it.next();

            if ( entry.indexOf( message ) > -1 )
            {
                return;
            }
        }

        fail( "Message not found in output: \'" + message + "\'" );
        assertTrue( "Message: \'" + message + "\' is missing in output.",
                    messages.contains( message ) );
    }

}
