package org.apache.maven.lifecycle;

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
import java.util.Map;

/**
 * Class Lifecycle.
 */
public class Lifecycle
{
    public Lifecycle()
    {
    }

    public Lifecycle( String id, List<LifecyclePhase> phases,
                      Map<String, org.apache.maven.lifecycle.mapping.LifecyclePhase> defaultPhases )
    {
        this.id = id;
        this.phases.getPhases().addAll( phases );
        this.defaultPhases = defaultPhases;
    }

    // <lifecycle>
    //   <id>clean</id>
    //   <phases>
    //     <phase>pre-clean</phase>
    //     <phase>clean</phase>
    //     <phase>post-clean</phase>
    //   </phases>
    //   <default-phases>
    //     <clean>org.apache.maven.plugins:maven-clean-plugin:clean</clean>
    //   </default-phases>
    // </lifecycle>

    private String id;

    // Must be called phases for backward compatibility; use to be List<String>
    private PhasesWrapper phases = new PhasesWrapper();

    private Map<String, org.apache.maven.lifecycle.mapping.LifecyclePhase> defaultPhases;

    public String getId()
    {
        return this.id;
    }

    /**
     * Flattened (original) lifecycle
     * 
     * @return
     */
    public List<String> getPhases()
    {
        List<String> names = new ArrayList<>();
        for ( LifecyclePhase phase : this.phases.getPhases() )
        {
            if ( phase instanceof Phase )
            {
                names.add( ( (Phase) phase ).getValue() );
            }
            else if ( phase instanceof Choice )
            {
                Choice choice = (Choice) phase;
                for ( Phase choicePhase : choice.getPhases() )
                {
                    names.add( choicePhase.getValue() );
                }
            }
        }
        return names;
    }
    
    /**
     * Structured lifecycle
     * 
     * @return
     */
    public List<LifecyclePhase> phases()
    {
        return phases.getPhases();
    }

    public Map<String, org.apache.maven.lifecycle.mapping.LifecyclePhase> getDefaultLifecyclePhases()
    {
        return defaultPhases;
    }
    
    @Deprecated
    public Map<String, String> getDefaultPhases()
    {
        return org.apache.maven.lifecycle.mapping.LifecyclePhase.toLegacyMap( getDefaultLifecyclePhases() );
    }

    @Override
    public String toString()
    {
        return id + " -> " + phases;
    }
}
