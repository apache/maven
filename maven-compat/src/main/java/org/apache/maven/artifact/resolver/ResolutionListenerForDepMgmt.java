package org.apache.maven.artifact.resolver;

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

import org.apache.maven.artifact.Artifact;

/**
 * Do not use!
 * <p>
 * Should only be implemented by DebugResolutionListener.  Remove this
 * when the ResolutionListener interface deprecation of the manageArtifact
 * method (and the [yet to be done] addition of these methods to that
 * interface) has had a chance to propagate to all interested plugins.
 */
@Deprecated
public interface ResolutionListenerForDepMgmt
{
    void manageArtifactVersion( Artifact artifact,
                                Artifact replacement );

    void manageArtifactScope( Artifact artifact,
                              Artifact replacement );

    void manageArtifactSystemPath( Artifact artifact,
                                   Artifact replacement );
}
