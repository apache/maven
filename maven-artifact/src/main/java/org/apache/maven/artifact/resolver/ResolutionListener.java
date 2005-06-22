package org.apache.maven.artifact.resolver;

import org.apache.maven.artifact.Artifact;

/*
* Copyright 2001-2005 The Apache Software Foundation.
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

/**
 * TODO: describe
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public interface ResolutionListener
{
    String ROLE = ResolutionListener.class.getName();

    int TEST_ARTIFACT = 1;

    int PROCESS_CHILDREN = 2;

    int FINISH_PROCESSING_CHILDREN = 3;

    int INCLUDE_ARTIFACT = 4;

    int OMIT_FOR_NEARER = 5;

    int UPDATE_SCOPE = 6;

    int MANAGE_ARTIFACT = 7;

    int OMIT_FOR_CYCLE = 8;

    void testArtifact( Artifact node );

    void startProcessChildren( Artifact artifact );

    void endProcessChildren( Artifact artifact );

    void includeArtifact( Artifact artifact );

    void omitForNearer( Artifact omitted, Artifact kept );

    void updateScope( Artifact artifact, String scope );

    void manageArtifact( Artifact artifact, Artifact replacement );

    void omitForCycle( Artifact artifact );
}
