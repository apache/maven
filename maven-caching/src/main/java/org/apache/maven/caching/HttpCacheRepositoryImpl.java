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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
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
@Named
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
    private static final ThreadLocal<HttpClient> httpClient =
            ThreadLocal.withInitial( HttpCacheRepositoryImpl::newHttpClient );

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

    @Override
    public Build findBuild( CacheContext context )
    {
        final String resourceUrl = getResourceUrl( context, BUILDINFO_XML );
        if ( exists( resourceUrl ) )
        {
            final byte[] bytes = getResourceContent( resourceUrl );
            final org.apache.maven.caching.xml.build.Build dto = xmlService.loadBuild( bytes );
            return new Build( dto, CacheSource.REMOTE );
        }
        return null;
    }

    @Override
    public byte[] getArtifactContent( CacheContext context, Artifact artifact )
    {
        return getResourceContent( getResourceUrl( context, artifact.getFileName() ) );
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

    @SuppressWarnings( "checkstyle:magicnumber" )
    private boolean exists( String url )
    {
        HttpHead head = null;
        try
        {
            head = new HttpHead( url );
            HttpResponse response = httpClient.get().execute( head );
            int statusCode = response.getStatusLine().getStatusCode();
            LOGGER.info( "Checking {}. Status: {}", url, statusCode );
            return statusCode == 200;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Cannot check " + url, e );
        }
        finally
        {
            if ( head != null )
            {
                head.releaseConnection();
            }
        }
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    public byte[] getResourceContent( String url )
    {
        HttpGet get = null;
        try
        {
            get = new HttpGet( url );
            HttpResponse response = httpClient.get().execute( get );
            int statusCode = response.getStatusLine().getStatusCode();
            LOGGER.info( "Downloading {}. Status: {}", url, statusCode );
            if ( statusCode != 200 )
            {
                throw new RuntimeException( "Cannot download " + url + ", unexpected status code: " + statusCode );
            }
            try ( InputStream content = response.getEntity().getContent() )
            {
                return IOUtils.toByteArray( content );
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Cannot get " + url, e );
        }
        finally
        {
            if ( get != null )
            {
                get.releaseConnection();
            }
        }
    }

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
     * @param instream     to be closed externally
     */
    private void putToRemoteCache( InputStream instream, String url ) throws IOException
    {
        HttpPut httpPut = null;
        try
        {
            httpPut = new HttpPut( url );
            httpPut.setEntity( new InputStreamEntity( instream ) );
            HttpResponse response = httpClient.get().execute( httpPut );
            int statusCode = response.getStatusLine().getStatusCode();
            LOGGER.info( "Saved to remote cache {}. Status: {}", url, statusCode );
        }
        finally
        {
            if ( httpPut != null )
            {
                httpPut.releaseConnection();
            }
        }
    }

    private final AtomicReference<Optional<CacheReport>> cacheReportSupplier = new AtomicReference<>();

    @Override
    public Optional<Build> findBaselineBuild( MavenProject project )
    {
        final Optional<List<ProjectReport>> cachedProjectsHolder = findCacheInfo()
                .map( CacheReport::getProjects );
        if ( !cachedProjectsHolder.isPresent() )
        {
            return Optional.empty();
        }

        Optional<ProjectReport> cachedProjectHolder = Optional.empty();
        for ( ProjectReport p : cachedProjectsHolder.get() )
        {
            if ( project.getArtifactId().equals( p.getArtifactId() )
                    && project.getGroupId().equals( p.getGroupId() ) )
            {
                cachedProjectHolder = Optional.of( p );
                break;
            }
        }

        if ( cachedProjectHolder.isPresent() )
        {
            String url;
            final ProjectReport projectReport = cachedProjectHolder.get();
            if ( projectReport.getUrl() != null )
            {
                url = cachedProjectHolder.get().getUrl();
                LOGGER.info( "Retrieving baseline buildinfo: {}", projectReport.getUrl() );
            }
            else
            {
                url = getResourceUrl( BUILDINFO_XML, project.getGroupId(),
                        project.getArtifactId(), projectReport.getChecksum() );
                LOGGER.info( "Baseline project record doesn't have url, trying default location" );
            }

            try
            {
                if ( exists( url ) )
                {
                    byte[] content = getResourceContent( url );
                    final org.apache.maven.caching.xml.build.Build dto = xmlService.loadBuild( content );
                    return Optional.of( new Build( dto, CacheSource.REMOTE ) );
                }
                else
                {
                    LOGGER.info( "Project buildinfo not found, skipping diff" );
                }
            }
            catch ( Exception e )
            {
                LOGGER.warn( "Error restoring baseline build at url: {}, skipping diff",
                             projectReport.getUrl() );
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private Optional<CacheReport> findCacheInfo()
    {
        Optional<CacheReport> report = cacheReportSupplier.get();
        if ( report == null )
        {
            try
            {
                LOGGER.info( "Downloading baseline cache report from: {}", cacheConfig.getBaselineCacheUrl() );
                byte[] content = getResourceContent( cacheConfig.getBaselineCacheUrl() );
                CacheReport cacheReportType = xmlService.loadCacheReport( content );
                report = Optional.of( cacheReportType );
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
