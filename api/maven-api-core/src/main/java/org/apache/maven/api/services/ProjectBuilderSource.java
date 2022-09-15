package org.apache.maven.api.services;

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

import java.io.IOException;
import java.io.InputStream;

import org.apache.maven.api.annotations.Experimental;

/**
 * The source for a project's XML model.
 *
 * @since 4.0
 */
@Experimental
public interface ProjectBuilderSource
{
    InputStream getInputStream() throws IOException;

    String getLocation();

    /**
     * Returns a new source identified by a relative path. Implementation <strong>MUST</strong>
     * be able to accept <code>relPath</code> parameter values that
     * <ul>
     * <li>use either / or \ file path separator</li>
     * <li>have .. parent directory references</li>
     * <li>point either at file or directory.</li>
     * </ul>
     *
     * @param relative is the path of the requested source relative to this source.
     * @return related source or <code>null</code> if no such source.
     */
    ProjectBuilderSource resolve( String relative );
}
