
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public class Bootstrapper
{
    public static final String SNAPSHOT_SIGNATURE = "-SNAPSHOT";

    private BootstrapPomParser bootstrapPomParser;

    private List dependencies;

    private List resources;

    private File mavenRepoLocal;

    private boolean useTimestamp = true;

    private boolean ignoreErrors = true;

    private List remoteRepos = new ArrayList();

    private String proxyHost;

    private String proxyPort;

    private String proxyUserName;

    private String proxyPassword;

    public static void main( String[] args )
        throws Exception
    {
        Bootstrapper bootstrapper = new Bootstrapper();

        bootstrapper.execute( args );
    }

    public void execute( String[] args )
        throws Exception
    {
        String basedir = args[0];

        Properties properties = loadProperties( new File( System.getProperty( "user.home" ), "build.properties" ) );

        setRemoteRepo( properties.getProperty( "maven.repo.remote" ) );

        String mavenRepoLocalProperty = properties.getProperty( "maven.repo.local" );

        if ( mavenRepoLocalProperty == null )
        {
            mavenRepoLocalProperty = System.getProperty( "user.home" ) + "/.maven/repository";
        }

        mavenRepoLocal = new File( mavenRepoLocalProperty );

        if ( !mavenRepoLocal.exists() )
        {
            if ( !mavenRepoLocal.mkdirs() )
            {
                System.err.println( "Cannot create the specified maven.repo.local: " + mavenRepoLocal );

                System.exit( 1 );
            }
        }

        if ( !mavenRepoLocal.canWrite() )
        {
            System.err.println( "Can't write to " + mavenRepoLocal.getAbsolutePath() );

            System.exit( 1 );
        }

        System.out.println( "Using the following for your maven.repo.local: " + mavenRepoLocal );

        bootstrapPomParser = new BootstrapPomParser();

        bootstrapPomParser.parse( new File( "project.xml" ) );

        dependencies = bootstrapPomParser.getDependencies();

        List list = new ArrayList();

        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();

            list.add( getArtifactPath( d, "/" ) );
        }

        downloadDependencies( list );

        StringBuffer classPath = new StringBuffer();

        StringBuffer libs = new StringBuffer();

        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();

            classPath.append( mavenRepoLocal + "/" + getArtifactPath( d, "/" ) + ":" );

            libs.append( mavenRepoLocal + "/" + getArtifactPath( d, "/" ) + "\n" );
        }

        writeFile( "bootstrap.classpath", classPath.toString() );

        writeFile( "bootstrap.libs", libs.toString() );

        resources = bootstrapPomParser.getResources();

        StringBuffer res = new StringBuffer();

        for ( Iterator i = resources.iterator(); i.hasNext(); )
        {
            Resource r = (Resource) i.next();

            // Not sure why r would be null. Happening in drools-core.
            if ( r == null )
            {
                continue;
            }

            res.append( r.getDirectory() );

            if ( r.getTargetPath() != null )
            {
                res.append( "," ).append( r.getTargetPath() );
            }

            res.append( "@" );

            int size = r.getIncludes().size();

            // If there are no includes specified then we want it all.
            if ( size == 0 )
            {
                res.append( "'*'" );
            }

            for ( int j = 0; j < size; j++ )
            {
                String include = (String) r.getIncludes().get( j );

                if ( include.startsWith( "**/" ) )
                {
                    include = include.substring( 3 );
                }

                res.append( "'" ).append( include ).append( "'" );

                if ( j != size - 1 )
                {
                    res.append( "," );
                }
            }

            res.append( "\n" );
        }

        writeFile( "bootstrap.resources", res.toString() );

        writeFile( "bootstrap.repo", mavenRepoLocal.getPath() );
    }

    private void writeFile( String name, String contents )
        throws Exception
    {
        Writer writer = new FileWriter( name );

        writer.write( contents );

        writer.close();
    }

    private String getArtifactPath( Dependency d, String pathSeparator )
    {
        return d.getArtifactDirectory() + pathSeparator + "jars" + pathSeparator + d.getArtifact();
    }

    private void downloadDependencies( List files )
        throws Exception
    {
        for ( Iterator j = files.iterator(); j.hasNext(); )
        {
            try
            {
                String file = (String) j.next();

                File destinationFile = new File( mavenRepoLocal, file );

                // The directory structure for this project may
                // not exists so create it if missing.
                File directory = destinationFile.getParentFile();

                if ( directory.exists() == false )
                {
                    directory.mkdirs();
                }

                if ( destinationFile.exists() && !file.endsWith( SNAPSHOT_SIGNATURE ) )
                {
                    continue;
                }

                log( "Downloading dependency: " + file );

                getRemoteArtifact( file, destinationFile );

                if ( !destinationFile.exists() )
                {
                    throw new Exception( "Failed to download " + file );
                }
            }
            catch ( Exception e )
            {
                throw new Exception( e );
            }
        }
    }

    private void setRemoteRepo( String repos )
    {
        remoteRepos = new ArrayList();

        if ( repos == null )
        {
            remoteRepos.add( "http://www.ibiblio.org/maven/" );
            return;
        }

        StringTokenizer st = new StringTokenizer( repos, "," );
        while ( st.hasMoreTokens() )
        {
            remoteRepos.add( st.nextToken().trim() );
        }
    }

    private List getRemoteRepo()
    {
        return remoteRepos;
    }

    private boolean getRemoteArtifact( String file, File destinationFile )
    {
        boolean fileFound = false;

        for ( Iterator i = getRemoteRepo().iterator(); i.hasNext(); )
        {
            String remoteRepo = (String) i.next();

            // The username and password parameters are not being
            // used here. Those are the "" parameters you see below.
            String url = remoteRepo + "/" + file;

            if ( !url.startsWith( "file" ) )
            {
                url = replace( url, "//", "/" );
                if ( url.startsWith( "https" ) )
                {
                    url = replace( url, "https:/", "https://" );
                }
                else
                {
                    url = replace( url, "http:/", "http://" );
                }
            }

            // Attempt to retrieve the artifact and set the checksum if retrieval
            // of the checksum file was successful.
            try
            {
                HttpUtils.getFile( url,
                                   destinationFile,
                                   ignoreErrors,
                                   useTimestamp,
                                   proxyHost,
                                   proxyPort,
                                   proxyUserName,
                                   proxyPassword,
                                   true );

                // Artifact was found, continue checking additional remote repos (if any)
                // in case there is a newer version (i.e. snapshots) in another repo
                fileFound = true;
            }
            catch ( FileNotFoundException e )
            {
                // Ignore
            }
            catch ( Exception e )
            {
                // If there are additional remote repos, then ignore exception
                // as artifact may be found in another remote repo. If there
                // are no more remote repos to check and the artifact wasn't found in
                // a previous remote repo, then artifactFound is false indicating
                // that the artifact could not be found in any of the remote repos
                //
                // arguably, we need to give the user better control (another command-
                // line switch perhaps) of what to do in this case? Maven already has
                // a command-line switch to work in offline mode, but what about when
                // one of two or more remote repos is unavailable? There may be multiple
                // remote repos for redundancy, in which case you probably want the build
                // to continue. There may however be multiple remote repos because some
                // artifacts are on one, and some are on another. In this case, you may
                // want the build to break.
                //
                // print a warning, in any case, so user catches on to mistyped
                // hostnames, or other snafus
                log( "Error retrieving artifact from [" + url + "]: " );
            }
        }

        return fileFound;
    }

    /**
     * <p>Replaces all occurrences of a String within another String.</p>
     *
     * This methods comes from Commons Lang
     *
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     *
     * <pre>
     * StringUtils.replace(null, *, *)        = null
     * StringUtils.replace("", *, *)          = ""
     * StringUtils.replace("any", null, *)    = "any"
     * StringUtils.replace("any", *, null)    = "any"
     * StringUtils.replace("any", "", *)      = "any"
     * StringUtils.replace("aba", "a", null)  = "aba"
     * StringUtils.replace("aba", "a", "")    = "b"
     * StringUtils.replace("aba", "a", "z")   = "zbz"
     * </pre>
     *
     * @param text  text to search and replace in, may be null
     * @param repl  the String to search for, may be null
     * @param with  the String to replace with, may be null
     * @return the text with any replacements processed,
     *  <code>null</code> if null String input
     */
    private String replace( String text, String repl, String with )
    {
        StringBuffer buf = new StringBuffer( text.length() );
        int start = 0, end = 0;
        while ( ( end = text.indexOf( repl, start ) ) != -1 )
        {
            buf.append( text.substring( start, end ) ).append( with );
            start = end + repl.length();
        }
        buf.append( text.substring( start ) );
        return buf.toString();
    }

    private void log( String message )
    {
        System.out.println( message );
    }

    private Properties loadProperties( File file )
    {
        try
        {
            return loadProperties( new FileInputStream( file ) );
        }
        catch ( Exception e )
        {
            // ignore
        }

        return new Properties();
    }

    private static Properties loadProperties( InputStream is )
    {
        Properties properties = new Properties();

        try
        {
            if ( is != null )
            {
                properties.load( is );
            }
        }
        catch ( IOException e )
        {
            // ignore
        }
        finally
        {
            try
            {
                if ( is != null )
                {
                    is.close();
                }
            }
            catch ( IOException e )
            {
                // ignore
            }
        }

        return properties;
    }

    static class BootstrapPomParser
        extends DefaultHandler
    {
        private List dependencies = new ArrayList();

        private List resources = new ArrayList();

        private Dependency currentDependency;

        private Resource currentResource;

        private static SAXParserFactory saxFactory;

        private boolean insideDependency = false;

        private boolean insideUnitTest = false;

        private boolean insideResource = false;

        private StringBuffer bodyText = new StringBuffer();

        private File file;

        public List getDependencies()
        {
            return dependencies;
        }

        public List getResources()
        {
            return resources;
        }

        public void parse( File file )
        {
            this.file = file;

            try
            {
                saxFactory = SAXParserFactory.newInstance();

                SAXParser parser = saxFactory.newSAXParser();

                InputSource is = new InputSource( new FileInputStream( file ) );

                parser.parse( is, this );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }

        public void startElement( String uri, String localName, String rawName, Attributes attributes )
        {
            if ( insideUnitTest )
            {
                return;
            }
            else if ( rawName.equals( "unitTest" ) )
            {
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
        {
            if ( rawName.equals( "extend" ) )
            {
                String extend = getBodyText();

                File f = new File( file.getParentFile(), extend );

                BootstrapPomParser p = new BootstrapPomParser();

                p.parse( f );

                dependencies.addAll( p.getDependencies() );

                resources.addAll( p.getResources() );
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
                resources.add( currentResource );

                insideResource = false;
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
                // We have something like:
                //
                // <dependency>
                //   <groupId>commons-jelly</groupId>
                //   <artifactId>commons-jelly-tags-velocity</artifactId>
                //   <version>SNAPSHOT</version>
                //  </dependency>

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
}
