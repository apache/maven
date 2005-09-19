/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.acm.convert;

import java.io.File;
import java.io.FilenameFilter;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id:$
 */
public class FileScanner
{
    public File[] scan( File directory, String extension )
    {
        File[] files = directory.listFiles( new ExtensionFilter( extension ) );

        return files;
    }

    class ExtensionFilter
        implements FilenameFilter
    {
        String extension;

        public ExtensionFilter( String ext )
        {
            this.extension = ext;
        }

        public boolean accept( File directory, String filename )
        {
            if ( filename.endsWith( extension ) )
            {
                return true;
            }

            return false;
        }
    }
}
