package org.apache.maven.artifact.ant;

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

import org.apache.maven.artifact.manager.DefaultWagonManager;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.providers.scm.ScmWagon;

import java.io.File;

/**
 * Custom wagon manager for the ant tasks - used to set the SCM checkout directory to the local repository.
 *
 * @todo find a better way and share with m2
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class CustomWagonManager
    extends DefaultWagonManager
{
    private File localRepository;

    public Wagon getWagon( String protocol )
        throws UnsupportedProtocolException
    {
        Wagon wagon = super.getWagon( protocol );

        if ( protocol.equals( "scm" ) )
        {
            ((ScmWagon)wagon).setCheckoutDirectory( localRepository );
        }

        return wagon;
    }

    public void setLocalRepository( File localRepository )
    {
        this.localRepository = localRepository;
    }

}
