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
package org.apache.maven.api.cli.mvn;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.cli.InvokerRequest;

/**
 * Represents a request to invoke Maven.
 * This interface extends the general {@link InvokerRequest}, specializing it for Maven-specific operations.
 *
 * <p>A {@link MavenInvokerRequest} encapsulates all the necessary information needed to perform
 * a Maven build, including any Maven-specific options defined in {@link MavenOptions}.</p>
 *
 * @param <O> The specific Options type this request carries
 *
 * @since 4.0.0
 */
@Experimental
public interface MavenInvokerRequest<O extends MavenOptions> extends InvokerRequest<O> {
    // This interface doesn't declare any additional methods beyond those inherited from InvokerRequest.
    // It serves to type-specify the Options as MavenOptions for Maven-specific requests.
}
