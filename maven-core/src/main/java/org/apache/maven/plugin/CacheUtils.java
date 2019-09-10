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
import java.util.Objects;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Plugin;

/**
 * @author Benjamin Bentmann
 */
class CacheUtils
{

    /**
     * @deprecated Use {@link Objects#equals(Object)}
     */
    @Deprecated
    public static <T> boolean eq( T s1, T s2 )
    {
        return Objects.equals( s1, s2 );
    }

    /**
     * @deprecated Use {@link Objects#hashCode(Object)}
     */
    @Deprecated
    public static int hash( Object obj )
    {
        return obj != null ? obj.hashCode() : 0;
    }

    public static int pluginHashCode( Plugin plugin )
    {
        int hash = 17;

        hash = hash * 31 + Objects.hashCode( plugin.getGroupId() );
        hash = hash * 31 + Objects.hashCode( plugin.getArtifactId() );
        hash = hash * 31 + Objects.hashCode( plugin.getVersion() );

        hash = hash * 31 + ( plugin.isExtensions() ? 1 : 0 );

        for ( Dependency dependency : plugin.getDependencies() )
        {
            hash = hash * 31 + Objects.hashCode( dependency.getGroupId() );
            hash = hash * 31 + Objects.hashCode( dependency.getArtifactId() );
            hash = hash * 31 + Objects.hashCode( dependency.getVersion() );
            hash = hash * 31 + Objects.hashCode( dependency.getType() );
            hash = hash * 31 + Objects.hashCode( dependency.getClassifier() );
            hash = hash * 31 + Objects.hashCode( dependency.getScope() );

            for ( Exclusion exclusion : dependency.getExclusions() )
            {
                hash = hash * 31 + Objects.hashCode( exclusion.getGroupId() );
                hash = hash * 31 + Objects.hashCode( exclusion.getArtifactId() );
            }
        }

        return hash;
    }

    public static boolean pluginEquals( Plugin a, Plugin b )
    {
        return Objects.equals( a.getArtifactId(), b.getArtifactId() ) //
            && Objects.equals( a.getGroupId(), b.getGroupId() ) //
            && Objects.equals( a.getVersion(), b.getVersion() ) //
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

            boolean r = Objects.equals( aD.getGroupId(), bD.getGroupId() ) //
                && Objects.equals( aD.getArtifactId(), bD.getArtifactId() ) //
                && Objects.equals( aD.getVersion(), bD.getVersion() ) //
                && Objects.equals( aD.getType(), bD.getType() ) //
                && Objects.equals( aD.getClassifier(), bD.getClassifier() ) //
                && Objects.equals( aD.getScope(), bD.getScope() );

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

            boolean r = Objects.equals( aD.getGroupId(), bD.getGroupId() ) //
                && Objects.equals( aD.getArtifactId(), bD.getArtifactId() );

            if ( !r )
            {
                return false;
            }
        }

        return true;
    }

}
