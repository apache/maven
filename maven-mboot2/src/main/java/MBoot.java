
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class MBoot
{
    // ----------------------------------------------------------------------
    // These are the bootstrap processes' dependencies
    // ----------------------------------------------------------------------
    
    String[] bootstrapDeps = new String[]
    {
        "maven/jars/wagon-http-lightweight-1.0-alpha-1-SNAPSHOT.jar",
        "junit/jars/junit-3.8.1.jar",
        "modello/jars/modello-core-1.0-SNAPSHOT.jar",
        "modello/jars/modello-xdoc-plugin-1.0-SNAPSHOT.jar",
        "modello/jars/modello-xml-plugin-1.0-SNAPSHOT.jar",
        "modello/jars/modello-xpp3-plugin-1.0-SNAPSHOT.jar",
        "surefire/jars/surefire-booter-1.2-SNAPSHOT.jar",
        "surefire/jars/surefire-1.2-SNAPSHOT.jar",
        "xpp3/jars/xpp3-1.1.3.3.jar",
        "xstream/jars/xstream-1.0-SNAPSHOT.jar",
        "qdox/jars/qdox-1.2.jar"
    };

    // ----------------------------------------------------------------------
    // These are plexus' runtime dependencies
    // ----------------------------------------------------------------------

    String[] plexusDeps = new String[]
    {
        "classworlds/jars/classworlds-1.1-SNAPSHOT.jar",
        "plexus/jars/plexus-container-api-1.0-alpha-1-SNAPSHOT.jar",
        "plexus/jars/plexus-container-default-1.0-alpha-1-SNAPSHOT.jar",
        "plexus/jars/plexus-utils-1.0-alpha-1-SNAPSHOT.jar",
        "xpp3/jars/xpp3-1.1.3.3.jar",
        "xstream/jars/xstream-1.0-SNAPSHOT.jar",
    };

    // ----------------------------------------------------------------------
    // These are modello's runtime dependencies
    // ----------------------------------------------------------------------

    String[] modelloDeps = new String[]
    {
        "classworlds/jars/classworlds-1.1-SNAPSHOT.jar",
        "plexus/jars/plexus-container-api-1.0-alpha-1-SNAPSHOT.jar",
        "plexus/jars/plexus-container-default-1.0-alpha-1-SNAPSHOT.jar",
        "plexus/jars/plexus-utils-1.0-alpha-1-SNAPSHOT.jar",
        "modello/jars/modello-core-1.0-SNAPSHOT.jar",
        "modello/jars/modello-xdoc-plugin-1.0-SNAPSHOT.jar",
        "modello/jars/modello-xml-plugin-1.0-SNAPSHOT.jar",
        "modello/jars/modello-xpp3-plugin-1.0-SNAPSHOT.jar",
        "xpp3/jars/xpp3-1.1.3.3.jar",
        "xstream/jars/xstream-1.0-SNAPSHOT.jar"
    };

    String[] builds = new String[]
    {
        "maven-model",
        "maven-plugin",
        "maven-plugin-tools",
        "maven-artifact",
        "maven-core",
        "maven-core-it-verifier"
    };

    String[] pluginBuilds = new String[]
    {
        "maven-plugins/maven-clean-plugin",
        "maven-plugins/maven-compiler-plugin",
        "maven-plugins/maven-install-plugin",
        "maven-plugins/maven-jar-plugin",
        "maven-plugins/maven-plugin-plugin",
        "maven-plugins/maven-pom-plugin",
        "maven-plugins/maven-resources-plugin",
        "maven-plugins/maven-surefire-plugin"
    };


    // ----------------------------------------------------------------------
    // Standard locations for resources in Maven projects.
    // ----------------------------------------------------------------------

    private static final String SOURCES = "src/main/java";

    private static final String TEST_SOURCES = "src/test/java";

    private static final String RESOURCES = "src/main/resources";

    private static final String TEST_RESOURCES = "src/test/resources";

    private static final String BUILD_DIR = "target";

    private static final String CLASSES = BUILD_DIR + "/classes";

    private static final String TEST_CLASSES = BUILD_DIR + "/test-classes";

    private static final String GENERATED_SOURCES = BUILD_DIR + "/generated-sources";

    private static final String GENERATED_DOCS = BUILD_DIR + "/generated-docs";

    // ----------------------------------------------------------------------
    // Per-session entities which we can reuse while building many projects.
    // ----------------------------------------------------------------------

    private ArtifactDownloader downloader;

    private ModelReader reader;

    private String repoLocal;

    private List mbootDependencies;

    private List coreDeps;

    private boolean online = true;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public static void main( String[] args )
        throws Exception
    {
        MBoot mboot = new MBoot();

        mboot.run( args );
    }

    public void run( String[] args )
        throws Exception
    {
        File userPomFile = new File( System.getProperty( "user.home" ), ".m2/override.xml" );

        reader = new ModelReader();

        if ( userPomFile.exists() && !reader.parse( userPomFile ) )
        {
            System.err.println( "Error reading user POM file" );

            System.exit( 1 );
        }

        String mavenRepoLocal = System.getProperty( "maven.repo.local", reader.getLocal().getRepository() );

        if ( mavenRepoLocal == null )
        {
            System.out.println( "You must have a ~/.m2/override.xml file and must contain the following entries:" );
            System.out.println( "<local>" );
            System.out.println( "  <repository>/path/to/m2/repository</repository> (required)" );
            System.out.println( "  <online>true</online> (optional)" );
            System.out.println( "</local>" );
            System.out.println();
            System.out.println( "Alternatively, you can specify -Dmaven.repo.local=/path/to/m2/repository" );

            System.exit( 1 );
        }

        String mavenHome = null;

        if ( args.length == 1 )
        {
            mavenHome = args[0];
        }
        else
        {
            mavenHome = System.getProperty( "maven.home" );

            if ( mavenHome == null )
            {
                mavenHome = new File( System.getProperty( "user.home" ), "m2" ).getAbsolutePath();
            }
        }

        File dist = new File( mavenHome );

        System.out.println( "Maven installation directory: " + dist );

        Date fullStop;

        Date fullStart = new Date();

        String onlineProperty = System.getProperty( "maven.online", reader.getLocal().getOnline() );

        if ( onlineProperty != null && onlineProperty.equals( "false" ) )
        {
            online = false;
        }

        downloader = new ArtifactDownloader( mavenRepoLocal, reader.getRemoteRepositories() );

        repoLocal = downloader.getMavenRepoLocal().getPath();

        reader = new ModelReader();

        String basedir = System.getProperty( "user.dir" );

        mbootDependencies = Arrays.asList( bootstrapDeps );

        if ( online )
        {
            checkMBootDeps();
        }

        // Install maven-components POM
        installPomFile( repoLocal, new File( basedir, "pom.xml" ) );

        // Install plugin-parent POM
        installPomFile( repoLocal, new File( basedir, "maven-plugins/pom.xml" ) );

        createToolsClassLoader();

        for ( int i = 0; i < builds.length; i++ )
        {
            String directory = new File( basedir, builds[i] ).getAbsolutePath();

            System.out.println( "Building project in " + directory + " ..." );

            System.out.println( "--------------------------------------------------------------------" );

            System.setProperty( "basedir", directory );

            buildProject( directory );

            if ( reader.artifactId.equals( "maven-core" ) )
            {
                coreDeps = reader.getDependencies();
            }

            reader.reset();

            System.out.println( "--------------------------------------------------------------------" );
        }

        cl.addURL( new File( repoLocal, "maven/jars/maven-plugin-2.0-SNAPSHOT.jar" ).toURL() );

        cl.addURL( new File( repoLocal, "maven/jars/maven-plugin-tools-2.0-SNAPSHOT.jar" ).toURL() );


        for ( int i = 0; i < pluginBuilds.length; i++ )
        {
            String directory = new File( basedir, pluginBuilds[i] ).getAbsolutePath();

            System.out.println( "Building project in " + directory + " ..." );

            System.out.println( "--------------------------------------------------------------------" );

            System.setProperty( "basedir", directory );

            buildProject( directory );

            reader.reset();

            System.out.println( "--------------------------------------------------------------------" );
        }


        // build the installation

        FileUtils.deleteDirectory( dist );

        // ----------------------------------------------------------------------
        // bin
        // ----------------------------------------------------------------------

        String bin = new File( dist, "bin" ).getAbsolutePath();

        FileUtils.mkdir( new File( bin ).getPath() );

        FileUtils.copyFileToDirectory( new File( basedir, "maven-core/src/bin/m2" ).getAbsolutePath(), bin );

        FileUtils.copyFileToDirectory( new File( basedir, "maven-core/src/bin/m2.bat" ).getAbsolutePath(), bin );

        FileUtils.copyFileToDirectory( new File( basedir, "maven-core/src/bin/classworlds.conf" ).getAbsolutePath(), bin );

        if ( Os.isFamily( "unix" ) )
        {
            Commandline cli = new Commandline();

            cli.setExecutable( "chmod" );

            cli.createArgument().setValue( "+x" );

            cli.createArgument().setValue( new File( dist, "bin/m2" ).getAbsolutePath() );

            cli.execute();
        }

        // ----------------------------------------------------------------------
        // core
        // ----------------------------------------------------------------------

        String core = new File( dist, "core" ).getAbsolutePath();

        FileUtils.mkdir( new File( core ).getPath() );

        String boot = new File( dist, "core/boot" ).getAbsolutePath();

        FileUtils.mkdir( new File( boot ).getPath() );

        for ( int i = 0; i < plexusDeps.length; i++ )
        {
            if ( plexusDeps[i].startsWith( "classworlds") )
            {
                FileUtils.copyFileToDirectory( repoLocal + "/" + plexusDeps[i], boot );
            }
            else
            {
                FileUtils.copyFileToDirectory( repoLocal + "/" + plexusDeps[i], core );
            }
        }

        // ----------------------------------------------------------------------
        // lib
        // ----------------------------------------------------------------------

        String lib = new File( dist, "lib" ).getAbsolutePath();

        FileUtils.mkdir( new File( lib ).getPath() );

        for ( Iterator i = coreDeps.iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();

            if ( d.getArtifactId().equals( "classworlds" ) ||
                 d.artifactId.equals( "plexus" ) ||
                 d.artifactId.equals( "xstream" ) ||
                 d.artifactId.equals( "xpp3" ) ||
                 d.artifactId.equals( "junit" ) )
            {
                continue;
            }

            FileUtils.copyFileToDirectory( repoLocal + "/" + getArtifactPath( d, "/" ), lib );
        }

        // Copy maven itself

        FileUtils.copyFileToDirectory( repoLocal + "/maven/jars/maven-core-2.0-SNAPSHOT.jar", lib );

        System.out.println();

        System.out.println( "Maven2 is installed in " + dist.getAbsolutePath() );

        System.out.println( "--------------------------------------------------------------------" );

        System.out.println();

        fullStop = new Date();

        stats( fullStart, fullStop );
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

    private void stats( Date fullStart, Date fullStop )
    {
        long fullDiff = fullStop.getTime() - fullStart.getTime();

        System.out.println( "Total time: " + formatTime( fullDiff ) );

        System.out.println( "Finished at: " + fullStop );
    }

    public void buildProject( String basedir )
        throws Exception
    {
        System.out.println( "Building project in " + basedir );

        if ( !reader.parse( new File( basedir, "pom.xml" ) ) )
        {
            System.err.println( "Could not parse pom.xml" );

            System.exit( 1 );
        }

        String sources = new File( basedir, SOURCES ).getAbsolutePath();

        String resources = new File( basedir, RESOURCES ).getAbsolutePath();

        String classes = new File( basedir, CLASSES ).getAbsolutePath();

        String testSources = new File( basedir, TEST_SOURCES ).getAbsolutePath();

        String testResources = new File( basedir, TEST_RESOURCES ).getAbsolutePath();

        String testClasses = new File( basedir, TEST_CLASSES ).getAbsolutePath();

        String generatedSources = new File( basedir, GENERATED_SOURCES ).getAbsolutePath();

        String generatedDocs = new File( basedir, GENERATED_DOCS ).getAbsolutePath();

        File buildDirFile = new File( basedir, BUILD_DIR );
        String buildDir = buildDirFile.getAbsolutePath();

        // clean
        System.out.println( "Cleaning " + buildDirFile + "..." );
        FileUtils.forceDelete( buildDirFile );

        // ----------------------------------------------------------------------
        // Download bootstrapDeps
        // ----------------------------------------------------------------------

        if ( online )
        {
            System.out.println( "Downloading dependencies ..." );

            downloadDependencies( reader.getDependencies() );
        }

        // ----------------------------------------------------------------------
        // Generating sources
        // ----------------------------------------------------------------------

        File model = new File( basedir, "maven.mdo" );

        if ( model.exists() )
        {
            System.out.println( "Model exists!" );

            File generatedSourcesDirectory = new File( basedir, GENERATED_SOURCES );

            if ( !generatedSourcesDirectory.exists() )
            {
                generatedSourcesDirectory.mkdirs();
            }

            File generatedDocsDirectory = new File( basedir, GENERATED_DOCS );

            if ( !generatedDocsDirectory.exists() )
            {
                generatedDocsDirectory.mkdirs();
            }

            generateSources( model.getAbsolutePath(), "java", generatedSources, "4.0.0", "false" );

            //generateSources( model.getAbsolutePath(), "java", generatedSources, "3.0.0", "true" );

            generateSources( model.getAbsolutePath(), "xpp3-reader", generatedSources, "4.0.0", "false" );

            //generateSources( model.getAbsolutePath(), "xpp3-reader", generatedSources, "3.0.0", "true" );

            generateSources( model.getAbsolutePath(), "xpp3-writer", generatedSources, "4.0.0", "false" );

            //generateSources( model.getAbsolutePath(), "xpp3-writer", generatedSources, "3.0.0", "true" );

            generateSources( model.getAbsolutePath(), "xdoc", generatedDocs, "4.0.0", "false" );

            //generateSources( model.getAbsolutePath(), "xdoc", generatedDocs, "3.0.0", "true" );
        }

        // ----------------------------------------------------------------------
        // Standard compile
        // ----------------------------------------------------------------------

        System.out.println( "Compiling sources ..." );

        if ( new File( generatedSources ).exists() )
        {
            compile( reader.getDependencies(), sources, classes, null, generatedSources );
        }
        else
        {
            compile( reader.getDependencies(), sources, classes, null, null );
        }

        // ----------------------------------------------------------------------
        // Plugin descriptor generation
        // ----------------------------------------------------------------------

        if ( reader.type != null && reader.type.equals( "plugin" ) )
        {
            System.out.println( "Generating maven plugin descriptor ..." );

            generatePluginDescriptor( sources,
                                      new File( classes, "META-INF/maven" ).getAbsolutePath(),
                                      new File( basedir, "pom.xml" ).getAbsolutePath() );
        }

        // ----------------------------------------------------------------------
        // Standard resources
        // ----------------------------------------------------------------------

        System.out.println( "Packaging resources ..." );

        copyResources( resources, classes );

        // ----------------------------------------------------------------------
        // Test compile
        // ----------------------------------------------------------------------

        System.out.println( "Compiling test sources ..." );

        List testDependencies = reader.getDependencies();

        Dependency junitDep = new Dependency();

        junitDep.setGroupId( "junit" );

        junitDep.setArtifactId( "junit" );

        junitDep.setVersion( "3.8.1" );

        testDependencies.add( junitDep );

        compile( testDependencies, testSources, testClasses, classes, null );

        // ----------------------------------------------------------------------
        // Test resources
        // ----------------------------------------------------------------------

        System.out.println( "Packaging test resources ..." );

        copyResources( testResources, testClasses );

        // ----------------------------------------------------------------------
        // Run tests
        // ----------------------------------------------------------------------

        runTests( basedir, classes, testClasses );

        // ----------------------------------------------------------------------
        // Create JAR
        // ----------------------------------------------------------------------

        createJar( classes, buildDir );

        installPom( basedir, repoLocal );

        if ( !reader.artifactId.equals( "maven-plugin" ) && reader.artifactId.endsWith( "plugin" ) )
        {
            installPlugin( basedir, repoLocal );
        }
        else
        {
            installJar( basedir, repoLocal );
        }
    }

    IsolatedClassLoader cl;

    private void createToolsClassLoader()
        throws Exception
    {
        cl = new IsolatedClassLoader();

        for ( Iterator i = mbootDependencies.iterator(); i.hasNext(); )
        {
            String dependency = (String) i.next();

            File f = new File( repoLocal, dependency );
            if ( !f.exists() )
            {
                throw new FileNotFoundException( "Missing dependency: " + dependency + 
                    ( !online ? "; run again online" : "; there was a problem downloading it earlier" ) );
            }

            cl.addURL( f.toURL() );
        }
    }

    private void generatePluginDescriptor( String sourceDirectory, String outputDirectory, String pom )
        throws Exception
    {
        Class c = cl.loadClass( "org.apache.maven.plugin.generator.PluginDescriptorGenerator" );

        Object generator = c.newInstance();

        Method m = c.getMethod( "execute", new Class[]{String.class, String.class, String.class} );

        m.invoke( generator, new Object[]{sourceDirectory, outputDirectory, pom} );
    }

    private void generateSources( String model, String mode, String dir, String modelVersion, String packageWithVersion )
        throws Exception
    {
        IsolatedClassLoader modelloClassLoader = new IsolatedClassLoader();

        for ( Iterator i = Arrays.asList( modelloDeps ).iterator(); i.hasNext(); )
        {
            String dependency = (String) i.next();

            File f = new File( repoLocal, dependency );
            if ( !f.exists() )
            {
                throw new FileNotFoundException( "Missing dependency: " + dependency + 
                    ( !online ? "; run again online" : "; there was a problem downloading it earlier" ) );
            }

            modelloClassLoader.addURL( f.toURL() );
        }

        Class c = modelloClassLoader.loadClass( "org.codehaus.modello.ModelloCli" );

        Object generator = c.newInstance();

        Method m = c.getMethod( "main", new Class[]{String[].class} );

        String[] args = new String[]{model, mode, dir, modelVersion, packageWithVersion};

        ClassLoader old = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader( modelloClassLoader );

        m.invoke( generator, new Object[]{args} );

        Thread.currentThread().setContextClassLoader( old );
    }

    private void checkMBootDeps()
        throws Exception
    {
        System.out.println( "Checking for MBoot's dependencies ..." );

        downloader.downloadDependencies( mbootDependencies );
    }

    private void createJar( String classes, String buildDir )
        throws Exception
    {
        JarMojo jarMojo = new JarMojo();

        String artifactId = reader.artifactId;

        String version = reader.version;

        jarMojo.execute( new File( classes ), buildDir, artifactId + "-" + version );
    }

    private void installPomFile( String repoLocal, File pomIn )
        throws Exception
    {
        if ( !reader.parse( pomIn ) )
        {
            System.err.println( "Could not parse pom.xml" );

            System.exit( 1 );
        }

        String artifactId = reader.artifactId;

        String version = reader.version;

        String groupId = reader.groupId;

        File pom = new File( repoLocal, "/" + groupId + "/poms/" + artifactId + "-" + version + ".pom" );

        System.out.println( "Installing POM: " + pom );

        FileUtils.copyFile( pomIn, pom );

        reader.reset();
    }

    private void installPom( String basedir, String repoLocal )
        throws Exception
    {
        String artifactId = reader.artifactId;

        String version = reader.version;

        String groupId = reader.groupId;

        File pom = new File( repoLocal, "/" + groupId + "/poms/" + artifactId + "-" + version + ".pom" );

        System.out.println( "Installing POM: " + pom );

        FileUtils.copyFile( new File( basedir, "pom.xml" ), pom );
    }

    private void installJar( String basedir, String repoLocal )
        throws Exception
    {
        String artifactId = reader.artifactId;

        String version = reader.version;

        String groupId = reader.groupId;

        File jar = new File( repoLocal, "/" + groupId + "/jars/" + artifactId + "-" + version + ".jar" );

        System.out.println( "Installing JAR: " + jar );

        FileUtils.copyFile( new File( basedir, BUILD_DIR + "/" + artifactId + "-" + version + ".jar" ), jar );
    }

    private void installPlugin( String basedir, String repoLocal )
        throws Exception
    {
        String artifactId = reader.artifactId;

        String version = reader.version;

        String groupId = reader.groupId;

        File jar = new File( repoLocal, "/" + groupId + "/plugins/" + artifactId + "-" + version + ".jar" );

        System.out.println( "Installing Plugin: " + jar );

        FileUtils.copyFile( new File( basedir, BUILD_DIR + "/" + artifactId + "-" + version + ".jar" ), jar );
    }

    private void runTests( String basedir, String classes, String testClasses )
        throws Exception
    {
        SurefirePlugin testRunner = new SurefirePlugin();

        List includes;

        List excludes;

        if ( reader.getUnitTests() != null )
        {
            if ( reader.getUnitTests().getIncludes().size() != 0 )
            {
                includes = reader.getUnitTests().getIncludes();
            }
            else
            {
                includes = new ArrayList();

                includes.add( "**/*Test.java" );
            }

            excludes = reader.getUnitTests().getExcludes();
        }
        else
        {
            includes = new ArrayList();

            includes.add( "**/*Test.java" );

            excludes = new ArrayList();

            excludes.add( "**/*Abstract*.java" );
        }

        boolean success = testRunner.execute( repoLocal, basedir, classes, testClasses, includes, excludes, classpath( reader.getDependencies(), null ) );

        if ( !success )
        {
            throw new Exception ( "Tests error" );
        }
    }

    // ----------------------------------------------------------------------
    // Download dependencies
    // ----------------------------------------------------------------------

    private void downloadDependencies( List dependencies )
        throws Exception
    {
        List list = new ArrayList();

        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();

            list.add( getArtifactPath( d, "/" ) );
        }

        downloader.downloadDependencies( list );
    }

    // ----------------------------------------------------------------------
    // Compile
    // ----------------------------------------------------------------------

    private String[] classpath( List dependencies, String extraClasspath )
    {
        String classpath[] = new String[dependencies.size() + 1];

        for ( int i = 0; i < dependencies.size(); i++ )
        {
            Dependency d = (Dependency) dependencies.get( i );

            classpath[i] = repoLocal + "/" + getArtifactPath( d, "/" );
        }

        classpath[classpath.length - 1] = extraClasspath;

        return classpath;
    }

    private void compile( List dependencies,
                          String sourceDirectory,
                          String outputDirectory,
                          String extraClasspath,
                          String generatedSources )
        throws Exception
    {
        JavacCompiler compiler = new JavacCompiler();

        String[] sourceDirectories = null;

        if ( generatedSources != null )
        {
            // We might only have generated sources

            if ( new File( sourceDirectory ).exists() )
            {
                sourceDirectories = new String[]{sourceDirectory, generatedSources};
            }
            else
            {
                sourceDirectories = new String[]{generatedSources};
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
            List errors = compiler.compile( classpath( dependencies, extraClasspath ), sourceDirectories, outputDirectory );

            for ( Iterator i = errors.iterator(); i.hasNext(); )
            {
                System.out.println( i.next() );
            }

            if ( errors.size() > 0 )
            {
                throw new Exception( "Compilation error." );
            }
        }
    }

    // ----------------------------------------------------------------------
    // Resource copying
    // ----------------------------------------------------------------------

    private void copyResources( String sourceDirectory, String destinationDirectory )
        throws Exception
    {
        File sd = new File( sourceDirectory );

        if ( !sd.exists() )
        {
            return;
        }

        List files = FileUtils.getFiles( sd, "**/**", "**/CVS/**", false );

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

    private String getArtifactPath( Dependency d, String pathSeparator )
    {
        return d.getArtifactDirectory() + pathSeparator + "jars" + pathSeparator + d.getArtifact();
    }

    class ModelReader
        extends DefaultHandler
    {
        String artifactId;

        String version;

        String groupId;

        String type;

        private String parentGroupId;

        private String parentArtifactId;

        private String parentVersion;

        private List dependencies = new ArrayList();

        private List remoteRepositories = new ArrayList();

        private UnitTests unitTests;

        private Local local = new Local();

        private List resources = new ArrayList();

        private Dependency currentDependency;

        private Resource currentResource;

        private SAXParserFactory saxFactory;

        private boolean insideParent = false;

        private boolean insideDependency = false;

        private boolean insideLocal = false;

        private boolean insideUnitTest = false;

        private boolean insideResource = false;

        private boolean insideRepository = false;

        private StringBuffer bodyText = new StringBuffer();

        private File file;

        public void reset()
        {
            dependencies = new ArrayList();
        }

        public List getRemoteRepositories()
        {
            return remoteRepositories;
        }

        public List getDependencies()
        {
            return dependencies;
        }

        public UnitTests getUnitTests()
        {
            return unitTests;
        }

        public List getResources()
        {
            return resources;
        }

        public Local getLocal()
        {
            return local;
        }

        public boolean parse( File file )
        {
            this.file = file;

            try
            {
                saxFactory = SAXParserFactory.newInstance();

                SAXParser parser = saxFactory.newSAXParser();

                InputSource is = new InputSource( new FileInputStream( file ) );

                parser.parse( is, this );

                return true;
            }
            catch ( Exception e )
            {
                e.printStackTrace();

                return false;
            }
        }

        public void startElement( String uri, String localName, String rawName, Attributes attributes )
        {
            if ( rawName.equals( "parent" ) )
            {
                insideParent = true;
            }
            else if ( rawName.equals( "repository" ) && !insideLocal )
            {
                insideRepository = true;
            }
            else if ( rawName.equals( "local" ) )
            {
                insideLocal = true;
            }
            else if ( rawName.equals( "unitTest" ) )
            {
                unitTests = new UnitTests();

                insideUnitTest = true;
            }
            else if ( rawName.equals( "dependency" ) )
            {
                currentDependency = new Dependency();

                insideDependency = true;
            }
            else if ( rawName.equals( "resource" ) )
            {
                currentResource = new Resource();

                insideResource = true;
            }
        }

        public void characters( char buffer[], int start, int length )
        {
            bodyText.append( buffer, start, length );
        }

        private String getBodyText()
        {
            return bodyText.toString().trim();
        }

        public void endElement( String uri, String localName, String rawName )
            throws SAXException
        {
            // support both v3 <extend> and v4 <parent>
            if ( rawName.equals( "parent" ) )
            {
                File f;

                if ( parentArtifactId == null || parentArtifactId.trim().length() == 0 )
                {
                    throw new SAXException( "Missing required element in <parent>: artifactId." );
                }

                if ( parentGroupId == null || parentGroupId.trim().length() == 0 )
                {
                    throw new SAXException( "Missing required element in <parent>: groupId." );
                }

                if ( parentVersion == null || parentVersion.trim().length() == 0 )
                {
                    throw new SAXException( "Missing required element in <parent>: version." );
                }

                f = new File( downloader.getMavenRepoLocal(), parentGroupId + "/poms/" + parentArtifactId + "-" + parentVersion + ".pom" );

                ModelReader p = new ModelReader();

                if ( !p.parse( f ) )
                {
                    throw new SAXException( "Could not parse parent pom.xml" );
                }

                dependencies.addAll( p.getDependencies() );

                unitTests = p.getUnitTests();

                resources.addAll( p.getResources() );

                insideParent = false;
            }
            else if ( rawName.equals( "local" ) )
            {
                insideLocal = false;
            }
            else if ( rawName.equals( "unitTest" ) )
            {
                insideUnitTest = false;
            }
            else if ( rawName.equals( "dependency" ) )
            {
                dependencies.add( currentDependency );

                insideDependency = false;
            }
            else if ( rawName.equals( "resource" ) )
            {
                if ( insideUnitTest )
                {
                    unitTests.addResource( currentResource );
                }
                else
                {
                    resources.add( currentResource );
                }

                insideResource = false;
            }
            else if ( insideParent )
            {
                if ( rawName.equals( "groupId" ) )
                {
                    parentGroupId = getBodyText();
                }
                else if ( rawName.equals( "artifactId" ) )
                {
                    parentArtifactId = getBodyText();
                }
                else if ( rawName.equals( "version" ) )
                {
                    parentVersion = getBodyText();
                }
            }
            else if ( insideDependency )
            {
                if ( rawName.equals( "id" ) )
                {
                    currentDependency.setId( getBodyText() );
                }
                else if ( rawName.equals( "version" ) )
                {
                    currentDependency.setVersion( getBodyText() );
                }
                else if ( rawName.equals( "jar" ) )
                {
                    currentDependency.setJar( getBodyText() );
                }
                else if ( rawName.equals( "type" ) )
                {
                    currentDependency.setType( getBodyText() );
                }
                else if ( rawName.equals( "groupId" ) )
                {
                    currentDependency.setGroupId( getBodyText() );
                }
                else if ( rawName.equals( "artifactId" ) )
                {
                    currentDependency.setArtifactId( getBodyText() );
                }
            }
            else if ( insideResource )
            {
                if ( rawName.equals( "directory" ) )
                {
                    currentResource.setDirectory( getBodyText() );
                }
                else if ( rawName.equals( "targetPath" ) )
                {
                    currentResource.setTargetPath( getBodyText() );
                }
                else if ( rawName.equals( "include" ) )
                {
                    currentResource.addInclude( getBodyText() );
                }
                else if ( rawName.equals( "exclude" ) )
                {
                    currentResource.addExclude( getBodyText() );
                }
            }
            else if ( !insideResource && insideUnitTest )
            {
                if ( rawName.equals( "include" ) )
                {
                    unitTests.addInclude( getBodyText() );
                }
                else if ( rawName.equals( "exclude" ) )
                {
                    unitTests.addExclude( getBodyText() );
                }
            }
            else if ( rawName.equals( "artifactId" ) )
            {
                artifactId = getBodyText();
            }
            else if ( rawName.equals( "version" ) )
            {
                version = getBodyText();
            }
            else if ( rawName.equals( "groupId" ) )
            {
                groupId = getBodyText();
            }
            else if ( rawName.equals( "type" ) )
            {
                type = getBodyText();
            }
            else if ( rawName.equals( "repository" ) )
            {
                if ( insideLocal )
                {
                    local.repository = getBodyText();
                }
                else 
                {
                    insideRepository = false;
                }
            }
            else if ( insideRepository )
            {
                if ( rawName.equals( "url" ) )
                {
                    remoteRepositories.add( getBodyText() );
                }
            }

            bodyText = new StringBuffer();
        }

        public void warning( SAXParseException spe )
        {
            printParseError( "Warning", spe );
        }

        public void error( SAXParseException spe )
        {
            printParseError( "Error", spe );
        }

        public void fatalError( SAXParseException spe )
        {
            printParseError( "Fatal Error", spe );
        }

        private final void printParseError( String type, SAXParseException spe )
        {
            System.err.println( type + " [line " + spe.getLineNumber() +
                                ", row " + spe.getColumnNumber() + "]: " +
                                spe.getMessage() );
        }
    }

    public static class Dependency
    {
        private String id;

        private String version;

        private String url;

        private String jar;

        private String artifactId;

        private String groupId;

        private String type = "jar";

        public Dependency()
        {
        }

        public void setId( String id )
        {
            this.id = id;
        }

        public String getId()
        {
            if ( isValid( getGroupId() )
                && isValid( getArtifactId() ) )
            {
                return getGroupId() + ":" + getArtifactId();
            }

            return id;
        }

        public void setGroupId( String groupId )
        {
            this.groupId = groupId;
        }

        public String getGroupId()
        {
            return groupId;
        }

        public String getArtifactDirectory()
        {
            if ( isValid( getGroupId() ) )
            {
                return getGroupId();
            }

            return getId();
        }

        public String getArtifactId()
        {
            return artifactId;
        }

        public void setArtifactId( String artifactId )
        {
            this.artifactId = artifactId;
        }

        public String getArtifact()
        {
            // If the jar name has been explicty set then use that. This
            // is when the <jar/> element is explicity used in the POM.
            if ( jar != null )
            {
                return jar;
            }

            if ( isValid( getArtifactId() ) )
            {
                return getArtifactId() + "-" + getVersion() + "." + getType();
            }
            else
            {
                return getId() + "-" + getVersion() + "." + getType();
            }
        }

        public void setVersion( String version )
        {
            this.version = version;
        }

        public String getVersion()
        {
            return version;
        }

        public void setJar( String jar )
        {
            // This is a check we need because of the jelly interpolation
            // process. If we don't check an empty string will be set and
            // screw up getArtifact() above.
            if ( jar.trim().length() == 0 )
            {
                return;
            }

            this.jar = jar;
        }

        public String getJar()
        {
            return jar;
        }

        public void setUrl( String url )
        {
            this.url = url;
        }

        public String getUrl()
        {
            return url;
        }

        public String getType()
        {
            return type;
        }

        public void setType( String type )
        {
            this.type = type;
        }

        private boolean isValid( String value )
        {
            if ( value != null
                && value.trim().equals( "" ) == false )
            {
                return true;
            }

            return false;
        }
    }

    public static class UnitTests
        implements Serializable
    {
        private String directory;

        private List unitTestIncludes = new ArrayList();

        private List unitTestExcludes = new ArrayList();

        private List unitTestResources = new ArrayList();

        public void addInclude( String pattern )
        {
            unitTestIncludes.add( pattern );
        }

        public void addExclude( String pattern )
        {
            unitTestExcludes.add( pattern );
        }

        public void addResource( Resource resource )
        {
            unitTestResources.add( resource );
        }

        public List getIncludes()
        {
            return unitTestIncludes;
        }

        public List getExcludes()
        {
            return unitTestExcludes;
        }

        public List getResources()
        {
            return unitTestResources;
        }

        public void setDirectory( String directory )
        {
            this.directory = directory;
        }

        public String getDirectory()
        {
            return this.directory;
        }
    }

    public static class Resource
        implements Serializable
    {
        private String directory;

        private String targetPath;

        private List includes = new ArrayList();

        private List excludes = new ArrayList();

        private boolean filtering;

        public void addInclude( String pattern )
        {
            this.includes.add( pattern );
        }

        public void addExclude( String pattern )
        {
            this.excludes.add( pattern );
        }

        public List getIncludes()
        {
            return this.includes;
        }

        public List getExcludes()
        {
            return this.excludes;
        }

        public void setDirectory( String directory )
        {
            this.directory = directory;
        }

        public String getDirectory()
        {
            return this.directory;
        }

        public void setTargetPath( String targetPath )
        {
            this.targetPath = targetPath;
        }

        public String getTargetPath()
        {
            return targetPath;
        }

        public boolean getFiltering()
        {
            return filtering;
        }

        public void setFiltering( boolean filtering )
        {
            this.filtering = filtering;
        }
    }

    public static class Local
        implements Serializable
    {
        private String repository;

        private String online;

        public String getRepository()
        {
            return this.repository;
        }

        public void setRepository( String repository )
        {
            this.repository = repository;
        }   
    
        public String getOnline()
        {
            return this.online;
        }

        public void setOnline( String online )
        {
            this.online = online;
        }   
    }
}
