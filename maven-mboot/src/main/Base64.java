/*
 * ====================================================================
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 */

import java.io.ByteArrayOutputStream;

// import org.apache.commons.logging.Log;
// import org.apache.commons.logging.LogFactory;

/**
 * Encode/Decode Base-64.
 * 
 * @author John Casey
 */
public final class Base64
{

    // private static final Log LOG = LogFactory.getLog( Base64.class );

    private static final String CRLF = System.getProperty( "line.separator" );

    private static final int LINE_END = 64;

    public static String encode( byte[] data )
    {
        return Base64.encode( data, true );
    }

    public static String encode( byte[] data, boolean useLineDelimiter )
    {
        if ( data == null )
        {
            return null;
        }
        else if ( data.length == 0 )
        {
            return "";
        }

        int padding = 3 - ( data.length % 3 );

        // if ( LOG.isDebugEnabled() )
        // {
            // LOG.debug( "padding = " + padding + "characters." );
        // }

        StringBuffer buffer = new StringBuffer();

        for ( int i = 0; i < data.length; i += 3 )
        {
            // if ( LOG.isDebugEnabled() )
            // {
                // LOG.debug( "iteration base offset = " + i );
            // }

            int neutral = ( data[i] < 0 ? data[i] + 256 : data[i] );

            int block = ( neutral & 0xff );
            // if ( LOG.isDebugEnabled() )
            // {
                // LOG.debug( "after first byte, block = " + Integer.toBinaryString( block ) );
            // }

            boolean inLastSegment = false;

            block <<= 8;
            if ( i + 1 < data.length )
            {
                neutral = ( data[i + 1] < 0 ? data[i + 1] + 256 : data[i + 1] );
                block |= ( neutral & 0xff );
            }
            else
            {
                inLastSegment = true;
            }
            // if ( LOG.isDebugEnabled() )
            // {
                // LOG.debug( "after second byte, block = " + Integer.toBinaryString( block ) + "; inLastSegment = "
                                // + inLastSegment );
            // }

            block <<= 8;
            if ( i + 2 < data.length )
            {
                neutral = ( data[i + 2] < 0 ? data[i + 2] + 256 : data[i + 2] );
                block |= ( neutral & 0xff );
            }
            else
            {
                inLastSegment = true;
            }
            // if ( LOG.isDebugEnabled() )
            // {
                // LOG.debug( "after third byte, block = " + Integer.toBinaryString( block ) + "; inLastSegment = "
                                // + inLastSegment );
            // }

            char[] encoded = new char[4];
            int encIdx = 0;
            encoded[0] = toBase64Char( ( block >>> 18 ) & 0x3f );
            // if ( LOG.isDebugEnabled() )
            // {
                // LOG.debug( "first character = " + encoded[0] );
            // }

            encoded[1] = toBase64Char( ( block >>> 12 ) & 0x3f );
            // if ( LOG.isDebugEnabled() )
            // {
                // LOG.debug( "second character = " + encoded[1] );
            // }

            if ( inLastSegment && padding > 1 )
            {
                encoded[2] = '=';
            }
            else
            {
                encoded[2] = toBase64Char( ( block >>> 6 ) & 0x3f );
            }
            // if ( LOG.isDebugEnabled() )
            // {
                // LOG.debug( "third character = " + encoded[2] );
            // }

            if ( inLastSegment && padding > 0 )
            {
                encoded[3] = '=';
            }
            else
            {
                encoded[3] = toBase64Char( block & 0x3f );
            }
            // if ( LOG.isDebugEnabled() )
            // {
                // LOG.debug( "fourth character = " + encoded[3] );
            // }

            buffer.append( encoded );
        }

        if ( useLineDelimiter )
        {
            return canonicalize( buffer.toString() );
        }
        else
        {
            return buffer.toString();
        }
    }

    public static byte[] decode( String src )
    {
        return Base64.decode( src, true );
    }

