package org.apache.maven.script.ant;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.path.PathTranslator;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.factory.ComponentInstantiationException;
import org.codehaus.plexus.component.factory.ant.AntScriptInvoker;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.easymock.MockControl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class AntMojoWrapperTest
    extends TestCase
{

    public void test2xStylePlugin()
        throws PlexusConfigurationException, IOException, ComponentInstantiationException, MojoExecutionException,
        ComponentConfigurationException, ArchiverException
    {
        String pluginXml = "META-INF/maven/plugin-2.1.xml";

        List messages = run( pluginXml, true );

        assertPresence( messages, "Unpacked Ant build scripts (in Maven build directory).", false );
        assertPresence( messages, "Maven parameter expression evaluator for Ant properties.", false );
        assertPresence( messages, "Maven standard project-based classpath references.", false );
        assertPresence( messages, "Maven standard plugin-based classpath references.", false );
        assertPresence( messages,
                        "Maven project, session, mojo-execution, or path-translation parameter information is", false );
        assertPresence( messages, "maven-script-ant < 2.1.0, or used maven-plugin-tools-ant < 2.2 during release",
                        false );

        assertPresence( messages, "path-is-missing", false );
    }

    public void test20StylePlugin()
        throws PlexusConfigurationException, IOException, ComponentInstantiationException, MojoExecutionException,
        ComponentConfigurationException, ArchiverException
    {
        String pluginXml = "META-INF/maven/plugin-2.0.xml";

        List messages = run( pluginXml, false );

        assertPresence( messages, "Unpacked Ant build scripts (in Maven build directory).", true );
        assertPresence( messages, "Maven parameter expression evaluator for Ant properties.", true );
        assertPresence( messages, "Maven standard project-based classpath references.", true );
        assertPresence( messages, "Maven standard plugin-based classpath references.", true );
        assertPresence( messages,
                        "Maven project, session, mojo-execution, or path-translation parameter information is", true );
        assertPresence( messages, "maven-script-ant < 2.1.0, or used maven-plugin-tools-ant < 2.2 during release", true );

        assertPresence( messages, "path-is-missing", true );
    }

    private void assertPresence( List messages, String test, boolean shouldBePresent )
    {
        boolean found = false;

        for ( Iterator it = messages.iterator(); it.hasNext(); )
        {
            String message = (String) it.next();
            if ( message.indexOf( test ) > -1 )
            {
                found = true;
                break;
            }
        }

        if ( !shouldBePresent && found )
        {
            fail( "Test string: '" + test + "' was found in output, but SHOULD NOT BE THERE." );
        }
        else if ( shouldBePresent && !found )
        {
            fail( "Test string: '" + test + "' was NOT found in output, but SHOULD BE THERE." );
        }
    }

    private List run( String pluginXml, boolean includeImplied )
        throws PlexusConfigurationException, IOException, ComponentInstantiationException, MojoExecutionException,
        ComponentConfigurationException, ArchiverException
    {
        StackTraceElement stack = new Throwable().getStackTrace()[1];
        System.out.println( "\n\nRunning: " + stack.getMethodName() + "\n\n" );

        URL resource = Thread.currentThread().getContextClassLoader().getResource( pluginXml );

        if ( resource == null )
        {
            fail( "plugin descriptor not found: '" + pluginXml + "'." );
        }

        Reader reader = null;
        PluginDescriptor pd;
        try
        {
            reader = new InputStreamReader( resource.openStream() );
            pd = new PluginDescriptorBuilder().build( reader, pluginXml );
        }
        finally
        {
            IOUtil.close( reader );
        }

        Map config = new HashMap();
        config.put( "basedir", new File( "." ).getAbsoluteFile() );
        config.put( "messageLevel", "info" );

        MojoDescriptor md = pd.getMojo( "test" );

        AntMojoWrapper wrapper =
            new AntMojoWrapper( new AntScriptInvoker( md, Thread.currentThread().getContextClassLoader() ) );

        wrapper.enableLogging( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        MockControl artifactCtl = null;
        MockControl pathTranslatorCtl = null;
        if ( includeImplied )
        {
            File pluginXmlFile = new File( StringUtils.replace( resource.getPath(), "%20", " " ) );

            File jarFile = File.createTempFile( "AntMojoWrapperTest.", ".test.jar" );
            jarFile.deleteOnExit();

            JarArchiver archiver = new JarArchiver();
            archiver.enableLogging( new ConsoleLogger( Logger.LEVEL_ERROR, "archiver" ) );
            archiver.setDestFile( jarFile );
            archiver.addFile( pluginXmlFile, pluginXml );
            archiver.createArchive();

            artifactCtl = MockControl.createControl( Artifact.class );
            Artifact artifact = (Artifact) artifactCtl.getMock();

            artifact.getFile();
            artifactCtl.setReturnValue( jarFile, MockControl.ZERO_OR_MORE );

            artifact.getGroupId();
            artifactCtl.setReturnValue( "groupId", MockControl.ZERO_OR_MORE );

            artifact.getArtifactId();
            artifactCtl.setReturnValue( "artifactId", MockControl.ZERO_OR_MORE );

            artifact.getVersion();
            artifactCtl.setReturnValue( "1", MockControl.ZERO_OR_MORE );

            artifact.getId();
            artifactCtl.setReturnValue( "groupId:artifactId:jar:1", MockControl.ZERO_OR_MORE );

            artifact.getClassifier();
            artifactCtl.setReturnValue( null, MockControl.ZERO_OR_MORE );

            pathTranslatorCtl = MockControl.createControl( PathTranslator.class );
            PathTranslator pt = (PathTranslator) pathTranslatorCtl.getMock();

            Model model = new Model();

            Build build = new Build();
            build.setDirectory( "target" );

            model.setBuild( build );

            MavenProject project = new MavenProject( model );
            project.setFile( new File( "pom.xml" ).getAbsoluteFile() );

            artifactCtl.replay();
            pathTranslatorCtl.replay();

            pd.setPluginArtifact( artifact );
            pd.setArtifacts( Collections.singletonList( artifact ) );

            config.put( "project", project );
            config.put( "session", new MavenSession( null, null, null, null, null, null, null, null, null, null ) );
            config.put( "mojoExecution", new MojoExecution( md ) );

            ComponentRequirement cr = new ComponentRequirement();
            cr.setRole( PathTranslator.class.getName() );

            wrapper.addComponentRequirement( cr, pt );
        }

        wrapper.setComponentConfiguration( config );

        TestBuildListener tbl = new TestBuildListener();
        wrapper.getAntProject().addBuildListener( tbl );
        
        PrintStream oldOut = System.out;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try
        {
            System.setOut( new PrintStream( baos ) );

            wrapper.execute();
        }
        finally
        {
            System.setOut( oldOut );
        }

        System.out.println( "\n\n" + stack.getMethodName() + " executed; verifying...\n\n" );

        if ( includeImplied )
        {
            artifactCtl.verify();
            pathTranslatorCtl.verify();
        }

        List messages = new ArrayList();
        if ( !tbl.messages.isEmpty() )
        {
            messages.addAll( tbl.messages );
        }
        
        messages.add( new String( baos.toByteArray() ) );
        
        return messages;
    }

    private static final class TestBuildListener
        implements BuildListener
    {
        private List messages = new ArrayList();

        public void buildFinished( BuildEvent arg0 )
        {
        }

        public void buildStarted( BuildEvent arg0 )
        {
        }

        public void messageLogged( BuildEvent event )
        {
            messages.add( event.getMessage() );
        }

        public void targetFinished( BuildEvent arg0 )
        {
        }

        public void targetStarted( BuildEvent arg0 )
        {
        }

        public void taskFinished( BuildEvent arg0 )
        {
        }

        public void taskStarted( BuildEvent arg0 )
        {
        }
    };

}
