package org.codehaus.plexus.util;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.codehaus.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Turbine" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact codehaus@codehaus.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Turbine", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.codehaus.org/>.
 *
 */

import java.io.File;
import java.io.IOException;

public class FileUtils
{
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
            if ( !deleteFile( file ) )
            {
                final String message = "File " + file + " unable to be deleted.";
                throw new IOException( message );
            }
        }
    }

    /**
     * Accommodate Windows bug encountered in both Sun and IBM JDKs.
     * Others possible. If the delete does not work, call System.gc(),
     * wait a little and try again.
     */
    private static boolean deleteFile( File file )
        throws IOException
    {
        if ( file.isDirectory() )
        {
            throw new IOException( "File " + file + " isn't a file." );
        }

        if ( !file.delete() )
        {
            if ( System.getProperty( "os.name" ).toLowerCase().indexOf( "windows" ) > -1 )
            {
                System.gc();
            }

            try
            {
                Thread.sleep( 10 );
                return file.delete();
            }
            catch ( InterruptedException ex )
            {
                return file.delete();
            }
        }

        return true;
    }

    /**
     * Schedule a file to be deleted when JVM exits.
     * If file is directory delete it and all sub-directories.
     */
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
            final String message =
                "Directory " + directory + " unable to be deleted.";
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
}