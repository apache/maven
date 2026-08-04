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

/**
 * Service Provider Interface (SPI) for the incremental build context.
 *
 * <h2>Overview</h2>
 *
 * <p>This package contains the interfaces that <strong>integrators</strong> implement
 * to connect the incremental build context to a specific runtime environment (Maven CLI,
 * an IDE, a custom build tool). Mojo authors typically do not implement these interfaces;
 * they use the consumer API in {@link org.apache.maven.api.build.incremental} instead.</p>
 *
 * <h2>Key SPI interfaces</h2>
 *
 * <table>
 *   <caption>SPI roles and their implementors</caption>
 *   <tr>
 *     <th>Interface</th>
 *     <th>Role</th>
 *     <th>Typical implementor</th>
 *   </tr>
 *   <tr>
 *     <td>{@link Workspace}</td>
 *     <td>File-system abstraction: read/write/delete files, detect changes</td>
 *     <td>IDE workspace adapter, filesystem implementation</td>
 *   </tr>
 *   <tr>
 *     <td>{@link IncrementalContextEnvironment}</td>
 *     <td>Provides the state file path, workspace, and mojo parameters</td>
 *     <td>Maven runtime, IDE plugin</td>
 *   </tr>
 *   <tr>
 *     <td>{@link IncrementalContextFinalizer}</td>
 *     <td>Commits all registered build contexts at the end of a mojo execution</td>
 *     <td>Maven runtime</td>
 *   </tr>
 *   <tr>
 *     <td>{@link CommittableIncrementalContext}</td>
 *     <td>Extends {@link org.apache.maven.api.build.incremental.IncrementalContext} with commit
 *         capabilities</td>
 *     <td>Build context implementation</td>
 *   </tr>
 * </table>
 *
 * <h2>How the pieces fit together</h2>
 *
 * <pre>
 *                +-----------------------+
 *                | IncrementalContextEnvironment|
 *                |  stateFile, workspace, |
 *                |  parameters, finalizer |
 *                +-----------+-----------+
 *                            |
 *                            v creates
 *                 +--------------------+
 *                 | CommittableIncrementalContext|
 *                 |  (extends IncrementalContext)|
 *                 +----------+---------+
 *                            |
 *           +----------------+----------------+
 *           |                                 |
 *           v reads/writes via                v at end of mojo
 *     +----------+                  +-------------------+
 *     | Workspace |                  | IncrementalContextFinalizer|
 *     |  file ops |                  |  commit()            |
 *     +----------+                  +-------------------+
 * </pre>
 *
 * <h2>Workspace modes</h2>
 *
 * <p>The {@link Workspace} abstraction supports four operating modes, allowing the
 * same mojo code to behave correctly in different environments:</p>
 * <ul>
 *   <li><strong>{@link Workspace.Mode#NORMAL NORMAL}</strong> — Full filesystem
 *       scan with timestamp/size comparison (command-line Maven).</li>
 *   <li><strong>{@link Workspace.Mode#ESCALATED ESCALATED}</strong> — All files treated
 *       as new (full rebuild). Used when the user explicitly requests a full rebuild or
 *       when the change set may be incomplete (e.g., after an IDE crash).</li>
 *   <li><strong>{@link Workspace.Mode#SUPPRESSED SUPPRESSED}</strong> — All inputs
 *       appear unmodified. Used for read-only analysis or when incremental support is
 *       explicitly disabled.</li>
 * </ul>
 *
 * <h2>Data classes</h2>
 *
 * <p>{@link FileState} is an immutable value object used to communicate file
 * metadata between the build context implementation and the workspace.</p>
 *
 * @since 4.1.0
 * @see org.apache.maven.api.build.incremental
 * @see Workspace
 * @see IncrementalContextEnvironment
 * @see CommittableIncrementalContext
 */
@Experimental
package org.apache.maven.api.build.incremental.spi;

import org.apache.maven.api.annotations.Experimental;
