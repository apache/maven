package util;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

/**
 * This class provides basic facilities for manipulating files and file paths.
 * <p/>
 * <h3>Path-related methods</h3>
 * <p/>
 * <p>Methods exist to retrieve the components of a typical file path. For example
 * <code>/www/hosted/mysite/index.html</code>, can be broken into:
 * <ul>
 * <li><code>/www/hosted/mysite/</code> -- retrievable through {@link #getPath}</li>
 * <li><code>index.html</code> -- retrievable through {@link #removePath}</li>
 * <li><code>/www/hosted/mysite/index</code> -- retrievable through {@link #removeExtension}</li>
 * <li><code>html</code> -- retrievable through {@link #getExtension}</li>
 * </ul>
 * There are also methods to {@link #catPath concatenate two paths}, {@link #resolveFile resolve a
 * path relative to a File} and {@link #normalize} a path.
 * </p>
 * <p/>
 * <h3>File-related methods</h3>
 * <p/>
 * There are methods to  create a {@link #toFile File from a URL}, copy a
 * {@link #copyFileToDirectory File to a directory},
 * copy a {@link #copyFile File to another File},
 * copy a {@link #copyURLToFile URL's contents to a File},
 * as well as methods to {@link #deleteDirectory(File) delete} and {@link #cleanDirectory(File)
 * clean} a directory.
 * </p>
 * <p/>
 * Common {@link java.io.File} manipulation routines.
 * <p/>
 * Taken from the commons-utils repo.
 * Also code from Alexandria's FileUtils.
 * And from Avalon Excalibur's IO.
 * And from Ant.
 *
 * @author <a href="mailto:burton@relativity.yi.org">Kevin A. Burton</A>
 * @author <a href="mailto:sanders@codehaus.org">Scott Sanders</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author <a href="mailto:Christoph.Reck@dlr.de">Christoph.Reck</a>
 * @author <a href="mailto:peter@codehaus.org">Peter Donald</a>
 * @author <a href="mailto:jefft@codehaus.org">Jeff Turner</a>
 * @version $Id$
 */
public class FileUtils
{
    /**
     * The number of bytes in a kilobyte.
     */
    public static final int ONE_KB = 1024;

    /**
     * The number of bytes in a megabyte.
     */
    public static final int ONE_MB = ONE_KB * ONE_KB;

    /**
     * The number of bytes in a gigabyte.
     */
    public static final int ONE_GB = ONE_KB * ONE_MB;

    /**
     * Returns a human-readable version of the file size (original is in
     * bytes).
     *
     * @param size The number of bytes.
     * @return A human-readable display value (includes units).
     */
    public static String byteCountToDisplaySize( int size )
    {
        String displaySize;

        if ( size / ONE_GB > 0 )
        {
            displaySize = String.valueOf( size / ONE_GB ) + " GB";
        }
        else if ( size / ONE_MB > 0 )
        {
            displaySize = String.valueOf( size / ONE_MB ) + " MB";
        }
        else if ( size / ONE_KB > 0 )
        {
            displaySize = String.valueOf( size / ONE_KB ) + " KB";
        }
        else
        {
            displaySize = String.valueOf( size ) + " bytes";
        }

        return displaySize;
    }

    /**
     * Returns the directory path portion of a file specification string.
     * Matches the equally named unix command.
     *
     * @return The directory portion excluding the ending file separator.
     */
    public static String dirname( String filename )
    {
        int i = filename.lastIndexOf( File.separator );
        return ( i >= 0 ? filename.substring( 0, i ) : "" );
    }

    /**
     * Returns the filename portion of a file specification string.
     *
     * @return The filename string with extension.
     */
    public static String filename( String filename )
    {
        int i = filename.lastIndexOf( File.separator );
        return ( i >= 0 ? filename.substring( i + 1 ) : filename );
    }

    /**
     * Returns the filename portion of a file specification string.
     * Matches the equally named unix command.
     *
     * @return The filename string without extension.
     */
    public static String basename( String filename )
    {
        return basename( filename, extension( filename ) );
    }

    /**
     * Returns the filename portion of a file specification string.
     * Matches the equally named unix command.
     */
    public static String basename( String filename, String suffix )
    {
        int i = filename.lastIndexOf( File.separator ) + 1;
        int lastDot = ( ( suffix != null ) && ( suffix.length() > 0 ) ) ? filename.lastIndexOf( suffix ) : -1;

        if ( lastDot >= 0 )
        {
            return filename.substring( i, lastDot );
        }
        else if ( i > 0 )
        {
            return filename.substring( i );
        }
        else
        {
            return filename; // else returns all (no path and no extension)
        }
    }

