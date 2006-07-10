package org.apache.maven.bootstrap.download;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.bootstrap.model.Dependency;
import org.apache.maven.bootstrap.model.Model;

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

/**
 * Failed download.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class DownloadFailedException
    extends Exception
{
    public DownloadFailedException( String message )
    {
        super( message );
    }

    public DownloadFailedException( Dependency dep )
    {
        super( createMessage( dep ) );
    }

    private static String createMessage( Dependency dep )
    {
        String msg = "Failed to download dependency: \n\n" + dep + "\n\nChain:\n";

        List repos = new ArrayList();

        for ( Iterator it = dep.getChain().iterator(); it.hasNext(); )
        {
            Model chainDep = (Model) it.next();
            msg += "\n\t" + chainDep;
            repos.addAll( chainDep.getRepositories() );
        }

        msg += "\n\nfrom the following repositories:\n\n";

        for ( Iterator it = repos.iterator(); it.hasNext(); )
        {
            msg += "\n\t" + it.next();
        }

        return msg;
    }
}
