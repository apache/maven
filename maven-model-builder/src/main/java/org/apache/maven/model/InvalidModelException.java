package org.apache.maven.model;

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

import org.apache.maven.model.validation.ModelValidationResult;

/**
 * Signals an error due to invalid or missing model values.
 * 
 * @author Benjamin Bentmann
 */
public class InvalidModelException
    extends ModelBuildingException
{

    /**
     * The validation result, can be {@code null}.
     */
    private ModelValidationResult validationResult;

    /**
     * Creates a new exception with specified detail message and validation result.
     * 
     * @param message The detail message, may be {@code null}.
     * @param validationResult The validation result, may be {@code null}.
     */
    public InvalidModelException( String message, ModelValidationResult validationResult )
    {
        super( message );
        this.validationResult = validationResult;
    }

    /**
     * Creates a new exception with specified detail message and cause.
     * 
     * @param message The detail message, may be {@code null}.
     * @param cause The cause, may be {@code null}.
     */
    public InvalidModelException( String message, Throwable cause )
    {
        super( message, cause );
        validationResult = new ModelValidationResult();
        validationResult.addMessage( ( cause != null ) ? cause.getMessage() : message );
    }

    /**
     * Gets the validation result.
     * 
     * @return The validation result or {@code null} if unknown.
     */
    public ModelValidationResult getValidationResult()
    {
        return validationResult;
    }

}
