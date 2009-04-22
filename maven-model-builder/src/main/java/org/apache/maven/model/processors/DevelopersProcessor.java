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

import org.apache.maven.model.Developer;
import org.apache.maven.model.Model;

public class DevelopersProcessor
    extends BaseProcessor
{

    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );

        Model p = (Model) parent;
        Model c = (Model) child;
        Model t = (Model) target;

        if ( !c.getDevelopers().isEmpty() )
        {
            copyDevelopers( c.getDevelopers(), t );
        }
        else if ( p != null && !p.getDevelopers().isEmpty() )
        {
            copyDevelopers( p.getDevelopers(), t );
        }
    }

    private static void copyDevelopers( List<Developer> developers, Model target )
    {
        for ( Developer developer : developers )
        {
            Developer copy = new Developer();
            copy.setId( developer.getId() );
            copy.setName( developer.getName() );
            copy.setEmail( developer.getEmail() );
            copy.setUrl( developer.getUrl() );
            copy.setOrganization( developer.getOrganization() );
            copy.setOrganizationUrl( developer.getOrganizationUrl() );
            copy.setTimezone( developer.getTimezone() );
            copy.setRoles( new ArrayList<String>( developer.getRoles() ) );
            Properties props = new Properties();
            props.putAll( developer.getProperties() );
            copy.setProperties( props );
            target.addDeveloper( copy );
        }
    }

}
