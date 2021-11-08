package org.apache.maven.caching.checksum;

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

import java.nio.file.Path;

/**
 * WalkKey
 */
public class WalkKey
{
    private final Path normalized;
    private final String glob;
    private final boolean recursive;

    public WalkKey( Path normalized, String glob, boolean recursive )
    {

        this.normalized = normalized;
        this.glob = glob;
        this.recursive = recursive;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        WalkKey key = (WalkKey) o;

        if ( recursive != key.recursive )
        {
            return false;
        }
        if ( !normalized.equals( key.normalized ) )
        {
            return false;
        }
        return glob.equals( key.glob );
    }

    @Override
    public int hashCode()
    {
        int result = normalized.hashCode();
        result = 31 * result + glob.hashCode();
        result = 31 * result + ( recursive ? 1 : 0 );
        return result;
    }

    public Path getPath()
    {
        return normalized;
    }

    public String getGlob()
    {
        return glob;
    }

    public boolean isRecursive()
    {
        return recursive;
    }

    @Override
    public String toString()
    {
        return "WalkKey{" + "normalized=" + normalized + ", glob='" + glob + '\'' + ", recursive=" + recursive + '}';
    }
}
