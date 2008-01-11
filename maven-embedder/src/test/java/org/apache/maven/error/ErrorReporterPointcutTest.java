package org.apache.maven.error;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.errors.CoreErrorReporter;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import junit.framework.TestCase;

public class ErrorReporterPointcutTest
    extends TestCase
{

    private MockControl reporterCtl;

    private CoreErrorReporter reporter;

    private MavenEmbedder maven;

    private String basedir;

    private static boolean isOffline;

    private static boolean offlineIsSet = false;

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

    private boolean checkOnline()
    {
        if ( !offlineIsSet )
        {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(
                                           "http://repo1.maven.org/maven2/org/apache/maven/maven-core/2.0/maven-core-2.0.pom" );

            HttpConnectionManager mgr = client.getHttpConnectionManager();
            mgr.getParams().setConnectionTimeout( 1 );

            try
            {
                int result = client.executeMethod( get );
                if ( result == HttpStatus.SC_OK )
                {
                    new MavenXpp3Reader().read( get.getResponseBodyAsStream() );
                    isOffline = false;
                }
            }
            catch ( HttpException e )
            {
                System.out.println( "System is offline" );
                isOffline = true;
            }
            catch ( IOException e )
            {
                System.out.println( "System is offline" );
                isOffline = true;
            }
            catch ( XmlPullParserException e )
            {
                System.out.println( "System is offline" );
                isOffline = true;
            }
            finally
            {
                offlineIsSet = true;
            }
        }

        if ( isOffline )
        {
            String method = getTestMethodName();
            System.out.println( "Test: " + method
                                + " requires an access to the Maven central repository. SKIPPING." );
            return false;
        }

        return true;
    }

    private String getTestMethodName()
    {
        String method = new Throwable().getStackTrace()[2].getMethodName();
        return method;
    }

    private File prepareProjectDir()
        throws URISyntaxException, IOException
    {
        String method = getTestMethodName();

        String resource = "error-reporting-projects/" + method;

        File testDirectory = new File( basedir, "src/test/" + resource );

        File targetDirectory = new File( basedir, "target/" + resource );

        if ( targetDirectory.exists() )
        {
            try
            {
                FileUtils.deleteDirectory( targetDirectory );
            }
            catch ( IOException e )
            {
            }
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

    // FIXME: Fix the offline detection for this one!
    public void testReportAggregatedMojoFailureException()
        throws URISyntaxException, IOException
    {
        if ( !checkOnline() )
        {
            return;
        }

        File projectDir = prepareProjectDir();

        buildTestAccessory( new File( projectDir, "plugin" ) );

        File basedir = new File( projectDir, "project" );

        reporter.reportAggregatedMojoFailureException( null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( basedir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "org.apache.maven.errortest:testReportAggregatedMojoFailureException-maven-plugin:1:test"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    private void buildTestAccessory( File basedir )
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( basedir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( new DummyCoreErrorReporter() )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "clean",
                                                                              "install"
                                                                          } ) );

        MavenExecutionResult result = maven.execute( request );

        if ( result.hasExceptions() )
        {
            StringWriter writer = new StringWriter();
            PrintWriter pWriter = new PrintWriter( writer );

            writer.write( "Failed to build project in: " );
            writer.write( basedir.getPath() );
            writer.write( "\nEncountered the following errors:" );

            for ( Iterator it = result.getExceptions().iterator(); it.hasNext(); )
            {
                Throwable error = (Throwable) it.next();
                writer.write( "\n\n" );
                error.printStackTrace( pWriter );
            }

            fail( writer.toString() );
        }
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
        reporter.reportNoGoalsSpecifiedException( null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Collections.EMPTY_LIST );

        maven.execute( request );

        reporterCtl.verify();
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
        throws URISyntaxException, IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportProjectCycle( null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
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

    // FIXME: How can I test this when it's masked by reportActivatorErrorWhileGettingRepositoriesFromProfiles?
    public void testReportActivatorError()
        throws URISyntaxException, IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportActivatorError( null, null, null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportActivatorLookupError()
    {
        // TODO Auto-generated method stub

    }

    public void testReportBadDependencyVersion()
        throws URISyntaxException, IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportBadDependencyVersion( null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "compile"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportBadManagedDependencyVersion()
        throws URISyntaxException, IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportBadManagedDependencyVersion( null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportBadNonDependencyProjectArtifactVersion()
        throws URISyntaxException, IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportBadNonDependencyProjectArtifactVersion( null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportErrorCreatingArtifactRepository()
        throws URISyntaxException, IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportErrorCreatingArtifactRepository( null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "compile"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportErrorCreatingDeploymentArtifactRepository()
        throws URISyntaxException, IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportErrorCreatingDeploymentArtifactRepository( null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "compile"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
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

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setPom( new File(
                                                                                             projectDir,
                                                                                             "pom.xml" ) )
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
        throws URISyntaxException, IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportProjectCollision( null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportProjectValidationFailure()
        throws URISyntaxException, IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportProjectValidationFailure( null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

}
