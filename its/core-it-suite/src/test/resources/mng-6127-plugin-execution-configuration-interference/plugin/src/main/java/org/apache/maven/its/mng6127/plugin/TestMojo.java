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
package org.apache.maven.its.mng6127.plugin;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Test mojo.
 *
 * @goal test
 */
public class TestMojo extends AbstractMojo {
    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     */
    private MavenProject project;

    /**
     * The name to write.
     *
     * @parameter
     */
    private String name;

    /**
     * The second name to write.
     *
     * @parameter
     */
    private String secondName;

    public void execute() throws MojoExecutionException {
        File file = new File(project.getBasedir(), "configuration.txt");
        file.getParentFile().mkdirs();

        Writer w = null;

        try {
            w = new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8");
            if (name != null) {
                w.write("name=" + name + ", ");
            }
            w.write("secondName=" + secondName);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
