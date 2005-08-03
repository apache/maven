package org.apache.maven.plugins.release;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.release.helpers.ReleaseProgressTracker;
import org.apache.maven.plugins.release.helpers.ScmHelper;
import org.apache.maven.scm.manager.ScmManager;

/**
 * 
 * @author <a href="mailto:jdcasey@apache.org">John Casey</a>
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public abstract class AbstractReleaseMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${org.apache.maven.scm.manager.ScmManager}"
     * @required
     * @readonly
     */
    private ScmManager scmManager;

    private ScmHelper scmHelper;

    protected abstract ReleaseProgressTracker getReleaseProgress()
        throws MojoExecutionException;

    protected ScmHelper getScm()
        throws MojoExecutionException
    {
        if ( scmHelper == null )
        {
            scmHelper = new ScmHelper();

            scmHelper.setScmManager( scmManager );

            ReleaseProgressTracker releaseProgress = getReleaseProgress();

            scmHelper.setUrl( releaseProgress.getScmUrl() );

            scmHelper.setTag( releaseProgress.getScmTag() );

            scmHelper.setTagBase( releaseProgress.getScmTagBase() );

            scmHelper.setUsername( releaseProgress.getUsername() );

            scmHelper.setPassword( releaseProgress.getPassword() );
        }

        return scmHelper;
    }

    public void execute()
        throws MojoExecutionException
    {
        executeTask();
    }

    protected abstract void executeTask()
        throws MojoExecutionException;

}
