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
package com.gitlab.tkslaw.ditests;

import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.Mojo;

public class DITestsMojoBase implements Mojo {
    @Inject
    protected Log log;

    @Inject
    protected Session session;

    @Inject
    protected Project project;

    @Override
    public void execute() {
        log.info(() -> "log = " + log);
        log.info(() -> "session = " + session);
        log.info(() -> "project = " + project);
    }

    protected void logService(String name, Object service) {
        log.info(() -> "   | %s = %s".formatted(name, service));
    }
}
