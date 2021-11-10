package org.apache.maven.caching;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Utils
{

    public static <T> Optional<T> getFirst( List<T> list )
    {
        int size = list.size();
        if ( size > 0 )
        {
            return Optional.of( list.get( 0 ) );
        }
        return Optional.empty();
    }

    public static <T> Optional<T> getLast( List<T> list )
    {
        int size = list.size();
        if ( size > 0 )
        {
            return Optional.of( list.get( size - 1 ) );
        }
        return Optional.empty();
    }

    public static <T> T getOnlyElement( Collection<T> col )
    {
        Iterator<T> iterator = col.iterator();
        T first = iterator.next();
        if ( iterator.hasNext() )
        {
            StringBuilder sb = new StringBuilder().append( "expected one element but was: <" ).append( first );
            for ( int i = 0; i < 4 && iterator.hasNext(); i++ )
            {
                sb.append( ", " ).append( iterator.next() );
            }
            if ( iterator.hasNext() )
            {
                sb.append( ", ..." );
            }
            sb.append( '>' );
            throw new IllegalArgumentException( sb.toString() );
        }
        return first;
    }

    public static class MultiMap<K, V> extends HashMap<K, Collection<V>>
    {
        public void add( K key, V value )
        {
            super.computeIfAbsent( key, k -> new ArrayList<>() ).add( value );
        }

        public Collection<V> allValues()
        {
            return values().stream().flatMap( Collection::stream ).collect( Collectors.toList() );
        }
    }

}