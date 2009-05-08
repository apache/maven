package org.apache.maven.model.inheritance;

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

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.model.merge.MavenModelMerger;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Handles inheritance of model values.
 * 
 * @author Benjamin Bentmann
 */
@Component( role = InheritanceAssembler.class )
public class DefaultInheritanceAssembler
    implements InheritanceAssembler
{

    private MavenModelMerger merger = new MavenModelMerger();

    public void assembleModelInheritance( Model child, Model parent, String childPathAdjustment )
    {
        Map<Object, Object> hints = new HashMap<Object, Object>();
        hints.put( MavenModelMerger.CHILD_PATH_ADJUSTMENT, childPathAdjustment );
        merger.merge( child, parent, false, hints );
    }

}