    /**
     * Returns the extension portion of a file specification string.
     * This everything after the last dot '.' in the filename (NOT including
     * the dot).
     */
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

    /**
     * Check if a file exits.
     *
     * @param fileName The name of the file to check.
     * @return true if file exists.
     */
    public static boolean fileExists( String fileName )
    {
        File file = new File( fileName );
        return file.exists();
    }

    public static String fileRead( String file )
        throws IOException
    {
        return fileRead( new File( file ) );
    }

    public static String fileRead( File file )
        throws IOException
    {
        StringBuffer buf = new StringBuffer();

        FileInputStream in = new FileInputStream( file );

        int count;
        byte[] b = new byte[512];
        while ( ( count = in.read( b ) ) > 0 )  // blocking read
        {
            buf.append( new String( b, 0, count ) );
        }

        in.close();

        return buf.toString();
    }

    /**
     * Writes data to a file. The file will be created if it does not exist.
     *
     * @param fileName The name of the file to write.
     * @param data     The content to write to the file.
     */
    public static void fileWrite( String fileName, String data )
        throws IOException
    {
        FileOutputStream out = new FileOutputStream( fileName );
        out.write( data.getBytes() );
        out.close();
    }

    /**
     * Deletes a file.
     *
     * @param fileName The name of the file to delete.
     */
    public static void fileDelete( String fileName )
    {
        File file = new File( fileName );
        file.delete();
    }

    /**
     * Waits for NFS to propagate a file creation, imposing a timeout.
     *
     * @param fileName The name of the file.
     * @param seconds  The maximum time in seconds to wait.
     * @return True if file exists.
     */
    public static boolean waitFor( String fileName, int seconds )
    {
        return waitFor( new File( fileName ), seconds );
    }

    public static boolean waitFor( File file, int seconds )
    {
        int timeout = 0;
        int tick = 0;
        while ( !file.exists() )
        {
            if ( tick++ >= 10 )
            {
                tick = 0;
                if ( timeout++ > seconds )
                {
                    return false;
                }
            }
            try
            {
                Thread.sleep( 100 );
            }
            catch ( InterruptedException ignore )
            {
            }
        }
        return true;
    }

    /**
     * Creates a file handle.
     *
     * @param fileName The name of the file.
     * @return A <code>File</code> manager.
     */
    public static File getFile( String fileName )
    {
        return new File( fileName );
    }

    /**
     * Given a directory and an array of extensions return an array of compliant files.
     * <p/>
     * TODO Should an ignore list be passed in?
     * TODO Should a recurse flag be passed in?
     * <p/>
     * The given extensions should be like "java" and not like ".java"
     */
    public static String[] getFilesFromExtension( String directory, String[] extensions )
    {

        Vector files = new Vector();

        java.io.File currentDir = new java.io.File( directory );

        String[] unknownFiles = currentDir.list();

        if ( unknownFiles == null )
        {
            return new String[0];
        }

        for ( int i = 0; i < unknownFiles.length; ++i )
        {
            String currentFileName = directory + System.getProperty( "file.separator" ) + unknownFiles[i];
            java.io.File currentFile = new java.io.File( currentFileName );

            if ( currentFile.isDirectory() )
            {

                //ignore all CVS directories...
                if ( currentFile.getName().equals( "CVS" ) )
                {
                    continue;
                }


                //ok... transverse into this directory and get all the files... then combine
                //them with the current list.

                String[] fetchFiles = getFilesFromExtension( currentFileName, extensions );
                files = blendFilesToVector( files, fetchFiles );

            }
            else
            {
                //ok... add the file

                String add = currentFile.getAbsolutePath();
                if ( isValidFile( add, extensions ) )
                {
                    files.addElement( add );

                }

            }
        }

        //ok... move the Vector into the files list...

        String[] foundFiles = new String[files.size()];
        files.copyInto( foundFiles );

        return foundFiles;

    }


    /**
     * Private hepler method for getFilesFromExtension()
     */
    private static Vector blendFilesToVector( Vector v, String[] files )
    {

        for ( int i = 0; i < files.length; ++i )
        {
            v.addElement( files[i] );
        }

        return v;
    }

