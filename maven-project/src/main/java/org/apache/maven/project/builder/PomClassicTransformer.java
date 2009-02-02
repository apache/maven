package org.apache.maven.project.builder;

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

import java.io.IOException;
import java.util.List;

import org.apache.maven.shared.model.DomainModel;
import org.apache.maven.shared.model.DomainModelFactory;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelProperty;

/**
 * Provides methods for transforming model properties into a domain model for the pom classic format and vice versa.
 */
public final class PomClassicTransformer
    extends PomTransformer
{

    public PomClassicTransformer(DomainModelFactory factory)
    {
        super(factory);
    }

    public void interpolateModelProperties(List<ModelProperty> modelProperties,
                                           List<InterpolatorProperty> interpolatorProperties,
                                           DomainModel domainModel)
            throws IOException
    {
        if(! ( domainModel instanceof PomClassicDomainModel ) )
        {
            return;    
        }
        Interpolator.interpolateModelProperties( modelProperties, interpolatorProperties, (PomClassicDomainModel) domainModel);
    }
}