    public static byte[] decode( String src, boolean useLineDelimiter )
    {
        if ( src == null )
        {
            return null;
        }
        else if ( src.length() < 1 )
        {
            return new byte[0];
        }

        // if ( LOG.isDebugEnabled() )
        // {
            // LOG.debug( "pre-canonicalization = \n" + src );
        // }
        String data = src;

        if ( useLineDelimiter )
        {
            data = deCanonicalize( src );
        }
        // if ( LOG.isDebugEnabled() )
        // {
            // LOG.debug( "post-canonicalization = \n" + data );
        // }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        char[] input = data.toCharArray();

        int index = 0;
        for ( int i = 0; i < input.length; i += 4 )
        {
            // if ( LOG.isDebugEnabled() )
            // {
                // LOG.debug( "iteration base offset = " + i );
            // }

            int block = ( toBase64Int( input[i] ) & 0x3f );
            // if ( LOG.isDebugEnabled() )
            // {
                // LOG.debug( "block after first char [" + input[i] + "] = " + Integer.toBinaryString( block ) );
            // }

            block <<= 6;
            block |= ( toBase64Int( input[i + 1] ) & 0x3f );
            // if ( LOG.isDebugEnabled() )
            // {
                // LOG.debug( "block after second char [" + input[i + 1] + "] = " + Integer.toBinaryString( block ) );
            // }

            boolean inPadding = false;
            boolean twoCharPadding = false;
            block <<= 6;
            if ( input[i + 2] != '=' )
            {
                block |= ( toBase64Int( input[i + 2] ) & 0x3f );
            }
            else
            {
                twoCharPadding = true;
                inPadding = true;
            }

            // if ( LOG.isDebugEnabled() )
            // {
                // LOG.debug( "block after third char [" + input[i + 2] + "] = " + Integer.toBinaryString( block ) );
            // }

            block <<= 6;
            if ( input[i + 3] != '=' )
            {
                block |= ( toBase64Int( input[i + 3] ) & 0x3f );
            }
            else
            {
                inPadding = true;
            }

            // if ( LOG.isDebugEnabled() )
            // {
                // LOG.debug( "block after fourth char [" + input[i + 3] + "] = " + Integer.toBinaryString( block ) );
            // }

            baos.write( ( block >>> 16 ) & 0xff );
            // if ( LOG.isDebugEnabled() )
            // {
                // LOG.debug( "byte[" + ( index++ ) + "] = " + ( ( block >>> 16 ) & 0xff ) );
            // }

            if ( !inPadding || !twoCharPadding )
            {
                baos.write( ( block >>> 8 ) & 0xff );
                // if ( LOG.isDebugEnabled() )
                // {
                    // LOG.debug( "byte[" + ( index++ ) + "] = " + ( ( block >>> 8 ) & 0xff ) );
                // }
            }

            if ( !inPadding )
            {
                baos.write( block & 0xff );
                // if ( LOG.isDebugEnabled() )
                // {
                    // LOG.debug( "byte[" + ( index++ ) + "] = " + ( block & 0xff ) );
                // }
            }
        }

        byte[] result = baos.toByteArray();
        // if ( LOG.isDebugEnabled() )
        // {
            // LOG.debug( "byte array is " + result.length + " bytes long." );
        // }

        return result;
    }

    private static char toBase64Char( int input )
    {
        if ( input > -1 && input < 26 )
        {
            return ( char ) ( 'A' + input );
        }
        else if ( input > 25 && input < 52 )
        {
            return ( char ) ( 'a' + input - 26 );
        }
        else if ( input > 51 && input < 62 )
        {
            return ( char ) ( '0' + input - 52 );
        }
        else if ( input == 62 )
        {
            return '+';
        }
        else if ( input == 63 )
        {
            return '/';
        }
        else
        {
            return '?';
        }
    }

    private static int toBase64Int( char input )
    {
        if ( input >= 'A' && input <= 'Z' )
        {
            return input - 'A';
        }
        else if ( input >= 'a' && input <= 'z' )
        {
            return input + 26 - 'a';
        }
        else if ( input >= '0' && input <= '9' )
        {
            return input + 52 - '0';
        }
        else if ( input == '+' )
        {
            return 62;
        }
        else if ( input == '/' )
        {
            return 63;
        }
        else
        {
            return 0;
        }
    }

    private static String deCanonicalize( String data )
    {
        if ( data == null )
        {
            return null;
        }

        StringBuffer buffer = new StringBuffer( data.length() );
        for ( int i = 0; i < data.length(); i++ )
        {
            char c = data.charAt( i );
            if ( c != '\r' && c != '\n' )
            {
                buffer.append( c );
            }
        }

        return buffer.toString();
    }

    private static String canonicalize( String data )
    {
        StringBuffer buffer = new StringBuffer( ( int ) ( data.length() * 1.1 ) );

        int col = 0;
        for ( int i = 0; i < data.length(); i++ )
        {
            if ( col == LINE_END )
            {
                buffer.append( CRLF );
                col = 0;
            }

            buffer.append( data.charAt( i ) );
            col++;
        }

        buffer.append( CRLF );

        return buffer.toString();
    }

}
