package org.apache.maven.it.cli;

/*
 * The MIT License
 *
 * Copyright (c) 2004, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/* ====================================================================
 * Copyright 2003-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

import java.util.StringTokenizer;

/**
 * The java.util.StringTokenizer is horribly broken.
 * Given the string  1,,,3,,4      (, delim)
 * It will return 1,3,4
 * Which is clearly wrong - 1,EMPTY,EMPTY,3,EMPTY,4 is what it should return
 */
public final class EnhancedStringTokenizer
{
    private StringTokenizer cst = null;
    String cdelim;
    final boolean cdelimSingleChar;
    final char cdelimChar;
    boolean creturnDelims;
    String lastToken = null;
    boolean delimLast = true;

    public EnhancedStringTokenizer(String str)
    {
        this(str, " \t\n\r\f", false);
    }

    public EnhancedStringTokenizer(String str, String delim)
    {
        this(str, delim, false);
    }

    public EnhancedStringTokenizer(String str, String delim, boolean returnDelims)
    {
        cst = new StringTokenizer(str, delim, true);
        cdelim = delim;
        creturnDelims = returnDelims;
        cdelimSingleChar = (delim.length() == 1);
        cdelimChar = delim.charAt(0);
    }

    public boolean hasMoreTokens()
    {
        return cst.hasMoreTokens();
    }

    private String internalNextToken()
    {
        if (lastToken != null)
        {
            String last = lastToken;
            lastToken = null;
            return last;
        }

        String token = cst.nextToken();
        if (isDelim(token))
        {
            if (delimLast)
            {
                lastToken = token;
                return "";
            }
            else
            {
                delimLast = true;
                return token;
            }
        }
        else
        {
            delimLast = false;
            return token;
        }
    }

    public String nextToken()
    {
        String token = internalNextToken();
        if (creturnDelims)
            return token;
        if (isDelim(token))
            return hasMoreTokens() ? internalNextToken() : "";
        else
            return token;
    }

    private boolean isDelim(String str)
    {
        if (str.length() == 1)
        {
            char ch = str.charAt(0);
            if (cdelimSingleChar)
            {
                if (cdelimChar == ch)
                {
                    return true;
                }
            }
            else
            {
                if (cdelim.indexOf(ch) >= 0)
                {
                    return true;
                }
            }
        }
        return false;

    }
}
