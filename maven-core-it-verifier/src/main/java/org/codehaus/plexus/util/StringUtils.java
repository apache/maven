/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.codehaus.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact codehaus@codehaus.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.codehaus.org/>.
 */
package org.codehaus.plexus.util;

import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * <p>Common <code>String</code> manipulation routines.</p>
 *
 * <p>Originally from
 * <a href="http://jakarta.apache.org/turbine/">Turbine</a> and the
 * GenerationJavaCore library.</p>
 *
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author <a href="mailto:gcoladonato@yahoo.com">Greg Coladonato</a>
 * @author <a href="mailto:bayard@generationjava.com">Henri Yandell</a>
 * @author <a href="mailto:ed@codehaus.org">Ed Korthof</a>
 * @author <a href="mailto:rand_mcneely@yahoo.com">Rand McNeely</a>
 * @author Stephen Colebourne
 * @author <a href="mailto:fredrik@westermarck.com">Fredrik Westermarck</a>
 * @author Holger Krauth
 * @author <a href="mailto:alex@purpletech.com">Alexander Day Chaffee</a>
 * @since 1.0
 * @version $Id$
 */
public class StringUtils
{

    /**
     * <p><code>StringUtils</code> instances should NOT be constructed in
     * standard programming. Instead, the class should be used as
     * <code>StringUtils.trim(" foo ");</code>.</p>
     *
     * <p>This constructor is public to permit tools that require a JavaBean
     * manager to operate.</p>
     */
    public StringUtils()
    {
    }

    // Empty
    //--------------------------------------------------------------------------

    /**
     * <p>Removes control characters, including whitespace, from both
     * ends of this String, handling <code>null</code> by returning
     * an empty String.</p>
     *
     * @see java.lang.String#trim()
     * @param str the String to check
     * @return the trimmed text (never <code>null</code>)
     */
    public static String clean( String str )
    {
        return ( str == null ? "" : str.trim() );
    }

    /**
     * <p>Removes control characters, including whitespace, from both
     * ends of this String, handling <code>null</code> by returning
     * <code>null</code>.</p>
     *
     * @see java.lang.String#trim()
     * @param str the String to check
     * @return the trimmed text (or <code>null</code>)
     */
    public static String trim( String str )
    {
        return ( str == null ? null : str.trim() );
    }

    /**
     * <p>Deletes all whitespaces from a String.</p>
     *
     * <p>Whitespace is defined by
     * {@link Character#isWhitespace(char)}.</p>
     *
     * @param str String target to delete whitespace from
     * @return the String without whitespaces
     * @throws NullPointerException
     */
    public static String deleteWhitespace( String str )
    {
        StringBuffer buffer = new StringBuffer();
        int sz = str.length();
        for ( int i = 0; i < sz; i++ )
        {
            if ( !Character.isWhitespace( str.charAt( i ) ) )
            {
                buffer.append( str.charAt( i ) );
            }
        }
        return buffer.toString();
    }

    /**
     * <p>Checks if a String is non <code>null</code> and is
     * not empty (<code>length > 0</code>).</p>
     *
     * @param str the String to check
     * @return true if the String is non-null, and not length zero
     */
    public static boolean isNotEmpty( String str )
    {
        return ( str != null && str.length() > 0 );
    }

    /**
     * <p>Checks if a (trimmed) String is <code>null</code> or empty.</p>
     *
     * @param str the String to check
     * @return <code>true</code> if the String is <code>null</code>, or
     *  length zero once trimmed
     */
    public static boolean isEmpty( String str )
    {
        return ( str == null || str.trim().length() == 0 );
    }

    // Equals and IndexOf
    //--------------------------------------------------------------------------

    /**
     * <p>Compares two Strings, returning <code>true</code> if they are equal.</p>
     *
     * <p><code>null</code>s are handled without exceptions. Two <code>null</code>
     * references are considered to be equal. The comparison is case sensitive.</p>
     *
     * @see java.lang.String#equals(Object)
     * @param str1 the first string
     * @param str2 the second string
     * @return <code>true</code> if the Strings are equal, case sensitive, or
     *  both <code>null</code>
     */
    public static boolean equals( String str1, String str2 )
    {
        return ( str1 == null ? str2 == null : str1.equals( str2 ) );
    }

    /**
     * <p>Compares two Strings, returning <code>true</code> if they are equal ignoring
     * the case.</p>
     *
     * <p><code>Nulls</code> are handled without exceptions. Two <code>null</code>
     * references are considered equal. Comparison is case insensitive.</p>
     *
     * @see java.lang.String#equalsIgnoreCase(String)
     * @param str1  the first string
     * @param str2  the second string
     * @return <code>true</code> if the Strings are equal, case insensitive, or
     *  both <code>null</code>
     */
    public static boolean equalsIgnoreCase( String str1, String str2 )
    {
        return ( str1 == null ? str2 == null : str1.equalsIgnoreCase( str2 ) );
    }

    /**
     * <p>Find the first index of any of a set of potential substrings.</p>
     *
     * <p><code>null</code> String will return <code>-1</code>.</p>
     *
     * @param str the String to check
     * @param searchStrs the Strings to search for
     * @return the first index of any of the searchStrs in str
     * @throws NullPointerException if any of searchStrs[i] is <code>null</code>
     */
    public static int indexOfAny( String str, String[] searchStrs )
    {
        if ( ( str == null ) || ( searchStrs == null ) )
        {
            return -1;
        }
        int sz = searchStrs.length;

        // String's can't have a MAX_VALUEth index.
        int ret = Integer.MAX_VALUE;

        int tmp = 0;
        for ( int i = 0; i < sz; i++ )
        {
            tmp = str.indexOf( searchStrs[i] );
            if ( tmp == -1 )
            {
                continue;
            }

            if ( tmp < ret )
            {
                ret = tmp;
            }
        }

        return ( ret == Integer.MAX_VALUE ) ? -1 : ret;
    }

    /**
     * <p>Find the latest index of any of a set of potential substrings.</p>
     *
     * <p><code>null</code> string will return <code>-1</code>.</p>
     *
     * @param str  the String to check
     * @param searchStrs  the Strings to search for
     * @return the last index of any of the Strings
     * @throws NullPointerException if any of searchStrs[i] is <code>null</code>
     */
    public static int lastIndexOfAny( String str, String[] searchStrs )
    {
        if ( ( str == null ) || ( searchStrs == null ) )
        {
            return -1;
        }
        int sz = searchStrs.length;
        int ret = -1;
        int tmp = 0;
        for ( int i = 0; i < sz; i++ )
        {
            tmp = str.lastIndexOf( searchStrs[i] );
            if ( tmp > ret )
            {
                ret = tmp;
            }
        }
        return ret;
    }

