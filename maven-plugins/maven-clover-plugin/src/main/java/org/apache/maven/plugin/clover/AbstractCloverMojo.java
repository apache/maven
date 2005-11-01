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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Taskdef;

public abstract class AbstractCloverMojo extends AbstractMojo
{
    /**
     * @parameter
     */
    private String licenseFile;

    /**
     * The <a href="http://cenqua.com/clover/doc/adv/flushpolicies.html">Clover flush policy</a>
     * to use. Valid values are <code>directed</code>, <code>interval</code> and
     * <code>threaded</code>.
     *  
     * @parameter default-value="threaded"
     */
    protected String flushPolicy;

    /**
     * When the Clover Flush Policy is set to "interval" or threaded this value is the minimum 
     * period between flush operations (in milliseconds).
     *
     * @parameter default-value="500"
     */
    protected int flushInterval;

    /**
     * If true we'll wait 2*flushInterval to ensure coverage data is flushed to the Clover 
     * database before running any query on it. 
     * 
     * Note: The only use case where you would want to turn this off is if you're running your 
     * tests in a separate JVM. In that case the coverage data will be flushed by default upon
     * the JVM shutdown and there would be no need to wait for the data to be flushed. As we
     * can't control whether users want to fork their tests or not, we're offering this parameter
     * to them.  
     * 
     * @parameter default-value="true"
     */
    protected boolean waitForFlush;
    
    /**
     * Whether the Clover instrumentation should use the Clover <code>jdk14</code> or
     * <code>jdk15</code> flags to parse sources.
     *
     * @parameter
     */
    protected String jdk;

    /**
     * Registers the license file for Clover runtime by setting the
     * <code>clover.license.path</code> system property. If the <code>licenseFile</code>
     * property has not been defined by the user we look it up in the classpath in
     * <code>/clover.license</code>.
     */
    protected void registerLicenseFile()
    {
        String licenseToUse = this.licenseFile;

        if (licenseToUse == null)
        {
            licenseToUse = getClass().getResource("/clover.license").getFile();
        }

        System.setProperty("clover.license.path", licenseToUse);
    }

    protected Project registerCloverAntTasks()
    {
        Project antProject = new Project();
        antProject.init();

        Taskdef taskdef = (Taskdef) antProject.createTask( "taskdef" );
        taskdef.setResource( "clovertasks" );
        taskdef.execute();

        return antProject;
    }

    /**
     * Wait 2*'flush interval' milliseconds to ensure that the coverage data have been flushed.
     */
    protected void waitForFlush()
    {
        if ( this.waitForFlush )
        {
            try
            {
                Thread.sleep( 2 * this.flushInterval );
            }
            catch ( InterruptedException e )
            {
                // Nothing to do... Just go on and try to check for coverage.
            }
        }
    }
}
