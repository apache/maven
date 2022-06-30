package org.apache.maven.cli.event;

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

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.slf4j.Logger;

class ExecutionEventLoggerTest
{
    @BeforeAll
    public static void setUp()
    {
        MessageUtils.setColorEnabled( false );
    }

    @AfterAll
    public static void tearDown()
    {
        MessageUtils.setColorEnabled( true );
    }

    @Test
    void testProjectStarted()
    {
        // prepare
        Logger logger = mock( Logger.class );
        when( logger.isInfoEnabled() ).thenReturn( true );
        ExecutionEventLogger executionEventLogger = new ExecutionEventLogger( logger );

        File basedir = new File( "" ).getAbsoluteFile();
        ExecutionEvent event = mock( ExecutionEvent.class );
        MavenProject project = mock( MavenProject.class );
        when( project.getGroupId() ).thenReturn( "org.apache.maven" );
        when( project.getArtifactId() ).thenReturn( "maven-embedder" );
        when( project.getPackaging() ).thenReturn( "jar" );
        when( project.getName() ).thenReturn( "Apache Maven Embedder" );
        when( project.getVersion() ).thenReturn( "3.5.4-SNAPSHOT" );
        when( project.getFile() ).thenReturn( new File( basedir, "maven-embedder/pom.xml" ) );
        when( event.getProject() ).thenReturn( project );

        MavenProject rootProject = mock( MavenProject.class );
        when( rootProject.getBasedir() ).thenReturn( basedir );
        MavenSession session = mock( MavenSession.class );
        when( session.getTopLevelProject() ).thenReturn( rootProject );
        when( event.getSession() ).thenReturn( session );

        // execute
        executionEventLogger.projectStarted( event );

        // verify
        InOrder inOrder = inOrder( logger );
        inOrder.verify( logger ).info( "" );
        inOrder.verify( logger ).info( "------------------< org.apache.maven:maven-embedder >-------------------" );
        inOrder.verify( logger ).info( "Building Apache Maven Embedder 3.5.4-SNAPSHOT" );
        inOrder.verify( logger ).info( adaptDirSeparator( "  from maven-embedder/pom.xml" ) );
        inOrder.verify( logger ).info( "--------------------------------[ jar ]---------------------------------" );
    }

    @Test
    void testProjectStartedOverflow()
    {
        // prepare
        Logger logger = mock( Logger.class );
        when( logger.isInfoEnabled() ).thenReturn( true );
        ExecutionEventLogger executionEventLogger = new ExecutionEventLogger( logger );

        File basedir = new File( "" ).getAbsoluteFile();
        ExecutionEvent event = mock( ExecutionEvent.class );
        MavenProject project = mock( MavenProject.class );
        when( project.getGroupId() ).thenReturn( "org.apache.maven.plugins.overflow" );
        when( project.getArtifactId() ).thenReturn( "maven-project-info-reports-plugin" );
        when( project.getPackaging() ).thenReturn( "maven-plugin" );
        when( project.getName() ).thenReturn( "Apache Maven Project Info Reports Plugin" );
        when( project.getVersion() ).thenReturn( "3.0.0-SNAPSHOT" );
        when( event.getProject() ).thenReturn( project );
        when( project.getFile() ).thenReturn( new File( basedir, "pom.xml" ) );
        when( project.getBasedir() ).thenReturn( basedir );

        MavenSession session = mock( MavenSession.class );
        when( session.getTopLevelProject() ).thenReturn( project );
        when( event.getSession() ).thenReturn( session );

        // execute
        executionEventLogger.projectStarted( event );

        // verify
        InOrder inOrder = inOrder( logger );
        inOrder.verify( logger ).info( "" );
        inOrder.verify( logger ).info( "--< org.apache.maven.plugins.overflow:maven-project-info-reports-plugin >--" );
        inOrder.verify( logger ).info( "Building Apache Maven Project Info Reports Plugin 3.0.0-SNAPSHOT" );
        inOrder.verify( logger ).info( adaptDirSeparator( "  from pom.xml" ) );
        inOrder.verify( logger ).info( "----------------------------[ maven-plugin ]----------------------------" );
    }

    @Test
    void testTerminalWidth()
    {
        // prepare
        Logger logger = mock( Logger.class );
        when( logger.isInfoEnabled() ).thenReturn( true );

        ExecutionEvent event = mock( ExecutionEvent.class );
        MavenProject project = mock( MavenProject.class );
        when( project.getGroupId() ).thenReturn( "org.apache.maven.plugins.overflow" );
        when( project.getArtifactId() ).thenReturn( "maven-project-info-reports-plugin" );
        when( project.getPackaging() ).thenReturn( "maven-plugin" );
        when( project.getName() ).thenReturn( "Apache Maven Project Info Reports Plugin" );
        when( project.getVersion() ).thenReturn( "3.0.0-SNAPSHOT" );
        when( event.getProject() ).thenReturn( project );

        // default width
        new ExecutionEventLogger( logger, -1 ).projectStarted( event );
        Mockito.verify( logger ).info( "----------------------------[ maven-plugin ]----------------------------" );

        // terminal width: 30
        new ExecutionEventLogger( logger, 30 ).projectStarted( event );
        Mockito.verify( logger ).info( "------------------[ maven-plugin ]------------------" );

        // terminal width: 70
        new ExecutionEventLogger( logger, 70 ).projectStarted( event );
        Mockito.verify( logger ).info( "-----------------------[ maven-plugin ]-----------------------" );

        // terminal width: 110
        new ExecutionEventLogger( logger, 110 ).projectStarted( event );
        Mockito.verify( logger ).info( "-------------------------------------------[ maven-plugin ]-------------------------------------------" );

        // terminal width: 200
        new ExecutionEventLogger( logger, 200 ).projectStarted( event );
        Mockito.verify( logger ).info( "-----------------------------------------------------[ maven-plugin ]-----------------------------------------------------" );
    }

    public void testProjectStartedNoPom()
    {
        // prepare
        Logger logger = mock( Logger.class );
        when( logger.isInfoEnabled() ).thenReturn( true );
        ExecutionEventLogger executionEventLogger = new ExecutionEventLogger( logger );

        File basedir = new File( "" ).getAbsoluteFile();
        ExecutionEvent event = mock( ExecutionEvent.class );
        MavenProject project = mock( MavenProject.class );
        when( project.getGroupId() ).thenReturn( "org.apache.maven" );
        when( project.getArtifactId() ).thenReturn( "standalone-pom" );
        when( project.getPackaging() ).thenReturn( "pom" );
        when( project.getName() ).thenReturn( "Maven Stub Project (No POM)" );
        when( project.getVersion() ).thenReturn( "1" );
        when( event.getProject() ).thenReturn( project );
        when( project.getFile() ).thenReturn( null );
        when( project.getBasedir() ).thenReturn( basedir );

        // execute
        executionEventLogger.projectStarted( event );

        // verify
        InOrder inOrder = inOrder( logger );
        inOrder.verify( logger ).info( "" );
        inOrder.verify( logger ).info( "------------------< org.apache.maven:standalone-pom >-------------------" );
        inOrder.verify( logger ).info( "Building Maven Stub Project (No POM) 1" );
        inOrder.verify( logger ).info( "--------------------------------[ pom ]---------------------------------" );
    }

    private static String adaptDirSeparator( String path )
    {
        return FilenameUtils.separatorsToSystem( path );
    }
}
