package org.apache.maven.api.services;

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
 * The Exception class in case a resolving does not work.
 */
public class DependencyResolverException
    extends MavenException
{
    private static final long serialVersionUID = 5320065249974323888L;

    /**
     * @param cause The {@link Exception cause} of the problem.
     */
    protected DependencyResolverException( Exception cause )
    {
        super( cause );
    }

    /**
     * @param message The message to give.
     * @param e The {@link Exception}.
     */
    public DependencyResolverException( String message, Exception e )
    {
        super( message, e );
    }

    /**
     * @return {@link DependencyResolverResult}
     */
    public DependencyResolverResult getResult()
    {
        return null;
    }
}
