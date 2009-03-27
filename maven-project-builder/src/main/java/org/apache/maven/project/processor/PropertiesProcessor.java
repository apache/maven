package org.apache.maven.project.processor;

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

import java.util.Properties;

import org.apache.maven.model.Model;

public class PropertiesProcessor
    extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        Model t = (Model) target, c = (Model) child, p = (Model) parent;

        Properties properties = new Properties();
        
        if ( p != null && p.getProperties() != null )
        {
            properties.putAll( p.getProperties() );
        }
        
        if ( c.getProperties() != null )
        {
            properties.putAll( c.getProperties() );
        }
              
        if ( !properties.isEmpty() )
        {
            if(t.getProperties().isEmpty())
            {
                t.setProperties( properties );   
            }
            else
            {
                t.getProperties().putAll( properties );
            }       
        }
    }
}
