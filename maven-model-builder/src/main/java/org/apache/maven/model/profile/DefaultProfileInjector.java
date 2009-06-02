package org.apache.maven.model.profile;

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

import java.util.Collections;

import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.ModelBuildingRequest;
import org.apache.maven.model.Profile;
import org.apache.maven.model.merge.MavenModelMerger;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Handles profile injection into the model.
 * 
 * @author Benjamin Bentmann
 */
@Component( role = ProfileInjector.class )
public class DefaultProfileInjector
    implements ProfileInjector
{

    private ProfileModelMerger merger = new ProfileModelMerger();

    public void injectProfile( Model model, Profile profile, ModelBuildingRequest request )
    {
        if ( profile != null )
        {
            merger.mergeModelBase( model, profile );

            if ( profile.getBuild() != null )
            {
                if ( model.getBuild() == null )
                {
                    model.setBuild( new Build() );
                }
                merger.mergeBuildBase( model.getBuild(), profile.getBuild() );
            }
        }
    }

    private static class ProfileModelMerger
        extends MavenModelMerger
    {

        public void mergeModelBase( ModelBase target, ModelBase source )
        {
            mergeModelBase( target, source, true, Collections.emptyMap() );
        }

        public void mergeBuildBase( BuildBase target, BuildBase source )
        {
            mergeBuildBase( target, source, true, Collections.emptyMap() );
        }

    }

}
