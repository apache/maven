package org.apache.maven;

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

import org.apache.maven.artifact.manager.WagonConfigurationException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.manager.WagonProviderMapping;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.monitor.event.DefaultEventDispatcher;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpsWagon;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class WagonSelectorTest
    extends PlexusTestCase
{

    private WagonProviderMapping mapping;

    private WagonManager manager;

    private ArtifactRepository localRepository;

    private Maven maven;

    private File dir;

    public void setUp()
        throws Exception
    {
        super.setUp();

        mapping = (WagonProviderMapping) lookup( WagonProviderMapping.ROLE );

        manager = (WagonManager) lookup( WagonManager.ROLE );

        ArtifactRepositoryLayout layout =
            (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.class.getName(), "default" );
        localRepository = new DefaultArtifactRepository( "local", System.getProperty( "java.io.tmpdir" ), layout );

        maven = (Maven) lookup( Maven.class.getName() );
    }

    public void tearDown()
        throws Exception
    {
        release( mapping );
        release( manager );
        super.tearDown();
        
        if ( dir != null )
        {
            try
            {
                FileUtils.forceDelete( dir );
            }
            catch ( IOException e )
            {
            }
        }
    }

    public void testSelectHttpWagonFromDefault()
        throws Exception
    {
        Properties executionProperties = new Properties();

        MavenExecutionRequest req = createMavenRequest( executionProperties );

        Wagon wagon = manager.getWagon( new Repository( "id", "http://www.google.com/" ) );

        assertTrue( "Should use " + LightweightHttpWagon.class.getName(), wagon instanceof LightweightHttpWagon );
    }

    public void testSelectHttpsWagonFromDefault()
        throws Exception
    {
        Properties executionProperties = new Properties();

        MavenExecutionRequest req = createMavenRequest( executionProperties );

        Wagon wagon = manager.getWagon( new Repository( "id", "https://www.google.com/" ) );

        assertTrue( "Should use " + LightweightHttpsWagon.class.getName(), wagon instanceof LightweightHttpsWagon );
    }

    public void testSelectHttpclientWagonFromSimulatedMavenCliConfiguration()
        throws WagonConfigurationException, UnsupportedProtocolException
    {
        mapping.setWagonProvider( "http", "httpclient" );

        Wagon wagon = manager.getWagon( new Repository( "id", "http://www.google.com/" ) );

        assertTrue( "Should use " + HttpWagon.class.getName(), wagon instanceof HttpWagon );
    }

    public void testSelectHttpclientWagonFromServerConfiguration()
        throws WagonConfigurationException, UnsupportedProtocolException
    {
        Xpp3Dom config = new Xpp3Dom( "configuration" );
        Xpp3Dom provider = new Xpp3Dom( "wagonProvider" );
        provider.setValue( "httpclient" );
        config.addChild( provider );

        manager.addConfiguration( "id", config );

        Wagon wagon = manager.getWagon( new Repository( "id", "http://www.google.com/" ) );
        assertTrue( "Should use " + HttpWagon.class.getName(), wagon instanceof HttpWagon );
    }

    public void testSelectHttpclientWagonFromMavenCLIParameter()
        throws WagonConfigurationException, UnsupportedProtocolException, MavenExecutionException, IOException
    {
        Properties executionProperties = new Properties();
        executionProperties.setProperty( "maven.wagon.provider.http", "httpclient" );

        MavenExecutionRequest req = createMavenRequest( executionProperties );

        Wagon wagon = manager.getWagon( new Repository( "id", "http://www.google.com/" ) );
        assertTrue( "Should use " + HttpWagon.class.getName(), wagon instanceof HttpWagon );
    }

    public MavenExecutionRequest createMavenRequest( Properties executionProperties )
        throws IOException, MavenExecutionException
    {
        Settings settings = new Settings();
        EventDispatcher eventDispatcher = new DefaultEventDispatcher();

        List<String> goals = new ArrayList<String>();
        goals.add( "validate" );

        dir = File.createTempFile( "WagonSelectorTest.", ".dir" );
        dir.delete();
        dir.mkdirs();

        File pom = new File( dir, "pom.xml" );

        Model model = new Model();
        model.setModelVersion( "4.0.0" );
        model.setGroupId( "wagon.selector.test" );
        model.setArtifactId( "wagon-selector-test" );
        model.setVersion( "1" );
        model.setPackaging( "pom" );
        
        FileWriter writer = null;
        try
        {
            writer = new FileWriter( pom );
            new MavenXpp3Writer().write( writer, model );
        }
        finally
        {
            IOUtil.close( writer );
        }

        String baseDirectory = dir.getAbsolutePath();
        ProfileManager globalProfileManager = new DefaultProfileManager( getContainer(), new Properties() );

        boolean showErrors = false;
        Properties userProperties = new Properties();

        MavenExecutionRequest req =
            new DefaultMavenExecutionRequest( localRepository, settings, eventDispatcher, goals, baseDirectory,
                                              globalProfileManager, executionProperties, userProperties, showErrors );
        
        req.setPomFile( pom.getAbsolutePath() );

        maven.execute( req );

        return req;
    }

    public static final class TestRuntimeInformation
        implements RuntimeInformation
    {

        public ArtifactVersion getApplicationVersion()
        {
            return new DefaultArtifactVersion( "TEST" );
        }

    }

}
