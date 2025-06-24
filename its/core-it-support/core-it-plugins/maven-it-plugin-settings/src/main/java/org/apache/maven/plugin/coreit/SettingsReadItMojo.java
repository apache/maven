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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;

/**
 * Goal which read settings and dump it to a file
 *
 */
@Mojo(name = "settings-read", defaultPhase = LifecyclePhase.VALIDATE)
public class SettingsReadItMojo extends AbstractMojo {
    @Parameter(defaultValue = "${settings}", required = true, readonly = true)
    private Settings settings;

    @Parameter(defaultValue = "target/settings-dump.xml", required = true)
    private File dumpFile;

    public void execute() throws MojoExecutionException {
        if (dumpFile.exists()) {
            dumpFile.delete();
        }
        dumpFile.getParentFile().mkdirs();
        try (FileWriter fw = new FileWriter(dumpFile)) {
            SettingsXpp3Writer writer = new SettingsXpp3Writer();
            writer.write(fw, settings);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
