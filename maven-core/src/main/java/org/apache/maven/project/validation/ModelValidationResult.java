package org.apache.maven.project.validation;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class ModelValidationResult
{
    /** */
    private final static String NEWLINE = System.getProperty( "line.separator" );

    /** */
    private List messages;

    public ModelValidationResult()
    {
        messages = new ArrayList();
    }

    public int getMessageCount()
    {
        return messages.size();
    }

    public String getMessage( int i )
    {
        return messages.get( i ).toString();
    }

    public List getMessages()
    {
        return Collections.unmodifiableList( messages );
    }

    public void addMessage( String message )
    {
        messages.add( message );
    }

    public String toString()
    {
        return render( "" );
    }

    public String render( String indentation )
    {
        if ( messages.size() == 0 )
        {
            return indentation + "There were no validation errors.";
        }

        StringBuffer message = new StringBuffer();

//        if ( messages.size() == 1 )
//        {
//            message.append( "There was 1 validation error: " );
//        }
//        else
//        {
//            message.append( "There was " + messages.size() + " validation errors: " + NEWLINE );
//        }
//
        for ( int i = 0; i < messages.size(); i++ )
        {
            message.append( indentation + "[" + i + "]  " + messages.get( i ).toString() + NEWLINE );
        }

        return message.toString();
    }
}
