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
import org.apache.maven.api.cli.Invoker;
import org.apache.maven.api.cli.InvokerException;

/**
 * Defines the contract for a component responsible for invoking the Maven encryption tool.
 * This interface extends the general Invoker interface, specializing it for encryption-related operations.
 *
 * <p>The EncryptInvoker is designed to handle encryption tasks within the Maven ecosystem,
 * such as encrypting passwords or other sensitive information in Maven settings files.</p>
 *
 * @since 4.0.0
 */
@Experimental
public interface EncryptInvoker extends Invoker<EncryptInvokerRequest> {
    /**
     * Invokes the encryption tool using the provided EncryptInvokerRequest.
     * This method is responsible for executing the encryption command or process
     * based on the information contained in the request.
     *
     * @param invokerRequest the request containing all necessary information for the encryption invocation
     * @return an integer representing the exit code of the invocation (0 typically indicates success)
     * @throws InvokerException if an error occurs during the encryption process
     */
    @Override
    int invoke(EncryptInvokerRequest invokerRequest) throws InvokerException;
}
