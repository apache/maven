package org.apache.maven.lifecycle;

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
 * Signals a failure to locate a lifecycle mapping.
 *
 * @author Christian Schulte
 *
 * @since 3.6.0
 */
public final class LifecycleMappingNotFoundException extends Exception
{

    private String packaging;

    public LifecycleMappingNotFoundException( final String packaging )
    {
        super( String.format( "No lifecycle mapping found for packaging '%s'.", packaging ) );
        this.packaging = packaging;
    }

    public String getPackaging()
    {
        return this.packaging;
    }

}
