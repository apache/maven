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
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.caching.checksum.MavenProjectInput;
import org.apache.maven.caching.xml.Build;
import org.apache.maven.caching.xml.CacheConfig;
import org.apache.maven.caching.xml.CacheSource;
import org.apache.maven.caching.xml.XmlService;
import org.apache.maven.caching.xml.build.Artifact;
import org.apache.maven.caching.xml.report.CacheReport;
import org.apache.maven.caching.xml.report.ProjectReport;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;

/**
 * HttpRepositoryImpl
 */
@Singleton
@Named
public class HttpRepositoryImpl implements RemoteArtifactsRepository
{

    public static final String BUILDINFO_XML = "buildinfo.xml";
    public static final String CACHE_REPORT_XML = "cache-report.xml";

    @Inject
    private Logger logger;

    @Inject
    LegacySupport legacySupport;

    @Inject
    XmlService xmlService;

    @Inject
    private CacheConfig cacheConfig;

    @SuppressWarnings( {"checkstyle:constantname", "checkstyle:magicnumber"} )
    private static final ThreadLocal<HttpClient> httpClient = new ThreadLocal<HttpClient>()
    {
        @Override
        protected HttpClient initialValue()
        {
            int timeoutSeconds = 60;
            RequestConfig config = RequestConfig.custom().setConnectTimeout(
                    timeoutSeconds * 1000 ).setConnectionRequestTimeout( timeoutSeconds * 1000 ).setSocketTimeout(
                    timeoutSeconds * 1000 ).build();
            return HttpClientBuilder.create().setDefaultRequestConfig( config ).build();
        }
    };

    @Override
    public Build findBuild( CacheContext context )
    {
        final String resourceUrl = getResourceUrl( context, BUILDINFO_XML );
        String artifactId = context.getProject().getArtifactId();
        if ( exists( artifactId, resourceUrl ) )
        {
            final byte[] bytes = getResourceContent( resourceUrl, artifactId );
            final org.apache.maven.caching.xml.build.Build dto = xmlService.loadBuild( bytes );
            return new Build( dto, CacheSource.REMOTE );
        }
        return null;
    }

    @Override
    public byte[] getArtifactContent( CacheContext context, Artifact artifact )
    {
        return getResourceContent( getResourceUrl( context, artifact.getFileName() ),
                context.getProject().getArtifactId() );
    }

    @Override
    public void saveBuildInfo( CacheResult cacheResult, Build build )
            throws IOException
    {
        CacheContext context = cacheResult.getContext();
        final String resourceUrl = getResourceUrl( cacheResult.getContext(), BUILDINFO_XML );
        putToRemoteCache( new ByteArrayInputStream( xmlService.toBytes( build.getDto() ) ), resourceUrl,
                context.getProject().getArtifactId() );
    }


    @Override
    public void saveCacheReport( String buildId, MavenSession session, CacheReport cacheReport ) throws IOException
    {
        MavenProject rootProject = session.getTopLevelProject();
        final String resourceUrl = cacheConfig.getUrl() + "/" + MavenProjectInput.CACHE_IMPLMENTATION_VERSION
                + "/" + rootProject.getGroupId()
                + "/" + rootProject.getArtifactId()
                + "/" + buildId
                + "/" + CACHE_REPORT_XML;
        putToRemoteCache( new ByteArrayInputStream( xmlService.toBytes( cacheReport ) ), resourceUrl,
                rootProject.getArtifactId() );
    }

