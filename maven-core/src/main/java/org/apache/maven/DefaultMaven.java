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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.lifecycle.goal.GoalNotFoundException;
import org.apache.maven.lifecycle.session.MavenSession;
import org.apache.maven.lifecycle.session.MavenSessionPhaseManager;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reactor.ReactorException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestHandler;
import org.apache.maven.execution.manager.MavenExecutionRequestHandlerManager;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.execution.manager.MavenExecutionRequestHandlerManager;

import org.codehaus.plexus.ArtifactEnabledContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public class DefaultMaven
    extends AbstractLogEnabled
    implements Maven
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private I18N i18n;

    private MavenExecutionRequestHandlerManager requestHandlerManager;

    // ----------------------------------------------------------------------
    // Project execution
    // ----------------------------------------------------------------------

    public MavenExecutionResponse execute( MavenExecutionRequest request )
        throws GoalNotFoundException, Exception
    {
        MavenExecutionRequestHandler handler = (MavenExecutionRequestHandler) requestHandlerManager.lookup( request.getType() );

        MavenExecutionResponse response = new MavenExecutionResponse();

        handler.handle( request, response );

        return response;
    }
}
