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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.SessionScoped;
import org.apache.maven.caching.checksum.MavenProjectInput;
import org.apache.maven.caching.xml.Build;
import org.apache.maven.caching.xml.CacheConfig;
import org.apache.maven.caching.xml.CacheSource;
import org.apache.maven.caching.xml.XmlService;
import org.apache.maven.caching.xml.report.CacheReport;
import org.apache.maven.caching.xml.report.ProjectReport;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.wagon.StreamingWagon;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.transfer.NoTransporterException;
import org.eclipse.aether.util.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SessionScoped
@Named( "wagon" )
@SuppressWarnings( "unused" )
public class WagonRemoteCacheRepository implements RemoteCacheRepository
{
    public static final String BUILDINFO_XML = "buildinfo.xml";
    public static final String CACHE_REPORT_XML = "cache-report.xml";

    private static final String CONFIG_PROP_CONFIG = "remote.caching.wagon.config";
    private static final String CONFIG_PROP_FILE_MODE = "remote.caching.wagon.perms.fileMode";
    private static final String CONFIG_PROP_DIR_MODE = "remote.caching.wagon.perms.dirMode";
    private static final String CONFIG_PROP_GROUP = "remote.caching.wagon.perms.group";


    private static final Logger LOGGER = LoggerFactory.getLogger( HttpCacheRepositoryImpl.class );

    private final MavenSession mavenSession;
    private final XmlService xmlService;
    private final CacheConfig cacheConfig;
    private final WagonConfigurator wagonConfigurator;
    private final WagonProvider wagonProvider;

    private final RemoteRepository repository;
    private final AuthenticationContext repoAuthContext;
    private final AuthenticationContext proxyAuthContext;
    private final String wagonHint;
    private final Repository wagonRepo;
    private final AuthenticationInfo wagonAuth;
    private final ProxyInfoProvider wagonProxy;
    private final Properties headers;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Queue<Wagon> wagons = new ConcurrentLinkedQueue<>();
    private final AtomicReference<Optional<CacheReport>> cacheReportSupplier = new AtomicReference<>();

    @Inject
    public WagonRemoteCacheRepository( MavenSession mavenSession,
                                       XmlService xmlService,
                                       CacheConfig cacheConfig,
                                       WagonProvider wagonProvider,
                                       WagonConfigurator wagonConfigurator,
                                       RepositorySystem repositorySystem )
            throws RepositoryException
    {
        this.xmlService = xmlService;
        this.cacheConfig = cacheConfig;
        this.mavenSession = mavenSession;
        this.wagonProvider = wagonProvider;
        this.wagonConfigurator = wagonConfigurator;

        MavenExecutionRequest request = mavenSession.getRequest();
        RepositorySystemSession session = mavenSession.getRepositorySession();

        cacheConfig.initialize();

        RemoteRepository repo = new RemoteRepository.Builder( "cache", "cache", cacheConfig.getUrl() ).build();
        RemoteRepository mirror = session.getMirrorSelector().getMirror( repo );
        RemoteRepository repoOrMirror = mirror != null ? mirror : repo;
        Proxy proxy = session.getProxySelector().getProxy( repoOrMirror );
        Authentication auth = session.getAuthenticationSelector().getAuthentication( repoOrMirror );
        repository = new RemoteRepository.Builder( repoOrMirror )
                        .setProxy( proxy )
                        .setAuthentication( auth )
                        .build();


        wagonRepo = new Repository( repository.getId(), repository.getUrl() );
        wagonRepo.setPermissions( getPermissions( repository.getId(), session ) );

        wagonHint = wagonRepo.getProtocol().toLowerCase( Locale.ENGLISH );
        if ( wagonHint.isEmpty() )
        {
            throw new IllegalArgumentException( "Could not find a wagon provider for " + wagonRepo );
        }

        try
        {
            wagons.add( lookupWagon() );
        }
        catch ( Exception e )
        {
            LOGGER.debug( "No transport {}", e, e );
            throw new NoTransporterException( repository, e );
        }

        repoAuthContext = AuthenticationContext.forRepository( session, repository );
        proxyAuthContext = AuthenticationContext.forProxy( session, repository );

        wagonAuth = getAuthenticationInfo( repoAuthContext );
        wagonProxy = getProxy( repository, proxyAuthContext );

        this.headers = new Properties();
        this.headers.put( "User-Agent", ConfigUtils.getString( session, ConfigurationProperties.DEFAULT_USER_AGENT,
                ConfigurationProperties.USER_AGENT ) );
        Map<?, ?> headers =
                ConfigUtils.getMap( session, null, ConfigurationProperties.HTTP_HEADERS + "." + repository.getId(),
                        ConfigurationProperties.HTTP_HEADERS );
        if ( headers != null )
        {
            this.headers.putAll( headers );
        }
    }

