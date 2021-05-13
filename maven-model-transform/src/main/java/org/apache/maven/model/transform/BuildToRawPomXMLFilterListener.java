package org.apache.maven.model.transform;

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

import java.nio.file.Path;

/**
 * Listener can be used to capture the result of the transformation of build to raw POM.
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
@FunctionalInterface
public interface BuildToRawPomXMLFilterListener
{
    /**
     * Captures the result of the XML transformation
     *
     * @param pomFile the original to being transformed
     * @param b the byte array
     * @param off the offset
     * @param len the length
     */
    void write( Path pomFile, byte[] b, int off, int len );
}
