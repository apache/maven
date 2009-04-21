package org.apache.maven.project.interpolation;

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

/**
 * @author jdcasey
 *         <p/>
 *         Created on Feb 2, 2005
 */
@SuppressWarnings("serial")
public class ModelInterpolationException
    extends Exception
{
    private String expression;

    private String originalMessage;

    public ModelInterpolationException( String message )
    {
        super( message );
    }

    public ModelInterpolationException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public ModelInterpolationException( String expression, String message, Throwable cause )
    {
        super( "The POM expression: " + expression + " could not be evaluated. Reason: " + message, cause );

        this.expression = expression;
        this.originalMessage = message;
    }

    public ModelInterpolationException( String expression, String message )
    {
        super( "The POM expression: " + expression + " could not be evaluated. Reason: " + message );

        this.expression = expression;
        this.originalMessage = message;
    }

    public String getExpression()
    {
        return expression;
    }

    public String getOriginalMessage()
    {
        return originalMessage;
    }

}