    @PreDestroy
    void destroy()
    {
        if ( closed.compareAndSet( false, true ) )
        {
            AuthenticationContext.close( repoAuthContext );
            AuthenticationContext.close( proxyAuthContext );

            for ( Wagon wagon = wagons.poll(); wagon != null; wagon = wagons.poll() )
            {
                disconnectWagon( wagon );
                releaseWagon( wagon );
            }
        }
    }


    private static RepositoryPermissions getPermissions( String repoId, RepositorySystemSession session )
    {
        RepositoryPermissions result = null;
        RepositoryPermissions perms = new RepositoryPermissions();
        String suffix = '.' + repoId;
        String fileMode = ConfigUtils.getString( session, null, CONFIG_PROP_FILE_MODE + suffix );
        if ( fileMode != null )
        {
            perms.setFileMode( fileMode );
            result = perms;
        }
        String dirMode = ConfigUtils.getString( session, null, CONFIG_PROP_DIR_MODE + suffix );
        if ( dirMode != null )
        {
            perms.setDirectoryMode( dirMode );
            result = perms;
        }
        String group = ConfigUtils.getString( session, null, CONFIG_PROP_GROUP + suffix );
        if ( group != null )
        {
            perms.setGroup( group );
            result = perms;
        }
        return result;
    }

    private AuthenticationInfo getAuthenticationInfo( final AuthenticationContext authContext )
    {
        AuthenticationInfo auth = null;

        if ( authContext != null )
        {
            auth = new AuthenticationInfo()
            {
                @Override
                public String getUserName()
                {
                    return authContext.get( AuthenticationContext.USERNAME );
                }

                @Override
                public String getPassword()
                {
                    return authContext.get( AuthenticationContext.PASSWORD );
                }

                @Override
                public String getPrivateKey()
                {
                    return authContext.get( AuthenticationContext.PRIVATE_KEY_PATH );
                }

                @Override
                public String getPassphrase()
                {
                    return authContext.get( AuthenticationContext.PRIVATE_KEY_PASSPHRASE );
                }
            };
        }

        return auth;
    }

    private ProxyInfoProvider getProxy( RemoteRepository repository, final AuthenticationContext authContext )
    {
        ProxyInfoProvider proxy = null;

        Proxy p = repository.getProxy();
        if ( p != null )
        {
            final ProxyInfo prox;
            if ( authContext != null )
            {
                prox = new ProxyInfo()
                {
                    @Override
                    public String getUserName()
                    {
                        return authContext.get( AuthenticationContext.USERNAME );
                    }

                    @Override
                    public String getPassword()
                    {
                        return authContext.get( AuthenticationContext.PASSWORD );
                    }

                    @Override
                    public String getNtlmDomain()
                    {
                        return authContext.get( AuthenticationContext.NTLM_DOMAIN );
                    }

                    @Override
                    public String getNtlmHost()
                    {
                        return authContext.get( AuthenticationContext.NTLM_WORKSTATION );
                    }
                };
            }
            else
            {
                prox = new ProxyInfo();
            }
            prox.setType( p.getType() );
            prox.setHost( p.getHost() );
            prox.setPort( p.getPort() );

            proxy = protocol -> prox;
        }

        return proxy;
    }

    @Override
    public Build findBuild( CacheContext context ) throws IOException
    {
        final String resourceUrl = doGetResourceUrl( context, BUILDINFO_XML );
        final byte[] bytes = getResourceContent( resourceUrl );
        if ( bytes != null )
        {
            final org.apache.maven.caching.xml.build.Build dto = xmlService.loadBuild( bytes );
            return new Build( dto, CacheSource.REMOTE );
        }
        return null;
    }

