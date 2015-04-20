package org.apache.maven.lifecycle.mapping;

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
import java.util.List;

import org.codehaus.plexus.util.StringUtils;

public class LifecyclePhase
{
    
    private List<LifecycleMojo> mojos;
    
    public LifecyclePhase()
    {
    }
    
    public LifecyclePhase( String goals )
    {
        set( goals );
    }
    
    public List<LifecycleMojo> getMojos()
    {
        return mojos;
    }
    
    public void setMojos( List<LifecycleMojo> mojos )
    {
        this.mojos = mojos;
    }
    
    public void set( String goals )
    {
        mojos = new ArrayList<LifecycleMojo>();
        
        String[] mojoGoals = StringUtils.split( goals, "," );
        
        for ( String mojoGoal: mojoGoals )
        {
            LifecycleMojo lifecycleMojo = new LifecycleMojo();
            lifecycleMojo.setGoal( mojoGoal.trim() );
            mojos.add( lifecycleMojo );
        }
    }
}
