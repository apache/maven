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
package org.apache.maven.its.mng8572.extension;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;

/**
 * A no-operation Mojo that does nothing. This is just to make the plugin valid.
 * The real functionality is in the TypeProvider.
 *
 * @goal noop
 */
@org.apache.maven.api.plugin.annotations.Mojo(name = "noop")
public class NoOpMojo implements Mojo {

    @Inject
    private Log log;

    @Parameter(defaultValue = "${project.name}", readonly = true)
    private String projectName;

    @Override
    public void execute() {
        log.info("NoOpMojo executed for project: " + projectName);
        log.info("This Mojo does nothing. The real functionality is in the TypeProvider.");
    }
}
