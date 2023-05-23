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
package org.apache.maven.api.build.plexus;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface BuildContext {
    int SEVERITY_WARNING = 1;

    int SEVERITY_ERROR = 2;

    // TODO should we add File getBasedir()?

    /**
     * Returns <code>true</code> if file or folder identified by <code>relpath</code> has
     * changed since last build.
     *
     * @param relpath is path relative to build context basedir
     */
    boolean hasDelta(String relpath);

    /**
     * Returns <code>true</code> if the file has changed since last build or is not
     * under basedir.
     *
     * @since 0.0.5
     */
    boolean hasDelta(File file);

    /**
     * Returns <code>true</code> if any file or folder identified by <code>relpaths</code> has
     * changed since last build.
     *
     * @param relpaths List<String> are paths relative to build context basedir
     */
    boolean hasDelta(List<File> relpaths);

    /**
     * Indicates that the file or folder content has been modified during the build.
     *
     * @see #newFileOutputStream(File)
     */
    void refresh(File file);

    /**
     * Returns new OutputStream that writes to the <code>file</code>.
     *
     * Files changed using OutputStream returned by this method do not need to be
     * explicitly refreshed using {@link #refresh(File)}.
     *
     * As an optional optimisation, OutputStreams created by incremental build
     * context will attempt to avoid writing to the file if file content
     * has not changed.
     */
    OutputStream newFileOutputStream(File file) throws IOException;

    /**
     * Convenience method, fully equal to newScanner(basedir, false)
     */
    Scanner newScanner(File basedir);

    /**
     * Returned Scanner scans <code>basedir</code> for files and directories
     * deleted since last build. Returns empty Scanner if <code>basedir</code>
     * is not under this build context basedir.
     */
    Scanner newDeleteScanner(File basedir);

    /**
     * Returned Scanner scans files and folders under <code>basedir</code>.
     *
     * If this is an incremental build context and  <code>ignoreDelta</code>
     * is <code>false</code>, the scanner will only "see" files and folders with
     * content changes since last build.
     *
     * If <code>ignoreDelta</code> is <code>true</code>, the scanner will "see" all
     * files and folders.
     *
     * Please beware that ignoreDelta=false does NOT work reliably for operations
     * that copy resources from source to target locations. Returned Scanner
     * only scans changed source resources and it does not consider changed or deleted
     * target resources. This results in missing or stale target resources.
     * Starting with 0.5.0, recommended way to process resources is to use
     * #newScanner(basedir,true) to locate all source resources and {@link #isUptodate(File, File)}
     * to optimized processing of uptodate target resources.
     *
     * Returns empty Scanner if <code>basedir</code> is not under this build context basedir.
     *
     * @see http://jira.codehaus.org/browse/MSHARED-125
     */
    Scanner newScanner(File basedir, boolean ignoreDelta);

    /**
     * Returns <code>true</code> if this build context is incremental.
     *
     * Scanners created by {@link #newScanner(File)} of an incremental build context
     * will ignore files and folders that were not changed since last build.
     * Additionally, {@link #newDeleteScanner(File)} will scan files and directories
     * deleted since last build.
     */
    boolean isIncremental();

    /**
     * Associate specified <code>key</code> with specified <code>value</code>
     * in the build context.
     *
     * Primary (and the only) purpose of this method is to allow preservation of
     * state needed for proper incremental behaviour between consecutive executions
     * of the same mojo needed to.
     *
     * For example, maven-plugin-plugin:descriptor mojo
     * can store collection of extracted MojoDescritpor during first invocation. Then
     * on each consecutive execution maven-plugin-plugin:descriptor will only need
     * to extract MojoDescriptors for changed files.
     *
     * @see #getValue(String)
     */
    void setValue(String key, Object value);

    /**
     * Returns value associated with <code>key</code> during previous mojo execution.
     *
     * This method always returns <code>null</code> for non-incremental builds
     * (i.e., {@link #isIncremental()} returns <code>false</code>) and mojos are
     * expected to fall back to full, non-incremental behaviour.
     *
     * @see #setValue(String, Object)
     * @see #isIncremental()
     */
    Object getValue(String key);

    /**
     * @deprecated Use addMessage with severity=SEVERITY_ERROR instead
     * @since 0.0.5
     */
    void addWarning(File file, int line, int column, String message, Throwable cause);

    /**
     * @deprecated Use addMessage with severity=SEVERITY_WARNING instead
     * @since 0.0.5
     */
    void addError(File file, int line, int column, String message, Throwable cause);

    /**
     * Adds a message to the build context. The message is associated with a file and a location inside that file.
     *
     * @param file The file or folder with which the message is associated. Should not be null and it is recommended to be
     *          an absolute path.
     * @param line The line number inside the file. Use 1 (not 0) for the first line. Use 0 for unknown/unspecified.
     * @param column The column number inside the file. Use 1 (not 0) for the first column. Use 0 for unknown/unspecified.
     * @param severity The severity of the message: SEVERITY_WARNING or SEVERITY_ERROR.
     * @param cause A Throwable object associated with the message. Can be null.
     * @since 0.0.7
     */
    void addMessage(File file, int line, int column, String message, int severity, Throwable cause);

    /**
     * Removes all messages associated with a file or folder during a previous build. It does not affect the messages
     * added during the current build.
     *
     * @since 0.0.7
     */
    void removeMessages(File file);

    /**
     * Returns true, if the target file exists and is uptodate compared to the source file.
     *
     * More specifically, this method returns true when both target and source files exist,
     * do not have changes since last incremental build and the target file was last modified
     * later than the source file. Returns false in all other cases.
     *
     * @since 0.0.5
     */
    boolean isUptodate(File target, File source);
}
