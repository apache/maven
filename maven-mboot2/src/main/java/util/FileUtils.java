package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FileUtils
{
    public static final int ONE_KB = 1024;

    public static final int ONE_MB = ONE_KB * ONE_KB;

    public static final int ONE_GB = ONE_KB * ONE_MB;

    public static String extension( String filename )
    {
        int lastDot = filename.lastIndexOf( '.' );

        if ( lastDot >= 0 )
        {
            return filename.substring( lastDot + 1 );
        }
        else
        {
            return "";
        }
    }

    public static void mkdir( String dir )
    {
        File file = new File( dir );
        if ( !file.exists() )
        {
            file.mkdirs();
        }
    }

    public static void copyFileToDirectory( final String source,
                                            final String destinationDirectory )
        throws IOException
    {
        copyFileToDirectory( new File( source ),
                             new File( destinationDirectory ) );
    }

    public static void copyFileToDirectory( final File source,
                                            final File destinationDirectory )
        throws IOException
    {
        if ( destinationDirectory.exists() && !destinationDirectory.isDirectory() )
        {
            throw new IllegalArgumentException( "Destination is not a directory" );
        }

        copyFile( source, new File( destinationDirectory, source.getName() ) );
    }

    public static void copyFile( final File source, final File destination )
        throws IOException
    {
        //check source exists
        if ( !source.exists() )
        {
            final String message = "File " + source + " does not exist";
            throw new IOException( message );
        }

        //does destinations directory exist ?
        if ( destination.getParentFile() != null &&
            !destination.getParentFile().exists() )
        {
            destination.getParentFile().mkdirs();
        }

        //make sure we can write to destination
        if ( destination.exists() && !destination.canWrite() )
        {
            final String message = "Unable to open file " +
                destination + " for writing.";
            throw new IOException( message );
        }

        final FileInputStream input = new FileInputStream( source );
        final FileOutputStream output = new FileOutputStream( destination );
        IOUtil.copy( input, output );

        input.close();
        output.close();

        if ( source.length() != destination.length() )
        {
            final String message = "Failed to copy full contents from " + source +
                " to " + destination;
            throw new IOException( message );
        }
    }

    public static void forceDelete( final String file )
        throws IOException
    {
        forceDelete( new File( file ) );
    }

    public static void forceDelete( final File file )
        throws IOException
    {
        if ( ! file.exists() )
        {
            return;
        }

        if ( file.isDirectory() )
        {
            deleteDirectory( file );
        }
        else
        {
            if ( !file.delete() )
            {
                final String message =
                    "File " + file + " unable to be deleted.";
                throw new IOException( message );
            }
        }
    }

    public static void forceDeleteOnExit( final File file )
        throws IOException
    {
        if ( ! file.exists() )
        {
            return;
        }

        if ( file.isDirectory() )
        {
            deleteDirectoryOnExit( file );
        }
        else
        {
            file.deleteOnExit();
        }
    }

    private static void deleteDirectoryOnExit( final File directory )
        throws IOException
    {
        if ( !directory.exists() )
        {
            return;
        }

        cleanDirectoryOnExit( directory );
        directory.deleteOnExit();
    }

    private static void cleanDirectoryOnExit( final File directory )
        throws IOException
    {
        if ( !directory.exists() )
        {
            final String message = directory + " does not exist";
            throw new IllegalArgumentException( message );
        }

        if ( !directory.isDirectory() )
        {
            final String message = directory + " is not a directory";
            throw new IllegalArgumentException( message );
        }

        IOException exception = null;

        final File[] files = directory.listFiles();
        for ( int i = 0; i < files.length; i++ )
        {
            final File file = files[i];
            try
            {
                forceDeleteOnExit( file );
            }
            catch ( final IOException ioe )
            {
                exception = ioe;
            }
        }

        if ( null != exception )
        {
            throw exception;
        }
    }

    public static void deleteDirectory( final File directory )
        throws IOException
    {
        if ( !directory.exists() )
        {
            return;
        }

        cleanDirectory( directory );
        if ( !directory.delete() )
        {
            final String message =
                "Directory " + directory + " unable to be deleted.";
            throw new IOException( message );
        }
    }

    public static void cleanDirectory( final File directory )
        throws IOException
    {
        if ( !directory.exists() )
        {
            final String message = directory + " does not exist";
            throw new IllegalArgumentException( message );
        }

        if ( !directory.isDirectory() )
        {
            final String message = directory + " is not a directory";
            throw new IllegalArgumentException( message );
        }

        IOException exception = null;

        final File[] files = directory.listFiles();
        for ( int i = 0; i < files.length; i++ )
        {
            final File file = files[i];
            try
            {
                forceDelete( file );
            }
            catch ( final IOException ioe )
            {
                exception = ioe;
            }
        }

        if ( null != exception )
        {
            throw exception;
        }
    }

    public static List getFiles( File directory, String includes, String excludes )
        throws IOException
    {
        return getFiles( directory, includes, excludes, true );
    }

    public static List getFiles( File directory, String includes, String excludes, boolean includeBasedir )
        throws IOException
    {
        List fileNames = getFileNames( directory, includes, excludes, includeBasedir );

        List files = new ArrayList();

        for ( Iterator i = fileNames.iterator(); i.hasNext(); )
        {
            files.add( new File( (String) i.next() ) );
        }

        return files;
    }

    public static String FS = System.getProperty( "file.separator" );

    public static List getFileNames( File directory, String includes, String excludes, boolean includeBasedir )
        throws IOException
    {
        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir( directory );

        if ( includes != null )
        {
            scanner.setIncludes( StringUtils.split( includes, "," ) );
        }

        if ( excludes != null )
        {
            scanner.setExcludes( StringUtils.split( excludes, "," ) );
        }

        scanner.scan();

        String[] files = scanner.getIncludedFiles();

        List list = new ArrayList();

        for ( int i = 0; i < files.length; i++ )
        {
            if ( includeBasedir )
            {
                list.add( directory + FS + files[i] );
            }
            else
            {
                list.add( files[i] );
            }
        }

        return list;
    }

    public static void copyDirectory( File sourceDirectory, File destinationDirectory )
        throws IOException
    {
        if ( !sourceDirectory.exists() )
        {
            return;
        }

        List files = getFiles( sourceDirectory, "**", null );

        for ( Iterator i = files.iterator(); i.hasNext(); )
        {
            File file = (File) i.next();

            copyFileToDirectory( file, destinationDirectory );
        }
    }
}
