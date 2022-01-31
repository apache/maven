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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.maven.model.Model;

/**
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
class DefaultTransformerContext implements TransformerContext
{
    final Map<String, String> userProperties = new ConcurrentHashMap<>();

    final Map<Path, Holder> modelByPath = new ConcurrentHashMap<>();

    final Map<GAKey, Holder> modelByGA = new ConcurrentHashMap<>();

    public static class Holder
    {
        private volatile boolean set;
        private volatile Model model;

        Holder()
        {
        }

        public static Model deref( Holder holder )
        {
            return holder != null ? holder.get() : null;
        }

        public Model get()
        {
            if ( !set )
            {
                synchronized ( this )
                {
                    if ( !set )
                    {
                        try
                        {
                            this.wait();
                        }
                        catch ( InterruptedException e )
                        {
                            // Ignore
                        }
                    }
                }
            }
            return model;
        }

        public Model computeIfAbsent( Supplier<Model> supplier )
        {
            if ( !set )
            {
                synchronized ( this )
                {
                    if ( !set )
                    {
                        this.set = true;
                        this.model = supplier.get();
                        this.notifyAll();
                    }
                }
            }
            return model;
        }

    }

    @Override
    public String getUserProperty( String key )
    {
        return userProperties.get( key );
    }

    @Override
    public Model getRawModel( Path p )
    {
        return Holder.deref( modelByPath.get( p ) );
    }

    @Override
    public Model getRawModel( String groupId, String artifactId )
    {
        return Holder.deref( modelByGA.get( new GAKey( groupId, artifactId ) ) );
    }

    static class GAKey
    {
        private final String groupId;
        private final String artifactId;
        private final int hashCode;

        GAKey( String groupId, String artifactId )
        {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.hashCode = Objects.hash( groupId, artifactId );
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( this == obj )
            {
                return true;
            }
            if ( !( obj instanceof GAKey ) )
            {
                return false;
            }

            GAKey other = (GAKey) obj;
            return Objects.equals( artifactId, other.artifactId ) && Objects.equals( groupId, other.groupId );
        }
    }
}