    // Substring
    //--------------------------------------------------------------------------

    /**
     * <p>Gets a substring from the specified string avoiding exceptions.</p>
     *
     * <p>A negative start position can be used to start <code>n</code>
     * characters from the end of the String.</p>
     *
     * @param str the String to get the substring from
     * @param start the position to start from, negative means
     *  count back from the end of the String by this many characters
     * @return substring from start position
     */
    public static String substring( String str, int start )
    {
        if ( str == null )
        {
            return null;
        }

        // handle negatives, which means last n characters
        if ( start < 0 )
        {
            start = str.length() + start; // remember start is negative
        }

        if ( start < 0 )
        {
            start = 0;
        }
        if ( start > str.length() )
        {
            return "";
        }

        return str.substring( start );
    }

    /**
     * <p>Gets a substring from the specified String avoiding exceptions.</p>
     *
     * <p>A negative start position can be used to start/end <code>n</code>
     * characters from the end of the String.</p>
     *
     * @param str the String to get the substring from
     * @param start the position to start from, negative means
     *  count back from the end of the string by this many characters
     * @param end the position to end at (exclusive), negative means
     *  count back from the end of the String by this many characters
     * @return substring from start position to end positon
     */
    public static String substring( String str, int start, int end )
    {
        if ( str == null )
        {
            return null;
        }

        // handle negatives
        if ( end < 0 )
        {
            end = str.length() + end; // remember end is negative
        }
        if ( start < 0 )
        {
            start = str.length() + start; // remember start is negative
        }

        // check length next
        if ( end > str.length() )
        {
            // check this works.
            end = str.length();
        }

        // if start is greater than end, return ""
        if ( start > end )
        {
            return "";
        }

        if ( start < 0 )
        {
            start = 0;
        }
        if ( end < 0 )
        {
            end = 0;
        }

        return str.substring( start, end );
    }

    /**
     * <p>Gets the leftmost <code>n</code> characters of a String.</p>
     *
     * <p>If <code>n</code> characters are not available, or the
     * String is <code>null</code>, the String will be returned without
     * an exception.</p>
     *
     * @param str the String to get the leftmost characters from
     * @param len the length of the required String
     * @return the leftmost characters
     * @throws IllegalArgumentException if len is less than zero
     */
    public static String left( String str, int len )
    {
        if ( len < 0 )
        {
            throw new IllegalArgumentException( "Requested String length " + len + " is less than zero" );
        }
        if ( ( str == null ) || ( str.length() <= len ) )
        {
            return str;
        }
        else
        {
            return str.substring( 0, len );
        }
    }

    /**
     * <p>Gets the rightmost <code>n</code> characters of a String.</p>
     *
     * <p>If <code>n</code> characters are not available, or the String
     * is <code>null</code>, the String will be returned without an
     * exception.</p>
     *
     * @param str the String to get the rightmost characters from
     * @param len the length of the required String
     * @return the leftmost characters
     * @throws IllegalArgumentException if len is less than zero
     */
    public static String right( String str, int len )
    {
        if ( len < 0 )
        {
            throw new IllegalArgumentException( "Requested String length " + len + " is less than zero" );
        }
        if ( ( str == null ) || ( str.length() <= len ) )
        {
            return str;
        }
        else
        {
            return str.substring( str.length() - len );
        }
    }

    /**
     * <p>Gets <code>n</code> characters from the middle of a String.</p>
     *
     * <p>If <code>n</code> characters are not available, the remainder
     * of the String will be returned without an exception. If the
     * String is <code>null</code>, <code>null</code> will be returned.</p>
     *
     * @param str the String to get the characters from
     * @param pos the position to start from
     * @param len the length of the required String
     * @return the leftmost characters
     * @throws IndexOutOfBoundsException if pos is out of bounds
     * @throws IllegalArgumentException if len is less than zero
     */
    public static String mid( String str, int pos, int len )
    {
        if ( ( pos < 0 ) ||
            ( str != null && pos > str.length() ) )
        {
            throw new StringIndexOutOfBoundsException( "String index " + pos + " is out of bounds" );
        }
        if ( len < 0 )
        {
            throw new IllegalArgumentException( "Requested String length " + len + " is less than zero" );
        }
        if ( str == null )
        {
            return null;
        }
        if ( str.length() <= ( pos + len ) )
        {
            return str.substring( pos );
        }
        else
        {
            return str.substring( pos, pos + len );
        }
    }

    // Splitting
    //--------------------------------------------------------------------------

    /**
     * <p>Splits the provided text into a array, using whitespace as the
     * separator.</p>
     *
     * <p>The separator is not included in the returned String array.</p>
     *
     * @param str the String to parse
     * @return an array of parsed Strings
     */
    public static String[] split( String str )
    {
        return split( str, null, -1 );
    }

    /**
     * @see #split(String, String, int)
     */
    public static String[] split( String text, String separator )
    {
        return split( text, separator, -1 );
    }

    /**
     * <p>Splits the provided text into a array, based on a given separator.</p>
     *
     * <p>The separator is not included in the returned String array. The
     * maximum number of splits to perfom can be controlled. A <code>null</code>
     * separator will cause parsing to be on whitespace.</p>
     *
     * <p>This is useful for quickly splitting a String directly into
     * an array of tokens, instead of an enumeration of tokens (as
     * <code>StringTokenizer</code> does).</p>
     *
     * @param str The string to parse.
     * @param separator Characters used as the delimiters. If
     *  <code>null</code>, splits on whitespace.
     * @param max The maximum number of elements to include in the
     *  array.  A zero or negative value implies no limit.
     * @return an array of parsed Strings
     */
    public static String[] split( String str, String separator, int max )
    {
        StringTokenizer tok = null;
        if ( separator == null )
        {
            // Null separator means we're using StringTokenizer's default
            // delimiter, which comprises all whitespace characters.
            tok = new StringTokenizer( str );
        }
        else
        {
            tok = new StringTokenizer( str, separator );
        }

        int listSize = tok.countTokens();
        if ( max > 0 && listSize > max )
        {
            listSize = max;
        }

        String[] list = new String[listSize];
        int i = 0;
        int lastTokenBegin = 0;
        int lastTokenEnd = 0;
        while ( tok.hasMoreTokens() )
        {
            if ( max > 0 && i == listSize - 1 )
            {
                // In the situation where we hit the max yet have
                // tokens left over in our input, the last list
                // element gets all remaining text.
                String endToken = tok.nextToken();
                lastTokenBegin = str.indexOf( endToken, lastTokenEnd );
                list[i] = str.substring( lastTokenBegin );
                break;
            }
            else
            {
                list[i] = tok.nextToken();
                lastTokenBegin = str.indexOf( list[i], lastTokenEnd );
                lastTokenEnd = lastTokenBegin + list[i].length();
            }
            i++;
        }
        return list;
    }

