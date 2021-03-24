package org.apache.maven.model.building;

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

import org.apache.maven.building.StringSource;

/**
 * Wraps an ordinary {@link CharSequence} as a model source.
 *
 * @author Benjamin Bentmann
 *
 * @deprecated instead use {@link StringSource}
 */
@Deprecated
public class StringModelSource extends StringSource
    implements ModelSource
{

    /**
     * Creates a new model source backed by the specified string.
     *
     * @param pom The POM's string representation, may be empty or {@code null}.
     */
    public StringModelSource( CharSequence pom )
    {
        this( pom, null );
    }

    /**
     * Creates a new model source backed by the specified string.
     *
     * @param pom The POM's string representation, may be empty or {@code null}.
     * @param location The location to report for this use, may be {@code null}.
     */
    public StringModelSource( CharSequence pom, String location )
    {
        super( pom, location );
    }
}
