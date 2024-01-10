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
package org.apache.maven.stax.xinclude;

/**
 * This class represents Exceptions that can happen while parsing an XPointer Expression.
 * <p>
 * This class is based upon a class of the same name in Apache Woden.
 */
public class InvalidXPointerException extends Exception {

    private final String fragment;
    private final Integer startChar;
    private final Integer endChar;

    /**
     * Constructs an InvalidXPointerException with a message and fragment properties.
     *
     * @param message  a String message of error
     * @param fragment a String fragment of the cause
     */
    public InvalidXPointerException(String message, String fragment) {
        this(message, fragment, null, null, null);
    }

    /**
     * Constructs an InvalidXPointerException with a message and fragment properties.
     * <p>
     * It also has a Throwable argument to support exception chaining.
     *
     * @param message  a String message of error
     * @param fragment a String fragment of the cause of the error
     * @param cause    a Throwable which caused this exception to be thrown
     */
    public InvalidXPointerException(String message, String fragment, Throwable cause) {
        this(message, fragment, null, null, cause);
    }

    /**
     * Constructs an InvalidXPointerException with a message and fragment properties,
     * and index to the cause inside the fragment.
     *
     * @param message   a String message of error
     * @param fragment  a String fragment of the cause of the error
     * @param startChar a int char index to the start of the cause in the fragment
     * @param endChar   a int char index to the end of the cause in the fragment
     */
    public InvalidXPointerException(String message, String fragment, int startChar, int endChar) {
        this(message, fragment, Integer.valueOf(startChar), Integer.valueOf(endChar), null);
    }

    /**
     * Constructs an InvalidXPointerException with a message and fragment properties,
     * and index to the cause inside the fragment.
     * <p>
     * It also has a Throwable argument to support exception chaining.
     *
     * @param message   a String message of error
     * @param fragment  a String fragment of the cause of the error
     * @param startChar an int char index to the start of the cause in the fragment
     * @param endChar   an int char index to the end of the cause in the fragment
     * @param cause     a Throwable which caused the exception to be thrown
     */
    public InvalidXPointerException(String message, String fragment, int startChar, int endChar, Throwable cause) {
        this(message, fragment, Integer.valueOf(startChar), Integer.valueOf(endChar), cause);
    }

    /**
     * Constructs a new InvalidXPointerException.
     * This constructor is called by all of the above constructors and stores the in indexes and Integers internally.
     *
     * @param message   a String message of error
     * @param fragment  a String fragment of the cause of the error
     * @param startChar an Integer char index to the start of the cause in the fragment
     * @param endChar   an Integer char index to the end of the cause in the fragment
     * @param cause     a Throwable which caused the exception to be thrown
     */
    private InvalidXPointerException(
            String message, String fragment, Integer startChar, Integer endChar, Throwable cause) {
        super(message, cause);
        this.fragment = fragment;
        this.startChar = startChar;
        this.endChar = endChar;
    }

    /**
     * Returns the fragment String stored inside this exception.
     *
     * @return a String fragment
     */
    public String getFragment() {
        return fragment;
    }

    /**
     * Returns the startChar index of the cause of this error in the fragment.
     *
     * @return an Integer of the startChar index if one exists, otherwise null
     */
    public Integer getStartChar() {
        return startChar;
    }

    /**
     * Returns the endChar index of the cause of this error in the fragment.
     *
     * @return an Integer of the startChar index if one exists, otherwise null
     */
    public Integer getEndChar() {
        return endChar;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Throwable#toString()
     */
    public String toString() {
        String postString;
        if (startChar != null && endChar != null) {
            postString = "{XPointer: " + fragment + ", start: " + startChar + ", end: " + endChar + ", substr: "
                    + fragment.substring(startChar, endChar) + "}";
        } else {
            postString = "{XPointer: " + fragment + "}";
        }
        return "InvalidXPointerException: " + getMessage() + ". " + postString;
    }
}
