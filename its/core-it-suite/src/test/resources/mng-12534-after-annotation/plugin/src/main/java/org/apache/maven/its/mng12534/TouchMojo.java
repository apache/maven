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
package org.apache.maven.its.mng12534;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.Project;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.annotations.After;
import org.apache.maven.api.plugin.annotations.Mojo;

/**
 * V4 Mojo that creates a marker file to prove the goal was executed.
 * The {@code @After} annotation declares that this mojo must run after
 * the "resources" phase. This is a real ordering constraint because
 * in the V4 lifecycle, "compile" and "resources" are parallel siblings
 * (compile depends on sources, not resources) — so without this
 * {@code @After} link, compile could start before resources completes.
 * <p>
 * The handcrafted V2 plugin descriptor mirrors this as an
 * {@code <afterLink>} element — once maven-plugin-tools learns to
 * scan {@code @After}, the descriptor will be generated automatically.
 */
@Mojo(name = "touch", defaultPhase = "compile")
@After(phase = "resources")
public class TouchMojo implements org.apache.maven.api.plugin.Mojo {

    @Inject
    private Log log;

    @Inject
    private Project project;

    @Override
    public void execute() throws Exception {
        log.info("[MNG-12534] touch goal executed - afterLinks wired correctly");
        Path targetDir = project.getBasedir().resolve("target");
        Files.createDirectories(targetDir);
        Path touchFile = targetDir.resolve("touch.txt");
        if (!Files.exists(touchFile)) {
            Files.createFile(touchFile);
        }
    }
}
