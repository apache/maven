package org.apache.maven.repository.legacy.resolver.conflict;

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

/**
 * Indicates that a specified conflict resolver implementation could not be found.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @since 3.0
 */
public class ConflictResolverNotFoundException
    extends Exception
{
    // constants --------------------------------------------------------------

    /** The serial version ID. */
    private static final long serialVersionUID = 3372412184339653914L;

    // constructors -----------------------------------------------------------

    /**
     * Creates a new <code>ConflictResolverNotFoundException</code> with the specified message.
     *
     * @param message the message
     */
    public ConflictResolverNotFoundException( String message )
    {
        super( message );
    }
}
