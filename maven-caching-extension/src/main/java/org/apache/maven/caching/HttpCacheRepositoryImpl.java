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
package org.apache.maven.caching;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.SessionScoped;
import org.apache.maven.caching.checksum.MavenProjectInput;
import org.apache.maven.caching.xml.Build;
import org.apache.maven.caching.xml.CacheConfig;
import org.apache.maven.caching.xml.CacheSource;
import org.apache.maven.caching.xml.XmlService;
import org.apache.maven.caching.xml.build.Artifact;
import org.apache.maven.caching.xml.report.CacheReport;
import org.apache.maven.caching.xml.report.ProjectReport;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP remote cache repository implementation.
 */
@SessionScoped
@Named( "http" )
public class HttpCacheRepositoryImpl implements RemoteCacheRepository
{

    public static final String BUILDINFO_XML = "buildinfo.xml";
    public static final String CACHE_REPORT_XML = "cache-report.xml";

    private static final Logger LOGGER = LoggerFactory.getLogger( HttpCacheRepositoryImpl.class );

    private final XmlService xmlService;
    private final CacheConfig cacheConfig;

    @Inject
    public HttpCacheRepositoryImpl( XmlService xmlService, CacheConfig cacheConfig )
    {
        this.xmlService = xmlService;
        this.cacheConfig = cacheConfig;
    }

    @SuppressWarnings( "checkstyle:constantname" )
    private static final ThreadLocal<HttpClient> httpClient = ThreadLocal
            .withInitial( HttpCacheRepositoryImpl::newHttpClient );

    @SuppressWarnings( "checkstyle:magicnumber" )
    private static CloseableHttpClient newHttpClient()
    {
        int timeoutSeconds = 60;
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout( timeoutSeconds * 1000 )
                .setConnectionRequestTimeout( timeoutSeconds * 1000 )
                .setSocketTimeout( timeoutSeconds * 1000 )
                .build();
        return HttpClientBuilder.create().setDefaultRequestConfig( config ).build();
    }

    @Nonnull
    @Override
    public Optional<Build> findBuild( CacheContext context ) throws IOException
    {
        final String resourceUrl = getResourceUrl( context, BUILDINFO_XML );
        return getResourceContent( resourceUrl )
                .map( content -> new Build( xmlService.loadBuild( content ), CacheSource.REMOTE ) );
    }

    @Nonnull
    @Override
    public boolean getArtifactContent( CacheContext context, Artifact artifact, Path target ) throws IOException
    {
        return getResourceContent( getResourceUrl( context, artifact.getFileName() ), target );
    }

    @Override
    public void saveBuildInfo( CacheResult cacheResult, Build build )
            throws IOException
    {
        final String resourceUrl = getResourceUrl( cacheResult.getContext(), BUILDINFO_XML );
        putToRemoteCache( new ByteArrayInputStream( xmlService.toBytes( build.getDto() ) ), resourceUrl );
    }

    @Override
    public void saveCacheReport( String buildId, MavenSession session, CacheReport cacheReport ) throws IOException
    {
        MavenProject rootProject = session.getTopLevelProject();
        final String resourceUrl = cacheConfig.getUrl() + "/" + MavenProjectInput.CACHE_IMPLEMENTATION_VERSION
                + "/" + rootProject.getGroupId()
                + "/" + rootProject.getArtifactId()
                + "/" + buildId
                + "/" + CACHE_REPORT_XML;
        putToRemoteCache( new ByteArrayInputStream( xmlService.toBytes( cacheReport ) ), resourceUrl );
    }

    @Override
    public void saveArtifactFile( CacheResult cacheResult,
            org.apache.maven.artifact.Artifact artifact ) throws IOException
    {
        final String resourceUrl = getResourceUrl( cacheResult.getContext(), CacheUtils.normalizedName( artifact ) );
        try ( InputStream inputStream = Files.newInputStream( artifact.getFile().toPath() ) )
        {
            putToRemoteCache( inputStream, resourceUrl );
        }
    }

    /**
     * Downloads content of the resource
     * 
     * @return null or content
     */
    @Nonnull
    public Optional<byte[]> getResourceContent( String url ) throws IOException
    {
        HttpGet get = new HttpGet( url );
        try
        {
            LOGGER.info( "Downloading {}", url );
            HttpResponse response = httpClient.get().execute( get );
            int statusCode = response.getStatusLine().getStatusCode();
            if ( statusCode != HttpStatus.SC_OK )
            {
                LOGGER.info( "Cannot download {}, status code: {}", url, statusCode );
                return Optional.empty();
            }
            try ( InputStream content = response.getEntity().getContent() )
            {
                return Optional.of( IOUtils.toByteArray( content ) );
            }
        }
        finally
        {
            get.releaseConnection();
        }
    }

