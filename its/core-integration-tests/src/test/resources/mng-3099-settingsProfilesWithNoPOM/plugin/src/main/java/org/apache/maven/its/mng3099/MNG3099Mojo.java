package org.apache.maven.its.mng3099;

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

/**
 * Tests MNG-3099.
 *
 * @goal profile-props
 * @requiresProject false
 */
public final class MNG3099Mojo extends AbstractMojo {

    /**
     * @parameter expression="${local-profile-prop}"
     */
    private String localProfileProp; 
    
    
    public void execute() throws MojoExecutionException {
    	
    	this.getLog().info("local-profile-prop=" + this.localProfileProp);
    }
}
