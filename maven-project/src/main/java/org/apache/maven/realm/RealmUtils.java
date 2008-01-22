package org.apache.maven.realm;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

public final class RealmUtils
{

    private RealmUtils()
    {
    }

    public static String createExtensionRealmId( Artifact extensionArtifact )
    {
        return "/extensions/" + extensionArtifact.getGroupId() + ":"
               + extensionArtifact.getArtifactId() + ":" + extensionArtifact.getVersion() +
               "/thread:" + Thread.currentThread().getName(); //add thread to the mix to prevent clashes in paralel execution
    }

    public static String createProjectId( String projectGroupId,
                                          String projectArtifactId,
                                          String projectVersion )
    {
        return "/projects/" + projectGroupId + ":" + projectArtifactId + ":" + projectVersion +
               "/thread:" + Thread.currentThread().getName(); //add thread to the mix to prevent clashes in paralel execution
    }

    public static String createPluginRealmId( Plugin plugin )
    {
        StringBuffer id = new StringBuffer().append( "/plugins/" )
                                            .append( plugin.getGroupId() )
                                            .append( ':' )
                                            .append( plugin.getArtifactId() )
                                            .append( ':' )
                                            .append( plugin.getVersion() );

        StringBuffer depId = new StringBuffer();

        Collection dependencies = plugin.getDependencies();
        if ( ( dependencies != null ) && !dependencies.isEmpty() )
        {
            dependencies = new LinkedHashSet( dependencies );

            for ( Iterator it = dependencies.iterator(); it.hasNext(); )
            {
                Dependency dep = (Dependency) it.next();

                depId.append( dep.getGroupId() )
                     .append( ':' )
                     .append( dep.getArtifactId() )
                     .append( ';' )
                     .append( dep.getVersion() );

                if ( it.hasNext() )
                {
                    depId.append( ',' );
                }
            }
        }
        else
        {
            depId.append( '0' );
        }

        id.append( '@' ).append( depId.toString().hashCode() )
                .append( "/thread:" ).append( Thread.currentThread().getName() ); //add thread to the mix to prevent clashes in paralel execution

        return id.toString();
    }

}
