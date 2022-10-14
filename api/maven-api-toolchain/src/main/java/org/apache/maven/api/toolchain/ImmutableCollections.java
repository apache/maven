package org.apache.maven.api.toolchain;

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

import java.io.Serializable;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

class ImmutableCollections
{

    private static final List<?> EMPTY_LIST = new AbstractImmutableList<Object>()
    {
        @Override
        public Object get( int index )
        {
            throw new IndexOutOfBoundsException();
        }
        @Override
        public int size()
        {
            return 0;
        }
    };

    private static final Map<?, ?> EMPTY_MAP = new AbstractImmutableMap<Object, Object>()
    {
        @Override
        public Set<Entry<Object, Object>> entrySet()
        {
            return new AbstractImmutableSet<Entry<Object, Object>>()
            {
                @Override
                public Iterator<Entry<Object, Object>> iterator()
                {
                    return new Iterator<Entry<Object, Object>>()
                    {
                        @Override
                        public boolean hasNext()
                        {
                            return false;
                        }
                        @Override
                        public Entry<Object, Object> next()
                        {
                            throw new NoSuchElementException();
                        }
                    };
                }
                @Override
                public int size()
                {
                    return 0;
                }
            };
        }
    };

    static <E> List<E> copy( Collection<E> collection )
    {
        if ( collection == null )
        {
            return emptyList();
        }
        else if ( collection instanceof AbstractImmutableList )
        {
            return ( List<E> ) collection;
        }
        else
        {
            switch ( collection.size() )
            {
                case 0:
                    return emptyList();
                case 1:
                    return singletonList( collection.iterator().next() );
                case 2:
                    Iterator<E> it = collection.iterator();
                    return new List2<>( it.next(), it.next() );
                default:
                    return new ListN<>( collection );
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    static <E> List<E> emptyList()
    {
        return ( List<E> ) EMPTY_LIST;
    }

    static <E> List<E> singletonList( E element )
    {
        return new List1<>( element );
    }

    static <K, V> Map<K, V> copy( Map<K, V> map )
    {
        if ( map == null )
        {
            return emptyMap();
        }
        else if ( map instanceof AbstractImmutableMap )
        {
            return map;
        }
        else
        {
            switch ( map.size() )
            {
                case 0:
                    return emptyMap();
                case 1:
                    Map.Entry<K, V> entry = map.entrySet().iterator().next();
                    return singletonMap( entry.getKey(), entry.getValue() );
                default:
                    return new MapN<>( map );
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    static <K, V> Map<K, V> emptyMap()
    {
        return ( Map<K, V> ) EMPTY_MAP;
    }

    static <K, V> Map<K, V> singletonMap( K key, V value )
    {
        return new Map1<>( key, value );
    }

    static Properties copy( Properties properties )
    {
        if ( properties instanceof ROProperties )
        {
            return properties;
        }
        return new ROProperties( properties );
    }

    private static class List1<E> extends AbstractImmutableList<E>
    {
        private final E element;

        private List1( E element )
        {
            this.element = element;
        }

        @Override
        public E get( int index )
        {
            if ( index == 0 )
            {
                return element;
            }
            throw outOfBounds( index );
        }

        @Override
        public int size()
        {
            return 1;
        }
    }

    private static class List2<E> extends AbstractImmutableList<E>
    {
        private final E element1;
        private final E element2;

        private List2( E element1, E element2 )
        {
            this.element1 = element1;
            this.element2 = element2;
        }

        @Override
        public E get( int index )
        {
            if ( index == 0 )
            {
                return element1;
            }
            else if ( index == 1 )
            {
                return element2;
            }
            throw outOfBounds( index );
        }

        @Override
        public int size()
        {
            return 2;
        }
    }

    private static class ListN<E> extends AbstractImmutableList<E>
    {
        private final Object[] elements;

        private ListN( Collection<E> elements )
        {
            this.elements = elements.toArray();
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public E get( int index )
        {
            return ( E ) elements[ index ];
        }

        @Override
        public int size()
        {
            return elements.length;
        }
    }

    private abstract static class AbstractImmutableList<E>
        extends AbstractList<E>
        implements RandomAccess, Serializable
    {
        @Override
        public boolean add( E e )
        {
            throw uoe();
        }

        @Override
        public boolean remove( Object o )
        {
            throw uoe();
        }

        @Override
        public boolean addAll( Collection<? extends E> c )
        {
            throw uoe();
        }

        @Override
        public boolean removeAll( Collection<?> c )
        {
            throw uoe();
        }

        @Override
        public boolean retainAll( Collection<?> c )
        {
            throw uoe();
        }

        @Override
        public void clear()
        {
            throw uoe();
        }

        @Override
        public boolean removeIf( Predicate<? super E> filter )
        {
            throw uoe();
        }

        @Override
        public void replaceAll( UnaryOperator<E> operator )
        {
            throw uoe();
        }

        @Override
        public void sort( Comparator<? super E> c )
        {
            throw uoe();
        }

        @Override
        public Iterator<E> iterator()
        {
            return new Itr( 0 );
        }

        @Override
        public ListIterator<E> listIterator()
        {
            return new Itr( 0 );
        }

        @Override
        public ListIterator<E> listIterator( int index )
        {
            if ( index < 0 || index > size() )
            {
                throw outOfBounds( index );
            }
            return new Itr( index );
        }

        @Override
        public List<E> subList( int fromIndex, int toIndex )
        {
            if ( fromIndex < 0 )
            {
                throw new IndexOutOfBoundsException( "fromIndex = " + fromIndex );
            }
            if ( toIndex > size() )
            {
                throw new IndexOutOfBoundsException( "toIndex = " + toIndex );
            }
            if ( fromIndex > toIndex )
            {
                throw new IllegalArgumentException( "fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")" );
            }
            return new SubList( fromIndex, toIndex );
        }

        protected IndexOutOfBoundsException outOfBounds( int index )
        {
            return new IndexOutOfBoundsException( "Index: " + index + ", Size: " + size() );
        }

        private class SubList extends AbstractImmutableList<E>
        {
            private final int fromIndex;
            private final int toIndex;

            private SubList( int fromIndex, int toIndex )
            {
                this.fromIndex = fromIndex;
                this.toIndex = toIndex;
            }

            @Override
            public E get( int index )
            {
                if ( index < 0 || index > size() )
                {
                    throw outOfBounds( index );
                }
                return AbstractImmutableList.this.get( fromIndex + index );
            }

            @Override
            public int size()
            {
                return toIndex - fromIndex;
            }
        }

        private class Itr implements ListIterator<E>
        {
            int index;

            private Itr( int index )
            {
                this.index = index;
            }

            @Override
            public boolean hasNext()
            {
                return index < size();
            }

            @Override
            public E next()
            {
                return get( index++ );
            }

            @Override
            public boolean hasPrevious()
            {
                return index > 0;
            }

            @Override
            public E previous()
            {
                return get( --index );
            }

            @Override
            public int nextIndex()
            {
                return index;
            }

            @Override
            public int previousIndex()
            {
                return index - 1;
            }

            @Override
            public void remove()
            {
                throw uoe();
            }

            @Override
            public void set( E e )
            {
                throw uoe();
            }

            @Override
            public void add( E e )
            {
                throw uoe();
            }
        }
    }

    private static class ROProperties extends Properties
    {
        private ROProperties( Properties props )
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
            throw uoe();
        }

        @Override
        public Object remove( Object key )
        {
            throw uoe();
        }

        @Override
        public void putAll( Map<?, ?> t )
        {
            throw uoe();
        }

        @Override
        public void clear()
        {
            throw uoe();
        }

        @Override
        public void replaceAll( BiFunction<? super Object, ? super Object, ?> function )
        {
            throw uoe();
        }

        @Override
        public Object putIfAbsent( Object key, Object value )
        {
            throw uoe();
        }

        @Override
        public boolean remove( Object key, Object value )
        {
            throw uoe();
        }

        @Override
        public boolean replace( Object key, Object oldValue, Object newValue )
        {
            throw uoe();
        }

        @Override
        public Object replace( Object key, Object value )
        {
            throw uoe();
        }

        @Override
        public Object computeIfAbsent( Object key, Function<? super Object, ?> mappingFunction )
        {
            throw uoe();
        }

        @Override
        public Object computeIfPresent( Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction )
        {
            throw uoe();
        }

        @Override
        public Object compute( Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction )
        {
            throw uoe();
        }

        @Override
        public Object merge( Object key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction )
        {
            throw uoe();
        }
    }

    private static class Map1<K, V> extends AbstractImmutableMap<K, V>
    {
        private final Entry<K, V> entry;

        private Map1( K key, V value )
        {
            this.entry = new SimpleImmutableEntry<>( key, value );
        }

        @Override
        public Set<Entry<K, V>> entrySet()
        {
            return new AbstractImmutableSet<Entry<K, V>>()
            {
                @Override
                public Iterator<Entry<K, V>> iterator()
                {
                    return new Iterator<Entry<K, V>>()
                    {
                        int index = 0;
                        @Override
                        public boolean hasNext()
                        {
                            return index == 0;
                        }

                        @Override
                        public Entry<K, V> next()
                        {
                            if ( index++ == 0 )
                            {
                                return entry;
                            }
                            throw new NoSuchElementException();
                        }
                    };
                }

                @Override
                public int size()
                {
                    return 1;
                }
            };
        }
    }

    private static class MapN<K, V> extends AbstractImmutableMap<K, V>
    {
        private final Object[] entries;

        private MapN( Map<K, V> map )
        {
            entries = map != null ? map.entrySet().toArray() : new Object[0];
        }

        @Override
        public Set<Entry<K, V>> entrySet()
        {
            return new AbstractImmutableSet<Entry<K, V>>()
            {
                @Override
                public Iterator<Entry<K, V>> iterator()
                {
                    return new Iterator<Entry<K, V>>()
                    {
                        int index = 0;
                        @Override
                        public boolean hasNext()
                        {
                            return index < entries.length;
                        }

                        @SuppressWarnings( "unchecked" )
                        @Override
                        public Entry<K, V> next()
                        {
                            if ( index < entries.length )
                            {
                                return ( Entry<K, V> ) entries[index++];
                            }
                            throw new NoSuchElementException();
                        }
                    };
                }

                @Override
                public int size()
                {
                    return entries.length;
                }
            };
        }
    }

    private abstract static class AbstractImmutableMap<K, V>
        extends AbstractMap<K, V>
        implements Serializable
    {
        @Override
        public void replaceAll( BiFunction<? super K, ? super V, ? extends V> function )
        {
            throw uoe();
        }

        @Override
        public V putIfAbsent( K key, V value )
        {
            throw uoe();
        }

        @Override
        public boolean remove( Object key, Object value )
        {
            throw uoe();
        }

        @Override
        public boolean replace( K key, V oldValue, V newValue )
        {
            throw uoe();
        }

        @Override
        public V replace( K key, V value )
        {
            throw uoe();
        }

        @Override
        public V computeIfAbsent( K key, Function<? super K, ? extends V> mappingFunction )
        {
            throw uoe();
        }

        @Override
        public V computeIfPresent( K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction )
        {
            throw uoe();
        }

        @Override
        public V compute( K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction )
        {
            throw uoe();
        }

        @Override
        public V merge( K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction )
        {
            throw uoe();
        }
    }

    private abstract static class AbstractImmutableSet<E>
        extends AbstractSet<E>
        implements Serializable
    {
        @Override
        public boolean removeAll( Collection<?> c )
        {
            throw uoe();
        }

        @Override
        public boolean add( E e )
        {
            throw uoe();
        }

        @Override
        public boolean remove( Object o )
        {
            throw uoe();
        }

        @Override
        public boolean retainAll( Collection<?> c )
        {
            throw uoe();
        }

        @Override
        public void clear()
        {
            throw uoe();
        }

        @Override
        public boolean removeIf( Predicate<? super E> filter )
        {
            throw uoe();
        }
    }

    private static UnsupportedOperationException uoe()
    {
        return new UnsupportedOperationException();
    }

}