    // Joining
    //--------------------------------------------------------------------------
    /**
     * <p>Concatenates elements of an array into a single String.</p>
     *
     * <p>The difference from join is that concatenate has no delimiter.</p>
     *
     * @param array the array of values to concatenate.
     * @return the concatenated string.
     */
    public static String concatenate( Object[] array )
    {
        return join( array, "" );
    }

    /**
     * <p>Joins the elements of the provided array into a single String
     * containing the provided list of elements.</p>
     *
     * <p>No delimiter is added before or after the list. A
     * <code>null</code> separator is the same as a blank String.</p>
     *
     * @param array the array of values to join together
     * @param separator the separator character to use
     * @return the joined String
     */
    public static String join( Object[] array, String separator )
    {
        if ( separator == null )
        {
            separator = "";
        }
        int arraySize = array.length;
        int bufSize = ( arraySize == 0 ? 0 : ( array[0].toString().length() +
            separator.length() ) * arraySize );
        StringBuffer buf = new StringBuffer( bufSize );

        for ( int i = 0; i < arraySize; i++ )
        {
            if ( i > 0 )
            {
                buf.append( separator );
            }
            buf.append( array[i] );
        }
        return buf.toString();
    }

    /**
     * <p>Joins the elements of the provided <code>Iterator</code> into
     * a single String containing the provided elements.</p>
     *
     * <p>No delimiter is added before or after the list. A
     * <code>null</code> separator is the same as a blank String.</p>
     *
     * @param iterator the <code>Iterator</code> of values to join together
     * @param separator  the separator character to use
     * @return the joined String
     */
    public static String join( Iterator iterator, String separator )
    {
        if ( separator == null )
        {
            separator = "";
        }
        StringBuffer buf = new StringBuffer( 256 );  // Java default is 16, probably too small
        while ( iterator.hasNext() )
        {
            buf.append( iterator.next() );
            if ( iterator.hasNext() )
            {
                buf.append( separator );
            }
        }
        return buf.toString();
    }



    // Replacing
    //--------------------------------------------------------------------------

    /**
     * <p>Replace a char with another char inside a larger String, once.</p>
     *
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     *
     * @see #replace(String text, char repl, char with, int max)
     * @param text text to search and replace in
     * @param repl char to search for
     * @param with char to replace with
     * @return the text with any replacements processed
     */
    public static String replaceOnce( String text, char repl, char with )
    {
        return replace( text, repl, with, 1 );
    }

    /**
     * <p>Replace all occurances of a char within another char.</p>
     *
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     *
     * @see #replace(String text, char repl, char with, int max)
     * @param text text to search and replace in
     * @param repl char to search for
     * @param with char to replace with
     * @return the text with any replacements processed
     */
    public static String replace( String text, char repl, char with )
    {
        return replace( text, repl, with, -1 );
    }

    /**
     * <p>Replace a char with another char inside a larger String,
     * for the first <code>max</code> values of the search char.</p>
     *
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     *
     * @param text text to search and replace in
     * @param repl char to search for
     * @param with char to replace with
     * @param max maximum number of values to replace, or <code>-1</code> if no maximum
     * @return the text with any replacements processed
     */
    public static String replace( String text, char repl, char with, int max )
    {
        return replace( text, String.valueOf( repl ), String.valueOf( with ), max );
    }

    /**
     * <p>Replace a String with another String inside a larger String, once.</p>
     *
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     *
     * @see #replace(String text, String repl, String with, int max)
     * @param text text to search and replace in
     * @param repl String to search for
     * @param with String to replace with
     * @return the text with any replacements processed
     */
    public static String replaceOnce( String text, String repl, String with )
    {
        return replace( text, repl, with, 1 );
    }

    /**
     * <p>Replace all occurances of a String within another String.</p>
     *
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     *
     * @see #replace(String text, String repl, String with, int max)
     * @param text text to search and replace in
     * @param repl String to search for
     * @param with String to replace with
     * @return the text with any replacements processed
     */
    public static String replace( String text, String repl, String with )
    {
        return replace( text, repl, with, -1 );
    }

    /**
     * <p>Replace a String with another String inside a larger String,
     * for the first <code>max</code> values of the search String.</p>
     *
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     *
     * @param text text to search and replace in
     * @param repl String to search for
     * @param with String to replace with
     * @param max maximum number of values to replace, or <code>-1</code> if no maximum
     * @return the text with any replacements processed
     */
    public static String replace( String text, String repl, String with, int max )
    {
        if ( text == null || repl == null || with == null || repl.length() == 0 )
        {
            return text;
        }

        StringBuffer buf = new StringBuffer( text.length() );
        int start = 0, end = 0;
        while ( ( end = text.indexOf( repl, start ) ) != -1 )
        {
            buf.append( text.substring( start, end ) ).append( with );
            start = end + repl.length();

            if ( --max == 0 )
            {
                break;
            }
        }
        buf.append( text.substring( start ) );
        return buf.toString();
    }

    /**
     * <p>Overlay a part of a String with another String.</p>
     *
     * @param text String to do overlaying in
     * @param overlay String to overlay
     * @param start int to start overlaying at
     * @param end int to stop overlaying before
     * @return String with overlayed text
     * @throws NullPointerException if text or overlay is <code>null</code>
     */
    public static String overlayString( String text, String overlay, int start, int end )
    {
        return new StringBuffer( start + overlay.length() + text.length() - end + 1 )
            .append( text.substring( 0, start ) )
            .append( overlay )
            .append( text.substring( end ) )
            .toString();
    }

    // Centering
    //--------------------------------------------------------------------------

