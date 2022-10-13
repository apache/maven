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
package org.apache.maven.project.inheritance;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.codehaus.plexus.component.annotations.Component;

/**
 * DefaultModelInheritanceAssembler
 */
@Component( role = ModelInheritanceAssembler.class )
public class DefaultModelInheritanceAssembler
    implements ModelInheritanceAssembler
{
    @Override
    public void assembleModelInheritance( Model child, Model parent, String childPathAdjustment )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void assembleModelInheritance( Model child, Model parent )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void assembleBuildInheritance( Build childBuild, Build parentBuild, boolean handleAsInheritance )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copyModel( Model dest, Model source )
    {
        throw new UnsupportedOperationException();
    }
}
