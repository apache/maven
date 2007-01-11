package org.apache.maven.script.ant;

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

import org.codehaus.classworlds.ClassRealm;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.factory.ComponentInstantiationException;
import org.codehaus.plexus.component.factory.ant.AntComponentFactory;
import org.codehaus.plexus.component.factory.ant.AntScriptInvoker;
import org.codehaus.plexus.component.repository.ComponentDescriptor;

public class AntMojoComponentFactory
    extends AntComponentFactory
{

    public Object newInstance( ComponentDescriptor descriptor, ClassRealm realm, PlexusContainer container )
        throws ComponentInstantiationException
    {
        return new AntMojoWrapper( (AntScriptInvoker) super.newInstance( descriptor, realm, container ) );
    }

}
