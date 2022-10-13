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
package org.apache.maven.model;

/**
 * Interface InputLocationTracker.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public interface InputLocationTracker
{

    // -----------/
    // - Methods -/
    // -----------/

    /**
     * Gets the location of the specified field in the input source.
     * 
     * @param field The key of the field, must not be <code>null</code>.
     * @return The location of the field in the input source or <code>null</code> if unknown.
     */
    public InputLocation getLocation( Object field );

    /**
     * Sets the location of the specified field.
     * 
     * @param field The key of the field, must not be <code>null</code>.
     * @param location The location of the field, may be <code>null</code>.
     */
    public void setLocation( Object field, InputLocation location );
}
