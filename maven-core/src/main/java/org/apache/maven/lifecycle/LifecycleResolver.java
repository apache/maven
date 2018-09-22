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

import org.codehaus.plexus.component.annotations.Component;

/**
 * @author Robert Scholte
 * @since 3.6.0
 */
@Component( role = LifecycleResolver.class )
public class LifecycleResolver
{

    /**
     * @param lifecycle the lifecycle
     * @param phase the target phase
     * @return all phases up until the target phase
     */
    public List<Phase> resolve( Lifecycle lifecycle, String phase )
    {
        List<Phase> phases = new ArrayList<>( lifecycle.getPhases().size() );
        
        for ( LifecyclePhase lp : lifecycle.phases() )
        {
            Phase resolvedPhase = lp.resolve( phase );
            phases.add( resolvedPhase );
            
            if ( phase.equals( resolvedPhase.getValue() ) )
            {
                break;
            }
        }
        
        return phases;
    }
}
