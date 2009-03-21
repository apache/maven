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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Model;

public class ProfilesModuleProcessor
    extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        Model t = (Model) target;
        Model c = (Model) child;
        Model p = (Model) parent;
        List<String> modules = new ArrayList<String>();

        for ( String module : c.getModules() )
        {
            if(!modules.contains( module ))
            {
                modules.add(module);
            }
        }       
        if(p != null)
        {
            for ( String module : p.getModules() )
            {
                if(!modules.contains( module ))
                {
                    modules.add(module);
                }
            }           
        }       
        t.setModules( modules );
    }
}
