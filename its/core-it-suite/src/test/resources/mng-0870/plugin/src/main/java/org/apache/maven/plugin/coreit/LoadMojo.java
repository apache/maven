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
package org.apache.maven.plugin.coreit;

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

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Creates a touch file if and only if a resource from the plugin dependency was successfully loaded, fails otherwise.
 *
 * @goal load
 * @phase validate
 *
 * @author Benjamin Bentmann
 *
 */
public class LoadMojo extends AbstractMojo {

    /**
     * The path to the output file, relative to the project base directory.
     *
     * @parameter expression="${touch.file}" default-value="target/touch.txt"
     */
    private File file;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If the output file could not be created.
     * @throws MojoFailureException If the output file has not been set.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        String resource = "mng0870.properties";

        getLog().info("[MAVEN-CORE-IT-LOG] Loading resource from plugin dependency: " + resource);

        URL url = getClass().getResource("/" + resource);

        getLog().info("[MAVEN-CORE-IT-LOG]   " + url);

        if (url == null) {
            throw new MojoExecutionException("Resource was not found, incomplete plugin class realm");
        }

        getLog().info("[MAVEN-CORE-IT-LOG] Creating output file: " + file);

        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException e) {
            throw new MojoExecutionException("Output file could not be created: " + file, e);
        }
    }
}
