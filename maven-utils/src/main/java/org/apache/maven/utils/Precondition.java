package org.apache.maven.utils;

import com.sun.tools.javac.util.StringUtils;
import org.apiguardian.api.API;

import java.util.List;

import static org.apiguardian.api.API.Status.INTERNAL;

/**
 * Convenient utility methods for assertion of different conditions.
 *
 * @author Karl Heinz Marbaise
 */
@API( status = INTERNAL, since = "3.6.4" )
public final class Precondition
{
    private Precondition()
    {
        // no-op
    }

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

    /**
     * assert that the given {@code str} is not {@code null} and not {@code empty}.
     *
     * @param str     The str which should not be {@code null} and not be empty.
     * @param message The message for the exception in case of {@code null}.
     * @return The supplied object as convenient.
     */
    public static String requireNotEmpty(String str, String message)
    {
        requireNotNull( str, message );
        if ( StringUtils.isBlank( str ) )
        {
            throw new IllegalArgumentException( message );
        }
        return str;
    }

