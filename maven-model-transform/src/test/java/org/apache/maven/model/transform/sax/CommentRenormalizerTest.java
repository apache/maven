package org.apache.maven.model.transform.sax;

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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.xml.sax.ext.LexicalHandler;

public class CommentRenormalizerTest
{
    private LexicalHandler lexicalHandler = mock( LexicalHandler.class );

    @ParameterizedTest
    @ValueSource( strings = { "\n", "\r\n", "\r" } )
    public void singleLine( String lineSeparator )
        throws Exception
    {
        CommentRenormalizer commentRenormalizer = new CommentRenormalizer( lexicalHandler, lineSeparator );

        char[] ch = "single line".toCharArray();

        commentRenormalizer.comment( ch, 0, ch.length );

        verify( lexicalHandler ).comment( ch, 0, ch.length );
    }

    @ParameterizedTest
    @ValueSource( strings = { "\n", "\r\n", "\r" } )
    public void multiLine( String lineSeparator )
        throws Exception
    {
        CommentRenormalizer commentRenormalizer = new CommentRenormalizer( lexicalHandler, lineSeparator );

        String text = "I%sam%sthe%sbest%s";

        char[] chIn = String.format( text, "\n", "\n", "\n", "\n" ).toCharArray();
        char[] chOut = String.format( text, lineSeparator, lineSeparator, lineSeparator, lineSeparator ).toCharArray();

        commentRenormalizer.comment( chIn, 0, chIn.length );

        verify( lexicalHandler ).comment( chOut, 0, chOut.length );
    }
}
