package org.slf4j.impl;

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

import org.junit.Test;
import org.slf4j.Logger;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class MavenLoggerFactoryTest
{
    @Test
    public void createsSimpleLogger()
    {
        MavenLoggerFactory mavenLoggerFactory = new MavenLoggerFactory();

        Logger logger = mavenLoggerFactory.getLogger( "Test" );

        assertThat( logger, instanceOf( MavenSimpleLogger.class ) );
    }

    @Test
    public void loggerCachingWorks()
    {
        MavenLoggerFactory mavenLoggerFactory = new MavenLoggerFactory();

        Logger logger = mavenLoggerFactory.getLogger( "Test" );
        Logger logger2 = mavenLoggerFactory.getLogger( "Test" );
        Logger differentLogger = mavenLoggerFactory.getLogger( "TestWithDifferentName" );

        assertNotNull( logger );
        assertNotNull( differentLogger );
        assertSame( logger, logger2 );
        assertNotSame( logger, differentLogger );
    }

    @Test
    public void createsFailLevelLogger()
    {
        MavenLoggerFactory mavenLoggerFactory = new MavenLoggerFactory();
        mavenLoggerFactory.breakOnLogsOfLevel( "WARN" );

        Logger logger = mavenLoggerFactory.getLogger( "Test" );

        assertThat( logger, instanceOf( MavenFailLevelLogger.class ) );
    }

    @Test
    public void reportsWhenFailLevelHasBeenHit()
    {
        MavenLoggerFactory mavenLoggerFactory = new MavenLoggerFactory();
        mavenLoggerFactory.breakOnLogsOfLevel( "ERROR" );

        MavenFailLevelLogger logger = ( MavenFailLevelLogger ) mavenLoggerFactory.getLogger( "Test" );
        assertFalse( mavenLoggerFactory.threwLogsOfBreakingLevel() );

        logger.warn( "This should not hit the fail level" );
        assertFalse( mavenLoggerFactory.threwLogsOfBreakingLevel() );

        logger.error( "This should hit the fail level" );
        assertTrue( mavenLoggerFactory.threwLogsOfBreakingLevel() );

        logger.warn( "This should not reset the fail level" );
        assertTrue( mavenLoggerFactory.threwLogsOfBreakingLevel() );
    }

    @Test( expected = IllegalStateException.class )
    public void failLevelThresholdCanOnlyBeSetOnce()
    {
        MavenLoggerFactory mavenLoggerFactory = new MavenLoggerFactory();
        mavenLoggerFactory.breakOnLogsOfLevel( "WARN" );
        mavenLoggerFactory.breakOnLogsOfLevel( "ERROR" );
    }

    @Test( expected = IllegalArgumentException.class )
    public void onlyWarningOrHigherFailLevelsCanBeSet()
    {
        MavenLoggerFactory mavenLoggerFactory = new MavenLoggerFactory();
        mavenLoggerFactory.breakOnLogsOfLevel( "INFO" );
    }
}