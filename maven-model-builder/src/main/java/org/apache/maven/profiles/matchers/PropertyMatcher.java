package org.apache.maven.profiles.matchers;

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
import java.util.Map.Entry;

import org.apache.maven.model.Profile;

public class PropertyMatcher implements ProfileMatcher
{
    public boolean isMatch( Profile profile, Properties properties )
    {
        if (profile == null) {
            throw new IllegalArgumentException("profile: null");
        }
        
        if(profile.getActivation() == null || profile.getActivation().getProperty() == null)
        {
            return false;
        }
        String value = profile.getActivation().getProperty().getValue();
        String name =  profile.getActivation().getProperty().getName();

        if(name == null )
        {
            return false;
        }

        if(value == null)
        {
            return !name.startsWith("!");
        }

        for ( Entry<Object, Object> ip : properties.entrySet() )
        {
        	if(ip.getKey().equals( name ))
        	{
        		return ((String) ip.getValue()).equals(value);
        	}         
        }
       
        return false;
    }
}
