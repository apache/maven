package org.apache.maven.api.model;

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

import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Function;

class ROProperties extends Properties
{
    ROProperties( Properties props )
    {
        super();
        if ( props != null )
        {
            // Do not use super.putAll, as it may delegate to put which throws an UnsupportedOperationException
            for ( Map.Entry<Object, Object> e : props.entrySet() )
            {
                super.put( e.getKey(), e.getValue() );
            }
        }
    }

    @Override
    public Object put( Object key, Object value )
    {
        throw new UnsupportedOperationException( "Properties are read-only" );
    }

    @Override
    public Object remove( Object key )
    {
        throw new UnsupportedOperationException( "Properties are read-only" );
    }

    @Override
    public void putAll( Map<?, ?> t )
    {
        throw new UnsupportedOperationException( "Properties are read-only" );
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException( "Properties are read-only" );
    }

    @Override
    public void replaceAll( BiFunction<? super Object, ? super Object, ?> function )
    {
        throw new UnsupportedOperationException( "Properties are read-only" );
    }

    @Override
    public Object putIfAbsent( Object key, Object value )
    {
        throw new UnsupportedOperationException( "Properties are read-only" );
    }

    @Override
    public boolean remove( Object key, Object value )
    {
        throw new UnsupportedOperationException( "Properties are read-only" );
    }

    @Override
    public boolean replace( Object key, Object oldValue, Object newValue )
    {
        throw new UnsupportedOperationException( "Properties are read-only" );
    }

    @Override
    public Object replace( Object key, Object value )
    {
        throw new UnsupportedOperationException( "Properties are read-only" );
    }

    @Override
    public Object computeIfAbsent( Object key, Function<? super Object, ?> mappingFunction )
    {
        throw new UnsupportedOperationException( "Properties are read-only" );
    }

    @Override
    public Object computeIfPresent( Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction )
    {
        throw new UnsupportedOperationException( "Properties are read-only" );
    }

    @Override
    public Object compute( Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction )
    {
        throw new UnsupportedOperationException( "Properties are read-only" );
    }

    @Override
    public Object merge( Object key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction )
    {
        throw new UnsupportedOperationException( "Properties are read-only" );
    }

}
