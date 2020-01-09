package org.apache.maven.model.interpolation;

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

import java.util.List;

import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.codehaus.plexus.interpolation.ValueSource;

/**
 * Wraps another value source and intercepts interpolated expressions, checking for problems.
 *
 * @author Benjamin Bentmann
 */
class ProblemDetectingValueSource
    implements ValueSource
{

    private final ValueSource valueSource;

    private final String bannedPrefix;

    private final String newPrefix;

    private final ModelProblemCollector problems;

    ProblemDetectingValueSource( ValueSource valueSource, String bannedPrefix, String newPrefix,
                                        ModelProblemCollector problems )
    {
        this.valueSource = valueSource;
        this.bannedPrefix = bannedPrefix;
        this.newPrefix = newPrefix;
        this.problems = problems;
    }

    @Override
    public Object getValue( String expression )
    {
        Object value = valueSource.getValue( expression );

        if ( value != null && expression.startsWith( bannedPrefix ) )
        {
            String msg = "The expression ${" + expression + "} is deprecated.";
            if ( newPrefix != null && newPrefix.length() > 0 )
            {
                msg += " Please use ${" + newPrefix + expression.substring( bannedPrefix.length() ) + "} instead.";
            }
            problems.add( new ModelProblemCollectorRequest( Severity.WARNING, Version.V20 ).setMessage( msg ) );
        }

        return value;
    }

    @Override
    public List getFeedback()
    {
        return valueSource.getFeedback();
    }

    @Override
    public void clearFeedback()
    {
        valueSource.clearFeedback();
    }

}
