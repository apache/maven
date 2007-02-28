package org.apache.maven.usability.diagnostics;

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

public final class DiagnosisUtils
{
    private DiagnosisUtils()
    {
    }

    public static boolean containsInCausality( Throwable error, Class test )
    {
        Throwable cause = error;

        while ( cause != null )
        {
            if ( test.isInstance( cause ) )
            {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    public static Throwable getRootCause( Throwable error )
    {
        Throwable cause = error;

        while ( true )
        {
            Throwable nextCause = cause.getCause();

            if ( nextCause == null )
            {
                break;
            }
            else
            {
                cause = nextCause;
            }
        }

        return cause;
    }

    public static Throwable getFromCausality( Throwable error, Class targetClass )
    {
        Throwable cause = error;

        while ( cause != null )
        {
            if ( targetClass.isInstance( cause ) )
            {
                return cause;
            }

            cause = cause.getCause();
        }

        return null;
    }

    public static void appendRootCauseIfPresentAndUnique( Throwable error, StringBuffer message,
                                                          boolean includeTypeInfo )
    {
        if ( error == null )
        {
            return;
        }
        
        Throwable root = getRootCause( error );

        if ( root != null && !root.equals( error ) )
        {
            String rootMsg = root.getMessage();

            if ( rootMsg != null && ( error.getMessage() == null || error.getMessage().indexOf( rootMsg ) < 0 ) )
            {
                message.append( "\n" ).append( rootMsg );

                if ( includeTypeInfo )
                {
                    message.append( "\nRoot error type: " ).append( root.getClass().getName() );
                }
            }
        }
    }
}
