/*
 * Copyright 2005 The Apache Software Foundation.
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
package org.apache.maven.plugin.clover;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Taskdef;

import com.cenqua.clover.cfg.Percentage;
import com.cenqua.clover.tasks.CloverPassTask;

/**
 * @goal check
 * @phase verify
 * @execute phase="test" lifecycle="clover"
 * @description Verify test percentage coverage and fail the build if it is below the defined threshold
 *
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 * @version $Id$
 */
public class CloverCheckMojo extends AbstractCloverMojo
{
    /**
     * @parameter expression="${project.build.directory}/clover/clover.db"
     * @required
     */
	protected String cloverDatabase;

    /**
     * @parameter expression="70"
     * @required
     */
    protected float targetPercentage;

    public void execute() throws MojoExecutionException
    {
        registerLicenseFile();
        checkCoverage();
    }

    private void registerCloverAntTasks(Project antProject)
    {
        Taskdef taskdef = (Taskdef) antProject.createTask("taskdef");
        taskdef.setResource("clovertasks");
        taskdef.execute();
    }
    
    private void checkCoverage()
    {
        Project antProject = new Project();
        antProject.init();
        
        registerCloverAntTasks(antProject);

        CloverPassTask cloverPassTask = (CloverPassTask) antProject.createTask("clover-check");
        cloverPassTask.setInitString(this.cloverDatabase);
        cloverPassTask.setHaltOnFailure(true);
        cloverPassTask.setTarget(new Percentage(this.targetPercentage));
        cloverPassTask.execute();
    }

}