    @Override
    public byte[] getArtifactContent( CacheContext context, org.apache.maven.caching.xml.build.Artifact artifact )
            throws IOException
    {
        return getResourceContent( doGetResourceUrl( context, artifact.getFileName() ) );
    }

    @Override
    public void saveBuildInfo( CacheResult cacheResult, Build build )
            throws IOException
    {
        final String resourceUrl = doGetResourceUrl( cacheResult.getContext(), BUILDINFO_XML );
        putToRemoteCache( xmlService.toBytes( build.getDto() ), resourceUrl );
    }


    @Override
    public void saveCacheReport( String buildId, MavenSession session, CacheReport cacheReport ) throws IOException
    {
        MavenProject rootProject = session.getTopLevelProject();
        final String resourceUrl = doGetResourceUrl( CACHE_REPORT_XML, rootProject, buildId );
        putToRemoteCache( xmlService.toBytes( cacheReport ), resourceUrl );
    }

    @Override
    public void saveArtifactFile( CacheResult cacheResult,
                                  org.apache.maven.artifact.Artifact artifact ) throws IOException
    {
        final String resourceUrl = doGetResourceUrl( cacheResult.getContext(), CacheUtils.normalizedName( artifact ) );
        putToRemoteCache( artifact.getFile().toPath(), resourceUrl );
    }

    @Override
    public String getResourceUrl( CacheContext context, String filename )
    {
        String base = cacheConfig.getUrl();
        return base.endsWith( "/" )
                ? base + doGetResourceUrl( context, filename )
                : base + "/" + doGetResourceUrl( context, filename );
    }

    public String doGetResourceUrl( CacheContext context, String filename )
    {
        return doGetResourceUrl( filename, context.getProject(), context.getInputInfo().getChecksum() );
    }

    private String doGetResourceUrl( String filename, MavenProject project, String checksum )
    {
        return doGetResourceUrl( filename, project.getGroupId(), project.getArtifactId(), checksum );
    }

    private String doGetResourceUrl( String filename, String groupId, String artifactId, String checksum )
    {
        return MavenProjectInput.CACHE_IMPLEMENTATION_VERSION + "/" + groupId + "/"
                + artifactId + "/" + checksum + "/" + filename;
    }