    /**
     * Checks to see if a file is of a particular type(s).
     * Note that if the file does not have an extension, an empty string
     * (&quot;&quot;) is matched for.
     */
    private static boolean isValidFile( String file, String[] extensions )
    {

        String extension = extension( file );
        if ( extension == null )
        {
            extension = "";
        }

        //ok.. now that we have the "extension" go through the current know
        //excepted extensions and determine if this one is OK.

        for ( int i = 0; i < extensions.length; ++i )
        {
            if ( extensions[i].equals( extension ) )
            {
                return true;
            }
        }

        return false;

    }

    /**
     * Simple way to make a directory
     */
    public static void mkdir( String dir )
    {
        File file = new File( dir );
        if ( !file.exists() )
        {
            file.mkdirs();
        }
    }

    /**
     * Compare the contents of two files to determine if they are equal or not.
     *
     * @param file1 the first file
     * @param file2 the second file
     * @return true if the content of the files are equal or they both don't exist, false otherwise
     */
    public static boolean contentEquals( final File file1, final File file2 )
        throws IOException
    {
        final boolean file1Exists = file1.exists();
        if ( file1Exists != file2.exists() )
        {
            return false;
        }

        if ( !file1Exists )
        {
            // two not existing files are equal
            return true;
        }

        if ( file1.isDirectory() || file2.isDirectory() )
        {
            // don't want to compare directory contents
            return false;
        }

        InputStream input1 = null;
        InputStream input2 = null;
        try
        {
            input1 = new FileInputStream( file1 );
            input2 = new FileInputStream( file2 );
            return IOUtil.contentEquals( input1, input2 );

        }
        finally
        {
            input1.close();
            input2.close();
        }
    }

    /**
     * Convert from a <code>URL</code> to a <code>File</code>.
     *
     * @param url File URL.
     * @return The equivalent <code>File</code> object, or <code>null</code> if the URL's protocol
     *         is not <code>file</code>
     */
    public static File toFile( final URL url )
    {
        if ( url.getProtocol().equals( "file" ) == false )
        {
            return null;
        }
        else
        {
            final String filename = url.getFile().replace( '/', File.separatorChar );
            return new File( filename );
        }
    }

    /**
     * Convert the array of Files into a list of URLs.
     *
     * @param files the array of files
     * @return the array of URLs
     * @throws IOException if an error occurs
     */
    public static URL[] toURLs( final File[] files )
        throws IOException
    {
        final URL[] urls = new URL[files.length];

        for ( int i = 0; i < urls.length; i++ )
        {
            urls[i] = files[i].toURL();
        }

        return urls;
    }

    /**
     * Remove extension from filename.
     * ie
     * <pre>
     * foo.txt    --> foo
     * a\b\c.jpg --> a\b\c
     * a\b\c     --> a\b\c
     * </pre>
     *
     * @param filename the filename
     * @return the filename minus extension
     */
    public static String removeExtension( final String filename )
    {
        final int index = filename.lastIndexOf( '.' );

        if ( -1 == index )
        {
            return filename;
        }
        else
        {
            return filename.substring( 0, index );
        }
    }

    /**
     * Get extension from filename.
     * ie
     * <pre>
     * foo.txt    --> "txt"
     * a\b\c.jpg --> "jpg"
     * a\b\c     --> ""
     * </pre>
     *
     * @param filename the filename
     * @return the extension of filename or "" if none
     */
    public static String getExtension( final String filename )
    {
        final int index = filename.lastIndexOf( '.' );

        if ( -1 == index )
        {
            return "";
        }
        else
        {
            return filename.substring( index + 1 );
        }
    }

    /**
     * Remove path from filename. Equivalent to the unix command <code>basename</code>
     * ie.
     * <pre>
     * a/b/c.txt --> c.txt
     * a.txt     --> a.txt
     * </pre>
     *
     * @param filepath the filepath
     * @return the filename minus path
     */
    public static String removePath( final String filepath )
    {
        return removePath( filepath, File.separatorChar );
    }

    /**
     * Remove path from filename.
     * ie.
     * <pre>
     * a/b/c.txt --> c.txt
     * a.txt     --> a.txt
     * </pre>
     *
     * @param filepath the filepath
     * @return the filename minus path
     */
    public static String removePath( final String filepath, final char fileSeparatorChar )
    {
        final int index = filepath.lastIndexOf( fileSeparatorChar );

        if ( -1 == index )
        {
            return filepath;
        }
        else
        {
            return filepath.substring( index + 1 );
        }
    }

