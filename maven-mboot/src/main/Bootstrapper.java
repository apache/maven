import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Bootstrapper
{
    private ArtifactDownloader downloader;

    private BootstrapPomParser bootstrapPomParser;

    private int pomVersion;

    private List dependencies;

    private UnitTests unitTests;

    private Properties properties;

    public static void main( String[] args )
        throws Exception
    {
        Bootstrapper bootstrapper = new Bootstrapper();

        bootstrapper.execute( args );
    }

    public void execute( String[] args )
        throws Exception
    {
        properties = loadProperties( new File( System.getProperty( "user.home" ), "maven.properties" ) );

        downloader = new ArtifactDownloader( properties );

        bootstrapPomParser = new BootstrapPomParser();

        if( ! bootstrapPomParser.parse( new File( "pom.xml" ) ) )
        {
            System.err.println( "Could not parse pom.xml" );
            System.exit( 1 );
        }

        dependencies = bootstrapPomParser.getDependencies();

        downloadDependencies();

        writeClasspath();

        writeUnitTest();

        if ( bootstrapPomParser.getResources().size() == 0 )
        {
            writeFile( "bootstrap.resources", "src/main/resources@'*'" );
        }
        else
        {
            writeResources( bootstrapPomParser.getResources(), "bootstrap.resources" );
        }

        writeFile( "bootstrap.repo", downloader.getMavenRepoLocal().getPath() );
    }

    private void downloadDependencies()
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

    private void writeClasspath()
        throws Exception
    {
        StringBuffer classPath = new StringBuffer();

        StringBuffer libs = new StringBuffer();

        StringBuffer deps = new StringBuffer();

        String repoLocal = replace( downloader.getMavenRepoLocal().getPath(), "\\", "/" );
        String classpathSeparator;
        if ( repoLocal.indexOf( ":" ) != -1 ) //Windows
        {
            classpathSeparator = ";";
        }
        else
        {
            classpathSeparator = ":";
        }

        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();

            classPath.append( repoLocal + "/" + getArtifactPath( d, "/" ) + classpathSeparator );

            libs.append( repoLocal + "/" + getArtifactPath( d, "/" ) + "\n" );

            deps.append( getArtifactPath( d, "/" ) + "\n" );
        }

        writeFile( "bootstrap.classpath", classPath.toString() );

        writeFile( "bootstrap.libs", libs.toString() );

        writeFile( "bootstrap.deps", deps.toString() );
    }

    private void writeUnitTest()
        throws Exception
    {
        int size;

        unitTests = bootstrapPomParser.getUnitTests();

        if ( unitTests != null )
        {
            StringBuffer tests = new StringBuffer();

            tests.append( "target/test-classes" );

            tests.append( "@" );

            size = unitTests.getIncludes().size();

            // If there are no unitTestIncludes specified then we want it all.
            if ( size == 0 )
            {
                tests.append( "'*Test.java'" );
            }

            for ( int j = 0; j < size; j++ )
            {
                String include = (String) unitTests.getIncludes().get( j );

                tests.append( include );

                if ( j != size - 1 )
                {
                    tests.append( "," );
                }
            }

            tests.append( "\n" );

            writeFile( "bootstrap.tests.includes", tests.toString() );

            tests = new StringBuffer();

            tests.append( "target/test-classes" );

            tests.append( "@" );

            size = unitTests.getExcludes().size();

            if ( size == 0 )
            {
                tests.append( "*Abstract*.java'" );
            }

            for ( int j = 0; j < size; j++ )
            {
                String exclude = (String) unitTests.getExcludes().get( j );

                tests.append( exclude );

                if ( j != size - 1 )
                {
                    tests.append( "," );
                }
            }

            tests.append( "\n" );

            writeFile( "bootstrap.tests.excludes", tests.toString() );

            writeResources( unitTests.getResources(), "bootstrap.tests.resources" );
        }
        else
        {
            writeFile( "bootstrap.tests.includes", "target/test-classes@**/*Test.java" );

            writeFile( "bootstrap.tests.excludes", "target/test-classes@**/*Abstract*.java" );

            writeFile( "bootstrap.tests.resources", "src/test/resources@'*'" );
        }
    }

    private void writeResources( List resources, String file )
        throws Exception
    {
        StringBuffer res = new StringBuffer();

        int size;

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

            size = r.getIncludes().size();

            // If there are no unitTestIncludes specified then we want it all.
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

        writeFile( file, res.toString() );
    }

    private void writeFile( String name, String contents )
        throws Exception
    {
        Writer writer = new FileWriter( name );

        writer.write( contents );

        writer.flush();

        writer.close();
    }

    private String getArtifactPath( Dependency d, String pathSeparator )
    {
        return d.getArtifactDirectory() + pathSeparator + "jars" + pathSeparator + d.getArtifact();
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

    private String interpolate( String text, Map namespace )
    {
        Iterator keys = namespace.keySet().iterator();

        while ( keys.hasNext() )
        {
            String key = keys.next().toString();

            Object obj = namespace.get( key );

            String value = obj.toString();

            text = replace( text, "${" + key + "}", value );

            if ( key.indexOf( " " ) == -1 )
            {
                text = replace( text, "$" + key, value );
            }
        }
        return text;
    }

    private String replace( String text, String repl, String with )
    {
        return replace( text, repl, with, -1 );
    }

    private String replace( String text, String repl, String with, int max )
    {
        if ( text == null || repl == null || with == null || repl.length() == 0 )
        {
            return text;
        }

        StringBuffer buf = new StringBuffer( text.length() );
        int start = 0, end = 0;
        while ( ( end = text.indexOf( repl, start ) ) != -1 )
        {
            buf.append( text.substring( start, end ) ).append( with );
            start = end + repl.length();

            if ( --max == 0 )
            {
                break;
            }
        }
        buf.append( text.substring( start ) );
        return buf.toString();
    }

    class BootstrapPomParser
        extends DefaultHandler
    {
        private String parentGroupId;

        private String parentArtifactId;

        private String parentVersion;

        private List dependencies = new ArrayList();

        private UnitTests unitTests;

        private List resources = new ArrayList();

        private Dependency currentDependency;

        private Resource currentResource;

        private SAXParserFactory saxFactory;

        private boolean insideParent = false;

        private boolean insideDependency = false;

        private boolean insideUnitTest = false;

        private boolean insideResource = false;

        private StringBuffer bodyText = new StringBuffer();

        private File file;

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
            if ( rawName.equals( "extend" ) || rawName.equals( "parent" ) )
            {
                File f;

                if( rawName.equals( "extend" ) )
                {
                    String extend = interpolate( getBodyText(), properties );

                    f = new File( file.getParentFile(), extend );
                }
                else
                {
                    if ( parentArtifactId == null || parentArtifactId.trim().length() == 0 )
                        throw new SAXException( "Missing required element in <parent>: artifactId." );

                    if ( parentGroupId == null || parentGroupId.trim().length() == 0 )
                        throw new SAXException( "Missing required element in <parent>: groupId." );

                    if ( parentVersion == null || parentVersion.trim().length() == 0 )
                        throw new SAXException( "Missing required element in <parent>: version." );

                    f = new File( downloader.getMavenRepoLocal(), parentGroupId + "/poms/" + parentArtifactId + "-" + parentVersion + ".pom" );
                }

                BootstrapPomParser p = new BootstrapPomParser();

                if ( ! p.parse( f ) )
                    throw new SAXException( "Could not parse parent pom.xml" );

                dependencies.addAll( p.getDependencies() );

                unitTests = p.getUnitTests();

                resources.addAll( p.getResources() );

                insideParent = false;
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
}