    @Override
    public byte[] getResourceContent( String resourceUrl ) throws IOException
    {
        return doGetResource( resourceUrl );
    }

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
                url = doGetResourceUrl( BUILDINFO_XML, project, projectReport.getChecksum() );
                LOGGER.info( "Baseline project record doesn't have url, trying default location" );
            }

            try
            {
                byte[] content = getResourceContent( url );
                if ( content != null )
                {
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
        if ( !report.isPresent() )
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

    private void putToRemoteCache( Path path, String url ) throws IOException
    {
        try
        {
            Wagon wagon = pollWagon();
            try
            {
                wagon.put( path.toFile(), url );
            }
            finally
            {
                wagons.add( wagon );
            }
        }
        catch ( Exception e )
        {
            throw new IOException( "Unable to upload resource " + url, e );
        }
    }

    private void putToRemoteCache( byte[] data, String resourceName ) throws IOException
    {
        /*
        try
        {
            Wagon wagon = pollWagon();
            try
            {
                if ( wagon instanceof StreamingWagon )
                {
                    ( ( StreamingWagon ) wagon ).putFromStream(
                            new ByteArrayInputStream( data ), resourceName, data.length, 0 );
                }
                else
                {
                    File temp = File.createTempFile( "maven-caching-", ".temp" );
                    try
                    {
                        Files.write( temp.toPath(), data );
                        wagon.put( temp, resourceName );
                    }
                    finally
                    {
                        delete( temp );
                    }
                }
            }
            finally
            {
                wagons.add( wagon );
            }
        }
        catch ( Exception e )
        {
            throw new IOException( "Unable to upload resource " + resourceName, e );
        }

         */
        File temp = File.createTempFile( "maven-caching-", ".temp" );
        try
        {
            Files.write( temp.toPath(), data );
            putToRemoteCache( temp.toPath(), resourceName );
        }
        finally
        {
            delete( temp );
        }
    }

    @SuppressWarnings( "ResultOfMethodCallIgnored" )
    private void delete( File temp )
    {
        temp.delete();
    }

    private byte[] doGetResource( String resourceName ) throws IOException
    {
        try
        {
            Wagon wagon = pollWagon();
            try
            {
                if ( wagon instanceof StreamingWagon )
                {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ( ( StreamingWagon ) wagon ).getToStream( resourceName, baos );
                    return baos.toByteArray();
                }
                else
                {
                    File temp = File.createTempFile( "maven-caching-", ".temp" );
                    try
                    {
                        wagon.get( resourceName, temp );
                        return Files.readAllBytes( temp.toPath() );
                    }
                    finally
                    {
                        delete( temp );
                    }
                }
            }
            finally
            {
                wagons.add( wagon );
            }
        }
        catch ( Exception e )
        {
            throw new IOException( "Unable to download resource " + resourceName, e );
        }
    }

    private Wagon lookupWagon() throws Exception
    {
        return wagonProvider.lookup( wagonHint );
    }

    private void releaseWagon( Wagon wagon )
    {
        wagonProvider.release( wagon );
    }

    private void connectWagon( Wagon wagon )
            throws WagonException
    {
        if ( !headers.isEmpty() )
        {
            try
            {
                Method setHttpHeaders = wagon.getClass().getMethod( "setHttpHeaders", Properties.class );
                setHttpHeaders.invoke( wagon, headers );
            }
            catch ( NoSuchMethodException e )
            {
                // normal for non-http wagons
            }
            catch ( InvocationTargetException | IllegalAccessException | RuntimeException e )
            {
                LOGGER.debug( "Could not set user agent for Wagon {}", wagon.getClass().getName(), e );
            }
        }

        RepositorySystemSession session = mavenSession.getRepositorySession();
        int connectTimeout =
                ConfigUtils.getInteger( session, ConfigurationProperties.DEFAULT_CONNECT_TIMEOUT,
                        ConfigurationProperties.CONNECT_TIMEOUT );
        int requestTimeout =
                ConfigUtils.getInteger( session, ConfigurationProperties.DEFAULT_REQUEST_TIMEOUT,
                        ConfigurationProperties.REQUEST_TIMEOUT );

        wagon.setTimeout( Math.max( Math.max( connectTimeout, requestTimeout ), 0 ) );

        wagon.setInteractive( ConfigUtils.getBoolean( session, ConfigurationProperties.DEFAULT_INTERACTIVE,
                ConfigurationProperties.INTERACTIVE ) );

        Object configuration = ConfigUtils.getObject( session, null,
                CONFIG_PROP_CONFIG + "." + repository.getId() );
        if ( configuration != null && wagonConfigurator != null )
        {
            try
            {
                wagonConfigurator.configure( wagon, configuration );
            }
            catch ( Exception e )
            {
                LOGGER.warn( "Could not apply configuration for {} to Wagon {}",
                        repository.getId(), wagon.getClass().getName(), e );
            }
        }

        wagon.connect( wagonRepo, wagonAuth, wagonProxy );
    }

    private void disconnectWagon( Wagon wagon )
    {
        try
        {
            if ( wagon != null )
            {
                wagon.disconnect();
            }
        }
        catch ( WagonException e )
        {
            LOGGER.debug( "Could not disconnect Wagon {}", wagon, e );
        }
    }

    private Wagon pollWagon()
            throws Exception
    {
        Wagon wagon = wagons.poll();

        if ( wagon == null )
        {
            try
            {
                wagon = lookupWagon();
                connectWagon( wagon );
            }
            catch ( Exception e )
            {
                releaseWagon( wagon );
                throw e;
            }
        }
        else if ( wagon.getRepository() == null )
        {
            try
            {
                connectWagon( wagon );
            }
            catch ( WagonException e )
            {
                wagons.add( wagon );
                throw e;
            }
        }

        return wagon;
    }

}
