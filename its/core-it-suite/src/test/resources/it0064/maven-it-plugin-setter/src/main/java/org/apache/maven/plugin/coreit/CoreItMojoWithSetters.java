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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Mojo to check that attribute injection through setter method (instead of direct parameter injection) works.
 */
@Mojo(name = "setter-touch")
public class CoreItMojoWithSetters extends AbstractMojo {
    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private String outputDirectoryValue;

    /**
     */
    @Parameter(name = "foo")
    private String fooValue;

    /**
     */
    @Parameter
    private String bar;

    // ----------------------------------------------------------------------
    // Setters
    // ----------------------------------------------------------------------

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectoryValue = outputDirectory;
    }

    boolean setFooSetterExecuted;

    public void setFoo(String fooValue) {

        getLog().info("setFoo: " + fooValue);

        this.fooValue = fooValue;

        setFooSetterExecuted = true;
    }

    boolean setBarSetterExecuted;

    public void setBar(String barValue) {

        getLog().info("setBar: " + barValue);

        this.bar = barValue + ".baz";

        setBarSetterExecuted = true;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void execute() throws MojoExecutionException {
        touch(new File(outputDirectoryValue), "touch.txt");

        File outDir = new File(outputDirectoryValue);

        // Test parameter setting
        if (fooValue != null && setFooSetterExecuted) {

            getLog().info("fooValue != null && setFooSetterExecuted");

            touch(outDir, fooValue);
        }

        if (bar != null && setBarSetterExecuted) {

            getLog().info("bar != null && setBarSetterExecuted");

            touch(outDir, bar);
        }
    }

    private void touch(File dir, String file) throws MojoExecutionException {

        getLog().info("touch: " + dir.getPath() + ":" + file);

        try {
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File touch = new File(dir, file);

            FileWriter w = new FileWriter(touch);

            w.write(file);

            w.close();
        } catch (IOException e) {
            throw new MojoExecutionException("Error touching file", e);
        }
    }
}
