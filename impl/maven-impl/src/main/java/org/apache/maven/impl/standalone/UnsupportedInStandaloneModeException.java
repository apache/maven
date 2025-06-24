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
package org.apache.maven.impl.standalone;

/**
 * Exception thrown when attempting to use features that are not supported in standalone mode.
 * <p>
 * This exception indicates that a requested operation is only available when running
 * within a full Maven build context, not in the simplified standalone environment.
 * </p>
 *
 * @since 4.0.0
 */
public class UnsupportedInStandaloneModeException extends UnsupportedOperationException {

    /**
     * Constructs a new exception with a default message.
     */
    public UnsupportedInStandaloneModeException() {
        super("This operation is not supported in standalone mode");
    }

    /**
     * Constructs a new exception with a specific message.
     *
     * @param message the detail message
     */
    public UnsupportedInStandaloneModeException(String message) {
        super(message);
    }
}
