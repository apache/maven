/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.maven.artifact.router.conf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum ArtifactRouterOption
{

    update( "Update routes" ), 
    offline( "Offline. Use only stored routes." ), 
    clear( "Clear stored routes." ), 
    unset( "Unset defaults." );

    private String description;

    private ArtifactRouterOption( String description )
    {
        this.description = description;
    }

    public static String list( String separator )
    {
        StringBuilder sb = new StringBuilder();
        for ( ArtifactRouterOption opt : values() )
        {
            sb.append( separator ).append( opt.name().toLowerCase() ).append( " - " ).append( opt.description );
        }

        return sb.toString();
    }

    public static Set<ArtifactRouterOption> parse( String... values )
    {
        Set<ArtifactRouterOption> options = new HashSet<ArtifactRouterOption>();

        Map<String, ArtifactRouterOption> allOptions = new HashMap<String, ArtifactRouterOption>();
        for ( ArtifactRouterOption value : values() )
        {
            allOptions.put( value.name(), value );
        }

        all: for ( String value : values )
        {
            String[] parts = value.split( "[|,]" );
            for ( String part : parts )
            {
                ArtifactRouterOption opt = allOptions.remove( part.toLowerCase().trim() );
                if ( opt != null )
                {
                    options.add( opt );
                }

                if ( allOptions.isEmpty() )
                {
                    break all;
                }
            }
        }

        return options;
    }

}
