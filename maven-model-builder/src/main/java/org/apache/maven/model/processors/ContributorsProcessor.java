package org.apache.maven.model.processors;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Contributor;
import org.apache.maven.model.Model;

public class ContributorsProcessor
    extends BaseProcessor
{

    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );

        Model p = (Model) parent;
        Model c = (Model) child;
        Model t = (Model) target;

        if ( !c.getContributors().isEmpty() )
        {
            copyContributors( c.getContributors(), t );
        }
        else if ( p != null && !p.getContributors().isEmpty() )
        {
            copyContributors( p.getContributors(), t );
        }
    }

    private static void copyContributors( List<Contributor> contributors, Model target )
    {
        for ( Contributor contributor : contributors )
        {
            Contributor copy = new Contributor();
            copy.setName( contributor.getName() );
            copy.setEmail( contributor.getEmail() );
            copy.setUrl( contributor.getUrl() );
            copy.setOrganization( contributor.getOrganization() );
            copy.setOrganizationUrl( contributor.getOrganizationUrl() );
            copy.setTimezone( contributor.getTimezone() );
            copy.setRoles( new ArrayList<String>( contributor.getRoles() ) );
            Properties props = new Properties();
            props.putAll( contributor.getProperties() );
            copy.setProperties( props );
            target.addContributor( copy );
        }
    }

}
