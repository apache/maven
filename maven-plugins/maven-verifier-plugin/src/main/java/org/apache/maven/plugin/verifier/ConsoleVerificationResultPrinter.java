package org.apache.maven.plugin.verifier;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.plugin.logging.Log;

import java.util.Iterator;

public class ConsoleVerificationResultPrinter
    implements VerificationResultPrinter
{
    private Log log;

    public ConsoleVerificationResultPrinter( Log log )
    {
        this.log = log;
    }

    public void print( VerificationResult results )
    {
        printExistenceFailures( results );
        printNonExistenceFailures( results );
        printContentFailures( results );
    }

    private void printExistenceFailures( VerificationResult results )
    {
        for ( Iterator i = results.getExistenceFailures().iterator(); i.hasNext(); )
        {
            org.apache.maven.plugin.verifier.model.File file = (org.apache.maven.plugin.verifier.model.File) i.next();

            printMessage( "File not found [" + file.getLocation() + "]" );
        }
    }

    private void printNonExistenceFailures( VerificationResult results )
    {
        for ( Iterator i = results.getNonExistenceFailures().iterator(); i.hasNext(); )
        {
            org.apache.maven.plugin.verifier.model.File file = (org.apache.maven.plugin.verifier.model.File) i.next();

            printMessage( "File should not exist [" + file.getLocation() + "]" );
        }
    }

    private void printContentFailures( VerificationResult results )
    {
        for ( Iterator i = results.getContentFailures().iterator(); i.hasNext(); )
        {
            org.apache.maven.plugin.verifier.model.File file = (org.apache.maven.plugin.verifier.model.File) i.next();

            printMessage( "File [" + file.getLocation() + "] does not match regexp [" + file.getContains() + "]" );
        }
    }

    private void printMessage( String message )
    {
        this.log.error( "[Verifier] " + message );
    }
}
