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
package org.apache.maven.buildcache.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.buildcache.xml.build.Build;
import org.apache.maven.buildcache.xml.build.io.xpp3.BuildCacheBuildXpp3Reader;
import org.apache.maven.buildcache.xml.build.io.xpp3.BuildCacheBuildXpp3Writer;
import org.apache.maven.buildcache.xml.config.CacheConfig;
import org.apache.maven.buildcache.xml.config.io.xpp3.BuildCacheConfigXpp3Reader;
import org.apache.maven.buildcache.xml.config.io.xpp3.BuildCacheConfigXpp3Writer;
import org.apache.maven.buildcache.xml.diff.Diff;
import org.apache.maven.buildcache.xml.diff.io.xpp3.BuildCacheDiffXpp3Reader;
import org.apache.maven.buildcache.xml.diff.io.xpp3.BuildCacheDiffXpp3Writer;
import org.apache.maven.buildcache.xml.report.CacheReport;
import org.apache.maven.buildcache.xml.report.io.xpp3.BuildCacheReportXpp3Reader;
import org.apache.maven.buildcache.xml.report.io.xpp3.BuildCacheReportXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * XmlService
 */
@Singleton
@Named
public class XmlService
{

    public byte[] toBytes( CacheConfig cache ) throws IOException
    {
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() )
        {
            new BuildCacheConfigXpp3Writer().write( baos, cache );
            return baos.toByteArray();
        }
    }

    public byte[] toBytes( Build build ) throws IOException
    {
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() )
        {
            new BuildCacheBuildXpp3Writer().write( baos, build );
            return baos.toByteArray();
        }
    }

    public byte[] toBytes( Diff diff ) throws IOException
    {
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() )
        {
            new BuildCacheDiffXpp3Writer().write( baos, diff );
            return baos.toByteArray();
        }
    }

    public byte[] toBytes( CacheReport cacheReportType ) throws IOException
    {
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() )
        {
            new BuildCacheReportXpp3Writer().write( baos, cacheReportType );
            return baos.toByteArray();
        }
    }

    public Build loadBuild( File file ) throws IOException
    {
        return fromFile( Build.class, file );
    }

    public Build loadBuild( byte[] bytes )
    {
        return fromBytes( Build.class, bytes );
    }

    public Build loadBuild( InputStream inputStream )
    {
        return fromInputStream( Build.class, inputStream );
    }

    public CacheConfig loadCacheConfig( File file ) throws IOException
    {
        return fromFile( CacheConfig.class, file );
    }

    public CacheConfig loadCacheConfig( byte[] bytes )
    {
        return fromBytes( CacheConfig.class, bytes );
    }

    public CacheConfig loadCacheConfig( InputStream inputStream )
    {
        return fromInputStream( CacheConfig.class, inputStream );
    }

    public CacheReport loadCacheReport( File file ) throws IOException
    {
        return fromFile( CacheReport.class, file );
    }

    public CacheReport loadCacheReport( byte[] bytes )
    {
        return fromBytes( CacheReport.class, bytes );
    }

    public CacheReport loadCacheReport( InputStream inputStream )
    {
        return fromInputStream( CacheReport.class, inputStream );
    }

    public Diff loadDiff( File file ) throws IOException
    {
        return fromFile( Diff.class, file );
    }

    public Diff loadDiff( byte[] bytes )
    {
        return fromBytes( Diff.class, bytes );
    }

    public Diff loadDiff( InputStream inputStream )
    {
        return fromInputStream( Diff.class, inputStream );
    }

    private <T> T fromFile( Class<T> clazz, File file ) throws IOException
    {
        return fromInputStream( clazz, Files.newInputStream( file.toPath() ) );
    }

    private <T> T fromBytes( Class<T> clazz, byte[] bytes )
    {
        return fromInputStream( clazz, new ByteArrayInputStream( bytes ) );
    }

    private <T> T fromInputStream( Class<T> clazz, InputStream inputStream )
    {
        try
        {
            if ( clazz == Build.class )
            {
                return clazz.cast( new BuildCacheBuildXpp3Reader().read( inputStream ) );
            }
            else if ( clazz == CacheConfig.class )
            {
                return clazz.cast( new BuildCacheConfigXpp3Reader().read( inputStream ) );
            }
            else if ( clazz == Diff.class )
            {
                return clazz.cast( new BuildCacheDiffXpp3Reader().read( inputStream ) );
            }
            else if ( clazz == CacheReport.class )
            {
                return clazz.cast( new BuildCacheReportXpp3Reader().read( inputStream ) );
            }
            else
            {
                throw new IllegalArgumentException( "Unsupported type " + clazz );
            }
        }
        catch ( IOException | XmlPullParserException e )
        {
            throw new RuntimeException( "Unable to parse cache xml element", e );
        }
    }
}
