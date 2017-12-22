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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Iterator;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Plugin;

/**
 * @author Benjamin Bentmann
 */
class CacheUtils
{

    public static <T> boolean eq( T s1, T s2 )
    {
        return s1 != null ? s1.equals( s2 ) : s2 == null;
    }

    public static int hash( Object obj )
    {
        return obj != null ? obj.hashCode() : 0;
    }

    public static int pluginHashCode( Plugin plugin )
    {
        int hash = 17;

        hash = hash * 31 + hash( plugin.getGroupId() );
        hash = hash * 31 + hash( plugin.getArtifactId() );
        hash = hash * 31 + hash( plugin.getVersion() );

        hash = hash * 31 + ( plugin.isExtensions() ? 1 : 0 );

        for ( Dependency dependency : plugin.getDependencies() )
        {
            hash = hash * 31 + hash( dependency.getGroupId() );
            hash = hash * 31 + hash( dependency.getArtifactId() );
            hash = hash * 31 + hash( dependency.getVersion() );
            hash = hash * 31 + hash( dependency.getType() );
            hash = hash * 31 + hash( dependency.getClassifier() );
            hash = hash * 31 + hash( dependency.getScope() );

            for ( Exclusion exclusion : dependency.getExclusions() )
            {
                hash = hash * 31 + hash( exclusion.getGroupId() );
                hash = hash * 31 + hash( exclusion.getArtifactId() );
            }
        }

        return hash;
    }

    public static boolean pluginEquals( Plugin a, Plugin b )
    {
        return eq( a.getArtifactId(), b.getArtifactId() ) //
            && eq( a.getGroupId(), b.getGroupId() ) //
            && eq( a.getVersion(), b.getVersion() ) //
            && a.isExtensions() == b.isExtensions() //
            && dependenciesEquals( a.getDependencies(), b.getDependencies() );
    }

    private static boolean dependenciesEquals( List<Dependency> a, List<Dependency> b )
    {
        if ( a.size() != b.size() )
        {
            return false;
        }

        Iterator<Dependency> aI = a.iterator();
        Iterator<Dependency> bI = b.iterator();

        while ( aI.hasNext() )
        {
            Dependency aD = aI.next();
            Dependency bD = bI.next();

            boolean r = eq( aD.getGroupId(), bD.getGroupId() ) //
                && eq( aD.getArtifactId(), bD.getArtifactId() ) //
                && eq( aD.getVersion(), bD.getVersion() ) //
                && eq( aD.getType(), bD.getType() ) //
                && eq( aD.getClassifier(), bD.getClassifier() ) //
                && eq( aD.getScope(), bD.getScope() );

            r &= exclusionsEquals( aD.getExclusions(), bD.getExclusions() );

            if ( !r )
            {
                return false;
            }
        }

        return true;
    }

    private static boolean exclusionsEquals( List<Exclusion> a, List<Exclusion> b )
    {
        if ( a.size() != b.size() )
        {
            return false;
        }

        Iterator<Exclusion> aI = a.iterator();
        Iterator<Exclusion> bI = b.iterator();

        while ( aI.hasNext() )
        {
            Exclusion aD = aI.next();
            Exclusion bD = bI.next();

            boolean r = eq( aD.getGroupId(), bD.getGroupId() ) //
                && eq( aD.getArtifactId(), bD.getArtifactId() );

            if ( !r )
            {
                return false;
            }
        }

        return true;
    }

}
