package org.apache.maven;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public class MavenTestCase
    extends PlexusTestCase
{        
    protected void setUp()
        throws Exception
    {
        super.setUp();

        File pluginsDirectory = new File( getBasedir(), "target/maven.home/plugins" );

        pluginsDirectory.mkdirs();
    }

    protected void customizeContext()
        throws Exception
    {
        ClassWorld classWorld = new ClassWorld();

        ClassRealm rootClassRealm = classWorld.newRealm( "root", Thread.currentThread().getContextClassLoader() );

        getContainer().addContextValue( "rootClassRealm", rootClassRealm );

        getContainer().addContextValue( "maven.home", new File( getBasedir(), "target/maven.home" ).getPath() );

        getContainer().addContextValue( "maven.home.local", new File( getBasedir(), "target/maven.home.local" ).getPath() );
    }
}
