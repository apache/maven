package org.apache.maven.its.plugins.plexuslifecycle;

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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;

/**
 * @author Olivier Lamy
 */
@Mojo( name = "do-nothing", defaultPhase = LifecyclePhase.VALIDATE )
public class MojoWithPlexusLifecycle
    extends AbstractMojo
    implements Contextualizable, Disposable
{
    /**
     */
    @Component
    private FakeComponent fakeComponent;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "execute MojoWithPlexusLifecycle" );
        fakeComponent.doNothing();
    }

    public void dispose()
    {
        getLog().info( "MojoWithPlexusLifecycle :: dispose" );
    }

    public void contextualize( Context context )
        throws ContextException
    {
        getLog().info( "MojoWithPlexusLifecycle :: contextualize" );
    }


}
