package org.apache.maven.model.merge;


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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.apache.maven.model.Model;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Profile;
import org.junit.Test;

public class MavenModelMergerTest
{
    private MavenModelMerger modelMerger = new MavenModelMerger();

    // Prerequisites are neither inherited nor injected
    @Test
    public void testMergeModel_Prerequisites()
    {
        Model parent = new Model();
        parent.setPrerequisites( new Prerequisites() );
        Model model = new Model();
        modelMerger.mergeModel_Prerequisites( model, parent, true, null );
        assertNull( model.getPrerequisites() );
        
        Prerequisites modelPrerequisites = new Prerequisites();
        modelPrerequisites.setMaven( "3.0" );
        model.setPrerequisites( modelPrerequisites );
        modelMerger.mergeModel_Prerequisites( model, parent, true, null );
        assertEquals( modelPrerequisites, model.getPrerequisites() );
    }

    // Profiles are neither inherited nor injected
    @Test
    public void testMergeModel_Profiles()
    {
        Model parent = new Model();
        parent.setProfiles( Collections.singletonList( new Profile() ) );;
        Model model = new Model();
        modelMerger.mergeModel_Profiles( model, parent, true, null );
        assertEquals( 0, model.getProfiles().size() );
        
        Profile modelProfile = new Profile();
        modelProfile.setId( "MODEL" );
        model.setProfiles( Collections.singletonList( modelProfile ) );
        modelMerger.mergeModel_Prerequisites( model, parent, true, null );
        assertEquals( Collections.singletonList( modelProfile ), model.getProfiles() );
    }

}
