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
package org.apache.maven.api.services;

import java.io.Serial;

import org.apache.maven.api.annotations.Experimental;

/**
 *
 *
 * @since 4.0.0
 */
@Experimental
public class ArtifactResolverException extends MavenException {

    @Serial
    private static final long serialVersionUID = 7252294837746943917L;

    private final ArtifactResolverResult result;

    /**
     * @param message the message for the exception
     * @param e the exception itself
     * @param result the resolution result containing detailed information
     */
    public ArtifactResolverException(String message, Exception e, ArtifactResolverResult result) {
        super(message, e);
        this.result = result;
    }

    public ArtifactResolverResult getResult() {
        return result;
    }
}
