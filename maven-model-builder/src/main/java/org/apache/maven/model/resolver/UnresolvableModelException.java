package org.apache.maven.model.resolver;

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

/**
 * Signals an error when resolving the path to an external model.
 * 
 * @author Benjamin Bentmann
 */
public class UnresolvableModelException
    extends Exception
{

    /**
     * Creates a new exception with specified detail message and cause.
     * 
     * @param message The detail message, may be {@code null}.
     * @param cause The cause, may be {@code null}.
     */
    public UnresolvableModelException( String message, Throwable cause )
    {
        super( message, cause );
    }

    /**
     * Creates a new exception with specified detail message.
     * 
     * @param message The detail message, may be {@code null}.
     */
    public UnresolvableModelException( String message )
    {
        super( message );
    }

}