    /**
     * <p>Center a String in a larger String of size <code>n</code>.<p>
     *
     * <p>Uses spaces as the value to buffer the String with.
     * Equivalent to <code>center(str, size, " ")</code>.</p>
     *
     * @param str String to center
     * @param size int size of new String
     * @return String containing centered String
     * @throws NullPointerException if str is <code>null</code>
     */
    public static String center( String str, int size )
    {
        return center( str, size, " " );
    }

    /**
     * <p>Center a String in a larger String of size <code>n</code>.</p>
     *
     * <p>Uses a supplied String as the value to buffer the String with.</p>
     *
     * @param str String to center
     * @param size int size of new String
     * @param delim String to buffer the new String with
     * @return String containing centered String
     * @throws NullPointerException if str or delim is <code>null</code>
     * @throws ArithmeticException if delim is the empty String
     */
    public static String center( String str, int size, String delim )
    {
        int sz = str.length();
        int p = size - sz;
        if ( p < 1 )
        {
            return str;
        }
        str = leftPad( str, sz + p / 2, delim );
        str = rightPad( str, size, delim );
        return str;
    }

    // Chomping
    //--------------------------------------------------------------------------

    /**
     * <p>Remove the last newline, and everything after it from a String.</p>
     *
     * @param str String to chomp the newline from
     * @return String without chomped newline
     * @throws NullPointerException if str is <code>null</code>
     */
    public static String chomp( String str )
    {
        return chomp( str, "\n" );
    }

    /**
     * <p>Remove the last value of a supplied String, and everything after
     * it from a String.</p>
     *
     * @param str String to chomp from
     * @param sep String to chomp
     * @return String without chomped ending
     * @throws NullPointerException if str or sep is <code>null</code>
     */
    public static String chomp( String str, String sep )
    {
        int idx = str.lastIndexOf( sep );
        if ( idx != -1 )
        {
            return str.substring( 0, idx );
        }
        else
        {
            return str;
        }
    }

    /**
     * <p>Remove a newline if and only if it is at the end
     * of the supplied String.</p>
     *
     * @param str String to chomp from
     * @return String without chomped ending
     * @throws NullPointerException if str is <code>null</code>
     */
    public static String chompLast( String str )
    {
        return chompLast( str, "\n" );
    }

    /**
     * <p>Remove a value if and only if the String ends with that value.</p>
     *
     * @param str String to chomp from
     * @param sep String to chomp
     * @return String without chomped ending
     * @throws NullPointerException if str or sep is <code>null</code>
     */
    public static String chompLast( String str, String sep )
    {
        if ( str.length() == 0 )
        {
            return str;
        }
        String sub = str.substring( str.length() - sep.length() );
        if ( sep.equals( sub ) )
        {
            return str.substring( 0, str.length() - sep.length() );
        }
        else
        {
            return str;
        }
    }

    /**
     * <p>Remove everything and return the last value of a supplied String, and
     * everything after it from a String.</p>
     *
     * @param str String to chomp from
     * @param sep String to chomp
     * @return String chomped
     * @throws NullPointerException if str or sep is <code>null</code>
     */
    public static String getChomp( String str, String sep )
    {
        int idx = str.lastIndexOf( sep );
        if ( idx == str.length() - sep.length() )
        {
            return sep;
        }
        else if ( idx != -1 )
        {
            return str.substring( idx );
        }
        else
        {
            return "";
        }
    }

    /**
     * <p>Remove the first value of a supplied String, and everything before it
     * from a String.</p>
     *
     * @param str String to chomp from
     * @param sep String to chomp
     * @return String without chomped beginning
     * @throws NullPointerException if str or sep is <code>null</code>
     */
    public static String prechomp( String str, String sep )
    {
        int idx = str.indexOf( sep );
        if ( idx != -1 )
        {
            return str.substring( idx + sep.length() );
        }
        else
        {
            return str;
        }
    }

    /**
     * <p>Remove and return everything before the first value of a
     * supplied String from another String.</p>
     *
     * @param str String to chomp from
     * @param sep String to chomp
     * @return String prechomped
     * @throws NullPointerException if str or sep is <code>null</code>
     */
    public static String getPrechomp( String str, String sep )
    {
        int idx = str.indexOf( sep );
        if ( idx != -1 )
        {
            return str.substring( 0, idx + sep.length() );
        }
        else
        {
            return "";
        }
    }

    // Chopping
    //--------------------------------------------------------------------------

    /**
     * <p>Remove the last character from a String.</p>
     *
     * <p>If the String ends in <code>\r\n</code>, then remove both
     * of them.</p>
     *
     * @param str String to chop last character from
     * @return String without last character
     * @throws NullPointerException if str is <code>null</code>
     */
    public static String chop( String str )
    {
        if ( "".equals( str ) )
        {
            return "";
        }
        if ( str.length() == 1 )
        {
            return "";
        }
        int lastIdx = str.length() - 1;
        String ret = str.substring( 0, lastIdx );
        char last = str.charAt( lastIdx );
        if ( last == '\n' )
        {
            if ( ret.charAt( lastIdx - 1 ) == '\r' )
            {
                return ret.substring( 0, lastIdx - 1 );
            }
        }
        return ret;
    }

    /**
     * <p>Remove <code>\n</code> from end of a String if it's there.
     * If a <code>\r</code> precedes it, then remove that too.</p>
     *
     * @param str String to chop a newline from
     * @return String without newline
     * @throws NullPointerException if str is <code>null</code>
     */
    public static String chopNewline( String str )
    {
        int lastIdx = str.length() - 1;
        char last = str.charAt( lastIdx );
        if ( last == '\n' )
        {
            if ( str.charAt( lastIdx - 1 ) == '\r' )
            {
                lastIdx--;
            }
        }
        else
        {
            lastIdx++;
        }
        return str.substring( 0, lastIdx );
    }


    // Conversion
    //--------------------------------------------------------------------------

