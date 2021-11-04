package org.apache.maven.caching.checksum;

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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.caching.hash.HashChecksum;
import org.apache.maven.caching.domain.DigestItemType;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.containsAny;
import static org.apache.commons.lang3.StringUtils.equalsAny;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.startsWithAny;

/**
 * DigestUtils
 */
public class DigestUtils
{

    private static final ThreadLocal<UniversalDetector> ENCODING_DETECTOR = new ThreadLocal<UniversalDetector>()
    {
        @Override
        protected UniversalDetector initialValue()
        {
            return new UniversalDetector( null );
        }
    };


    public static DigestItemType pom( HashChecksum checksum, String effectivePom )
    {
        return item( "pom", effectivePom, checksum.update( effectivePom.getBytes( UTF_8 ) ) );
    }

    public static DigestItemType file( HashChecksum checksum, Path basedir, Path file ) throws IOException
    {
        byte[] content = Files.readAllBytes( file );
        DigestItemType item = item( "file", normalize( basedir, file ), checksum.update( content ) );
        try
        {
            populateContentDetails( file, content, item );
        }
        catch ( IOException ignore )
        {
            System.out.println( "hello" );
        }
        return item;
    }

    private static void populateContentDetails( Path file, byte[] content, DigestItemType item ) throws IOException
    {
        String contentType = Files.probeContentType( file );
        if ( contentType != null )
        {
            item.setContent( contentType );
        }
        final boolean binary = isBinary( contentType );
        item.setIsText( isText( contentType ) ? "yes" : binary ? "no" : "unknown" );
        if ( !binary )
        { // probing application/ files as well though might be binary
            UniversalDetector detector = ENCODING_DETECTOR.get();
            detector.reset();
            detector.handleData( content, 0, Math.min( content.length, 16 * 1024 ) );
            detector.dataEnd();
            String detectedCharset = detector.getDetectedCharset();
            Charset charset = UTF_8;
            if ( detectedCharset != null )
            {
                item.setCharset( detectedCharset );
                charset = Charset.forName( detectedCharset );
            }
            CharBuffer charBuffer = charset.decode( ByteBuffer.wrap( content ) );
            String lineSeparator = detectLineSeparator( charBuffer );
            item.setEol( StringUtils.defaultString( lineSeparator, "unknown" ) );
        }
    }

    // TODO add support for .gitattributes to statically configure file type before falling back to probe based content checks
    private static boolean isText( String contentType )
    {
        return startsWith( contentType, "text/" )
                || containsAny( contentType, "+json", "+xml" ) // common mime type suffixes
                || equalsAny( contentType, // some common text types
                "application/json",
                "application/rtf",
                "application/x-sh",
                "application/xml",
                "application/javascript",
                "application/sql"
        );
    }

    private static boolean isBinary( String contentType )
    {
        return startsWithAny( contentType, "image/", "audio/", "video/", "font/" )
                || containsAny( contentType, "+zip", "+gzip" )
                || equalsAny( contentType,
                "application/octet-stream",
                "application/java-archive",
                "application/x-bzip",
                "application/x-bzip2",
                "application/zip",
                "application/gzip",
                "application/x-tar",
                "application/msword",
                "application/vnd.ms-excel",
                "application/vnd.ms-powerpoint",
                "application/pdf"
        );
    }

    public static DigestItemType dependency( HashChecksum checksum, String key, String hash )
    {
        return item( "dependency", key, checksum.update( hash ) );
    }

    private static String normalize( Path basedirPath, Path file )
    {
        return FilenameUtils.separatorsToUnix( relativize( basedirPath, file ).toString() );
    }

    private static Path relativize( Path basedirPath, Path file )
    {
        try
        {
            return basedirPath.relativize( file );
        }
        catch ( Exception ignore )
        {
            return file;
        }
    }

    private static DigestItemType item( String type, String reference, String hash )
    {
        final DigestItemType item = new DigestItemType();
        item.setType( type );
        item.setValue( reference );
        item.setHash( hash );
        return item;
    }

    private DigestUtils()
    {
    }

    public static String detectLineSeparator( CharSequence text )
    {
        // first line break only
        int index = StringUtils.indexOfAny( text, "\n\r" );
        if ( index == -1 || index >= text.length() )
        {
            return null;
        }
        char ch = text.charAt( index );
        if ( ch == '\r' )
        {
            return index + 1 < text.length() && text.charAt( index + 1 ) == '\n' ? "CRLF" : "CR";
        }
        return ch == '\n' ? "LF" : null;
    }
}
