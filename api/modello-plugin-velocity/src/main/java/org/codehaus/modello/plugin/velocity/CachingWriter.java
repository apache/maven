package org.codehaus.modello.plugin.velocity;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Caching Writer to avoid overwriting a file with
 * the same content.
 */
public class CachingWriter extends OutputStreamWriter
{
    private final CachingOutputStream cos;

    public CachingWriter( File path, Charset charset ) throws IOException
    {
        this( Objects.requireNonNull( path ).toPath(), charset );
    }

    public CachingWriter( Path path, Charset charset ) throws IOException
    {
        this( path, charset, 32 * 1024 );
    }

    public CachingWriter( Path path, Charset charset, int bufferSize ) throws IOException
    {
        this( new CachingOutputStream( path, bufferSize ), charset );
    }

    private CachingWriter( CachingOutputStream outputStream, Charset charset ) throws IOException
    {
        super( outputStream, charset );
        this.cos = outputStream;
    }

    public boolean isModified()
    {
        return cos.isModified();
    }
}