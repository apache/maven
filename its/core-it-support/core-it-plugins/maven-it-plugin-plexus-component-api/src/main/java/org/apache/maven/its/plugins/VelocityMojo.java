/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.its.plugins;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.velocity.VelocityComponent;

/**
 */
@Mojo(name = "velocity", defaultPhase = LifecyclePhase.VALIDATE)
public class VelocityMojo extends AbstractMojo {
    /**
     */
    @Component
    protected VelocityComponent velocityComponent;

    public void execute() throws MojoExecutionException, MojoFailureException {
        // velocityComponent should not be null
        velocityComponent.getEngine();

        try {
            // velocityComponent engine should not be null
            // this is the real test to check that we got the right Initializable interface in both Plexus and the
            // component
            /*
             * NOTE: There's a bug in the plexus-velocity:1.1.7 component that fails to transform "/template.vm" into
             * a proper resource name before searching the context class loader so we avoid the leading slash here.
             */
            velocityComponent.getEngine().getTemplate("template.vm");
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