    /**
     * Get path from filename. Roughly equivalent to the unix command <code>dirname</code>.
     * ie.
     * <pre>
     * a/b/c.txt --> a/b
     * a.txt     --> ""
     * </pre>
     *
     * @param filepath the filepath
     * @return the filename minus path
     */
    public static String getPath( final String filepath )
    {
        return getPath( filepath, File.separatorChar );
    }

    /**
     * Get path from filename.
     * ie.
     * <pre>
     * a/b/c.txt --> a/b
     * a.txt     --> ""
     * </pre>
     *
     * @param filepath the filepath
     * @return the filename minus path
     */
    public static String getPath( final String filepath, final char fileSeparatorChar )
    {
        final int index = filepath.lastIndexOf( fileSeparatorChar );
        if ( -1 == index )
        {
            return "";
        }
        else
        {
            return filepath.substring( 0, index );
        }
    }

    /**
     * Copy file from source to destination. If <code>destinationDirectory</code> does not exist, it
     * (and any parent directories) will be created. If a file <code>source</code> in
     * <code>destinationDirectory</code> exists, it will be overwritten.
     *
     * @param source               An existing <code>File</code> to copy.
     * @param destinationDirectory A directory to copy <code>source</code> into.
     * @throws java.io.FileNotFoundException if <code>source</code> isn't a normal file.
     * @throws IllegalArgumentException      if <code>destinationDirectory</code> isn't a directory.
     * @throws IOException                   if <code>source</code> does not exist, the file in
     *                                       <code>destinationDirectory</code> cannot be written to, or an IO error occurs during copying.
     */
    public static void copyFileToDirectory( final String source, final String destinationDirectory )
        throws IOException
    {
        copyFileToDirectory( new File( source ), new File( destinationDirectory ) );
    }

    /**
     * Copy file from source to destination. If <code>destinationDirectory</code> does not exist, it
     * (and any parent directories) will be created. If a file <code>source</code> in
     * <code>destinationDirectory</code> exists, it will be overwritten.
     *
     * @param source               An existing <code>File</code> to copy.
     * @param destinationDirectory A directory to copy <code>source</code> into.
     * @throws java.io.FileNotFoundException if <code>source</code> isn't a normal file.
     * @throws IllegalArgumentException      if <code>destinationDirectory</code> isn't a directory.
     * @throws IOException                   if <code>source</code> does not exist, the file in
     *                                       <code>destinationDirectory</code> cannot be written to, or an IO error occurs during copying.
     */
    public static void copyFileToDirectory( final File source, final File destinationDirectory )
        throws IOException
    {
        if ( destinationDirectory.exists() && !destinationDirectory.isDirectory() )
        {
            throw new IllegalArgumentException( "Destination is not a directory" );
        }

        copyFile( source, new File( destinationDirectory, source.getName() ) );
    }

    /**
     * Copy file from source to destination. The directories up to <code>destination</code> will be
     * created if they don't already exist. <code>destination</code> will be overwritten if it
     * already exists.
     *
     * @param source      An existing non-directory <code>File</code> to copy bytes from.
     * @param destination A non-directory <code>File</code> to write bytes to (possibly
     *                    overwriting).
     * @throws IOException                   if <code>source</code> does not exist, <code>destination</code> cannot be
     *                                       written to, or an IO error occurs during copying.
     * @throws java.io.FileNotFoundException if <code>destination</code> is a directory
     *                                       (use {@link #copyFileToDirectory}).
     */
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
        if ( destination.getParentFile() != null && !destination.getParentFile().exists() )
        {
            destination.getParentFile().mkdirs();
        }

        //make sure we can write to destination
        if ( destination.exists() && !destination.canWrite() )
        {
            final String message = "Unable to open file " + destination + " for writing.";
            throw new IOException( message );
        }

        final FileInputStream input = new FileInputStream( source );
        final FileOutputStream output = new FileOutputStream( destination );
        IOUtil.copy( input, output );

        input.close();
        output.close();

