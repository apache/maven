package org.apache.maven.script.marmalade;

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

import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.FailureResponse;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.codehaus.marmalade.model.MarmaladeScript;
import org.codehaus.marmalade.runtime.DefaultContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;

import java.util.Iterator;
import java.util.Map;

/**
 * @author jdcasey
 */
public class MarmaladeMojo
    extends AbstractPlugin
{

    private MarmaladeScript script;

    public MarmaladeMojo( MarmaladeScript script )
    {
        this.script = script;
    }

    public void execute( PluginExecutionRequest request, PluginExecutionResponse response ) throws Exception
    {

        MarmaladeExecutionContext context = new DefaultContext();

        context.setVariable( MarmaladeMojoExecutionDirectives.REQUEST_INVAR, request );
        context.setVariable( MarmaladeMojoExecutionDirectives.RESPONSE_INVAR, response );

        try
        {
            script.execute( context );
        }
        catch ( MarmaladeExecutionException e )
        {
            FailureResponse failure = new MarmaladeMojoFailureResponse( script.getLocation(), e );
            response.setExecutionFailure( failure );
        }

        Map externalizedVars = context.getExternalizedVariables();
        for ( Iterator it = externalizedVars.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
            request.addContextValue( entry.getKey(), entry.getValue() );
        }
    }

}