package org.apache.maven.tools.repoclean.patch;

import org.apache.maven.model.v4_0_0.Model;
import org.apache.maven.tools.repoclean.report.Reporter;

import java.io.File;

import junit.framework.TestCase;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

/**
 * @author jdcasey
 */
public class V4ModelPatcherTest
    extends TestCase
{

    public void testShouldPatchWithInfoFromXPP3PomPath()
    {
        String pomPath = "xpp3/poms/xpp3-1.1.3.3.pom";
        Model model = new Model();

        V4ModelPatcher patcher = new V4ModelPatcher();
        Reporter reporter = new Reporter( new File( "." ), "testXpp3Patching.txt" );

        patcher.patchModel( model, pomPath, reporter );

        assertEquals( "xpp3", model.getGroupId() );
        assertEquals( "xpp3", model.getArtifactId() );
        assertEquals( "1.1.3.3", model.getVersion() );
    }

    public void testShouldPatchWithInfoFromPlexusContainerDefaultPomPath()
    {
        String pomPath = "plexus/poms/plexus-container-default-1.0-alpha-2-SNAPSHOT.pom";
        Model model = new Model();

        V4ModelPatcher patcher = new V4ModelPatcher();
        Reporter reporter = new Reporter( new File( "." ), "testPlexusPatching.txt" );

        patcher.patchModel( model, pomPath, reporter );

        assertEquals( "plexus", model.getGroupId() );
        assertEquals( "plexus-container-default", model.getArtifactId() );
        assertEquals( "1.0-alpha-2-SNAPSHOT", model.getVersion() );
    }

}