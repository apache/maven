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

import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

import java.io.File;

/**
 * This is a utility class for helping to configure a PlexusTestCase for testing with maven.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class MavenTestUtils
{
    private MavenTestUtils()
    {
    }

    public static PlexusContainer getContainerInstance()
        throws PlexusContainerException
    {
        return new DefaultPlexusContainer();
    }

    public static void customizeContext( PlexusContainer container, File basedir, File mavenHome, File mavenHomeLocal )
        throws Exception
    {
        ClassWorld classWorld = new ClassWorld();

        ClassRealm rootClassRealm = classWorld.newRealm( "root", Thread.currentThread().getContextClassLoader() );

        container.addContextValue( "rootClassRealm", rootClassRealm );

        container.addContextValue( "maven.home", mavenHome.getAbsolutePath() );

        container.addContextValue( "maven.home.local", mavenHomeLocal.getAbsolutePath() );
    }
}
