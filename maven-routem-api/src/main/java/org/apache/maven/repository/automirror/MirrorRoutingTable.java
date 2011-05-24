package org.apache.maven.repository.automirror;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MirrorRoutingTable
    implements Iterable<MirrorRoute>
{

    private List<MirrorRoute> mirrors = new ArrayList<MirrorRoute>();

    private final transient Random random = new Random();

    private transient Map<String, int[]> indexGrabBags = new HashMap<String, int[]>();
    
    public synchronized MirrorRoutingTable addMirror( final MirrorRoute mirror )
    {
        if ( !mirrors.contains( mirror ) )
        {
            mirrors.add( mirror );
            for ( String url : mirror.getMirrorOfUrls() )
            {
                indexGrabBags.remove( url );
            }
        }

        return this;
    }

    public synchronized MirrorRoute getMirror( final String canonicalUrl )
    {
        return getWeightedRandomMirror( canonicalUrl );
    }
    
//    private MirrorRoute getHighestPriorityMirror( final String canonicalUrl )
//    {
//        if ( mirrors.isEmpty() )
//        {
//            return null;
//        }
//
//        final List<MirrorRoute> available = new ArrayList<MirrorRoute>( mirrors );
//
//        // sort by weight.
//        Collections.sort( available );
//
//        for ( final MirrorRoute mirror : available )
//        {
//            // return the highest-priority ENABLED mirror.
//            if ( mirror.isEnabled() && canonicalUrl.equals( mirror.getUrl() ) )
//            {
//                return mirror;
//            }
//        }
//
//        return null;
//    }

    private MirrorRoute getWeightedRandomMirror( final String canonicalUrl )
    {
        if ( mirrors.isEmpty() )
        {
            return null;
        }

        int[] indexGrabBag = null;
        synchronized ( canonicalUrl )
        {
            indexGrabBag = indexGrabBags.get( canonicalUrl );
            if ( indexGrabBag == null )
            {
                final List<Integer> gb = new ArrayList<Integer>();
                for ( int idx = 0; idx < mirrors.size(); idx++ )
                {
                    final MirrorRoute mirror = mirrors.get( idx );
                    if ( !mirror.isEnabled() || !mirror.isMirrorOf( canonicalUrl ) )
                    {
                        // only select from enabled mirrors that actually mirror the given canonical repository URL.
                        continue;
                    }

                    if ( mirror.getWeight() < 1 )
                    {
                        // make sure this mirror has at least one chance of being picked.
                        gb.add( idx );
                    }
                    else
                    {
                        // if weight == 9, give this mirror 9 chances of being picked randomly.
                        for ( int i = 0; i < mirror.getWeight(); i++ )
                        {
                            gb.add( idx );
                        }
                    }
                }

                indexGrabBag = new int[gb.size()];
                for ( int i = 0; i < indexGrabBag.length; i++ )
                {
                    indexGrabBag[i] = gb.get( i );
                }

                indexGrabBags.put( canonicalUrl, indexGrabBag );
            }
        }

        if ( indexGrabBag.length == 0 )
        {
            return null;
        }
        
        // generate a random number that will correspond to an index stored in the index grab bag.
        int idx = Math.abs( random.nextInt() ) % indexGrabBag.length;

        // use that random number to select the index of the mirror in the mirrors list.
        idx = indexGrabBag[idx];

        // lookup the mirror instance associated with the index from the grab bag.
        return mirrors.get( idx );
    }

    public int size()
    {
        return mirrors.size();
    }

    public boolean isEmpty()
    {
        return mirrors.isEmpty();
    }

    public boolean contains( final MirrorRoute o )
    {
        return mirrors.contains( o );
    }

    public Iterator<MirrorRoute> iterator()
    {
        return mirrors.iterator();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( mirrors == null ) ? 0 : mirrors.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final MirrorRoutingTable other = (MirrorRoutingTable) obj;
        if ( mirrors == null )
        {
            if ( other.mirrors != null )
            {
                return false;
            }
        }
        else if ( !mirrors.equals( other.mirrors ) )
        {
            return false;
        }
        return true;
    }

    List<MirrorRoute> getMirrors()
    {
        return mirrors;
    }

    void setMirrors( final List<MirrorRoute> mirrors )
    {
        this.mirrors = mirrors;
    }

    public boolean containsMirrorOf( final String canonicalUrl )
    {
        for ( final MirrorRoute mirror : mirrors )
        {
            if ( mirror.isEnabled() && mirror.isMirrorOf( canonicalUrl ) )
            {
                return true;
            }
        }

        return false;
    }

}
