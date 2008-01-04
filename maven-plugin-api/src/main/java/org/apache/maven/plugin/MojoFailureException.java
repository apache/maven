package org.apache.maven.plugin;

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
 * An exception occuring during the execution of a plugin (such as a compilation failure).
 * <br/>
 * Throwing this exception causes a "BUILD FAILURE" message to be displayed.
 *
 * @author Brett Porter
 * @version $Id$
 */
public class MojoFailureException
    extends AbstractMojoExecutionException
{
    /**
     * Construct a new <code>MojoFailureException</code> exception providing the source and a short and long message.
     *
     * @param source
     * @param shortMessage
     * @param longMessage
     */
    public MojoFailureException( Object source, String shortMessage, String longMessage )
    {
        super( shortMessage );
        this.source = source;
        this.longMessage = longMessage;
    }

    /**
     * Construct a new <code>MojoFailureException</code> exception providing a message.
     *
     * @param message
     */
    public MojoFailureException( String message )
    {
        super( message );
    }

    /**
     * Construct a new <code>MojoFailureException</code> exception wrapping an underlying <code>Throwable</code>
     * and providing a <code>message</code>.
     *
     * @param message
     * @param cause
     * @since 2.0.9
     */
    public MojoFailureException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
