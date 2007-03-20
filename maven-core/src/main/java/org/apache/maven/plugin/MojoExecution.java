package org.apache.maven.plugin;

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

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a single mojo invocation.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class MojoExecution
{
    private final String executionId;

    private final MojoDescriptor mojoDescriptor;

    private Xpp3Dom configuration;

    public MojoExecution( MojoDescriptor mojoDescriptor )
    {
        this.mojoDescriptor = mojoDescriptor;
        this.executionId = null;
        this.configuration = null;
    }

    public MojoExecution( MojoDescriptor mojoDescriptor, String executionId )
    {
        this.mojoDescriptor = mojoDescriptor;
        this.executionId = executionId;
        this.configuration = null;
    }

    public MojoExecution( MojoDescriptor mojoDescriptor, Xpp3Dom configuration )
    {
        this.mojoDescriptor = mojoDescriptor;
        this.configuration = configuration;
        this.executionId = null;
    }

    public String getExecutionId()
    {
        return executionId;
    }

    public MojoDescriptor getMojoDescriptor()
    {
        return mojoDescriptor;
    }

    public Xpp3Dom getConfiguration()
    {
        return configuration;
    }

    public void setConfiguration( Xpp3Dom configuration )
    {
        this.configuration = configuration;
    }
}
