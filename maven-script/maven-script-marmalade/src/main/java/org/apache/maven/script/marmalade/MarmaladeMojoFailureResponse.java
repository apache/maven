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

import org.apache.maven.plugin.FailureResponse;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author jdcasey Created on Feb 8, 2005
 */
public class MarmaladeMojoFailureResponse
    extends FailureResponse
{

    private final String scriptLocation;

    private final MarmaladeExecutionException error;

    public MarmaladeMojoFailureResponse( String scriptLocation, MarmaladeExecutionException error )
    {
        super( scriptLocation );

        this.scriptLocation = scriptLocation;
        this.error = error;
    }

    public String shortMessage()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "Script: " ).append( scriptLocation ).append( " failed to execute." );
        buffer.append( "\nError: " ).append( error.getLocalizedMessage() );
        return buffer.toString();
    }

    public String longMessage()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "Script: " ).append( scriptLocation ).append( " failed to execute." );
        buffer.append( "\nError:\n" );

        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter( sWriter );

        error.printStackTrace( pWriter );

        buffer.append( sWriter.toString() );

        pWriter.close();

        return buffer.toString();
    }

}