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

/**
 * Convenient utility methods for assertion of different conditions.
 *
 * @author Karl Heinz Marbaise
 */
public final class Precondition
{
    private Precondition()
    {
        // no-op
    }


    public static void isTrue( boolean expression, String message, final Object... values )
    {
        if ( !expression )
        {
            throw new IllegalArgumentException( String.format( message, values ) );
        }
    }

    public static Long greaterOrEqualToZero( Long currentValue, String message, final long value )
    {
        if ( currentValue == null )
        {
            throw new IllegalArgumentException( String.format( message, value ) );
        }

        if ( currentValue < 0 )
        {
            throw new IllegalArgumentException( String.format( message, value ) );
        }
        return currentValue;
    }


    public static boolean notBlank( String str, String message )
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


    public static boolean isEmpty( final CharSequence cs )
    {
        return cs == null || cs.length() == 0;
    }

    public static boolean isDigits( final String str )
    {
        return isNumeric( str );
    }

    public static boolean isNumeric( final CharSequence cs )
    {
        if ( isEmpty( cs ) )
        {
            return false;
        }
        final int sz = cs.length();
        for ( int i = 0; i < sz; i++ )
        {
            if ( !Character.isDigit( cs.charAt( i ) ) )
            {
                return false;
            }
        }
        return true;
    }


}

