package org.apache.maven.cli;

import org.apache.maven.AggregatedBuildFailureException;
import org.apache.maven.BuildFailureException;
import org.apache.maven.InvalidTaskException;
import org.apache.maven.NoGoalsSpecifiedException;
import org.apache.maven.ProjectBuildFailureException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.TaskValidationResult;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.InvalidProjectModelException;
import org.apache.maven.project.InvalidProjectVersionException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.validation.ModelValidationResult;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import junit.framework.TestCase;

public class CLIReportingUtilsTest
    extends TestCase
{

    // =====================================================================
    // Still left to test for ProjectBuildingException:
    // =====================================================================
    //
    // ProjectBuildingException(String, String, File, XmlPullParserException)
    //
    // ProjectBuildingException(String, String, IOException)
    // ProjectBuildingException(String, String, File, IOException)
    //
    // ProjectBuildingException(String, String, ArtifactNotFoundException)
    // ProjectBuildingException(String, String, File, ArtifactNotFoundException)
    //
    // ProjectBuildingException(String, String, ArtifactResolutionException)
    // ProjectBuildingException(String, String, File, ArtifactResolutionException)
    //
    // ProjectBuildingException(String, String, File, ProfileActivationException)
    //
    // ProjectBuildingException(String, String, InvalidRepositoryException)
    // ProjectBuildingException(String, String, File, InvalidRepositoryException)

    public void test_handleProjectBuildingException_ShowReasonableMessageForInvalidProjectModel()
    {
        String validationMessage = "dependencies.dependency.version is required";

        ModelValidationResult results = new ModelValidationResult();
        results.addMessage( validationMessage );

        String projectId = "test:project";
        File projectPath = new File( "/path/to/somewhere" );
        String message = "message";

        InvalidProjectModelException e = new InvalidProjectModelException( projectId, message,
                                                                           projectPath, results );

        StringWriter writer = new StringWriter();
        CLIReportingUtils.buildErrorMessage( e, false, writer );

        toConsole( writer );

        String[] contentLines = writer.toString().split( "\r?\n" );
        assertPresent( projectId, contentLines );
        assertPresent( projectPath.getPath(), contentLines );
        assertPresent( message, contentLines );
        assertPresent( validationMessage, contentLines );
    }

    public void test_handleProjectBuildingException_ShowLocationInfoForInvalidDependencyVersionException()
        throws IOException
    {
        String version = "[1.0";

        InvalidVersionSpecificationException cause = null;
        try
        {
            VersionRange.createFromVersionSpec( version );
            fail( "Version should be invalid." );
        }
        catch ( InvalidVersionSpecificationException versionException )
        {
            cause = versionException;
        }

        Dependency dep = new Dependency();
        dep.setGroupId( "org.group" );
        dep.setArtifactId( "dep-artifact" );
        dep.setVersion( version );

        File pomFile = File.createTempFile( "CLIReportingUtils.test.", "" );
        pomFile.deleteOnExit();

        String projectId = "org.group.id:some-artifact:1";

        InvalidDependencyVersionException e = new InvalidDependencyVersionException( projectId,
                                                                                     dep, pomFile,
                                                                                     cause );

        StringWriter writer = new StringWriter();
        CLIReportingUtils.buildErrorMessage( e, false, writer );

        toConsole( writer );

        String[] contentLines = writer.toString().split( "\r?\n" );
        assertPresent( projectId, contentLines );
        assertPresent( "Group-Id: " + dep.getGroupId(), contentLines );
        assertPresent( "Artifact-Id: " + dep.getArtifactId(), contentLines );
        assertPresent( version, contentLines );
    }

    public void test_handleProjectBuildingException_ShowLocationInfoForInvalidProjectVersionException()
        throws IOException
    {
        String version = "[1.0";

        InvalidVersionSpecificationException cause = null;
        try
        {
            VersionRange.createFromVersionSpec( version );
            fail( "Version should be invalid." );
        }
        catch ( InvalidVersionSpecificationException versionException )
        {
            cause = versionException;
        }

        File pomFile = File.createTempFile( "CLIReportingUtils.test.", "" );
        pomFile.deleteOnExit();

        String projectId = "org.group.id:some-artifact:1";
        String extLocation = "extension: org.group:extension-artifact";

        InvalidProjectVersionException e = new InvalidProjectVersionException( projectId,
                                                                               extLocation,
                                                                               version, pomFile,
                                                                               cause );

        StringWriter writer = new StringWriter();
        CLIReportingUtils.buildErrorMessage( e, false, writer );

        toConsole( writer );

        String[] contentLines = writer.toString().split( "\r?\n" );
        assertPresent( projectId, contentLines );
        assertPresent( extLocation, contentLines );
        assertPresent( version, contentLines );
    }

    // =====================================================================
    // Still left to test for BuildFailureException:
    // =====================================================================
    // ProjectCycleException(List, String, CycleDetectedException)

    public void test_handleBuildFailureException_ShowAdviceMessageForNoGoalsSpecifiedException()
    {
        NoGoalsSpecifiedException e = new NoGoalsSpecifiedException( "No goals were specified." );

        StringWriter writer = new StringWriter();
        CLIReportingUtils.buildErrorMessage( e, false, writer );

        toConsole( writer );

        String[] contentLines = writer.toString().split( "\r?\n" );
        assertPresent( "http://maven.apache.org/users/", contentLines );
        assertPresent( "http://maven.apache.org/plugins/", contentLines );
    }

    public void test_handleBuildFailureException_ShowAdviceMessageForInvalidTaskException()
    {
        Plugin plugin = new Plugin();
        plugin.setGroupId( "org.apache.maven.plugins" );
        plugin.setArtifactId( "maven-something-plugin" );
        plugin.setVersion( "[1.0" );

        TaskValidationResult tvr = new TaskValidationResult(
                                                             "something-dumb",
                                                             "test message.",
                                                             new LifecycleLoaderException(
                                                                                           "No such lifecycle phase: something-dumb" ) );

        InvalidTaskException e = tvr.generateInvalidTaskException();

        StringWriter writer = new StringWriter();
        CLIReportingUtils.buildErrorMessage( e, false, writer );

        toConsole( writer );

        String[] contentLines = writer.toString().split( "\r?\n" );
        assertPresent( "test message", contentLines );
        assertPresent( "something-dumb", contentLines );
        assertPresent( "No such lifecycle phase: something-dumb", contentLines );
    }

    public void test_handleBuildFailureException_ShowLongMessageForMojoFailureException()
    {
        String longMessage = "This is a longer message.";

        MojoFailureException e = new MojoFailureException( "test-id", "Short Message", longMessage );

        MojoBinding binding = new MojoBinding();
        binding.setGroupId( "plugin.group" );
        binding.setArtifactId( "plugin-artifact" );
        binding.setVersion( "10" );

        BuildFailureException buildError = new ProjectBuildFailureException( "test:project:1",
                                                                             binding, e );

        StringWriter writer = new StringWriter();
        CLIReportingUtils.buildErrorMessage( buildError, false, writer );

        toConsole( writer );

        String[] contentLines = writer.toString().split( "\r?\n" );
        assertPresent( longMessage, contentLines );
    }

    public void test_handleBuildFailureException_ShowLongMessageForAggregatedMojoFailureException()
    {
        String longMessage = "This is a longer message.";

        MojoFailureException e = new MojoFailureException( "test-id", "Short Message", longMessage );

        MojoBinding binding = new MojoBinding();
        binding.setGroupId( "plugin.group" );
        binding.setArtifactId( "plugin-artifact" );
        binding.setVersion( "10" );

        BuildFailureException buildError = new AggregatedBuildFailureException(
                                                                                "/path/to/project/dir",
                                                                                binding, e );

        StringWriter writer = new StringWriter();
        CLIReportingUtils.buildErrorMessage( buildError, false, writer );

        toConsole( writer );

        String[] contentLines = writer.toString().split( "\r?\n" );
        assertPresent( longMessage, contentLines );
    }

    private void assertPresent( String message,
                                String[] contentLines )
    {
        for ( int i = 0; i < contentLines.length; i++ )
        {
            if ( contentLines[i].indexOf( message ) > -1 )
            {
                return;
            }
        }

        fail( "Message not found in output: \'" + message + "\'" );
    }

    private void toConsole( StringWriter writer )
    {
        System.out.println( new Throwable().getStackTrace()[1].getMethodName() + ":" );
        System.out.println( "==========================" );
        System.out.println( writer.toString() );
        System.out.println( "==========================" );
        System.out.println( "\n\n" );
    }

}
