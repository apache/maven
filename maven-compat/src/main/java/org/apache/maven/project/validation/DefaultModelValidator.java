package org.apache.maven.project.validation;

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

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 */
@Component( role = ModelValidator.class )
@Deprecated
public class DefaultModelValidator
    implements ModelValidator
{

    @Requirement
    private org.apache.maven.model.validation.ModelValidator modelValidator;

    public ModelValidationResult validate( final Model model )
    {
        final ModelValidationResult result = new ModelValidationResult();

        final ModelBuildingRequest request =
            new DefaultModelBuildingRequest().setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 );

        final SimpleModelProblemCollector problems = new SimpleModelProblemCollector( result );

        modelValidator.validateEffectiveModel( model, request, problems );

        return result;
    }

    private static class SimpleModelProblemCollector
        implements ModelProblemCollector
    {

        ModelValidationResult result;

        SimpleModelProblemCollector( final ModelValidationResult result )
        {
            this.result = result;
        }

        public void add( final ModelProblemCollectorRequest req )
        {
            if ( !ModelProblem.Severity.WARNING.equals( req.getSeverity() ) )
            {
                result.addMessage( req.getMessage() );
            }
        }
    }
}
