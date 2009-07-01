package org.apache.maven.model.validation;

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

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the warnings and errors from the model validator.
 * 
 * @author Benjamin Bentmann
 */
public class ModelValidationResult
{

    private List<String> warnings;

    private List<String> errors;

    /**
     * Creates a new validation result.
     */
    public ModelValidationResult()
    {
        warnings = new ArrayList<String>();
        errors = new ArrayList<String>();
    }

    /**
     * Gets the warnings from the validator.
     * 
     * @return The warnings from the validator, can be empty but never {@code null}.
     */
    public List<String> getWarnings()
    {
        return warnings;
    }

    /**
     * Records the specified warning.
     * 
     * @param message The detail message about the validation warning.
     */
    public void addWarning( String message )
    {
        warnings.add( message );
    }

    /**
     * Gets the errors from the validator.
     * 
     * @return The errors from the validator, can be empty but never {@code null}.
     */
    public List<String> getErrors()
    {
        return errors;
    }

    /**
     * Records the specified error.
     * 
     * @param message The detail message about the validation error.
     */
    public void addError( String message )
    {
        errors.add( message );
    }

}
