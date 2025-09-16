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
package ${package};

/**
 * Tracks input locations for model fields.
 * <p>
 * Implementations store a mapping from keys (usually field names or indices) to
 * the corresponding InputLocation in the source. Keys must be non-null.
 */
public interface InputLocationTracker {
    /**
     * Gets the location of the specified field in the input source.
     *
     * @param field the key of the field, must not be {@code null}
     * @return the location of the field in the input source or {@code null} if unknown
     * @throws NullPointerException if {@code field} is {@code null}
     */
    InputLocation getLocation(Object field);
}
