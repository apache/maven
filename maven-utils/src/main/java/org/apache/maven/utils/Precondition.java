package org.apache.maven.utils;

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

import java.util.List;

import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.INTERNAL;

/**
 * Convenient utility methods for assertion of different conditions.
 *
 * @author Karl Heinz Marbaise
 */
@API( status = INTERNAL, since = "3.7.0" )
public final class Precondition
{
    private Precondition()
    {
        // no-op
    }
/*
    int c = str != null && str.length() > 0 ? str.charAt( 0 ) : 0;
        if ( ( c < '0' || c > '9' ) && ( c < 'a' || c > 'z' ) )
    {
        Validate.notBlank( str, message );
    }
*/
    public static boolean notBlank(String str, String message)
    {
        for ( int i = 0; i < str.length(); i++ )
        {
            if ( !Character.isWhitespace( str.charAt( i ) ) )
            {
                return false;
            }
        }
        throw new IllegalArgumentException( message );
    }

    /**
     * assert that the given {@code obj} is not {@code null}.
     *
     * @param obj     The instance which should be not {@code null}.
     * @param message The message will be part of the exception.
     * @return The supplied object as convenient.
     */
    public static < T > T requireNotNull(T obj, String message)
    {
        if ( obj == null )
            throw new IllegalArgumentException( message );
        return obj;
    }

    /**
     * assert that the given {@code List<T>} is not {@code empty}.
     *
     * @param obj     The instance which should be not {@code empty}.
     * @param message The message will be part of the exception.
     * @return The supplied object as convenient.
     */
    public static < T > List< T > requireNotEmpty(List< T > obj, String message)
    {
        if ( obj.isEmpty() )
        {
            throw new IllegalArgumentException( message );
        }
        return obj;
    }

    /**
     * assert that the given {@code longValue} is greater than {@code 0}.
     *
     * @param longValue The instance which should be not {@code null}
     *                  and has to be greater than {@code 0}.
     * @param message   The message will be part of the exception.
     * @return The supplied object as convenient.
     */
    public static Long requireGreaterThanZero(Long longValue, String message)
    {
        if ( longValue == null )
        {
            throw new IllegalArgumentException( message );
        }

        if ( longValue <= 0 )
        {
            throw new IllegalArgumentException( message );
        }
        return longValue;
    }

    public static void isTrue( boolean expression, String message, final long value )
    {
        if ( !expression )
        {
            throw new IllegalArgumentException( String.format( message, Long.valueOf( value ) ) );
        }
    }

    public static void isTrue( boolean expression, String message, final Object... values )
    {
        if ( !expression )
        {
            throw new IllegalArgumentException( String.format( message, values ) );
        }
    }

    public static Long requireGreaterThanZero(Long longValue, String message, final long value) {
        if ( longValue == null )
        {
            throw new IllegalArgumentException( String.format( message, value ) );
        }

        if ( longValue <= 0 )
        {
            throw new IllegalArgumentException( String.format( message, value ) );
        }
        return longValue;
    }

    /**
     * assert that the given {@code integerValue} is greater than {@code 0}.
     *
     * @param integerValue The instance which should be not {@code null}
     *                     and has to be greater than {@code 0}.
     * @param message      The message will be part of the exception.
     * @return The supplied object as convenient.
     */
    public static Integer requireGreaterThanZero(Integer integerValue, String message)
    {
        if ( integerValue == null )
        {
            throw new IllegalArgumentException( message );
        }

        if ( integerValue <= 0 )
        {
            throw new IllegalArgumentException( message );
        }
        return integerValue;
    }

//    /**
//     * assert that the given {@code str} is not {@code null} and not {@code empty}.
//     *
//     * @param str     The str which should not be {@code null} and not be empty.
//     * @param message The message for the exception in case of {@code null}.
//     * @return The supplied object as convenient.
//     */
//    public static String requireNotEmpty(String str, String message)
//    {
//        requireNotNull( str, message );
//        if ( StringUtils.isBlank( str ) )
//        {
//            throw new IllegalArgumentException( message );
//        }
//        return str;
//    }


    public static boolean isNotEmpty( String str )
    {
        return ( ( str != null ) && ( !str.isEmpty() ) );
    }

    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isBlank(String str, String message)
    {
        if ( str == null || str.trim().isEmpty() )
        {
            return true;
        }
        return true;
    }

    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }


}

