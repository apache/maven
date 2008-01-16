package org.apache.maven.error;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.errors.CoreErrorReporter;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import junit.framework.TestCase;

public class ErrorReporterPointcutTest
    extends TestCase
{

    private static final int ONE_SECOND = 1000;

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
            mgr.getParams().setConnectionTimeout( 3 * ONE_SECOND );

            try
            {
                int result = client.executeMethod( get );
                if ( result == HttpStatus.SC_OK )
                {
                    String body = get.getResponseBodyAsString();
                    new MavenXpp3Reader().read( new StringReader( body ) );
                    isOffline = false;
                }
                else
                {
                    System.out.println( "Got HTTP status of: " + result );
                    System.out.println( "System is offline" );
                    isOffline = true;
                }
            }
            catch ( HttpException e )
            {
                System.out.println( "Got error: " + e.getMessage() );
                System.out.println( "System is offline" );
                isOffline = true;
            }
            catch ( IOException e )
            {
                System.out.println( "Got error: " + e.getMessage() );
                System.out.println( "System is offline" );
                isOffline = true;
            }
            catch ( XmlPullParserException e )
            {
                System.out.println( "Got error: " + e.getMessage() );
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
        throws IOException
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

        if ( testDirectory.exists() )
        {
            FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );
        }
        else
        {
            testDirectory.mkdirs();
        }

        return targetDirectory;
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
            reportExceptions( result, basedir );
        }
    }

    private void reportExceptions( MavenExecutionResult result, File basedir )
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

    public void testReportErrorResolvingExtensionDirectDependencies()
        throws IOException
    {
        File projectDir = prepareProjectDir();
        File localRepo = new File( projectDir, "local-repo" );
        File project = new File( projectDir, "project" );

        // TODO: Verify that the actual error reported is the one that identified the failing project as an extension POM.
        reporter.reportBadDependencyVersion( null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporter.reportErrorResolvingExtensionDirectDependencies( null, null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( project )
                                                                          .setLocalRepositoryPath( localRepo )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportAggregatedMojoFailureException()
        throws IOException
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

    public void testReportAttemptToOverrideUneditableMojoParameter()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorApplyingMojoConfiguration()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorConfiguringExtensionPluginRealm()
        throws IOException
    {
        File projectDir = prepareProjectDir();

        buildTestAccessory( new File( projectDir, "plugin" ) );

        File project = new File( projectDir, "project" );

        reporter.reportErrorConfiguringExtensionPluginRealm( null, null, null, null, (PluginManagerException) null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( project )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportErrorFormulatingBuildPlan()
        throws IOException
    {
        if ( !checkOnline() )
        {
            return;
        }

        File projectDir = prepareProjectDir();

        buildTestAccessory( new File( projectDir, "plugin" ) );

        File basedir = new File( projectDir, "project" );

        reporter.reportErrorFormulatingBuildPlan( null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( basedir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "org.apache.maven.errortest:testReportErrorFormulatingBuildPlan-maven-plugin:1:test"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportErrorInterpolatingModel_UsingProjectInstance()
        throws IOException
    {
        if ( !checkOnline() )
        {
            return;
        }

        File projectDir = prepareProjectDir();
        File localRepo = new File( projectDir, "local-repo" );
        File project = new File( projectDir, "project" );

        reporter.reportErrorInterpolatingModel( null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( project )
                                                                          .setLocalRepositoryPath( localRepo )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "compile"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportErrorLoadingPlugin()
    {
        // TODO Auto-generated method stub

    }

    public void testReportErrorManagingRealmForExtension()
        throws IOException
    {
        if ( !checkOnline() )
        {
            return;
        }

        File projectDir = prepareProjectDir();

        buildTestAccessory( new File( projectDir, "ext" ) );

        File project = new File( projectDir, "project" );

        reporter.reportErrorManagingRealmForExtension( null, null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( project )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportErrorResolvingExtensionDependencies()
        throws IOException
    {
        File projectDir = prepareProjectDir();
        File localRepo = new File( projectDir, "local-repo" );
        File project = new File( projectDir, "project" );

        reporter.reportErrorResolvingExtensionDependencies( null, null, null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( project )
                                                                          .setLocalRepositoryPath( localRepo )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
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
        throws IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportInvalidMavenVersion( null, null, null );
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

    public void testReportInvalidPluginExecutionEnvironment()
        throws IOException
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
        throws IOException
    {
        File projectDir = prepareProjectDir();
        File localRepo = new File( projectDir, "local-repo" );

        Settings settings = new Settings();
        settings.setLocalRepository( localRepo.getAbsolutePath() );
        settings.setOffline( true );

        reporter.reportLifecycleLoaderErrorWhileValidatingTask( null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
                                                                          .setShowErrors( true )
                                                                          .setSettings( settings )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "invalid:test"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportLifecycleSpecErrorWhileValidatingTask()
    {
        reporter.reportLifecycleSpecErrorWhileValidatingTask( null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "name:of:invalid:direct:mojo:for:test"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
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

    public void testReportProjectCycle()
        throws IOException
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
        throws IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportProjectDependenciesNotFound( null, null, (MultipleArtifactsNotFoundException) null );
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

    // FIXME: Get the wagon to fail (in a way other than 'not found')
    public void testReportProjectDependenciesUnresolvable()
        throws IOException
    {
//        File projectDir = prepareProjectDir();
//
//        reporter.reportProjectDependenciesUnresolvable( null, null, null );
//        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
//        reporterCtl.setVoidCallable();
//
//        reporterCtl.replay();
//
//        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
//                                                                          .setShowErrors( true )
//                                                                          .setErrorReporter( reporter )
//                                                                          .setGoals( Arrays.asList( new String[] {
//                                                                              "compile"
//                                                                          } ) );
//
//        maven.execute( request );
//
//        reporterCtl.verify();
    }

    public void testReportProjectMojoFailureException()
        throws IOException
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
        throws IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportActivatorError( null, null, null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
//                                                                          .setErrorReporter( new DummyCoreErrorReporter() )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        MavenExecutionResult result = maven.execute( request );

//        if ( result.hasExceptions() )
//        {
//            reportExceptions( result, projectDir );
//        }

        reporterCtl.verify();
    }

    public void testReportActivatorLookupError()
        throws IOException
    {
        if ( !checkOnline() )
        {
            return;
        }

        File projectDir = prepareProjectDir();

        buildTestAccessory( new File( projectDir, "ext" ) );

        File project = new File( projectDir, "project" );

        reporter.reportActivatorLookupError( null, null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( project )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
//                                                                          .setErrorReporter( new DummyCoreErrorReporter() )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        MavenExecutionResult result = maven.execute( request );

//        if ( result.hasExceptions() )
//        {
//            reportExceptions( result, project );
//        }

        reporterCtl.verify();
    }

    public void testReportBadDependencyVersion()
        throws IOException
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
        throws IOException
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
        throws IOException
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
        throws IOException
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
        throws IOException
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
        throws IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportErrorInterpolatingModel( null, null, null, null, null );
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

    public void testReportErrorLoadingExternalProfilesFromFile_XmlPullParserException()
        throws IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportErrorLoadingExternalProfilesFromFile( null, null, null, (XmlPullParserException) null );
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

    public void testReportErrorLoadingExternalProfilesFromFile_IOException()
        throws IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportErrorLoadingExternalProfilesFromFile( null, null, null, (IOException) null );
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

    public void testReportErrorParsingParentProjectModel_XmlPullParserException()
        throws IOException
    {
        File projectDir = prepareProjectDir();
        File childDir = new File( projectDir, "child" );

        reporter.reportErrorParsingParentProjectModel( null, null, (XmlPullParserException) null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( childDir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportErrorParsingParentProjectModel_IOException()
        throws IOException
    {
        File projectDir = prepareProjectDir();
        File childDir = new File( projectDir, "child" );

        reporter.reportErrorParsingParentProjectModel( null, null, (IOException) null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( childDir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportErrorParsingProjectModel_XmlPullParserException()
        throws IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportErrorParsingProjectModel( null, null, (XmlPullParserException) null );
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

    public void testReportErrorParsingProjectModel_IOException()
        throws IOException
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
        throws IOException
    {
        File projectDir = prepareProjectDir();

        reporter.reportInvalidRepositoryWhileGettingRepositoriesFromProfiles( null, null, null, null );
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

    // TODO: Finish this test!
    public void testReportParentPomArtifactNotFound()
        throws IOException
    {
//        File projectDir = prepareProjectDir();
//
//        reporter.reportInvalidRepositoryWhileGettingRepositoriesFromProfiles( null, null, null, null );
//        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
//        reporterCtl.setVoidCallable();
//
//        reporterCtl.replay();
//
//        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
//                                                                          .setShowErrors( true )
//                                                                          .setErrorReporter( reporter )
//                                                                          .setGoals( Arrays.asList( new String[] {
//                                                                              "initialize"
//                                                                          } ) );
//
//        maven.execute( request );
//
//        reporterCtl.verify();
    }

    public void testReportParentPomArtifactUnresolvable()
    {
        // TODO Auto-generated method stub

    }

    public void testReportProjectCollision()
        throws IOException
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
        throws IOException
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
