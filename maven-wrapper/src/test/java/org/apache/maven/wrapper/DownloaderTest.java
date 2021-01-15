package org.apache.maven.wrapper;

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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DownloaderTest
{
    @TempDir
    public Path testDir;

    private DefaultDownloader download;

    private Path downloadFile;

    private Path rootDir;

    private URI sourceRoot;

    private Path remoteFile;

    @BeforeEach
    public void setUp()
        throws Exception
    {
        download = new DefaultDownloader( "mvnw", "aVersion" );
        rootDir = testDir.resolve( "root" );
        downloadFile = rootDir.resolve( "file" );
        remoteFile = testDir.resolve( "remoteFile" );
        Files.write( remoteFile, Arrays.asList( "sometext" ) );
        sourceRoot = remoteFile.toUri();
    }

    @Test
    public void testDownload()
        throws Exception
    {
        assert !Files.exists( downloadFile );
        download.download( sourceRoot, downloadFile );
        assert Files.exists( downloadFile );
        assertEquals( "sometext",
                      Files.readAllLines( downloadFile ).stream().collect( Collectors.joining() ) );
    }
}