    public boolean getResourceContent( String url, Path target ) throws IOException
    {
        HttpGet get = new HttpGet( url );
        try
        {
            LOGGER.info( "Downloading {}", url );
            HttpResponse response = httpClient.get().execute( get );
            int statusCode = response.getStatusLine().getStatusCode();
            if ( statusCode != HttpStatus.SC_OK )
            {
                LOGGER.info( "Cannot download {}, status code: {}", url, statusCode );
                return false;
            }
            try ( InputStream content = response.getEntity().getContent() )
            {
                Files.copy( content, target );
                return true;
            }
        }
        finally
        {
            get.releaseConnection();
        }
    }

    @Nonnull
    @Override
    public String getResourceUrl( CacheContext context, String filename )
    {
        return getResourceUrl( filename, context.getProject().getGroupId(), context.getProject().getArtifactId(),
                context.getInputInfo().getChecksum() );
    }

    private String getResourceUrl( String filename, String groupId, String artifactId, String checksum )
    {
        return cacheConfig.getUrl() + "/" + MavenProjectInput.CACHE_IMPLEMENTATION_VERSION + "/" + groupId + "/"
                + artifactId + "/" + checksum + "/" + filename;
    }

    /**
     * @param instream to be closed externally
     */
    private void putToRemoteCache( InputStream instream, String url ) throws IOException
    {
        HttpPut httpPut = new HttpPut( url );
        try
        {
            httpPut.setEntity( new InputStreamEntity( instream ) );
            HttpResponse response = httpClient.get().execute( httpPut );
            int statusCode = response.getStatusLine().getStatusCode();
            LOGGER.info( "Saved to remote cache {}. Status: {}", url, statusCode );
        }
        finally
        {
            httpPut.releaseConnection();
        }
    }

    private final AtomicReference<Optional<CacheReport>> cacheReportSupplier = new AtomicReference<>();

    @Nonnull
    @Override
    public Optional<Build> findBaselineBuild( MavenProject project )
    {
        Optional<List<ProjectReport>> cachedProjectsHolder = findCacheInfo()
                .map( CacheReport::getProjects );

        if ( !cachedProjectsHolder.isPresent() )
        {
            return Optional.empty();
        }

        final List<ProjectReport> projects = cachedProjectsHolder.get();
        final Optional<ProjectReport> projectReportHolder = projects.stream()
                .filter( p -> project.getArtifactId().equals( p.getArtifactId() )
                        && project.getGroupId().equals( p.getGroupId() ) )
                .findFirst();

        if ( !projectReportHolder.isPresent() )
        {
            return Optional.empty();
        }

        final ProjectReport projectReport = projectReportHolder.get();

        String url;
        if ( projectReport.getUrl() != null )
        {
            url = projectReport.getUrl();
            LOGGER.info( "Retrieving baseline buildinfo: {}", url );
        }
        else
        {
            url = getResourceUrl( BUILDINFO_XML, project.getGroupId(),
                    project.getArtifactId(), projectReport.getChecksum() );
            LOGGER.info( "Baseline project record doesn't have url, trying default location {}", url );
        }

        try
        {
            return getResourceContent( url )
                    .map( content -> new Build( xmlService.loadBuild( content ), CacheSource.REMOTE ) );
        }
        catch ( Exception e )
        {
            LOGGER.warn( "Error restoring baseline build at url: {}, skipping diff", url, e );
            return Optional.empty();
        }
    }

    private Optional<CacheReport> findCacheInfo()
    {
        Optional<CacheReport> report = cacheReportSupplier.get();
        if ( !report.isPresent() )
        {
            try
            {
                LOGGER.info( "Downloading baseline cache report from: {}", cacheConfig.getBaselineCacheUrl() );
                report = getResourceContent( cacheConfig.getBaselineCacheUrl() ).map( xmlService::loadCacheReport );
            }
            catch ( Exception e )
            {
                LOGGER.error( "Error downloading baseline report from: {}, skipping diff.",
                        cacheConfig.getBaselineCacheUrl(), e );
                report = Optional.empty();
            }
            cacheReportSupplier.compareAndSet( null, report );
        }
        return report;
    }

}