    // spec 3.10.6
    /**
     * <p>Escapes any values it finds into their String form.</p>
     *
     * <p>So a tab becomes the characters <code>'\\'</code> and
     * <code>'t'</code>.</p>
     *
     * @param str String to escape values in
     * @return String with escaped values
     * @throws NullPointerException if str is <code>null</code>
     */
    public static String escape( String str )
    {
        // improved with code from  cybertiger@cyberiantiger.org
        // unicode from him, and defaul for < 32's.
        int sz = str.length();
        StringBuffer buffer = new StringBuffer( 2 * sz );
        for ( int i = 0; i < sz; i++ )
        {
            char ch = str.charAt( i );

            // handle unicode
            if ( ch > 0xfff )
            {
                buffer.append( "\\u" + Integer.toHexString( ch ) );
            }
            else if ( ch > 0xff )
            {
                buffer.append( "\\u0" + Integer.toHexString( ch ) );
            }
            else if ( ch > 0x7f )
            {
                buffer.append( "\\u00" + Integer.toHexString( ch ) );
            }
            else if ( ch < 32 )
            {
                switch ( ch )
                {
                    case '\b':
                        buffer.append( '\\' );
                        buffer.append( 'b' );
                        break;
                    case '\n':
                        buffer.append( '\\' );
                        buffer.append( 'n' );
                        break;
                    case '\t':
                        buffer.append( '\\' );
                        buffer.append( 't' );
                        break;
                    case '\f':
                        buffer.append( '\\' );
                        buffer.append( 'f' );
                        break;
                    case '\r':
                        buffer.append( '\\' );
                        buffer.append( 'r' );
                        break;
                    default :
                        if ( ch > 0xf )
                        {
                            buffer.append( "\\u00" + Integer.toHexString( ch ) );
                        }
                        else
                        {
                            buffer.append( "\\u000" + Integer.toHexString( ch ) );
                        }
                        break;
                }
            }
            else
            {
                switch ( ch )
                {
                    case '\'':
                        buffer.append( '\\' );
                        buffer.append( '\'' );
                        break;
                    case '"':
                        buffer.append( '\\' );
                        buffer.append( '"' );
                        break;
                    case '\\':
                        buffer.append( '\\' );
                        buffer.append( '\\' );
                        break;
                    default :
                        buffer.append( ch );
                        break;
                }
            }
        }
        return buffer.toString();
    }

    // Padding
    //--------------------------------------------------------------------------

    /**
     * <p>Repeat a String <code>n</code> times to form a
     * new string.</p>
     *
     * @param str String to repeat
     * @param repeat number of times to repeat str
     * @return String with repeated String
     * @throws NegativeArraySizeException if <code>repeat < 0</code>
     * @throws NullPointerException if str is <code>null</code>
     */
    public static String repeat( String str, int repeat )
    {
        StringBuffer buffer = new StringBuffer( repeat * str.length() );
        for ( int i = 0; i < repeat; i++ )
        {
            buffer.append( str );
        }
        return buffer.toString();
    }

    /**
     * <p>Right pad a String with spaces.</p>
     *
     * <p>The String is padded to the size of <code>n</code>.</p>
     *
     * @param str String to repeat
     * @param size number of times to repeat str
     * @return right padded String
     * @throws NullPointerException if str is <code>null</code>
     */
    public static String rightPad( String str, int size )
    {
        return rightPad( str, size, " " );
    }

    /**
     * <p>Right pad a String with a specified string.</p>
     *
     * <p>The String is padded to the size of <code>n</code>.</p>
     *
     * @param str String to pad out
     * @param size size to pad to
     * @param delim String to pad with
     * @return right padded String
     * @throws NullPointerException if str or delim is <code>null</code>
     * @throws ArithmeticException if delim is the empty String
     */
    public static String rightPad( String str, int size, String delim )
    {
        size = ( size - str.length() ) / delim.length();
        if ( size > 0 )
        {
            str += repeat( delim, size );
        }
        return str;
    }

    /**
     * <p>Left pad a String with spaces.</p>
     *
     * <p>The String is padded to the size of <code>n</code>.</p>
     *
     * @param str String to pad out
     * @param size size to pad to
     * @return left padded String
     * @throws NullPointerException if str or delim is <code>null</code>
     */
    public static String leftPad( String str, int size )
    {
        return leftPad( str, size, " " );
    }

    /**
     * Left pad a String with a specified string. Pad to a size of n.
     *
     * @param str String to pad out
     * @param size size to pad to
     * @param delim String to pad with
     * @return left padded String
     * @throws NullPointerException if str or delim is null
     * @throws ArithmeticException if delim is the empty string
     */
    public static String leftPad( String str, int size, String delim )
    {
        size = ( size - str.length() ) / delim.length();
        if ( size > 0 )
        {
            str = repeat( delim, size ) + str;
        }
        return str;
    }

    // Stripping
    //--------------------------------------------------------------------------

    /**
     * <p>Remove whitespace from the front and back of a String.</p>
     *
     * @param str the String to remove whitespace from
     * @return the stripped String
     */
    public static String strip( String str )
    {
        return strip( str, null );
    }

    /**
     * <p>Remove a specified String from the front and back of a
     * String.</p>
     *
     * <p>If whitespace is wanted to be removed, used the
     * {@link #strip(java.lang.String)} method.</p>
     *
     * @param str the String to remove a string from
     * @param delim the String to remove at start and end
     * @return the stripped String
     */
    public static String strip( String str, String delim )
    {
        str = stripStart( str, delim );
        return stripEnd( str, delim );
    }

    /**
     * <p>Strip whitespace from the front and back of every String
     * in the array.</p>
     *
     * @param strs the Strings to remove whitespace from
     * @return the stripped Strings
     */
    public static String[] stripAll( String[] strs )
    {
        return stripAll( strs, null );
    }

    /**
     * <p>Strip the specified delimiter from the front and back of
     * every String in the array.</p>
     *
     * @param strs the Strings to remove a String from
     * @param delimiter the String to remove at start and end
     * @return the stripped Strings
     */
    public static String[] stripAll( String[] strs, String delimiter )
    {
        if ( ( strs == null ) || ( strs.length == 0 ) )
        {
            return strs;
        }
        int sz = strs.length;
        String[] newArr = new String[sz];
        for ( int i = 0; i < sz; i++ )
        {
            newArr[i] = strip( strs[i], delimiter );
        }
        return newArr;
    }

    /**
     * <p>Strip any of a supplied String from the end of a String.</p>
     *
     * <p>If the strip String is <code>null</code>, whitespace is
     * stripped.</p>
     *
     * @param str the String to remove characters from
     * @param strip the String to remove
     * @return the stripped String
     */
    public static String stripEnd( String str, String strip )
    {
        if ( str == null )
        {
            return null;
        }
        int end = str.length();

        if ( strip == null )
        {
            while ( ( end != 0 ) && Character.isWhitespace( str.charAt( end - 1 ) ) )
            {
                end--;
            }
        }
        else
        {
            while ( ( end != 0 ) && ( strip.indexOf( str.charAt( end - 1 ) ) != -1 ) )
            {
                end--;
            }
        }
        return str.substring( 0, end );
    }

