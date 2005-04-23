package jar;

import util.DirectoryScanner;
import util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class JarMojo
{
    private byte[] buffer = new byte[4096];

    public void execute( File basedir, String outputDirectory, String jarName )
        throws Exception
    {
        File jarFile = new File( new File( outputDirectory ), jarName + ".jar" );

        Map includes = new LinkedHashMap();

        addDirectory( includes, "**/**", "**/package.html,**/.svn/**", "", basedir );

        createJar( jarFile, includes );
    }

    /**
     * Add all files in the specified directory to the archive.
     *
     * @param includes a map <String, File> of items to be include in the outpur
     * @param baseDir  the directory to add
     */
    protected void addDirectory( Map includes, File baseDir ) throws IOException
    {
        addDirectory( includes, "", baseDir );
    }

    /**
     * Add all files in the specified directory to the archive.
     *
     * @param includes a map <String, File> of items to be include in the outpur
     * @param prefix   value to be added to the front of jar entry names
     * @param baseDir  the directory to add
     */
    protected void addDirectory( Map includes, String prefix, File baseDir ) throws IOException
    {
        addDirectory( includes, null, null, prefix, baseDir );
    }

    /**
     * Add all files in the specified directory to the archive.
     *
     * @param includes        a map <String, File> of items to be include in the outpur
     * @param includesPattern Sets the list of include patterns to use
     * @param excludesPattern Sets the list of exclude patterns to use
     * @param prefix          value to be added to the front of jar entry names
     * @param baseDir         the directory to add
     */
    protected void addDirectory( Map includes, String includesPattern, String excludesPattern, String prefix, File baseDir )
        throws IOException
    {
        if ( !baseDir.exists() )
        {
            return;
        }

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( baseDir );
        if ( includesPattern != null )
        {
            scanner.setIncludes( StringUtils.split( includesPattern, "," ) );
        }

        if ( excludesPattern != null )
        {
            scanner.setExcludes( StringUtils.split( excludesPattern, "," ) );
        }
        scanner.scan();
        String[] files = scanner.getIncludedFiles();
        for ( int i = 0; i < files.length; i++ )
        {
            String file = files[i];
            file = file.replace( '\\', '/' ); // todo shouldn't the scanner return platform independent names?
            includes.put( prefix + file, new File( baseDir, file ) );
        }
    }

    /**
     * Create the jar file specified and include the listed files.
     *
     * @param jarFile  the jar file to create
     * @param includes a Map<String, File>of items to include; the key is the jar entry name
     * @throws IOException if there is a problem writing the archive or reading the sources
     */
    protected void createJar( File jarFile, Map includes ) throws IOException
    {
        File parentJarFile = jarFile.getParentFile();
        if ( !parentJarFile.exists() )
        {
            parentJarFile.mkdirs();
        }
        JarOutputStream jos = createJar( jarFile, createManifest() );
        try
        {
            addEntries( jos, includes );
        }
        finally
        {
            jos.close();
        }
    }

    /**
     * Create a manifest for the jar file
     *
     * @return a default manifest; the Manifest-Version and Created-By attributes are initialized
     */
    protected Manifest createManifest()
    {
        Manifest mf = new Manifest();
        Attributes attrs = mf.getMainAttributes();
        attrs.putValue( Attributes.Name.MANIFEST_VERSION.toString(), "1.0" );
        attrs.putValue( "Created-By", "2.0 (Apache Maven)" );
        return mf;
    }

    /**
     * Create the specified jar file and return a JarOutputStream to it
     *
     * @param jarFile the jar file to create
     * @param mf      the manifest to use
     * @return a JarOutputStream that can be used to write to that file
     * @throws IOException if there was a problem opening the file
     */
    protected JarOutputStream createJar( File jarFile, Manifest mf ) throws IOException
    {
        jarFile.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream( jarFile );
        try
        {
            return new JarOutputStream( fos, mf );
        }
        catch ( IOException e )
        {
            try
            {
                fos.close();
                jarFile.delete();
            }
            catch ( IOException e1 )
            {
                // ignore
            }
            throw e;
        }
    }

    /**
     * Add all entries in the supplied Map to the jar
     *
     * @param jos      a JarOutputStream that can be used to write to the jar
     * @param includes a Map<String, File> of entries to add
     * @throws IOException if there is a problem writing the archive or reading the sources
     */
    protected void addEntries( JarOutputStream jos, Map includes ) throws IOException
    {
        for ( Iterator i = includes.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();
            String name = (String) entry.getKey();
            File file = (File) entry.getValue();
            addEntry( jos, name, file );
        }
    }

    /**
     * Add a single entry to the jar
     *
     * @param jos    a JarOutputStream that can be used to write to the jar
     * @param name   the entry name to use; must be '/' delimited
     * @param source the file to add
     * @throws IOException if there is a problem writing the archive or reading the sources
     */
    protected void addEntry( JarOutputStream jos, String name, File source ) throws IOException
    {
        FileInputStream fis = new FileInputStream( source );
        try
        {
            jos.putNextEntry( new JarEntry( name ) );
            int count;
            while ( ( count = fis.read( buffer ) ) > 0 )
            {
                jos.write( buffer, 0, count );
            }
            jos.closeEntry();
        }
        finally
        {
            fis.close();
        }
    }

}
