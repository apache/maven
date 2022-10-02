package org.apache.maven.api.services;

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

import org.apache.maven.api.Service;
import org.apache.maven.api.annotations.Experimental;

/**
 * Service used to interact with the end user.
 *
 * @since 4.0
 */
@Experimental
public interface Prompter extends Service
{
    /**
     * Prompts the user for a string.
     *
     * @param message the message to display to the user
     * @return the string entered by the user
     * @throws PrompterException if an exception occurs
     */
    default String prompt( String message )
            throws PrompterException
    {
        return prompt( message, null, null );
    }

    /**
     * Prompts the user for a string using a default value.
     *
     * @param message the message to display
     * @param defaultReply the default reply value
     * @return the string entered by the user
     * @throws PrompterException if an exception occurs
     */
    default String prompt( String message, String defaultReply )
            throws PrompterException
    {
        return prompt( message, null, defaultReply );
    }

    /**
     * Prompts the user for a string using a list of possible values.
     *
     * @param message the message to display
     * @param possibleValues the list of possible values
     * @return the string entered by the user
     * @throws PrompterException if an exception occurs
     */
    default String prompt( String message, List<String> possibleValues )
            throws PrompterException
    {
        return prompt( message, possibleValues, null );
    }

    /**
     * Prompts the user for a string using a list of possible values and a default reply.
     *
     * @param message the message to display
     * @param possibleValues the list of possible values
     * @param defaultReply the default reply value
     * @return the string entered by the user
     * @throws PrompterException if an exception occurs
     */
    String prompt( String message, List<String> possibleValues, String defaultReply )
            throws PrompterException;

    /**
     * Prompts the user for a password.
     *
     * @param message the message to display
     * @return the password entered by the user
     * @throws PrompterException if an exception occurs
     */
    String promptForPassword( String message )
            throws PrompterException;

    /**
     * Displays a message to the user.
     *
     * @param message the message to display
     * @throws PrompterException if an exception occurs
     */
    void showMessage( String message )
            throws PrompterException;
}
