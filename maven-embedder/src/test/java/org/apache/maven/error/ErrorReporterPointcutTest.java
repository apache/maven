package org.apache.maven.error;

import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.errors.CoreErrorReporter;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import junit.framework.TestCase;

public class ErrorReporterPointcutTest
    extends TestCase
{

    private MockControl reporterCtl;

    private CoreErrorReporter reporter;

    private MavenEmbedder maven;

    private String basedir;

    public void setUp()
        throws Exception
    {
        super.setUp();

        reporterCtl = MockControl.createStrictControl( CoreErrorReporter.class );
        reporter = (CoreErrorReporter) reporterCtl.getMock();

        reporter.clearErrors();
        reporterCtl.setVoidCallable( MockControl.ONE_OR_MORE );

        basedir = System.getProperty( "basedir" );

        if ( basedir == null )
        {
            basedir = new File( "." ).getCanonicalPath();
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Configuration configuration = new DefaultConfiguration().setClassLoader( classLoader )
                                                                .setMavenEmbedderLogger( new MavenEmbedderConsoleLogger() );

        maven = new MavenEmbedder( configuration );
    }

    private File prepareProjectDir()
        throws URISyntaxException, IOException
    {
        String method = new Throwable().getStackTrace()[1].getMethodName();

        String resource = "error-reporting-projects/" + method;

        File testDirectory = new File( basedir, "src/test/" + resource );

        File targetDirectory = new File( basedir, "target/" + resource );

        if ( targetDirectory.exists() )
        {
            try
            {
                FileUtils.deleteDirectory( targetDirectory );
            }
            catch( IOException e )
            {}
        }

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        return targetDirectory;
    }

    public void testHandleErrorBuildingExtensionPluginPOM()
        throws URISyntaxException
    {
        // TODO Auto-generated method stub

    }

    public void testHandleProjectBuildingError()
    {
        // TODO Auto-generated method stub

    }

    public void testHandleSuperPomBuildingError_XmlPullParserException()
    {
        // TODO Auto-generated method stub

    }

    public void testHandleSuperPomBuildingError_IOException()
    {
        // TODO Auto-generated method stub

    }

    public void testReportAggregatedMojoFailureException()
    {
        // TODO Auto-generated method stub

    }

    public void testReportAttemptToOverrideUneditableMojoParameter()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorApplyingMojoConfiguration()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorConfiguringExtensionPluginRealm()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorFormulatingBuildPlan()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorInterpolatingModel_UsingProjectInstance()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorLoadingPlugin()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorManagingRealmForExtension()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorManagingRealmForExtensionPlugin()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorResolvingExtensionDependencies()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorResolvingExtensionDirectDependencies()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorSearchingforCompatibleExtensionPluginVersion()
    {
        // TODO Auto-generated method stub

    }

    public void testReportExtensionPluginArtifactNotFound()
    {
        // TODO Auto-generated method stub

    }

    public void testReportExtensionPluginVersionNotFound()
    {
        // TODO Auto-generated method stub

    }

    public void testReportIncompatibleMavenVersionForExtensionPlugin()
    {
        // TODO Auto-generated method stub

    }

    public void testReportInvalidDependencyVersionInExtensionPluginPOM()
    {
        // TODO Auto-generated method stub

    }

    public void testReportInvalidMavenVersion()
    {
        // TODO Auto-generated method stub

    }

    public void testReportInvalidPluginExecutionEnvironment()
        throws URISyntaxException, IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportInvalidPluginExecutionEnvironment( null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "compiler:compile"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportLifecycleLoaderErrorWhileValidatingTask()
    {
        // TODO Auto-generated method stub

    }

    public void testReportLifecycleSpecErrorWhileValidatingTask()
    {
        // TODO Auto-generated method stub

    }

    public void testReportMissingArtifactWhileAddingExtensionPlugin()
    {
        // TODO Auto-generated method stub

    }

    public void testReportMissingPluginDescriptor()
    {
        // TODO Auto-generated method stub

    }

    public void testReportMissingRequiredMojoParameter()
    {
        // TODO Auto-generated method stub

    }

    public void testReportMojoExecutionException()
    {
        // TODO Auto-generated method stub

    }

    public void testReportMojoLookupError()
    {
        // TODO Auto-generated method stub

    }

    public void testReportNoGoalsSpecifiedException()
    {
        // TODO Auto-generated method stub

    }

    public void testReportPluginErrorWhileValidatingTask()
    {
        // TODO Auto-generated method stub

    }

    public void testReportPomFileCanonicalizationError()
    {
        // TODO Auto-generated method stub

    }

    public void testReportPomFileScanningError()
    {
        // TODO Auto-generated method stub

    }

    public void testReportProjectCycle()
    {
        // TODO Auto-generated method stub

    }

    public void testReportProjectDependenciesNotFound()
    {
        // TODO Auto-generated method stub

    }

    public void testReportProjectDependenciesUnresolvable()
    {
        // TODO Auto-generated method stub

    }

    public void testReportProjectDependencyArtifactNotFound()
    {
        // TODO Auto-generated method stub

    }

    public void testReportProjectDependencyArtifactUnresolvable()
    {
        // TODO Auto-generated method stub

    }

    public void testReportProjectMojoFailureException()
        throws URISyntaxException, IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportProjectMojoFailureException( null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "clean",
                                                                              "package"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportReflectionErrorWhileEvaluatingMojoParameter()
    {
        // TODO Auto-generated method stub

    }

    public void testReportUnresolvableArtifactWhileAddingExtensionPlugin()
    {
        // TODO Auto-generated method stub

    }

    public void testReportUnresolvableExtensionPluginPOM()
    {
        // TODO Auto-generated method stub

    }

    public void testReportUseOfBannedMojoParameter()
    {
        // TODO Auto-generated method stub

    }

    public void clearErrors()
    {
        // TODO Auto-generated method stub

    }

    public Throwable findReportedException( Throwable error )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getFormattedMessage( Throwable error )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Throwable getRealCause( Throwable error )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void testReportActivatorErrorWhileApplyingProfiles()
    {
        // TODO Auto-generated method stub

    }

    public void testReportActivatorErrorWhileGettingRepositoriesFromProfiles()
    {
        // TODO Auto-generated method stub

    }

    public void testReportActivatorLookupErrorWhileApplyingProfiles()
    {
        // TODO Auto-generated method stub

    }

    public void testReportActivatorLookupErrorWhileGettingRepositoriesFromProfiles()
    {
        // TODO Auto-generated method stub

    }

    public void testReportBadDependencyVersion()
    {
        // TODO Auto-generated method stub

    }

    public void testReportBadManagedDependencyVersion()
    {
        // TODO Auto-generated method stub

    }

    public void testReportBadNonDependencyProjectArtifactVersion()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorCreatingArtifactRepository()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorCreatingDeploymentArtifactRepository()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorInterpolatingModel_UsingModelInstance()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorLoadingExternalProfilesFromFile_XmlPullParserException()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorLoadingExternalProfilesFromFile_IOException()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorParsingParentProjectModel_XmlPullParserException()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorParsingParentProjectModel_IOException()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorParsingProjectModel_XmlPullParserException()
        throws URISyntaxException, IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportErrorParsingProjectModel( null, null, (XmlPullParserException) null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
                                                                          .setLoggingLevel( Logger.LEVEL_DEBUG )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportErrorParsingProjectModel_IOException()
        throws URISyntaxException, IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportErrorParsingProjectModel( null, null, (IOException) null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setPom( new File( projectDir, "pom.xml" ) )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportInvalidRepositoryWhileGettingRepositoriesFromProfiles()
    {
        // TODO Auto-generated method stub

    }

    public void testReportParentPomArtifactNotFound()
    {
        // TODO Auto-generated method stub

    }

    public void testReportParentPomArtifactUnresolvable()
    {
        // TODO Auto-generated method stub

    }

    public void testReportProjectCollision()
    {
        // TODO Auto-generated method stub

    }

    public void testReportProjectValidationFailure()
    {
        // TODO Auto-generated method stub

    }

}
