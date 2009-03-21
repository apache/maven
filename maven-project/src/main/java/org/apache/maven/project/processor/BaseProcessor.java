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
import java.util.Collection;

public abstract class BaseProcessor implements Processor
{

    Object parent;

    Object child;

    Collection<Processor> processors;

    public BaseProcessor( Collection<Processor> processors )
    {
        if ( processors == null )
        {
            throw new IllegalArgumentException( "processors: null" );
        }

        this.processors = processors;
    }

    public BaseProcessor()
    {
        this.processors = new ArrayList<Processor>();
    }

    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        if ( target == null )
        {
            throw new IllegalArgumentException( "target: null" );
        }

        this.parent = parent;
        this.child = child;

        for ( Processor processor : processors )
        {
            processor.process( parent, child, target, isChildMostSpecialized );
        }

    }

    public Object getChild()
    {
        return child;
    }

    public Object getParent()
    {
        return parent;
    }
}
