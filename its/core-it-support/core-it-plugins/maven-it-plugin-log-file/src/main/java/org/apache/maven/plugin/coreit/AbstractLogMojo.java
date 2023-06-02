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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Provides common services for the mojos of this plugin.
 *
 * @author Benjamin Bentmann
 *
 */
public abstract class AbstractLogMojo extends AbstractMojo {

    /**
     * The project's base directory, used for manual path translation.
     */
    @Parameter(defaultValue = "${basedir}", readonly = true)
    private File basedir;

    /**
     * The path to the output file, relative to the project's base directory.
     */
    @Parameter(property = "log.logFile")
    private File logFile;

    /**
     * The character encoding of the log file.
     */
    private String encoding = "UTF-8";

    /**
     * Gets the absolute path to the log file.
     *
     * @return The absolute path to the log file, never <code>null</code>.
     */
    private File getLogFile() {
        /*
         * NOTE: We don't want to test path translation here.
         */
        return logFile.isAbsolute() ? logFile : new File(basedir, logFile.getPath());
    }

    /**
     * Appends the string representation of the specified object to the log file. Logging <code>null</code> has no other
     * effect than touching the file. For each value different from <code>null</code>, a line terminator will be
     * appended to the value's string representation.
     *
     * @param value The object to log, may be <code>null</code>.
     * @throws MojoExecutionException If the log file could not be updated.
     */
    protected void append(Object value) throws MojoExecutionException {
        File file = getLogFile();
        getLog().info("[MAVEN-CORE-IT-LOG] Updating log file: " + file);
        getLog().info("[MAVEN-CORE-IT-LOG]   " + value);
        try {
            file.getParentFile().mkdirs();
            OutputStream out = new FileOutputStream(file, true);
            try {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, encoding));
                if (value != null) {
                    writer.write(value.toString());
                    writer.newLine();
                    writer.flush();
                }
            } finally {
                try {
                    out.close();
                } catch (IOException e) {
                    // just ignore, we tried our best to clean up
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to update log file " + logFile, e);
        }
    }

    /**
     * Clears the contents of the log file by creating a new empty log file.
     *
     * @throws MojoExecutionException If the log file could not be reset.
     */
    protected void reset() throws MojoExecutionException {
        File file = getLogFile();
        getLog().info("[MAVEN-CORE-IT-LOG] Resetting log file: " + file);
        try {
            /*
             * NOTE: Intentionally don't delete the file but create a new empty one to check the plugin was executed.
             */
            file.getParentFile().mkdirs();
            OutputStream out = new FileOutputStream(file);
            try {
                out.close();
            } catch (IOException e) {
                // just ignore, we tried our best to clean up
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to reset log file " + logFile, e);
        }
    }
}
