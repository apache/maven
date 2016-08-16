package org.apache.maven.model.condition;

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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.component.annotations.Component;

/**
 * @author Loïc B.
 */
@Component( role = CombinedValueEvaluator.class )
public class DefaultCombinedValueEvaluator
    implements CombinedValueEvaluator
{
    private static final Pattern PATTERN_AND = Pattern.compile( "^(?:all|and)\\((?:([^,]*),)*([^,]*)\\)$" );

    private static final Pattern PATTERN_OR = Pattern.compile( "^(?:any|or)\\((?:([^,]*),)*([^,]*)\\)$" );

    /*
     * (non-Javadoc)
     * @see org.apache.maven.model.profile.activation.CombinedValueEvaluator#evaluate(java.lang.String,
     * org.apache.maven.model.profile.activation.SingleValueEvaluator)
     */
    @Override
    public boolean evaluate( String expression, SingleValueEvaluator evaluator )
    {
        Matcher andMatcher = PATTERN_AND.matcher( expression );
        if ( andMatcher.matches() )
        {
            boolean result = true;
            for ( int i = 1; i <= andMatcher.groupCount(); i++ )
            {
                result &= evaluator.evaluate( andMatcher.group( i ) );
            }
            return result;
        }
        Matcher orMatcher = PATTERN_OR.matcher( expression );
        if ( orMatcher.matches() )
        {
            boolean result = false;
            for ( int i = 1; i <= andMatcher.groupCount(); i++ )
            {
                result |= evaluator.evaluate( orMatcher.group( i ) );
            }
            return result;
        }
        // Default
        return evaluator.evaluate( expression );
    }
}