    /**
     * <p>Strip any of a supplied String from the start of a String.</p>
     *
     * <p>If the strip String is <code>null</code>, whitespace is
     * stripped.</p>
     *
     * @param str the String to remove characters from
     * @param strip the String to remove
     * @return the stripped String
     */
    public static String stripStart( String str, String strip )
    {
        if ( str == null )
        {
            return null;
        }

        int start = 0;

        int sz = str.length();

        if ( strip == null )
        {
            while ( ( start != sz ) && Character.isWhitespace( str.charAt( start ) ) )
            {
                start++;
            }
        }
        else
        {
            while ( ( start != sz ) && ( strip.indexOf( str.charAt( start ) ) != -1 ) )
            {
                start++;
            }
        }
        return str.substring( start );
    }

    // Case conversion
    //--------------------------------------------------------------------------

    /**
     * <p>Convert a String to upper case, <code>null</code> String
     * returns <code>null</code>.</p>
     *
     * @param str the String to uppercase
     * @return the upper cased String
     */
    public static String upperCase( String str )
    {
        if ( str == null )
        {
            return null;
        }
        return str.toUpperCase();
    }

    /**
     * <p>Convert a String to lower case, <code>null</code> String
     * returns <code>null</code>.</p>
     *
     * @param str the string to lowercase
     * @return the lower cased String
     */
    public static String lowerCase( String str )
    {
        if ( str == null )
        {
            return null;
        }
        return str.toLowerCase();
    }

    /**
     * <p>Uncapitalise a String.</p>
     *
     * <p>That is, convert the first character into lower-case.
     * <code>null</code> is returned as <code>null</code>.</p>
     *
     * @param str the String to uncapitalise
     * @return uncapitalised String
     */
    public static String uncapitalise( String str )
    {
        if ( str == null )
        {
            return null;
        }
        else if ( str.length() == 0 )
        {
            return "";
        }
        else
        {
            return new StringBuffer( str.length() )
                .append( Character.toLowerCase( str.charAt( 0 ) ) )
                .append( str.substring( 1 ) )
                .toString();
        }
    }

    /**
     * <p>Capitalise a String.</p>
     *
     * <p>That is, convert the first character into title-case.
     * <code>null</code> is returned as <code>null</code>.</p>
     *
     * @param str the String to capitalise
     * @return capitalised String
     */
    public static String capitalise( String str )
    {
        if ( str == null )
        {
            return null;
        }
        else if ( str.length() == 0 )
        {
            return "";
        }
        else
        {
            return new StringBuffer( str.length() )
                .append( Character.toTitleCase( str.charAt( 0 ) ) )
                .append( str.substring( 1 ) )
                .toString();
        }
    }

    /**
     * <p>Swaps the case of String.</p>
     *
     * <p>Properly looks after making sure the start of words
     * are Titlecase and not Uppercase.</p>
     *
     * <p><code>null</code> is returned as <code>null</code>.</p>
     *
     * @param str the String to swap the case of
     * @return the modified String
     */
    public static String swapCase( String str )
    {
        if ( str == null )
        {
            return null;
        }
        int sz = str.length();
        StringBuffer buffer = new StringBuffer( sz );

        boolean whitespace = false;
        char ch = 0;
        char tmp = 0;

        for ( int i = 0; i < sz; i++ )
        {
            ch = str.charAt( i );
            if ( Character.isUpperCase( ch ) )
            {
                tmp = Character.toLowerCase( ch );
            }
            else if ( Character.isTitleCase( ch ) )
            {
                tmp = Character.toLowerCase( ch );
            }
            else if ( Character.isLowerCase( ch ) )
            {
                if ( whitespace )
                {
                    tmp = Character.toTitleCase( ch );
                }
                else
                {
                    tmp = Character.toUpperCase( ch );
                }
            }
            else
            {
                tmp = ch;
            }
            buffer.append( tmp );
            whitespace = Character.isWhitespace( ch );
        }
        return buffer.toString();
    }


    /**
     * <p>Capitalise all the words in a String.</p>
     *
     * <p>Uses {@link Character#isWhitespace(char)} as a
     * separator between words.</p>
     *
     * <p><code>null</code> will return <code>null</code>.</p>
     *
     * @param str the String to capitalise
     * @return capitalised String
     */
    public static String capitaliseAllWords( String str )
    {
        if ( str == null )
        {
            return null;
        }
        int sz = str.length();
        StringBuffer buffer = new StringBuffer( sz );
        boolean space = true;
        for ( int i = 0; i < sz; i++ )
        {
            char ch = str.charAt( i );
            if ( Character.isWhitespace( ch ) )
            {
                buffer.append( ch );
                space = true;
            }
            else if ( space )
            {
                buffer.append( Character.toTitleCase( ch ) );
                space = false;
            }
            else
            {
                buffer.append( ch );
            }
        }
        return buffer.toString();
    }

    /**
     * <p>Uncapitalise all the words in a string.</p>
     *
     * <p>Uses {@link Character#isWhitespace(char)} as a
     * separator between words.</p>
     *
     * <p><code>null</code> will return <code>null</code>.</p>
     *
     * @param str  the string to uncapitalise
     * @return uncapitalised string
     */
    public static String uncapitaliseAllWords( String str )
    {
        if ( str == null )
        {
            return null;
        }
        int sz = str.length();
        StringBuffer buffer = new StringBuffer( sz );
        boolean space = true;
        for ( int i = 0; i < sz; i++ )
        {
            char ch = str.charAt( i );
            if ( Character.isWhitespace( ch ) )
            {
                buffer.append( ch );
                space = true;
            }
            else if ( space )
            {
                buffer.append( Character.toLowerCase( ch ) );
                space = false;
            }
            else
            {
                buffer.append( ch );
            }
        }
        return buffer.toString();
    }

    // Nested extraction
    //--------------------------------------------------------------------------

    /**
     * <p>Get the String that is nested in between two instances of the
     * same String.</p>
     *
     * <p>If <code>str</code> is <code>null</code>, will
     * return <code>null</code>.</p>
     *
     * @param str the String containing nested-string
     * @param tag the String before and after nested-string
     * @return the String that was nested, or <code>null</code>
     * @throws NullPointerException if tag is <code>null</code>
     */
    public static String getNestedString( String str, String tag )
    {
        return getNestedString( str, tag, tag );
    }

