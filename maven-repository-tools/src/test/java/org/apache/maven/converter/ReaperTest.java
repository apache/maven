package org.apache.maven.converter;

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

import java.io.File;

import org.codehaus.plexus.DefaultArtifactEnabledContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class ReaperTest
    extends PlexusTestCase
{
    public PlexusContainer getContainerInstance()
    {
        return new DefaultArtifactEnabledContainer();
    }

    public void testReaper()
        throws Exception
    {
        RepoReaper reaper = new RepoReaper();
/*
        File inbase = new File( System.getProperty( "user.home" ), ".maven/repository" );

        File outbase = new File( getTestFile( "target/outrepo" ) );
*/
        File inbase = new File( "ibiblio" );

        File outbase = new File( "maven" );

        FileUtils.deleteDirectory( outbase );

        reaper.work( inbase, outbase );
    }
}
