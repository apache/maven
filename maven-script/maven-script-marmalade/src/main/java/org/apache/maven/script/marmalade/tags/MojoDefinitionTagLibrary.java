package org.apache.maven.script.marmalade.tags;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.marmalade.metamodel.AbstractMarmaladeTagLibrary;

/**
 * @author jdcasey
 */
public class MojoDefinitionTagLibrary
    extends AbstractMarmaladeTagLibrary
{

    public MojoDefinitionTagLibrary()
    {
        registerTag( "aggregator", AggregatorTag.class );
        registerTag( "default", ParamDefaultTag.class );
        registerTag( "description", DescriptionTag.class );
        registerTag( "execute", ExecuteTag.class );
        registerTag( "executionStrategy", ExecutionStrategyTag.class );
        registerTag( "expression", ParamExpressionTag.class );
        registerTag( "goal", GoalTag.class );
        registerTag( "inheritByDefault", InheritByDefaultTag.class );
        registerTag( "instantiationStrategy", InstantiationStrategyTag.class );
        registerTag( "lifecyclePhase", LifecyclePhaseTag.class );
        registerTag( "metadata", MetadataTag.class );
        registerTag( "mojo", MojoTag.class );
        registerTag( "name", ParamNameTag.class );
        registerTag( "parameters", ParametersTag.class );
        registerTag( "parameter", ParameterTag.class );
        registerTag( "phase", LifecyclePhaseTag.class );
        registerTag( "required", ParamRequiredTag.class );
        registerTag( "requiresDependencyResolution", RequiresDependencyResolutionTag.class );
        registerTag( "requiresProject", RequiresProjectTag.class );
        registerTag( "requiresOnline", RequiresOnlineTag.class );
        registerTag( "type", ParamTypeTag.class );
    }

}