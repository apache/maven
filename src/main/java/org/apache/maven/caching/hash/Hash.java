/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.caching.hash;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Hash
 */
public class Hash
{

    /**
     * Algorithm
     */
    public interface Algorithm
    {

        byte[] hash( byte[] array );

        byte[] hash( Path path ) throws IOException;
    }

    /**
     * accumulates states and should be completed by {@link #digest()}
     */
    public interface Checksum
    {

        void update( byte[] hash );

        byte[] digest();
    }

    /**
     * Factory
     */
    public interface Factory
    {

        String getAlgorithm();

        Algorithm algorithm();

        Checksum checksum( int count );
    }
}
