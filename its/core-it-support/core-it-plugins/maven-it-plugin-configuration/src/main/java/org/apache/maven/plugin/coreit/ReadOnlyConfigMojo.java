package org.apache.maven.plugin.coreit;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Mojo for testing read-only parameters.
 *
 * @author Slawomir Jaranowski
 */
@Mojo( name = "read-only-config", defaultPhase = LifecyclePhase.VALIDATE )
public class ReadOnlyConfigMojo extends AbstractMojo
{
    /**
     * Only such has sense ...
     */
    @Parameter( defaultValue = "${project.version}", readonly = true )
    String readOnlyWithDefault;

    /**
     * strange definition ... but possible
     */
    @Parameter( readonly = true )
    private String readOnlyWithOutDefaults;

    @Parameter( property = "project.version", readonly = true )
    String readOnlyWithProperty;

    @Parameter( property = "user.property", readonly = true )
    String readOnlyWithUserProperty;

    @Override
    public void execute()
    {
        getLog().info( "[MAVEN-CORE-IT-LOG]" );
    }
}
