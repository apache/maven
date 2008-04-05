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

    public void tearDown()
        throws Exception
    {
        super.tearDown();

        maven.stop();
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

    private File prepareProjectDir( String basename )
        throws IOException
    {
        String resource = "error-reporting-projects/" + basename;

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
            reportExceptions( result, basedir, true );
        }
    }

    private void reportExceptions( MavenExecutionResult result,
                                   File basedir )
    {
        reportExceptions( result, basedir, false );
    }

    private void reportExceptions( MavenExecutionResult result,
                                   File basedir,
                                   boolean expectExceptions )
    {
        assertTrue( !expectExceptions || result.hasExceptions() );

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

        if ( expectExceptions )
        {
            fail( writer.toString() );
        }
        else
        {
            System.out.println( writer.toString() );
        }
    }

    public void testReportErrorResolvingExtensionDirectDependencies()
        throws IOException
    {
        File projectDir = prepareProjectDir( "bad-ext-direct-deps" );
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

        File projectDir = prepareProjectDir( "aggregate-mojo-failure");

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
                                                                              "org.apache.maven.errortest:aggregate-mojo-failure-maven-plugin:1:test"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportAttemptToOverrideUneditableMojoParameter()
        throws IOException
    {
        if ( !checkOnline() )
        {
            return;
        }

        File projectDir = prepareProjectDir( "config-rdonly-mojo-param");

        buildTestAccessory( new File( projectDir, "plugin" ) );

        File basedir = new File( projectDir, "project" );

        reporter.reportAttemptToOverrideUneditableMojoParameter( null,
                                                                 null,
                                                                 null,
                                                                 null,
                                                                 null,
                                                                 null,
                                                                 null,
                                                                 null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( basedir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportErrorApplyingMojoConfiguration()
        throws IOException
    {
        if ( !checkOnline() )
        {
            return;
        }

        File projectDir = prepareProjectDir( "mojo-config-error");
        File plugin = new File( projectDir, "plugin" );
        File project = new File( projectDir, "project" );

        buildTestAccessory( plugin );

        reporter.reportErrorApplyingMojoConfiguration( null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( project )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "org.apache.maven.errortest:mojo-config-error-maven-plugin:1:test"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportErrorConfiguringExtensionPluginRealm()
        throws IOException
    {
        File projectDir = prepareProjectDir( "ext-plugin-realm-error" );

        buildTestAccessory( new File( projectDir, "plugin" ) );

        File project = new File( projectDir, "project" );

        reporter.reportErrorConfiguringExtensionPluginRealm( null,
                                                             null,
                                                             null,
                                                             null,
                                                             (PluginManagerException) null );
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

        File projectDir = prepareProjectDir( "bad-build-plan" );

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
                                                                              "org.apache.maven.errortest:bad-build-plan-maven-plugin:1:test"
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

        File projectDir = prepareProjectDir( "interp-from-project");
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
        throws IOException
    {
        File projectDir = prepareProjectDir( "err-loading-plugin" );
        File localRepo = new File( projectDir, "local-repo" );
        File project = new File( projectDir, "project" );

        Settings settings = new Settings();
        settings.setOffline( true );
        settings.setLocalRepository( localRepo.getAbsolutePath() );

        reporter.reportErrorLoadingPlugin( null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable( MockControl.ONE_OR_MORE );

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( project )
                                                                          .setSettings( settings )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "org.apache.maven.errortest:err-loading-plugin-maven-plugin:1:test"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportErrorManagingRealmForExtension()
        throws IOException
    {
        if ( !checkOnline() )
        {
            return;
        }

        File projectDir = prepareProjectDir( "ext-realm-error" );

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
        File projectDir = prepareProjectDir( "ext-deps-resolve-err" );
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

    public void testReportExtensionPluginArtifactNotFound()
        throws IOException
    {
        File projectDir = prepareProjectDir( "ext-plugin-artifact-missing" );
        File localRepo = new File( projectDir, "local-repo" );
        File project = new File( projectDir, "project" );

        reporter.reportExtensionPluginArtifactNotFound( null, null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( project )
                                                                          .setShowErrors( true )
                                                                          .setLocalRepositoryPath( localRepo )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportExtensionPluginVersionNotFound()
        throws IOException
    {
        File projectDir = prepareProjectDir( "ext-plugin-version-err" );
        File localRepo = new File( projectDir, "local-repo" );
        File project = new File( projectDir, "project" );

        Settings settings = new Settings();
        settings.setOffline( true );
        settings.setLocalRepository( localRepo.getAbsolutePath() );

        reporter.reportExtensionPluginVersionNotFound( null, null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( project )
                                                                          .setShowErrors( true )
                                                                          .setSettings( settings )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportIncompatibleMavenVersionForExtensionPlugin()
        throws IOException
    {
        File projectDir = prepareProjectDir( "bad-ext-plugin-maven-ver" );
        File localRepo = new File( projectDir, "local-repo" );
        File project = new File( projectDir, "project" );

        reporter.reportIncompatibleMavenVersionForExtensionPlugin( null,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( project )
                                                                          .setShowErrors( true )
                                                                          .setLocalRepositoryPath( localRepo )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportInvalidDependencyVersionInExtensionPluginPOM()
        throws IOException
    {
        File projectDir = prepareProjectDir( "bad-ext-plugin-dep-ver" );
        File localRepo = new File( projectDir, "local-repo" );
        File project = new File( projectDir, "project" );

        // TODO: Verify that the actual error reported is the one that identified the failing project as an extension POM.
        reporter.reportBadDependencyVersion( null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporter.reportInvalidDependencyVersionInExtensionPluginPOM( null, null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( project )
                                                                          .setShowErrors( true )
                                                                          .setLocalRepositoryPath( localRepo )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportInvalidMavenVersion()
        throws IOException
    {
        File projectDir = prepareProjectDir( "bad-maven-version" );

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
        File projectDir = prepareProjectDir( "bad-plugin-exec-env" );

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
        File projectDir = prepareProjectDir( "task-lifecycle-loader-err" );
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

    public void testReportMissingRequiredMojoParameter()
        throws IOException
    {
        if ( !checkOnline() )
        {
            return;
        }

        File projectDir = prepareProjectDir( "missing-req-mojo-param" );

        buildTestAccessory( new File( projectDir, "plugin" ) );

        File basedir = new File( projectDir, "project" );

        reporter.reportMissingRequiredMojoParameter( null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( basedir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "org.apache.maven.errortest:missing-req-mojo-param-maven-plugin:1:test"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportMojoExecutionException()
        throws IOException
    {
        if ( !checkOnline() )
        {
            return;
        }

        File projectDir = prepareProjectDir( "mojo-exec-err" );

        buildTestAccessory( new File( projectDir, "plugin" ) );

        File basedir = new File( projectDir, "project" );

        reporter.reportMojoExecutionException( null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( basedir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "org.apache.maven.errortest:mojo-exec-err-maven-plugin:1:test"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportMojoLookupError()
        throws IOException
    {
        if ( !checkOnline() )
        {
            return;
        }

        File projectDir = prepareProjectDir( "mojo-lookup-err" );

        buildTestAccessory( new File( projectDir, "plugin" ) );

        reporter.reportMojoLookupError( null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "org.apache.maven.errortest:mojo-lookup-err-maven-plugin:1:test"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
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

    public void testReportProjectCycle()
        throws IOException
    {
        File projectDir = prepareProjectDir( "project-cycle" );

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
        File projectDir = prepareProjectDir( "project-dep-missing" );

        reporter.reportProjectDependenciesNotFound( null,
                                                    null,
                                                    (MultipleArtifactsNotFoundException) null );
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

    public void testReportProjectDependenciesUnresolvable()
        throws IOException
    {
        if ( !checkOnline() )
        {
            return;
        }

        File projectDir = prepareProjectDir( "err-resolving-project-dep" );
        File localRepo = new File( projectDir, "local-repo" );
        File project = new File( projectDir, "project" );

        reporter.reportProjectDependenciesUnresolvable( null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( project )
                                                                          .setShowErrors( true )
                                                                          .setLocalRepositoryPath( localRepo )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "compile"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportProjectMojoFailureException()
        throws IOException
    {
        File projectDir = prepareProjectDir( "project-mojo-failure" );

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

    public void testReportUnresolvableArtifactWhileAddingExtensionPlugin()
        throws IOException
    {
        File projectDir = prepareProjectDir( "err-resolving-ext-plugin" );
        File localRepo = new File( projectDir, "local-repo" );
        File project = new File( projectDir, "project" );

        Settings settings = new Settings();
        settings.setOffline( true );
        settings.setLocalRepository( localRepo.getAbsolutePath() );

        reporter.reportUnresolvableArtifactWhileAddingExtensionPlugin( null, null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( project )
                                                                          .setShowErrors( true )
                                                                          .setSettings( settings )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportActivatorError()
        throws IOException
    {
        File projectDir = prepareProjectDir( "profile-activator-err" );

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
        throws IOException
    {
        if ( !checkOnline() )
        {
            return;
        }

        File projectDir = prepareProjectDir( "profile-activator-lookup-err" );

        buildTestAccessory( new File( projectDir, "ext" ) );

        File project = new File( projectDir, "project" );

        reporter.reportActivatorLookupError( null, null, null, null, null );
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

    public void testReportBadDependencyVersion()
        throws IOException
    {
        File projectDir = prepareProjectDir( "bad-dep-version" );

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
        File projectDir = prepareProjectDir( "bad-mg-dep-version" );

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
        File projectDir = prepareProjectDir( "bad-non-dep-version" );

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
        File projectDir = prepareProjectDir( "repo-creation-err" );

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
        File projectDir = prepareProjectDir( "deploy-repo-creation-err" );

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
        File projectDir = prepareProjectDir( "interp-from-model" );

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
        File projectDir = prepareProjectDir( "load-extern-profiles-xex" );

        reporter.reportErrorLoadingExternalProfilesFromFile( null,
                                                             null,
                                                             null,
                                                             (XmlPullParserException) null );
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
        File projectDir = prepareProjectDir( "load-extern-profiles-ioex" );

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
        File projectDir = prepareProjectDir( "parent-parse-xex");
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
        File projectDir = prepareProjectDir( "parent-parse-ioex" );
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
        File projectDir = prepareProjectDir( "project-parse-xex" );

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
        File projectDir = prepareProjectDir( "project-parse-ioex" );

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

    // FIXME: Something keeps flip-flopping about this test, between the two reporter methods below...need to revisit this pronto.
//    public void testReportErrorCreatingArtifactRepository_fromProfilesXml()
//        throws IOException
//    {
//        File projectDir = prepareProjectDir( "bad-profile-repo" );
//
//        reporter.reportInvalidRepositoryWhileGettingRepositoriesFromProfiles( null, null, null, null );
//        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
////        reporterCtl.setVoidCallable( MockControl.ZERO_OR_MORE );
//
////        reporter.reportErrorCreatingArtifactRepository( null, null, null, null );
////        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
//        reporterCtl.setVoidCallable();
//
//        reporterCtl.replay();
//
//        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
//        .setLoggingLevel( Logger.LEVEL_DEBUG )
//                                                                          .setShowErrors( true )
//                                                                          .setErrorReporter( reporter )
//                                                                          .setGoals( Arrays.asList( new String[] {
//                                                                              "initialize"
//                                                                          } ) );
//
//        MavenExecutionResult result = maven.execute( request );
//
//        reportExceptions( result, projectDir, false );
//
//        reporterCtl.verify();
//    }

    public void testReportParentPomArtifactNotFound()
        throws IOException
    {
        File projectDir = prepareProjectDir( "missing-parent-pom" );
        File localRepo = new File( projectDir, "local-repo" );

        Settings settings = new Settings();
        settings.setLocalRepository( localRepo.getAbsolutePath() );
        settings.setOffline( true );

        reporter.reportParentPomArtifactNotFound( null, null, null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
                                                                          .setSettings( settings )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "initialize"
                                                                          } ) );

        maven.execute( request );

        reporterCtl.verify();
    }

    public void testReportProjectCollision()
        throws IOException
    {
        File projectDir = prepareProjectDir( "project-collision" );

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
        File projectDir = prepareProjectDir( "project-validation" );

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

    public void testReportMissingModulePom()
        throws IOException
    {
        File projectDir = prepareProjectDir( "missing-module-pom" );

        reporter.reportMissingModulePom( null );
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

    public void testReportInvalidPluginForDirectInvocation()
        throws IOException
    {
        File projectDir = prepareProjectDir( "missing-direct-invoke-mojo" );

        File plugin = new File( projectDir, "plugin" );

        buildTestAccessory( plugin );

        Settings settings = new Settings();
        settings.addPluginGroup( "org.apache.maven.errortest" );

        reporter.reportInvalidPluginForDirectInvocation( null, null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( projectDir )
                                                                          .setSettings( settings )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "missing-direct-invoke-mojo:test"
                                                                          } ) );

        MavenExecutionResult result = maven.execute( request );

        assertTrue( result.hasExceptions() );
        reportExceptions( result, projectDir );

        reporterCtl.verify();
    }

    public void testReportDuplicateAttachmentException()
        throws IOException
    {
        File projectDir = prepareProjectDir( "duplicated-attachments" );

        File plugin = new File( projectDir, "plugin" );
        File project = new File( projectDir, "project" );

        buildTestAccessory( plugin );

        Settings settings = new Settings();
        settings.addPluginGroup( "org.apache.maven.errortest" );

        reporter.reportDuplicateAttachmentException( null, null, null );
        reporterCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        reporterCtl.setVoidCallable();

        reporterCtl.replay();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( project )
                                                                          .setSettings( settings )
                                                                          .setShowErrors( true )
                                                                          .setErrorReporter( reporter )
                                                                          .setGoals( Arrays.asList( new String[] {
                                                                              "duplicated-attachments:test"
                                                                          } ) );

        MavenExecutionResult result = maven.execute( request );

        assertTrue( result.hasExceptions() );
        reportExceptions( result, project );

        reporterCtl.verify();
    }

}
