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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.marmalade.model.MarmaladeScript;
import org.codehaus.marmalade.runtime.DefaultContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;
import org.codehaus.plexus.component.MapOrientedComponent;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author jdcasey
 */
public class MarmaladeMojo
    extends AbstractMojo
    implements MapOrientedComponent
{

    private MarmaladeScript script;

    private Map contextMap = new TreeMap();

    public MarmaladeMojo( MarmaladeScript script )
    {
        this.script = script;
    }

    public void execute()
        throws MojoExecutionException
    {
        MarmaladeExecutionContext context = new DefaultContext( contextMap );

        StringWriter sOutWriter = new StringWriter();
        PrintWriter outWriter = new PrintWriter( sOutWriter );

        context.setOutWriter( outWriter );

        StringWriter sErrWriter = new StringWriter();
        PrintWriter errWriter = new PrintWriter( sErrWriter );

        context.setErrWriter( errWriter );

        try
        {
            script.execute( context );
        }
        catch ( MarmaladeExecutionException e )
        {
            throw new MojoExecutionException( "[ERROR] While executing mojo script.\n Error: " + e.getLocalizedMessage(), e );
        }

        StringBuffer output = sOutWriter.getBuffer();
        if ( output.length() > 0 )
        {
            getLog().info( output );
        }

        StringBuffer error = sErrWriter.getBuffer();
        if ( error.length() > 0 )
        {
            getLog().error( error );
        }

        // TODO: need to be able to pass back results
        //        Map externalizedVars = context.getExternalizedVariables();
        //        for ( Iterator it = externalizedVars.entrySet().iterator(); it.hasNext(); )
        //        {
        //            Map.Entry entry = (Map.Entry) it.next();
        //
        //            request.addContextValue( entry.getKey(), entry.getValue() );
        //        }
    }

    public void addComponentRequirement( ComponentRequirement requirementDescriptor, Object requirementValue )
    {
        String key = requirementDescriptor.getFieldName();

        if ( StringUtils.isEmpty( key ) )
        {
            key = requirementDescriptor.getRole();
        }

        contextMap.put( key, requirementValue );
    }

    public void setComponentConfiguration( Map componentConfiguration )
    {
        contextMap.putAll( componentConfiguration );
    }

}