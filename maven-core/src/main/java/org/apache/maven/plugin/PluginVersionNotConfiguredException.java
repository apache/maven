package org.apache.maven.plugin;

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
public class PluginVersionNotConfiguredException
    extends Exception
{

    private final String groupId;

    private final String artifactId;

    public PluginVersionNotConfiguredException( String groupId, String artifactId )
    {
        super( "The maven plugin with groupId: \'" + groupId + "\' and artifactId: \'" + artifactId
            + "\' which was configured for use in this project does not have a version associated with it." );

        this.groupId = groupId;

        this.artifactId = artifactId;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

}