    /**
     * <p>Get the String that is nested in between two Strings.</p>
     *
     * @param str the String containing nested-string
     * @param open the String before nested-string
     * @param close the String after nested-string
     * @return the String that was nested, or <code>null</code>
     * @throws NullPointerException if open or close is <code>null</code>
     */
    public static String getNestedString( String str, String open, String close )
    {
        if ( str == null )
        {
            return null;
        }
        int start = str.indexOf( open );
        if ( start != -1 )
        {
            int end = str.indexOf( close, start + open.length() );
            if ( end != -1 )
            {
                return str.substring( start + open.length(), end );
            }
        }
        return null;
    }

    /**
     * <p>How many times is the substring in the larger String.</p>
     *
     * <p><code>null</code> returns <code>0</code>.</p>
     *
     * @param str the String to check
     * @param sub the substring to count
     * @return the number of occurances, 0 if the String is <code>null</code>
     * @throws NullPointerException if sub is <code>null</code>
     */
    public static int countMatches( String str, String sub )
    {
        if ( sub.equals( "" ) )
        {
            return 0;
        }
        if ( str == null )
        {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ( ( idx = str.indexOf( sub, idx ) ) != -1 )
        {
            count++;
            idx += sub.length();
        }
        return count;
    }

    // Character Tests
    //--------------------------------------------------------------------------

    /**
     * <p>Checks if the String contains only unicode letters.</p>
     *
     * <p><code>null</code> will return <code>false</code>.
     * An empty String will return <code>true</code>.</p>
     *
     * @param str the String to check
     * @return <code>true</code> if only contains letters, and is non-null
     */
    public static boolean isAlpha( String str )
    {
        if ( str == null )
        {
            return false;
        }
        int sz = str.length();
        for ( int i = 0; i < sz; i++ )
        {
            if ( Character.isLetter( str.charAt( i ) ) == false )
            {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Checks if the String contains only whitespace.</p>
     *
     * <p><code>null</code> will return <code>false</code>. An
     * empty String will return <code>true</code>.</p>
     *
     * @param str the String to check
     * @return <code>true</code> if only contains whitespace, and is non-null
     */
    public static boolean isWhitespace( String str )
    {
        if ( str == null )
        {
            return false;
        }
        int sz = str.length();
        for ( int i = 0; i < sz; i++ )
        {
            if ( ( Character.isWhitespace( str.charAt( i ) ) == false ) )
            {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Checks if the String contains only unicode letters and
     * space (<code>' '</code>).</p>
     *
     * <p><code>null</code> will return <code>false</code>. An
     * empty String will return <code>true</code>.</p>
     *
     * @param str the String to check
     * @return <code>true</code> if only contains letters and space,
     *  and is non-null
     */
    public static boolean isAlphaSpace( String str )
    {
        if ( str == null )
        {
            return false;
        }
        int sz = str.length();
        for ( int i = 0; i < sz; i++ )
        {
            if ( ( Character.isLetter( str.charAt( i ) ) == false ) &&
                ( str.charAt( i ) != ' ' ) )
            {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Checks if the String contains only unicode letters or digits.</p>
     *
     * <p><code>null</code> will return <code>false</code>. An empty
     * String will return <code>true</code>.</p>
     *
     * @param str the String to check
     * @return <code>true</code> if only contains letters or digits,
     *  and is non-null
     */
    public static boolean isAlphanumeric( String str )
    {
        if ( str == null )
        {
            return false;
        }
        int sz = str.length();
        for ( int i = 0; i < sz; i++ )
        {
            if ( Character.isLetterOrDigit( str.charAt( i ) ) == false )
            {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Checks if the String contains only unicode letters, digits
     * or space (<code>' '</code>).</p>
     *
     * <p><code>null</code> will return <code>false</code>. An empty
     * String will return <code>true</code>.</p>
     *
     * @param str the String to check
     * @return <code>true</code> if only contains letters, digits or space,
     *  and is non-null
     */
    public static boolean isAlphanumericSpace( String str )
    {
        if ( str == null )
        {
            return false;
        }
        int sz = str.length();
        for ( int i = 0; i < sz; i++ )
        {
            if ( ( Character.isLetterOrDigit( str.charAt( i ) ) == false ) &&
                ( str.charAt( i ) != ' ' ) )
            {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Checks if the String contains only unicode digits.</p>
     *
     * <p><code>null</code> will return <code>false</code>.
     * An empty String will return <code>true</code>.</p>
     *
     * @param str the String to check
     * @return <code>true</code> if only contains digits, and is non-null
     */
    public static boolean isNumeric( String str )
    {
        if ( str == null )
        {
            return false;
        }
        int sz = str.length();
        for ( int i = 0; i < sz; i++ )
        {
            if ( Character.isDigit( str.charAt( i ) ) == false )
            {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Checks if the String contains only unicode digits or space
     * (<code>' '</code>).</p>
     *
     * <p><code>null</code> will return <code>false</code>. An empty
     * String will return <code>true</code>.</p>
     *
     * @param str the String to check
     * @return <code>true</code> if only contains digits or space,
     *  and is non-null
     */
    public static boolean isNumericSpace( String str )
    {
        if ( str == null )
        {
            return false;
        }
        int sz = str.length();
        for ( int i = 0; i < sz; i++ )
        {
            if ( ( Character.isDigit( str.charAt( i ) ) == false ) &&
                ( str.charAt( i ) != ' ' ) )
            {
                return false;
            }
        }
        return true;
    }

    // Defaults
    //--------------------------------------------------------------------------

    /**
     * <p>Returns either the passed in <code>Object</code> as a String,
     * or, if the <code>Object</code> is <code>null</code>, an empty
     * String.</p>
     *
     * @param obj the Object to check
     * @return the passed in Object's toString, or blank if it was
     *  <code>null</code>
     */
    public static String defaultString( Object obj )
    {
        return defaultString( obj, "" );
    }

    /**
     * <p>Returns either the passed in <code>Object</code> as a String,
     * or, if the <code>Object</code> is <code>null</code>, a passed
     * in default String.</p>
     *
     * @param obj the Object to check
     * @param defaultString  the default String to return if str is
     *  <code>null</code>
     * @return the passed in string, or the default if it was
     *  <code>null</code>
     */
    public static String defaultString( Object obj, String defaultString )
    {
        return ( obj == null ) ? defaultString : obj.toString();
    }

    // Reversing
    //--------------------------------------------------------------------------

    /**
     * <p>Reverse a String.</p>
     *
     * <p><code>null</code> String returns <code>null</code>.</p>
     *
     * @param str the String to reverse
     * @return the reversed String
     */
    public static String reverse( String str )
    {
        if ( str == null )
        {
            return null;
        }
        return new StringBuffer( str ).reverse().toString();
    }

    /**
     * <p>Reverses a String that is delimited by a specific character.</p>
     *
     * <p>The Strings between the delimiters are not reversed.
     * Thus java.lang.String becomes String.lang.java (if the delimiter
     * is <code>'.'</code>).</p>
     *
     * @param str the String to reverse
     * @param delimiter the delimiter to use
     * @return the reversed String
     */
    public static String reverseDelimitedString( String str, String delimiter )
    {
        // could implement manually, but simple way is to reuse other,
        // probably slower, methods.
        String[] strs = split( str, delimiter );
        reverseArray( strs );
        return join( strs, delimiter );
    }

    /**
     * <p>Reverses an array.</p>
     *
     * <p>TAKEN FROM CollectionsUtils.</p>
     *
     * @param array  the array to reverse
     */
    private static void reverseArray( Object[] array )
    {
        int i = 0;
        int j = array.length - 1;
        Object tmp;

        while ( j > i )
        {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    // Abbreviating
    //--------------------------------------------------------------------------

    /**
     * Turn "Now is the time for all good men" into "Now is the time for..."
     * <p>
     * Specifically:
     * <p>
     * If str is less than max characters long, return it.
     * Else abbreviate it to (substring(str, 0, max-3) + "...").
     * If maxWidth is less than 3, throw an IllegalArgumentException.
     * In no case will it return a string of length greater than maxWidth.
     *
     * @param maxWidth maximum length of result string
     **/
    public static String abbreviate( String s, int maxWidth )
    {
        return abbreviate( s, 0, maxWidth );
    }

    /**
     * Turn "Now is the time for all good men" into "...is the time for..."
     * <p>
     * Works like abbreviate(String, int), but allows you to specify a "left edge"
     * offset.  Note that this left edge is not necessarily going to be the leftmost
     * character in the result, or the first
     * character following the ellipses, but it will appear somewhere in the result.
     * In no case will it return a string of length greater than maxWidth.
     *
     * @param offset left edge of source string
     * @param maxWidth maximum length of result string
     **/
    public static String abbreviate( String s, int offset, int maxWidth )
    {
        if ( maxWidth < 4 )
            throw new IllegalArgumentException( "Minimum abbreviation width is 4" );
        if ( s.length() <= maxWidth )
            return s;
        if ( offset > s.length() )
            offset = s.length();
        if ( ( s.length() - offset ) < ( maxWidth - 3 ) )
            offset = s.length() - ( maxWidth - 3 );
        if ( offset <= 4 )
            return s.substring( 0, maxWidth - 3 ) + "...";
        if ( maxWidth < 7 )
            throw new IllegalArgumentException( "Minimum abbreviation width with offset is 7" );
        if ( ( offset + ( maxWidth - 3 ) ) < s.length() )
            return "..." + abbreviate( s.substring( offset ), maxWidth - 3 );
        return "..." + s.substring( s.length() - ( maxWidth - 3 ) );
    }

    // Difference
    //--------------------------------------------------------------------------

    /**
     * Compare two strings, and return the portion where they differ.
     * (More precisely, return the remainder of the second string,
     * starting from where it's different from the first.)
     * <p>
     * E.g. strdiff("i am a machine", "i am a robot") -> "robot"
     *
     * @return the portion of s2 where it differs from s1; returns the empty string ("") if they are equal
     **/
    public static String difference( String s1, String s2 )
    {
        int at = differenceAt( s1, s2 );
        if ( at == -1 )
            return "";
        return s2.substring( at );
    }

    /**
     * Compare two strings, and return the index at which the strings begin to differ.
     * <p>
     * E.g. strdiff("i am a machine", "i am a robot") -> 7
     * </p>
     *
     * @return the index where s2 and s1 begin to differ; -1 if they are equal
     **/
    public static int differenceAt( String s1, String s2 )
    {
        int i;
        for ( i = 0; i < s1.length() && i < s2.length(); ++i )
        {
            if ( s1.charAt( i ) != s2.charAt( i ) )
            {
                break;
            }
        }
        if ( i < s2.length() || i < s1.length() )
        {
            return i;
        }
        return -1;
    }

    public static String interpolate( String text, Map namespace )
    {
        Iterator keys = namespace.keySet().iterator();

        while ( keys.hasNext() )
        {
            String key = keys.next().toString();

            Object obj = namespace.get( key );

            if ( obj == null )
            {
                throw new NullPointerException( "The value of " + key + "key is null." );
            }

            String value = obj.toString();

            text = StringUtils.replace( text, "${" + key + "}", value );

            if ( key.indexOf( " " ) == -1 )
            {
                text = StringUtils.replace( text, "$" + key, value );
            }
        }
        return text;
    }

    public static String removeAndHump( String data, String replaceThis )
    {
        String temp = null;

        StringBuffer out = new StringBuffer();

        temp = data;

        StringTokenizer st = new StringTokenizer( temp, replaceThis );

        while ( st.hasMoreTokens() )
        {
            String element = (String) st.nextElement();

            out.append( capitalizeFirstLetter( element ) );
        }

        return out.toString();
    }

    public static String capitalizeFirstLetter( String data )
    {
        char firstLetter = Character.toTitleCase( data.substring( 0, 1 ).charAt( 0 ) );

        String restLetters = data.substring( 1 );

        return firstLetter + restLetters;
    }

    public static String lowercaseFirstLetter( String data )
    {
        char firstLetter = Character.toLowerCase( data.substring( 0, 1 ).charAt( 0 ) );

        String restLetters = data.substring( 1 );

        return firstLetter + restLetters;
    }

    public static String addAndDeHump( String view )
    {
        StringBuffer sb = new StringBuffer();

        for ( int i = 0; i < view.length(); i++ )
        {
            if ( i != 0 && Character.isUpperCase( view.charAt( i ) ) )
            {
                sb.append( '-' );
            }

            sb.append( view.charAt( i ) );
        }

        return sb.toString().trim().toLowerCase();
    }
}
