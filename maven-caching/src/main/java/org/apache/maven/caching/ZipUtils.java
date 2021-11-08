package org.apache.maven.caching;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * ZipUtils
 */
public class ZipUtils
{

    private static final int BUFFER_SIZE = 4096;

    public static InputStream zipFolder( final Path dir ) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try ( ZipOutputStream zipOutputStream = new ZipOutputStream( out ) )
        {
            processFolder( dir, zipOutputStream );
        }
        return new ByteArrayInputStream( out.toByteArray() );
    }

    private static void processFolder( final Path dir, final ZipOutputStream zipOutputStream ) throws IOException
    {
        Files.walkFileTree( dir, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile( Path path, BasicFileAttributes basicFileAttributes ) throws IOException
            {
                final ZipEntry zipEntry = new ZipEntry( dir.relativize( path ).toString() );
                zipOutputStream.putNextEntry( zipEntry );
                try ( InputStream inputStream = Files.newInputStream( path ) )
                {
                    IOUtils.copy( inputStream, zipOutputStream );
                }
                zipOutputStream.closeEntry();
                return FileVisitResult.CONTINUE;
            }
        } );
    }

    public static void unzip( InputStream is, Path out ) throws IOException
    {
        try ( ZipInputStream zis = new ZipInputStream( is ) )
        {

            ZipEntry entry = zis.getNextEntry();

            while ( entry != null )
            {
                File file = new File( out.toFile(), entry.getName() );
                file.setLastModified( entry.getTime() );

                if ( entry.isDirectory() )
                {
                    file.mkdirs();
                }
                else
                {
                    File parent = file.getParentFile();

                    if ( !parent.exists() )
                    {
                        parent.mkdirs();
                    }

                    try ( BufferedOutputStream bos = new BufferedOutputStream( new FileOutputStream( file ) ) )
                    {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int location;
                        while ( ( location = zis.read( buffer ) ) != -1 )
                        {
                            bos.write( buffer, 0, location );
                        }
                    }
                }
                entry = zis.getNextEntry();
            }
        }
    }
}
