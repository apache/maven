package org.apache.maven.tools.repoclean;

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

import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.embed.Embedder;

/**
 * @author jdcasey
 */
public class Main
{

    public static void main( String[] inputArgs )
    {
        Embedder embedder = new Embedder();
        try
        {
            embedder.start( new ClassWorld() );

            String[] args = inputArgs;

            if ( args.length < 2 )
            {
                printUsage();
                System.exit( 0 );
            }

            boolean reportOnly = false;
            if ( args.length > 2 )
            {
                reportOnly = Boolean.valueOf( args[2] ).booleanValue();
            }

            RepositoryCleaner cleaner = null;
            try
            {
                cleaner = (RepositoryCleaner) embedder.lookup( RepositoryCleaner.ROLE );

                cleaner.cleanRepository( args[0], args[1], reportOnly );
            }
            finally
            {
                if ( cleaner != null )
                {
                    embedder.release( cleaner );
                }
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    private static void printUsage()
    {
        System.out.println( "No repository directory specified.\n\n" + "Usage:\n"
            + "--------------------------------------------------\n\n"
            + "repoclean <repository-path> <reports-path> [<report-only (use true|false)>]\n" );
    }

}