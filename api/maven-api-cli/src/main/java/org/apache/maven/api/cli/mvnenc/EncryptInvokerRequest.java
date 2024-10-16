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
package org.apache.maven.api.cli.mvnenc;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.cli.InvokerRequest;

/**
 * Represents a request to invoke the Maven encryption tool.
 * This interface extends the general InvokerRequest, specializing it for encryption-related operations.
 *
 * <p>An EncryptInvokerRequest encapsulates all the necessary information needed to perform
 * an encryption operation, including any encryption-specific options defined in EncryptOptions.</p>
 *
 * @since 4.0.0
 */
@Experimental
public interface EncryptInvokerRequest extends InvokerRequest<EncryptOptions> {
    // This interface doesn't declare any additional methods beyond those inherited from InvokerRequest.
    // It serves to type-specify the Options as EncryptOptions for encryption-related requests.
}
