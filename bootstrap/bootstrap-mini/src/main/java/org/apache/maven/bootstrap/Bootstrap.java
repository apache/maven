package org.apache.maven.bootstrap;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.bootstrap.compile.CompilerConfiguration;
import org.apache.maven.bootstrap.compile.JavacCompiler;
import org.apache.maven.bootstrap.download.ArtifactResolver;
import org.apache.maven.bootstrap.download.OfflineArtifactResolver;
import org.apache.maven.bootstrap.download.OnlineArtifactDownloader;
import org.apache.maven.bootstrap.download.RepositoryMetadata;
import org.apache.maven.bootstrap.model.Dependency;
import org.apache.maven.bootstrap.model.Model;
import org.apache.maven.bootstrap.model.ModelReader;
import org.apache.maven.bootstrap.model.Plugin;
import org.apache.maven.bootstrap.model.Repository;
import org.apache.maven.bootstrap.settings.Mirror;
import org.apache.maven.bootstrap.settings.Proxy;
import org.apache.maven.bootstrap.settings.Settings;
import org.apache.maven.bootstrap.util.FileUtils;
import org.apache.maven.bootstrap.util.IsolatedClassLoader;
import org.apache.maven.bootstrap.util.JarMojo;
import org.apache.maven.bootstrap.util.SimpleArgumentParser;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Main class for bootstrap module.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class Bootstrap
{
    private static final String MODELLO_PLUGIN_ID = "org.codehaus.modello:modello-maven-plugin";

    private Set inProgress = new HashSet();

    private Map modelFileCache = new HashMap();

    private Map modelCache = new HashMap();

    private final ArtifactResolver resolver;

    private static final String USER_HOME = System.getProperty( "user.home" );

    public Bootstrap( SimpleArgumentParser parser )
        throws Exception
    {
        File settingsXml = new File( parser.getArgumentValue( "--settings" ) );

        System.out.println( "Using settings from " + settingsXml );

        Settings settings = Settings.read( USER_HOME, settingsXml );

        // TODO: have an alternative implementation of ArtifactResolver for source compiles
        //      - if building from source, checkout and build then resolve to built jar (still download POM?)
        resolver = setupRepositories( settings );
    }

    public static SimpleArgumentParser createDefaultParser()
    {
        File defaultSettingsFile = new File( USER_HOME, ".m2/settings.xml" );
        SimpleArgumentParser parser = new SimpleArgumentParser();
        parser.addArgument( "--settings", "The location of the settings.xml file", "-s", true,
                            defaultSettingsFile.getAbsolutePath() );
        return parser;
    }

    public static void main( String[] args )
        throws Exception
    {
        SimpleArgumentParser parser = createDefaultParser();

        parser.parseCommandLineArguments( args );

        Bootstrap bootstrap = new Bootstrap( parser );

        String goal = null;
        for ( int i = 0; i < args.length && goal == null; i++ )
        {
            if ( args[i].equals( "install" ) || args[i].equals( "package" ) )
            {
                goal = args[i];
            }
        }

        if ( goal == null )
        {
            System.err.println( "Goal 'package' or 'install' must be specified" );
            return;
        }

        bootstrap.run( goal );
    }

    private void run( String goal )
        throws Exception
    {
        Date fullStart = new Date();

        String basedir = System.getProperty( "user.dir" );

        File pom = new File( basedir, "pom.xml" );
        Model reader = readModel( pom, true );
        File jar = buildProject( reader );

        if ( "install".equals( goal ) )
        {
            install( reader, pom, jar );
        }

        for ( Iterator i = reader.getAllDependencies().iterator(); i.hasNext(); )
        {
            Dependency dep = (Dependency) i.next();

            FileUtils.copyFileToDirectory( resolver.getArtifactFile( dep ), jar.getParentFile() );
        }

        stats( fullStart, new Date() );
    }

    private void install( Model model, File pom, File jar )
        throws Exception
    {
        String artifactId = model.getArtifactId();

        String version = model.getVersion();

        String groupId = model.getGroupId();

        String type = model.getPackaging();

        Repository localRepository = resolver.getLocalRepository();
        File file = localRepository.getArtifactFile(
            new Dependency( groupId, artifactId, version, type, Collections.EMPTY_LIST ) );

        System.out.println( "Installing: " + file );

        FileUtils.copyFile( jar, file );

        installPomFile( model, pom );

        RepositoryMetadata metadata = new RepositoryMetadata();
        metadata.setReleaseVersion( version );
        metadata.setLatestVersion( version );
        file = localRepository.getMetadataFile( groupId, artifactId, null, type, "maven-metadata-local.xml" );
        metadata.write( file );

        metadata = new RepositoryMetadata();
        metadata.setLocalCopy( true );
        metadata.setLastUpdated( getCurrentUtcDate() );
        file = localRepository.getMetadataFile( groupId, artifactId, version, type, "maven-metadata-local.xml" );
        metadata.write( file );
    }

    private void installPomFile( Model model, File source )
        throws IOException
    {
        String artifactId = model.getArtifactId();

        String version = model.getVersion();

        String groupId = model.getGroupId();

        Repository localRepository = resolver.getLocalRepository();
        File pom = localRepository.getMetadataFile( groupId, artifactId, version, model.getPackaging(),
                                                    artifactId + "-" + version + ".pom" );

        System.out.println( "Installing POM: " + pom );

        FileUtils.copyFile( source, pom );
    }

    private void cacheModels( File basedir, ArtifactResolver resolver )
        throws IOException, ParserConfigurationException, SAXException
    {
        Model model = readModel( new File( basedir, "pom.xml" ), false );

        for ( Iterator i = model.getModules().iterator(); i.hasNext(); )
        {
            String module = (String) i.next();

            cacheModels( new File( basedir, module ), resolver );
        }
    }

    public void buildProject( File basedir, boolean buildModules )
        throws Exception
    {
        if ( buildModules )
        {
            // Pre-cache models so we know where they are for dependencies
            cacheModels( basedir, resolver );
        }

        System.setProperty( "basedir", basedir.getAbsolutePath() );

        File file = new File( basedir, "pom.xml" );

        Model model = readModel( file, true );

        String key = model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getPackaging();
        if ( inProgress.contains( key ) )
        {
            return;
        }

        if ( model.getPackaging().equals( "pom" ) )
        {
            if ( buildModules )
            {
                for ( Iterator i = model.getModules().iterator(); i.hasNext(); )
                {
                    String module = (String) i.next();

                    buildProject( new File( basedir, module ), true );
                }
            }

            return;
        }

        inProgress.add( key );

        if ( resolver.isAlreadyBuilt( key ) )
        {
            return;
        }

        buildProject( model );

        inProgress.remove( key );
    }

    private File buildProject( Model model )
        throws Exception
    {
        File basedir = model.getProjectFile().getParentFile();

        String sources = new File( basedir, "src/main/java" ).getAbsolutePath();

        String resources = new File( basedir, "src/main/resources" ).getAbsolutePath();

        String classes = new File( basedir, "target/classes" ).getAbsolutePath();

        File buildDirFile = new File( basedir, "target" );
        String buildDir = buildDirFile.getAbsolutePath();

        System.out.println( "Analysing dependencies ..." );

        for ( Iterator i = model.getAllDependencies().iterator(); i.hasNext(); )
        {
            Dependency dep = (Dependency) i.next();

            dep.getRepositories().addAll( model.getRepositories() );

            if ( modelFileCache.containsKey( dep.getId() ) )
            {
                buildProject( resolver.getArtifactFile( dep.getPomDependency() ).getParentFile(), false );
            }
        }

        resolver.downloadDependencies( model.getAllDependencies() );

        System.out.println();
        System.out.println();
        System.out.println( "Building project in " + basedir );

        line();

        // clean
        System.out.println( "Cleaning " + buildDirFile + "..." );
        FileUtils.forceDelete( buildDirFile );

        // ----------------------------------------------------------------------
        // Generate sources - modello
        // ----------------------------------------------------------------------

        File generatedSourcesDirectory = null;
        if ( model.getPlugins().containsKey( MODELLO_PLUGIN_ID ) )
        {
            Plugin plugin = (Plugin) model.getPlugins().get( MODELLO_PLUGIN_ID );

            File modelFile = new File( basedir, (String) plugin.getConfiguration().get( "model" ) );

            System.out.println( "Model exists!" );

            String modelVersion = (String) plugin.getConfiguration().get( "version" );
            if ( modelVersion == null || modelVersion.trim().length() < 1 )
            {
                System.out.println( "No model version configured. Using \'1.0.0\'..." );
                modelVersion = "1.0.0";
            }

            generatedSourcesDirectory = new File( basedir, "target/generated-sources/modello" );

            if ( !generatedSourcesDirectory.exists() )
            {
                generatedSourcesDirectory.mkdirs();
            }

            Dependency dependency = plugin.asDependencyPom();
            resolver.downloadDependencies( Collections.singletonList( dependency ) );
            File artifactFile = resolver.getArtifactFile( dependency );
            Model pluginReader = readModel( artifactFile, true );

            List dependencies = new ArrayList();
            for ( Iterator i = pluginReader.getAllDependencies().iterator(); i.hasNext(); )
            {
                Dependency d = (Dependency) i.next();
                if ( !d.getGroupId().equals( "org.apache.maven" ) )
                {
                    dependencies.add( d );
                }
            }

            ClassLoader classLoader = createClassloaderFromDependencies( dependencies, null, resolver );

            System.out.println( "Generating model bindings for version \'" + modelVersion + "\' from '" + model + "'" );

            generateModelloSources( modelFile.getAbsolutePath(), "java", generatedSourcesDirectory, modelVersion, "false",
                                    classLoader );
            generateModelloSources( modelFile.getAbsolutePath(), "xpp3-reader", generatedSourcesDirectory, modelVersion,
                                    "false", classLoader );
            generateModelloSources( modelFile.getAbsolutePath(), "xpp3-writer", generatedSourcesDirectory, modelVersion,
                                    "false", classLoader );
        }

        // ----------------------------------------------------------------------
        // Standard compile
        // ----------------------------------------------------------------------

        System.out.println( "Compiling sources ..." );

        compile( model.getAllDependencies(), sources, classes, null, generatedSourcesDirectory, Dependency.SCOPE_COMPILE,
                 resolver );

        // ----------------------------------------------------------------------
        // Standard resources
        // ----------------------------------------------------------------------

        System.out.println( "Packaging resources ..." );

        copyResources( resources, classes );

        // ----------------------------------------------------------------------
        // Create JAR
        // ----------------------------------------------------------------------

        File jarFile = createJar( new File( basedir, "pom.xml" ), classes, buildDir, model );

        System.out.println( "Packaging " + jarFile + " ..." );

        resolver.addBuiltArtifact( model.getGroupId(), model.getArtifactId(), "jar", jarFile );

        line();

        return jarFile;
    }

    public Model readModel( File file, boolean resolveTransitiveDependencies )
        throws ParserConfigurationException, SAXException, IOException
    {
        ModelReader reader = new ModelReader( resolver, resolveTransitiveDependencies );

        Model model = reader.parseModel( file, Collections.EMPTY_LIST );

        resolver.addBuiltArtifact( model.getGroupId(), model.getArtifactId(), "pom", file );

        String id = model.getGroupId() + ":" + model.getArtifactId();
        modelFileCache.put( id, file );

        modelCache.put( id, model );

        return model;
    }

    public void line()
    {
        System.out.println( "------------------------------------------------------------------" );
    }

    private File createJar( File pomFile, String classes, String buildDir, Model reader )
        throws Exception
    {
        JarMojo jarMojo = new JarMojo();

        String artifactId = reader.getArtifactId();

        // ----------------------------------------------------------------------
        // Create pom.properties file
        // ----------------------------------------------------------------------

        Properties p = new Properties();

        p.setProperty( "groupId", reader.getGroupId() );

        p.setProperty( "artifactId", reader.getArtifactId() );

        p.setProperty( "version", reader.getVersion() );

        File pomPropertiesDir =
            new File( new File( classes ), "META-INF/maven/" + reader.getGroupId() + "/" + reader.getArtifactId() );

        pomPropertiesDir.mkdirs();

        File pomPropertiesFile = new File( pomPropertiesDir, "pom.properties" );

        OutputStream os = new FileOutputStream( pomPropertiesFile );

        p.store( os, "Generated by Maven" );

        os.close(); // stream is flushed but not closed by Properties.store()

        FileUtils.copyFile( pomFile, new File( pomPropertiesDir, "pom.xml" ) );

        File jarFile = new File( buildDir, artifactId + ".jar" );
        jarMojo.execute( new File( classes ), jarFile );

        return jarFile;
    }

    public String getCurrentUtcDate()
    {
        TimeZone timezone = TimeZone.getTimeZone( "UTC" );
        DateFormat fmt = new SimpleDateFormat( "yyyyMMddHHmmss" );
        fmt.setTimeZone( timezone );
        return fmt.format( new Date() );
    }

    private void copyResources( String sourceDirectory, String destinationDirectory )
        throws Exception
    {
        File sd = new File( sourceDirectory );

        if ( !sd.exists() )
        {
            return;
        }

        List files = FileUtils.getFiles( sd, "**/**", "**/CVS/**,**/.svn/**", false );

        for ( Iterator i = files.iterator(); i.hasNext(); )
        {
            File f = (File) i.next();

            File source = new File( sourceDirectory, f.getPath() );

            File dest = new File( destinationDirectory, f.getPath() );

            if ( !dest.getParentFile().exists() )
            {
                dest.getParentFile().mkdirs();
            }

            FileUtils.copyFile( source, dest );
        }
    }

    private static ArtifactResolver setupRepositories( Settings settings )
        throws Exception
    {
        boolean online = true;

        String onlineProperty = System.getProperty( "maven.online" );

        if ( onlineProperty != null && onlineProperty.equals( "false" ) )
        {
            online = false;
        }

        Repository localRepository =
            new Repository( "local", settings.getLocalRepository(), Repository.LAYOUT_DEFAULT, false, false );

        File repoLocalFile = new File( localRepository.getBasedir() );
        repoLocalFile.mkdirs();

        if ( !repoLocalFile.canWrite() )
        {
            throw new Exception( "Can't write to " + repoLocalFile );
        }

        ArtifactResolver resolver;
        if ( online )
        {
            OnlineArtifactDownloader downloader = new OnlineArtifactDownloader( localRepository );
            resolver = downloader;
            if ( settings.getActiveProxy() != null )
            {
                Proxy proxy = settings.getActiveProxy();
                downloader.setProxy( proxy.getHost(), proxy.getPort(), proxy.getUserName(), proxy.getPassword() );
            }

            List remoteRepos = downloader.getRemoteRepositories();
            List newRemoteRepos = new ArrayList();

            for ( Iterator i = remoteRepos.iterator(); i.hasNext(); )
            {
                Repository repo = (Repository) i.next();

                boolean foundMirror = false;
                for ( Iterator j = settings.getMirrors().iterator(); j.hasNext() && !foundMirror; )
                {
                    Mirror m = (Mirror) j.next();
                    if ( m.getMirrorOf().equals( repo.getId() ) )
                    {
                        newRemoteRepos.add( new Repository( m.getId(), m.getUrl(), repo.getLayout(), repo.isSnapshots(),
                                                            repo.isReleases() ) );
                        foundMirror = true;
                    }
                }
                if ( !foundMirror )
                {
                    newRemoteRepos.add( repo );
                }
            }

            downloader.setRemoteRepositories( newRemoteRepos );

            System.out.println( "Using the following for your local repository: " + localRepository );
            System.out.println( "Using the following for your remote repository: " + newRemoteRepos );
        }
        else
        {
            resolver = new OfflineArtifactResolver( localRepository );
        }

        return resolver;
    }

    protected static String formatTime( long ms )
    {
        long secs = ms / 1000;

        long min = secs / 60;
        secs = secs % 60;

        if ( min > 0 )
        {
            return min + " minutes " + secs + " seconds";
        }
        else
        {
            return secs + " seconds";
        }
    }

    public static void stats( Date fullStart, Date fullStop )
    {
        long fullDiff = fullStop.getTime() - fullStart.getTime();

        System.out.println( "Total time: " + formatTime( fullDiff ) );

        System.out.println( "Finished at: " + fullStop );
    }

    private void compile( Collection dependencies, String sourceDirectory, String outputDirectory,
                          String extraClasspath, File generatedSources, String scope, ArtifactResolver resolver )
        throws Exception
    {
        JavacCompiler compiler = new JavacCompiler();

        String[] sourceDirectories = null;

        if ( generatedSources != null )
        {
            // We might only have generated sources

            if ( new File( sourceDirectory ).exists() )
            {
                sourceDirectories = new String[]{sourceDirectory, generatedSources.getAbsolutePath()};
            }
            else
            {
                sourceDirectories = new String[]{generatedSources.getAbsolutePath()};
            }
        }
        else
        {
            if ( new File( sourceDirectory ).exists() )
            {
                sourceDirectories = new String[]{sourceDirectory};
            }
        }

        if ( sourceDirectories != null )
        {
            CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
            compilerConfiguration.setOutputLocation( outputDirectory );
            List classpathEntries = classpath( dependencies, extraClasspath, scope, resolver );
            compilerConfiguration.setNoWarn( true );
            compilerConfiguration.setClasspathEntries( classpathEntries );
            compilerConfiguration.setSourceLocations( Arrays.asList( sourceDirectories ) );

            /* Compile with debugging info */
            String debugAsString = System.getProperty( "maven.compiler.debug", "true" );

            if ( !Boolean.valueOf( debugAsString ).booleanValue() )
            {
                compilerConfiguration.setDebug( false );
            }
            else
            {
                compilerConfiguration.setDebug( true );
            }

            List messages = compiler.compile( compilerConfiguration );

            for ( Iterator i = messages.iterator(); i.hasNext(); )
            {
                System.out.println( i.next() );
            }

            if ( messages.size() > 0 )
            {
                throw new Exception( "Compilation error." );
            }
        }
    }

    private List classpath( Collection dependencies, String extraClasspath, String scope, ArtifactResolver resolver )
    {
        List classpath = new ArrayList( dependencies.size() + 1 );

        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();

            String element = resolver.getArtifactFile( d ).getAbsolutePath();

            if ( Dependency.SCOPE_COMPILE.equals( scope ) )
            {
                if ( d.getScope().equals( Dependency.SCOPE_COMPILE ) )
                {
                    classpath.add( element );
                }
            }
            else if ( Dependency.SCOPE_RUNTIME.equals( scope ) )
            {
                if ( d.getScope().equals( Dependency.SCOPE_COMPILE ) ||
                    d.getScope().equals( Dependency.SCOPE_RUNTIME ) )
                {
                    classpath.add( element );
                }
            }
            else if ( Dependency.SCOPE_TEST.equals( scope ) )
            {
                classpath.add( element );
            }
        }

        if ( extraClasspath != null )
        {
            classpath.add( extraClasspath );
        }

        return classpath;
    }

    private void generateModelloSources( String model, String mode, File dir, String modelVersion,
                                         String packageWithVersion, ClassLoader modelloClassLoader )
        throws Exception
    {
        Class c = modelloClassLoader.loadClass( "org.codehaus.modello.ModelloCli" );

        Object generator = c.newInstance();

        Method m = c.getMethod( "main", new Class[]{String[].class} );

        String[] args = new String[]{model, mode, dir.getAbsolutePath(), modelVersion, packageWithVersion};

        ClassLoader old = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader( modelloClassLoader );

        m.invoke( generator, new Object[]{args} );

        Thread.currentThread().setContextClassLoader( old );
    }

    private IsolatedClassLoader createClassloaderFromDependencies( Collection dependencies, ClassLoader parent,
                                                                   ArtifactResolver resolver )
        throws Exception
    {
        System.out.println( "Checking for dependencies ..." );

        resolver.downloadDependencies( dependencies );

        IsolatedClassLoader cl;
        if ( parent == null )
        {
            cl = new IsolatedClassLoader();
        }
        else
        {
            cl = new IsolatedClassLoader( parent );
        }

        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Dependency dependency = (Dependency) i.next();

            File f = resolver.getArtifactFile( dependency );
            if ( !f.exists() )
            {
                String msg =
                    ( !resolver.isOnline() ? "; run again online" : "; there was a problem downloading it earlier" );
                throw new FileNotFoundException( "Missing dependency: " + dependency + msg );
            }

            // Classes won't be unloaded, but we might delete the JAR, so they need to be copied to a temporary location
            File newFile = File.createTempFile( "maven-bootstrap", "dep" );
            newFile.deleteOnExit();
            FileUtils.copyFile( f, newFile );
            cl.addURL( newFile.toURL() );
        }

        return cl;
    }

    public Model getCachedModel( String groupId, String artifactId )
    {
        return (Model) modelCache.get( groupId + ":" + artifactId );
    }

    public File getArtifactFile( Dependency dep )
    {
        return resolver.getArtifactFile( dep );
    }
}