        if ( source.length() != destination.length() )
        {
            final String message = "Failed to copy full contents from " + source + " to " + destination;
            throw new IOException( message );
        }
    }

    /**
     * Copies bytes from the URL <code>source</code> to a file <code>destination</code>.
     * The directories up to <code>destination</code> will be created if they don't already exist.
     * <code>destination</code> will be overwritten if it already exists.
     *
     * @param source      A <code>URL</code> to copy bytes from.
     * @param destination A non-directory <code>File</code> to write bytes to (possibly
     *                    overwriting).
     * @throws IOException if
     *                     <ul>
     *                     <li><code>source</code> URL cannot be opened</li>
     *                     <li><code>destination</code> cannot be written to</li>
     *                     <li>an IO error occurs during copying</li>
     *                     </ul>
     */
    public static void copyURLToFile( final URL source, final File destination )
        throws IOException
    {
        //does destination directory exist ?
        if ( destination.getParentFile() != null && !destination.getParentFile().exists() )
        {
            destination.getParentFile().mkdirs();
        }

        //make sure we can write to destination
        if ( destination.exists() && !destination.canWrite() )
        {
            final String message = "Unable to open file " + destination + " for writing.";
            throw new IOException( message );
        }

        final InputStream input = source.openStream();
        final FileOutputStream output = new FileOutputStream( destination );
        IOUtil.copy( input, output );

        input.close();
        output.close();
    }

    /**
     * Normalize a path.
     * Eliminates "/../" and "/./" in a string. Returns <code>null</code> if the ..'s went past the
     * root.
     * Eg:
     * <pre>
     * /foo//               -->     /foo/
     * /foo/./              -->     /foo/
     * /foo/../bar          -->     /bar
     * /foo/../bar/         -->     /bar/
     * /foo/../bar/../baz   -->     /baz
     * //foo//./bar         -->     /foo/bar
     * /../                 -->     null
     * </pre>
     *
     * @param path the path to normalize
     * @return the normalized String, or <code>null</code> if too many ..'s.
     */
    public static String normalize( final String path )
    {
        String normalized = path;
        // Resolve occurrences of "//" in the normalized path
        while ( true )
        {
            int index = normalized.indexOf( "//" );
            if ( index < 0 )
            {
                break;
            }
            normalized = normalized.substring( 0, index ) + normalized.substring( index + 1 );
        }

        // Resolve occurrences of "/./" in the normalized path
        while ( true )
        {
            int index = normalized.indexOf( "/./" );
            if ( index < 0 )
            {
                break;
            }
            normalized = normalized.substring( 0, index ) + normalized.substring( index + 2 );
        }

        // Resolve occurrences of "/../" in the normalized path
        while ( true )
        {
            int index = normalized.indexOf( "/../" );
            if ( index < 0 )
            {
                break;
            }
            if ( index == 0 )
            {
                return null;  // Trying to go outside our context
            }
            int index2 = normalized.lastIndexOf( '/', index - 1 );
            normalized = normalized.substring( 0, index2 ) + normalized.substring( index + 3 );
        }

        // Return the normalized path that we have completed
        return normalized;
    }

    /**
     * Will concatenate 2 paths.  Paths with <code>..</code> will be
     * properly handled.
     * <p>Eg.,<br />
     * <code>/a/b/c</code> + <code>d</code> = <code>/a/b/d</code><br />
     * <code>/a/b/c</code> + <code>../d</code> = <code>/a/d</code><br />
     * </p>
     * <p/>
     * Thieved from Tomcat sources...
     *
     * @return The concatenated paths, or null if error occurs
     */
    public static String catPath( final String lookupPath, final String path )
    {
        // Cut off the last slash and everything beyond
        int index = lookupPath.lastIndexOf( "/" );
        String lookup = lookupPath.substring( 0, index );
        String pth = path;

        // Deal with .. by chopping dirs off the lookup path
        while ( pth.startsWith( "../" ) )
        {
            if ( lookup.length() > 0 )
            {
                index = lookup.lastIndexOf( "/" );
                lookup = lookup.substring( 0, index );
            }
            else
            {
                // More ..'s than dirs, return null
                return null;
            }

            index = pth.indexOf( "../" ) + 3;
            pth = pth.substring( index );
        }

        return new StringBuffer( lookup ).append( "/" ).append( pth ).toString();
    }

    /**
     * Resolve a file <code>filename</code> to it's canonical form. If <code>filename</code> is
     * relative (doesn't start with <code>/</code>), it will be resolved relative to
     * <code>baseFile</code>, otherwise it is treated as a normal root-relative path.
     *
     * @param baseFile Where to resolve <code>filename</code> from, if <code>filename</code> is
     *                 relative.
     * @param filename Absolute or relative file path to resolve.
     * @return The canonical <code>File</code> of <code>filename</code>.
     */
    public static File resolveFile( final File baseFile, String filename )
    {
        String filenm = filename;
        if ( '/' != File.separatorChar )
        {
            filenm = filename.replace( '/', File.separatorChar );
        }

        if ( '\\' != File.separatorChar )
        {
            filenm = filename.replace( '\\', File.separatorChar );
        }

        // deal with absolute files
        if ( filenm.startsWith( File.separator ) )
        {
            File file = new File( filenm );

            try
            {
                file = file.getCanonicalFile();
            }
            catch ( final IOException ioe )
            {
            }

            return file;
        }
        // FIXME: I'm almost certain this // removal is unnecessary, as getAbsoluteFile() strips
        // them. However, I'm not sure about this UNC stuff. (JT)
        final char[] chars = filename.toCharArray();
        final StringBuffer sb = new StringBuffer();

        //remove duplicate file separators in succession - except
        //on win32 at start of filename as UNC filenames can
        //be \\AComputer\AShare\myfile.txt
        int start = 0;
        if ( '\\' == File.separatorChar )
        {
            sb.append( filenm.charAt( 0 ) );
            start++;
        }

        for ( int i = start; i < chars.length; i++ )
        {
            final boolean doubleSeparator = File.separatorChar == chars[i] && File.separatorChar == chars[i - 1];

            if ( !doubleSeparator )
            {
                sb.append( chars[i] );
            }
        }

        filenm = sb.toString();

        //must be relative
        File file = ( new File( baseFile, filenm ) ).getAbsoluteFile();

        try
        {
            file = file.getCanonicalFile();
        }
        catch ( final IOException ioe )
        {
        }

        return file;
    }

    /**
     * Delete a file. If file is directory delete it and all sub-directories.
     */
    public static void forceDelete( final String file )
        throws IOException
    {
        forceDelete( new File( file ) );
    }

    /**
     * Delete a file. If file is directory delete it and all sub-directories.
     */
    public static void forceDelete( final File file )
        throws IOException
    {
        if ( !file.exists() )
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
                final String message = "File " + file + " unable to be deleted.";
                throw new IOException( message );
            }
        }
    }

    /**
     * Schedule a file to be deleted when JVM exits.
     * If file is directory delete it and all sub-directories.
     */
    public static void forceDeleteOnExit( final File file )
        throws IOException
    {
        if ( !file.exists() )
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

    /**
     * Recursively schedule directory for deletion on JVM exit.
     */
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

    /**
     * Clean a directory without deleting it.
     */
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


    /**
     * Make a directory. If there already exists a file with specified name or
     * the directory is unable to be created then an exception is thrown.
     */
    public static void forceMkdir( final File file )
        throws IOException
    {
        if ( file.exists() )
        {
            if ( file.isFile() )
            {
                final String message = "File " + file + " exists and is " +
                    "not a directory. Unable to create directory.";
                throw new IOException( message );
            }
        }
        else
        {
            if ( false == file.mkdirs() )
            {
                final String message = "Unable to create directory " + file;
                throw new IOException( message );
            }
        }
    }

    /**
     * Recursively delete a directory.
     */
    public static void deleteDirectory( final String directory )
        throws IOException
    {
        deleteDirectory( new File( directory ) );
    }

    /**
     * Recursively delete a directory.
     */
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
            final String message = "Directory " + directory + " unable to be deleted.";
            throw new IOException( message );
        }
    }

    /**
     * Clean a directory without deleting it.
     */
    public static void cleanDirectory( final String directory )
        throws IOException
    {
        cleanDirectory( new File( directory ) );
    }

    /**
     * Clean a directory without deleting it.
     */
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

    /**
     * Recursively count size of a directory.
     *
     * @return size of directory in bytes.
     */
    public static long sizeOfDirectory( final String directory )
    {
        return sizeOfDirectory( new File( directory ) );
    }

    /**
     * Recursively count size of a directory.
     *
     * @return size of directory in bytes.
     */
    public static long sizeOfDirectory( final File directory )
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

        long size = 0;

        final File[] files = directory.listFiles();
        for ( int i = 0; i < files.length; i++ )
        {
            final File file = files[i];

            if ( file.isDirectory() )
            {
                size += sizeOfDirectory( file );
            }
            else
            {
                size += file.length();
            }
        }

        return size;
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
        copyDirectory( sourceDirectory, destinationDirectory, "**", null );
    }

    public static void copyDirectory( File sourceDirectory, File destinationDirectory, String includes,
                                      String excludes )
        throws IOException
    {
        if ( !sourceDirectory.exists() )
        {
            return;
        }

        List files = getFiles( sourceDirectory, includes, excludes );

        for ( Iterator i = files.iterator(); i.hasNext(); )
        {
            File file = (File) i.next();

            copyFileToDirectory( file, destinationDirectory );
        }
    }

    /**
     * Copies a entire directory structure.
     * <p/>
     * Note:
     * <ul>
     * <li>It will include empty directories.
     * <li>The <code>sourceDirectory</code> must exists.
     * </ul>
     *
     * @param sourceDirectory
     * @param destinationDirectory
     * @throws IOException
     */
    public static void copyDirectoryStructure( File sourceDirectory, File destinationDirectory )
        throws IOException
    {
        if ( !sourceDirectory.exists() )
        {
            throw new IOException( "Source directory doesn't exists (" + sourceDirectory.getAbsolutePath() + ")." );
        }

        File[] files = sourceDirectory.listFiles();

        String sourcePath = sourceDirectory.getAbsolutePath();

        for ( int i = 0; i < files.length; i++ )
        {
            File file = files[i];

            String dest = file.getAbsolutePath();

            dest = dest.substring( sourcePath.length() + 1 );

            File destination = new File( destinationDirectory, dest );

            if ( file.isFile() )
            {
                destination = destination.getParentFile();

                FileUtils.copyFileToDirectory( file, destination );
            }
            else if ( file.isDirectory() )
            {
                if ( !destination.exists() && !destination.mkdirs() )
                {
                    throw new IOException(
                        "Could not create destination directory '" + destination.getAbsolutePath() + "'." );
                }

                copyDirectoryStructure( file, destination );
            }
            else
            {
                throw new IOException( "Unknown file type: " + file.getAbsolutePath() );
            }
        }
    }

    /**
     * Renames a file, even if that involves crossing file system boundaries.
     * <p/>
     * <p>This will remove <code>to</code> (if it exists), ensure that
     * <code>to</code>'s parent directory exists and move
     * <code>from</code>, which involves deleting <code>from</code> as
     * well.</p>
     *
     * @param from the file to move
     * @param to   the new file name
     * @throws IOException if anything bad happens during this
     *                     process.  Note that <code>to</code> may have been deleted
     *                     already when this happens.
     */
    public static void rename( File from, File to )
        throws IOException
    {
        if ( to.exists() && !to.delete() )
        {
            throw new IOException( "Failed to delete " + to + " while trying to rename " + from );
        }

        File parent = to.getParentFile();
        if ( parent != null && !parent.exists() && !parent.mkdirs() )
        {
            throw new IOException( "Failed to create directory " + parent + " while trying to rename " + from );
        }

        if ( !from.renameTo( to ) )
        {
            copyFile( from, to );
            if ( !from.delete() )
            {
                throw new IOException( "Failed to delete " + from + " while trying to rename it." );
            }
        }
    }

    /**
     * Create a temporary file in a given directory.
     * <p/>
     * <p>The file denoted by the returned abstract pathname did not
     * exist before this method was invoked, any subsequent invocation
     * of this method will yield a different file name.</p>
     * <p/>
     * The filename is prefixNNNNNsuffix where NNNN is a random number
     * </p>
     * <p>This method is different to File.createTempFile of JDK 1.2
     * as it doesn't create the file itself.
     * It uses the location pointed to by java.io.tmpdir
     * when the parentDir attribute is
     * null.</p>
     *
     * @param prefix    prefix before the random number
     * @param suffix    file extension; include the '.'
     * @param parentDir Directory to create the temporary file in -
     *                  java.io.tmpdir used if not specificed
     * @return a File reference to the new temporary file.
     */
    public static File createTempFile( String prefix, String suffix, File parentDir )
    {

        File result = null;
        String parent = System.getProperty( "java.io.tmpdir" );
        if ( parentDir != null )
        {
            parent = parentDir.getPath();
        }
        DecimalFormat fmt = new DecimalFormat( "#####" );
        Random rand = new Random( System.currentTimeMillis() + Runtime.getRuntime().freeMemory() );
        synchronized ( rand )
        {
            do
            {
                result = new File( parent, prefix + fmt.format( Math.abs( rand.nextInt() ) ) + suffix );
            }
            while ( result.exists() );
        }
        return result;
    }
}
