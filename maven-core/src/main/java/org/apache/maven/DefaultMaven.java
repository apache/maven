package org.apache.maven;

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

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestHandler;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.execution.manager.MavenExecutionRequestHandlerManager;
import org.apache.maven.lifecycle.goal.GoalNotFoundException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.logging.AbstractLogEnabled;

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
