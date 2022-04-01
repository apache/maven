package org.apache.maven.model;

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

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.RandomAccess;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class ImmutableCollections
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
                default:
                    return (List<E>) new ListN<>( collection );
            }
        }
    }

    static <E> List<E> emptyList()
    {
        return ( List<E> ) EMPTY_LIST;
    }

    static <E> List<E> singletonList( E element )
    {
        return new List1<>( element );
    }

    private static class List1<E> extends AbstractImmutableList<E>
    {
        private final E element;

        public List1( E element )
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

    private static class ListN<E> extends AbstractImmutableList<E>
    {
        private final E[] elements;

        private ListN( Collection<E> elements )
        {
            E[] tmp = (E[]) Array.newInstance( elements.getClass().getComponentType(), elements.size() );
            this.elements = elements.toArray( tmp );
        }

        @Override
        public E get( int index )
        {
            return elements[ index ];
        }

        @Override
        public int size()
        {
            return elements.length;
        }
    }

    private static abstract class AbstractImmutableList<E> extends AbstractList<E> implements RandomAccess
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
            return new Itr();
        }

        @Override
        public ListIterator<E> listIterator()
        {
            return new Itr();
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
            if ( fromIndex < 0)
                throw new IndexOutOfBoundsException( "fromIndex = " + fromIndex );
            if ( toIndex > size() )
                throw new IndexOutOfBoundsException( "toIndex = " + toIndex );
            if ( fromIndex > toIndex )
                throw new IllegalArgumentException( "fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")" );
            return new SubList( fromIndex, toIndex );
        }

        protected IndexOutOfBoundsException outOfBounds( int index )
        {
            return new IndexOutOfBoundsException( "Index: " + index + ", Size: " + size() );
        }

        class SubList extends AbstractImmutableList<E>
        {
            private final int fromIndex;
            private final int toIndex;

            public SubList( int fromIndex, int toIndex )
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

        class Itr implements ListIterator<E>
        {
            int index;

            public Itr()
            {
                this.index = 0;
            }

            public Itr( int index )
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

    static UnsupportedOperationException uoe()
    {
        return new UnsupportedOperationException();
    }

}