    @Override
    public void saveArtifactFile( CacheResult cacheResult,
                                  org.apache.maven.artifact.Artifact artifact ) throws IOException
    {
        CacheContext context = cacheResult.getContext();
        final String resourceUrl = getResourceUrl( cacheResult.getContext(), ProjectUtils.normalizedName( artifact ) );
        try ( InputStream inputStream = Files.newInputStream( artifact.getFile().toPath() ) )
        {
            putToRemoteCache( inputStream, resourceUrl, context.getProject().getArtifactId() );
        }
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    private boolean exists( String logReference, String url )
    {
        HttpHead head = null;
        try
        {
            head = new HttpHead( url );
            HttpResponse response = httpClient.get().execute( head );
            int statusCode = response.getStatusLine().getStatusCode();
            logger.info( "[CACHE][" + logReference + "] Checking " + url + ". Status: " + statusCode );
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
    public byte[] getResourceContent( String url, String logReference )
    {
        HttpGet get = null;
        try
        {
            get = new HttpGet( url );
            HttpResponse response = httpClient.get().execute( get );
            int statusCode = response.getStatusLine().getStatusCode();
            logger.info( "[CACHE][" + logReference + "] Downloading " + url + ". Status: " + statusCode );
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
        return cacheConfig.getUrl() + "/" + MavenProjectInput.CACHE_IMPLMENTATION_VERSION + "/" + groupId + "/"
                + artifactId + "/" + checksum + "/" + filename;
    }

    /**
     * @param logReference
     * @param instream     to be closed externally
     */
    private void putToRemoteCache( InputStream instream, String url, String logReference ) throws IOException
    {

        HttpPut httpPut = null;
        try
        {
            httpPut = new HttpPut( url );
            httpPut.setEntity( new InputStreamEntity( instream ) );
            HttpResponse response = httpClient.get().execute( httpPut );
            int statusCode = response.getStatusLine().getStatusCode();
            logInfo( "Saved to remote cache " + url + ". RESPONSE CODE: " + statusCode, logReference );
        }
        finally
        {
            if ( httpPut != null )
            {
                httpPut.releaseConnection();
            }
        }
    }

    private final AtomicReference<Supplier<Optional<CacheReport>>> cacheReportSupplier = new AtomicReference<>();

    @Override
    public Optional<Build> findBaselineBuild( MavenProject project )
    {
        final Optional<List<ProjectReport>> cachedProjectsHolder = findCacheInfo()
                .transform( CacheReport::getProjects );
        if ( !cachedProjectsHolder.isPresent() )
        {
            return Optional.absent();
        }

        Optional<ProjectReport> cachedProjectHolder = Optional.absent();
        for ( ProjectReport p : cachedProjectsHolder.get() )
        {
            if ( project.getArtifactId().equals( p.getArtifactId() ) && project.getGroupId().equals(
                    p.getGroupId() ) )
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
                logInfo( "Retrieving baseline buildinfo: " + projectReport.getUrl(), project.getArtifactId() );
            }
            else
            {
                url = getResourceUrl( BUILDINFO_XML, project.getGroupId(),
                        project.getArtifactId(), projectReport.getChecksum() );
                logInfo( "Baseline project record doesn't have url, trying default location", project.getArtifactId() );
            }

            try
            {
                if ( exists( project.getArtifactId(), url ) )
                {
                    byte[] content = getResourceContent( url, project.getArtifactId() );
                    final org.apache.maven.caching.xml.build.Build dto = xmlService.loadBuild( content );
                    return Optional.of( new Build( dto, CacheSource.REMOTE ) );
                }
                else
                {
                    logInfo( "Project buildinfo not found, skipping diff",
                            project.getArtifactId() );
                }
            }
            catch ( Exception e )
            {
                logger.warn( "[CACHE][" + project.getArtifactId() + "] Error restoring baseline build at url: "
                        + projectReport.getUrl() + ", skipping diff" );
                return Optional.absent();
            }
        }
        return Optional.absent();
    }

    private Optional<CacheReport> findCacheInfo()
    {

        Supplier<Optional<CacheReport>> candidate = Suppliers.memoize( () ->
        {
            try
            {
                logInfo( "Downloading baseline cache report from: " + cacheConfig.getBaselineCacheUrl(),
                        "DEBUG" );
                byte[] content = getResourceContent( cacheConfig.getBaselineCacheUrl(), "cache-info" );
                CacheReport cacheReportType = xmlService.loadCacheReport( content );
                return Optional.of( cacheReportType );
            }
            catch ( Exception e )
            {
                logger.error( "Error downloading baseline report from: " + cacheConfig.getBaselineCacheUrl()
                        + ", skipping diff.", e );
                return Optional.absent();
            }
        } );
        cacheReportSupplier.compareAndSet( null, candidate );

        return cacheReportSupplier.get().get();
    }

    private void logInfo( String message, String logReference )
    {
        logger.info( "[CACHE][" + logReference + "] " + message );
    }
}
