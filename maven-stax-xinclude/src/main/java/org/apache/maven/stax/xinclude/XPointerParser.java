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

import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;

import com.ctc.wstx.io.WstxInputData;

/**
 * This class parses a String to the XPointer Framework specification for shorthand and scheme based pointers.
 * For scheme based pointers each know pointer part
 * <p>
 * See the <a href="http://www.w3.org/TR/xptr-framework/">XPointer Framework Recommendation</a> for
 * more information on the XPointer Framework, ShortHand and Scheme based Pointers.
 * <p>
 * This class is based upon a class of the same name in Apache Woden.
 */
@SuppressWarnings({"checkstyle:MissingSwitchDefault", "checkstyle:AvoidNestedBlocks"})
final class XPointerParser {

    private static final String ELEMENT_SCHEME_NAME = "element"; // Supported schemes

    /**
     * Parses a String XPointer and stores the results into the given XPointer object.
     *
     * @throws InvalidXPointerException if the XPointer being parsed contains invalid syntax
     */
    public static void parseXPointer(String xpointerString, XPointer xpointer) throws InvalidXPointerException {

        final Tokens tokens = new Tokens(); // tokens

        // scan the XPointer expression
        int length = xpointerString.length();
        boolean success = Scanner.scanExpr(tokens, xpointerString, 0, length);

        if (!success) {
            throw new InvalidXPointerException("Invalid XPointer expression", xpointerString);
        }

        while (tokens.hasMore()) {
            int token = tokens.nextToken();

            switch (token) {
                case Tokens.XPTRTOKEN_SHORTHAND: {

                    // The shorthand name
                    token = tokens.nextToken();
                    String shortHandPointerName = tokens.getTokenString(token);

                    if (shortHandPointerName == null) {
                        throw new InvalidXPointerException("Invalid Shorthand XPointer", xpointerString);
                    }
                    if (isInvalidNCName(shortHandPointerName)) {
                        throw new InvalidXPointerException(
                                "Shorthand XPointer is not a valid NCName: " + shortHandPointerName, xpointerString);
                    }

                    xpointer.setShorthandPointer(shortHandPointerName);
                    break;
                }
                case Tokens.XPTRTOKEN_SCHEMENAME: {

                    // Retrieve the local name and prefix to form the scheme name
                    token = tokens.nextToken();
                    String prefix = tokens.getTokenString(token);
                    token = tokens.nextToken();
                    String localName = tokens.getTokenString(token);

                    String schemeName = prefix + localName;

                    // The next character should be an open parenthesis
                    int openParenCount = 0;
                    int closeParenCount = 0;

                    token = tokens.nextToken();
                    String openParen = tokens.getTokenString(token);
                    if (!Objects.equals(openParen, "XPTRTOKEN_OPEN_PAREN")) {

                        // can not have more than one ShortHand Pointer
                        if (token == Tokens.XPTRTOKEN_SHORTHAND) {
                            throw new InvalidXPointerException("Multiple Shorthand pointers", xpointerString);
                        } else {
                            throw new InvalidXPointerException("Invalid XPointer expression", xpointerString);
                        }
                    }
                    openParenCount++;

                    // followed by zero or more ( and  the schemeData
                    String schemeData = null;
                    while (tokens.hasMore()) {
                        token = tokens.nextToken();
                        schemeData = tokens.getTokenString(token);
                        if (!Objects.equals(schemeData, "XPTRTOKEN_OPEN_PAREN")) {
                            break;
                        }
                        openParenCount++;
                    }
                    token = tokens.nextToken();
                    schemeData = tokens.getTokenString(token);

                    // followed by the same number of )
                    if (tokens.hasMore()) {
                        token = tokens.nextToken();
                        String closeParen = tokens.getTokenString(token);
                        if (!Objects.equals(closeParen, "XPTRTOKEN_CLOSE_PAREN")) {
                            throw new InvalidXPointerException(
                                    "SchemeData not followed by close parenthesis", xpointerString);
                        }
                    } else {
                        throw new InvalidXPointerException(
                                "SchemeData not followed by close parenthesis", xpointerString);
                    }

                    closeParenCount++;

                    while (tokens.hasMore()) {
                        if (!Objects.equals(tokens.getTokenString(tokens.peekToken()), "XPTRTOKEN_OPEN_PAREN")) {
                            break;
                        }
                        closeParenCount++;
                    }

                    // check if the number of open parenthesis are equal to the number of close parenthesis
                    if (openParenCount != closeParenCount) {
                        throw new InvalidXPointerException(
                                "Unbalanced parenthesis in XPointer expression", xpointerString);
                    }

                    // Perform scheme specific parsing of the pointer part, make this more generic for any pointer part?
                    if (schemeName.equals(ELEMENT_SCHEME_NAME)) {
                        PointerPart elementSchemePointer = ElementPointerPart.parseFromString(schemeData);
                        xpointer.addPointerPart(elementSchemePointer);
                    } // Else an unknown scheme.
                    break;
                }
                default:
                    throw new InvalidXPointerException("Invalid XPointer expression", xpointerString);
            }
        }
    }

    public static boolean isInvalidNCName(String stValue) {
        return WstxInputData.findIllegalNameChar(stValue, true, true) >= 0;
    }

    /**
     * List of XPointer Framework tokens.
     */
    private static class Tokens {

        /**
         * XPointer Framework tokens
         * [1] Pointer     ::= Shorthand | SchemeBased
         * [2] Shorthand   ::= NCName
         * [3] SchemeBased ::= PointerPart (S? PointerPart)*
         * [4] PointerPart ::= SchemeName '(' SchemeData ')'
         * [5] SchemeName  ::= QName
         * [6] SchemeData  ::= EscapedData*
         * [7] EscapedData ::= NormalChar | '^(' | '^)' | '^^' | '(' SchemeData ')'
         * [8] NormalChar  ::= UnicodeChar - [()^]
         * [9] UnicodeChar ::= [#x0-#x10FFFF]
         */
        private static final int XPTRTOKEN_OPEN_PAREN = 0,
                XPTRTOKEN_CLOSE_PAREN = 1,
                XPTRTOKEN_SHORTHAND = 2,
                XPTRTOKEN_SCHEMENAME = 3,
                XPTRTOKEN_SCHEMEDATA = 4;

        // Token count
        private static final int INITIAL_TOKEN_COUNT = 1 << 8;

        private int[] fTokens = new int[INITIAL_TOKEN_COUNT];

        private int fTokenCount = 0;

        // Current token position
        private int fCurrentTokenIndex;

        private Hashtable<Integer, String> fTokenNames = new Hashtable<>();

        /**
         * Constructor
         */
        private Tokens() {
            fTokenNames.put(XPTRTOKEN_OPEN_PAREN, "XPTRTOKEN_OPEN_PAREN");
            fTokenNames.put(XPTRTOKEN_CLOSE_PAREN, "XPTRTOKEN_CLOSE_PAREN");
            fTokenNames.put(XPTRTOKEN_SHORTHAND, "XPTRTOKEN_SHORTHAND");
            fTokenNames.put(XPTRTOKEN_SCHEMENAME, "XPTRTOKEN_SCHEMENAME");
            fTokenNames.put(XPTRTOKEN_SCHEMEDATA, "XPTRTOKEN_SCHEMEDATA");
        }

        /**
         * Returns the token String
         *
         * @param token The index of the token
         * @return String The token string
         */
        private String getTokenString(int token) {
            return (String) fTokenNames.get(token);
        }

        /**
         * Add the specified string as a token
         *
         * @param token The token string
         */
        private void addToken(String token) {
            Integer tokenInt = fTokenNames.entrySet().stream()
                    .filter(e -> Objects.equals(e.getValue(), token))
                    .findFirst()
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (tokenInt == null) {
                tokenInt = fTokenNames.size();
                fTokenNames.put(tokenInt, token);
            }
            addToken(tokenInt);
        }

        /**
         * Add the specified int token
         *
         * @param token The int specifying the token
         */
        private void addToken(int token) {
            try {
                fTokens[fTokenCount] = token;
            } catch (ArrayIndexOutOfBoundsException ex) {
                int[] oldList = fTokens;
                fTokens = new int[fTokenCount << 1];
                System.arraycopy(oldList, 0, fTokens, 0, fTokenCount);
                fTokens[fTokenCount] = token;
            }
            fTokenCount++;
        }

        /**
         * Returns true if the {@link #nextToken()} method
         * returns a valid token.
         */
        private boolean hasMore() {
            return fCurrentTokenIndex < fTokenCount;
        }

        /**
         * Obtains the token at the current position, then advance
         * the current position by one.
         * <p>
         * throws If there's no such next token, this method throws
         * <tt>new XNIException("XPointerProcessingError");</tt>.
         */
        private int nextToken() {
            if (fCurrentTokenIndex == fTokenCount) {
                throw new IndexOutOfBoundsException("There are no more tokens to return.");
            }
            return fTokens[fCurrentTokenIndex++];
        }

        /**
         * Obtains the token at the current position, without advancing
         * the current position.
         * <p>
         * If there's no such next token, this method throws
         * <tt>new XNIException("XPointerProcessingError");</tt>.
         */
        private int peekToken() {
            if (fCurrentTokenIndex == fTokenCount) {
                throw new IndexOutOfBoundsException("There are no more tokens to return.");
            }
            return fTokens[fCurrentTokenIndex];
        }
    }

    /**
     * The XPointer expression scanner.  Scans the XPointer framework expression.
     */
    private static class Scanner {

        /**
         * 7-bit ASCII subset
         * <p>
         * 0   1   2   3   4   5   6   7   8   9   A   B   C   D   E   F
         * 0,  0,  0,  0,  0,  0,  0,  0,  0, HT, LF,  0,  0, CR,  0,  0,  // 0
         * 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  // 1
         * SP,  !,  ",  #,  $,  %,  &,  ',  (,  ),  *,  +,  ,,  -,  .,  /,  // 2
         * 0,  1,  2,  3,  4,  5,  6,  7,  8,  9,  :,  ;,  <,  =,  >,  ?,  // 3
         *
         * @, A,  B,  C,  D,  E,  F,  G,  H,  I,  J,  K,  L,  M,  N,  O,  // 4
         * P,  Q,  R,  S,  T,  U,  V,  W,  X,  Y,  Z,  [,  \,  ],  ^,  _,  // 5
         * `,  a,  b,  c,  d,  e,  f,  g,  h,  i,  j,  k,  l,  m,  n,  o,  // 6
         * p,  q,  r,  s,  t,  u,  v,  w,  x,  y,  z,  {,  |,  },  ~, DEL  // 7
         */
        private static final byte CHARTYPE_INVALID = 0, // invalid XML character
                CHARTYPE_OTHER = 1, // not special - one of "#%&;?\`{}~" or DEL
                CHARTYPE_WHITESPACE = 2, // one of "\t\n\r " (0x09, 0x0A, 0x0D, 0x20)
                CHARTYPE_CARRET = 3, // ^
                CHARTYPE_OPEN_PAREN = 4, // '(' (0x28)
                CHARTYPE_CLOSE_PAREN = 5, // ')' (0x29)
                CHARTYPE_MINUS = 6, // '-' (0x2D)
                CHARTYPE_PERIOD = 7, // '.' (0x2E)
                CHARTYPE_SLASH = 8, // '/' (0x2F)
                CHARTYPE_DIGIT = 9, // '0'-'9' (0x30 to 0x39)
                CHARTYPE_COLON = 10, // ':' (0x3A)
                CHARTYPE_EQUAL = 11, // '=' (0x3D)
                CHARTYPE_LETTER = 12, // 'A'-'Z' or 'a'-'z' (0x41 to 0x5A and 0x61 to 0x7A)
                CHARTYPE_UNDERSCORE = 13, // '_' (0x5F)
                CHARTYPE_NONASCII = 14; // Non-ASCII Unicode codepoint (>= 0x80)

        private static final byte[] ASCII_CHAR_MAP = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 1, 1, 1,
            1, 1, 1, 1, 4, 5, 1, 1, 1, 6, 7, 8, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 10, 1, 1, 11, 1, 1, 1, 12, 12, 12, 12, 12,
            12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 1, 1, 1, 3, 13, 1, 12,
            12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 1, 1, 1,
            1, 1
        };

        /**
         * Scans the XPointer Expression
         */
        private static boolean scanExpr(Tokens tokens, String data, int currentOffset, int endOffset)
                throws InvalidXPointerException {
            int ch;
            int openParen = 0;
            int closeParen = 0;
            int nameOffset, dataOffset;
            String name = null;
            String prefix;
            String schemeData;
            StringBuffer schemeDataBuff = new StringBuffer();

            while (true) {

                if (currentOffset == endOffset) {
                    break;
                }
                ch = data.charAt(currentOffset);

                //
                while (ch == ' ' || ch == 0x0A || ch == 0x09 || ch == 0x0D) {
                    if (++currentOffset == endOffset) {
                        break;
                    }
                    ch = data.charAt(currentOffset);
                }
                if (currentOffset == endOffset) {
                    break;
                }

                //
                // [1]    Pointer      ::=    Shorthand | SchemeBased
                // [2]    Shorthand    ::=    NCName
                // [3]    SchemeBased  ::=    PointerPart (S? PointerPart)*
                // [4]    PointerPart  ::=    SchemeName '(' SchemeData ')'
                // [5]    SchemeName   ::=    QName
                // [6]    SchemeData   ::=    EscapedData*
                // [7]    EscapedData  ::=    NormalChar | '^(' | '^)' | '^^' | '(' SchemeData ')'
                // [8]    NormalChar   ::=    UnicodeChar - [()^]
                // [9]    UnicodeChar  ::=    [#x0-#x10FFFF]
                // [?]    QName        ::=    (NCName ':')? NCName
                // [?]    NCName       ::=    (Letter | '_') (NCNameChar)*
                // [?]    NCNameChar   ::=    Letter | Digit | '.' | '-' | '_'  (ascii subset of 'NCNameChar')
                // [?]    Letter       ::=    [A-Za-z]                              (ascii subset of 'Letter')
                // [?]    Digit        ::=    [0-9]                                  (ascii subset of 'Digit')
                //
                byte chartype = (ch >= 0x80) ? CHARTYPE_NONASCII : ASCII_CHAR_MAP[ch];

                switch (chartype) {
                    case CHARTYPE_OPEN_PAREN: // '('
                        addToken(tokens, Tokens.XPTRTOKEN_OPEN_PAREN);
                        openParen++;
                        ++currentOffset;
                        break;

                    case CHARTYPE_CLOSE_PAREN: // ')'
                        addToken(tokens, Tokens.XPTRTOKEN_CLOSE_PAREN);
                        closeParen++;
                        ++currentOffset;
                        break;

                    case CHARTYPE_CARRET:
                    case CHARTYPE_COLON:
                    case CHARTYPE_DIGIT:
                    case CHARTYPE_EQUAL:
                    case CHARTYPE_LETTER:
                    case CHARTYPE_MINUS:
                    case CHARTYPE_NONASCII:
                    case CHARTYPE_OTHER:
                    case CHARTYPE_PERIOD:
                    case CHARTYPE_SLASH:
                    case CHARTYPE_UNDERSCORE:
                    case CHARTYPE_WHITESPACE:
                        // Scanning SchemeName | Shorthand
                        if (openParen == 0) {
                            nameOffset = currentOffset;
                            currentOffset = scanNCName(data, endOffset, currentOffset);

                            if (currentOffset == nameOffset) {
                                throw new InvalidXPointerException("InvalidShortHandPointer", data);
                            }

                            if (currentOffset < endOffset) {
                                ch = data.charAt(currentOffset);
                            } else {
                                ch = -1;
                            }

                            name = data.substring(nameOffset, currentOffset).intern();
                            prefix = "";

                            // The name is a QName => a SchemeName
                            if (ch == ':') {
                                if (++currentOffset == endOffset) {
                                    return false;
                                }

                                ch = data.charAt(currentOffset);
                                prefix = name;
                                nameOffset = currentOffset;
                                currentOffset = scanNCName(data, endOffset, currentOffset);

                                if (currentOffset == nameOffset) {
                                    return false;
                                }

                                if (currentOffset < endOffset) {
                                    ch = data.charAt(currentOffset);
                                } else {
                                    ch = -1;
                                }

                                name = data.substring(nameOffset, currentOffset).intern();
                            }

                            // REVISIT:
                            if (currentOffset != endOffset) {
                                addToken(tokens, Tokens.XPTRTOKEN_SCHEMENAME);
                                tokens.addToken(prefix);
                                tokens.addToken(name);
                            } else {
                                // NCName => Shorthand
                                addToken(tokens, Tokens.XPTRTOKEN_SHORTHAND);
                                tokens.addToken(name);
                            }

                            // reset open/close paren for the next pointer part
                            closeParen = 0;

                            break;

                        } else if (openParen > 0 && closeParen == 0 && name != null) {
                            // Scanning SchemeData
                            dataOffset = currentOffset;
                            currentOffset = scanData(data, schemeDataBuff, endOffset, currentOffset);

                            if (currentOffset == dataOffset) {
                                throw new InvalidXPointerException("InvalidSchemeDataInXPointer", data);
                            }

                            if (currentOffset < endOffset) {
                                ch = data.charAt(currentOffset);
                            } else {
                                ch = -1;
                            }

                            schemeData = schemeDataBuff.toString().intern();
                            addToken(tokens, Tokens.XPTRTOKEN_SCHEMEDATA);
                            tokens.addToken(schemeData);

                            // reset open/close paren for the next pointer part
                            openParen = 0;
                            schemeDataBuff.delete(0, schemeDataBuff.length());

                        } else {
                            // ex. schemeName()
                            // Should we throw an exception with a more suitable message instead??
                            return false;
                        }
                }
            } // end while
            return true;
        }

        /**
         * Scans a NCName.
         * From Namespaces in XML
         * [5] NCName ::= (Letter | '_') (NCNameChar)*
         * [6] NCNameChar ::= Letter | Digit | '.' | '-' | '_' | CombiningChar | Extender
         *
         * @param data          A String containing the XPointer expression
         * @param endOffset     The int XPointer expression length
         * @param currentOffset An int representing the current position of the XPointer expression pointer
         */
        private static int scanNCName(String data, int endOffset, int currentOffset) {
            int ch = data.charAt(currentOffset);
            if (!WstxInputData.isNameStartChar((char) ch, true, true)) {
                return currentOffset;
            }
            while (++currentOffset < endOffset) {
                ch = data.charAt(currentOffset);
                if (!WstxInputData.isNameChar((char) ch, true, true)) {
                    break;
                }
            }
            return currentOffset;
        }

        /**
         * Scans the SchemeData.
         * [6]    SchemeData   ::=    EscapedData*
         * [7]    EscapedData  ::=    NormalChar | '^(' | '^)' | '^^' | '(' SchemeData ')'
         * [8]    NormalChar   ::=    UnicodeChar - [()^]
         * [9]    UnicodeChar  ::=    [#x0-#x10FFFF]
         */
        private static int scanData(String data, StringBuffer schemeData, int endOffset, int currentOffset) {
            while (true) {

                if (currentOffset == endOffset) {
                    break;
                }

                int ch = data.charAt(currentOffset);
                byte chartype = (ch >= 0x80) ? CHARTYPE_NONASCII : ASCII_CHAR_MAP[ch];

                if (chartype == CHARTYPE_OPEN_PAREN) {
                    schemeData.append(ch);
                    // schemeData.append(Tokens.XPTRTOKEN_OPEN_PAREN);
                    currentOffset = scanData(data, schemeData, endOffset, ++currentOffset);
                    if (currentOffset == endOffset) {
                        return currentOffset;
                    }

                    ch = data.charAt(currentOffset);
                    chartype = (ch >= 0x80) ? CHARTYPE_NONASCII : ASCII_CHAR_MAP[ch];

                    if (chartype != CHARTYPE_CLOSE_PAREN) {
                        return endOffset;
                    }
                    schemeData.append((char) ch);
                    ++currentOffset; //

                } else if (chartype == CHARTYPE_CLOSE_PAREN) {
                    return currentOffset;

                } else if (chartype == CHARTYPE_CARRET) {
                    ch = data.charAt(++currentOffset);
                    chartype = (ch >= 0x80) ? CHARTYPE_NONASCII : ASCII_CHAR_MAP[ch];

                    if (chartype != CHARTYPE_CARRET
                            && chartype != CHARTYPE_OPEN_PAREN
                            && chartype != CHARTYPE_CLOSE_PAREN) {
                        break;
                    }
                    schemeData.append((char) ch);
                    ++currentOffset;

                } else {
                    schemeData.append((char) ch);
                    ++currentOffset; //
                }
            }

            return currentOffset;
        }

        //
        // Protected methods
        //

        /**
         * This method adds the specified token to the token list. By
         * default, this method allows all tokens. However, subclasses
         * of the XPathExprScanner can override this method in order
         * to disallow certain tokens from being used in the scanned
         * XPath expression. This is a convenient way of allowing only
         * a subset of XPath.
         */
        protected static void addToken(Tokens tokens, int token) {
            if (token == Tokens.XPTRTOKEN_OPEN_PAREN
                    || token == Tokens.XPTRTOKEN_CLOSE_PAREN
                    || token == Tokens.XPTRTOKEN_SCHEMENAME
                    || token == Tokens.XPTRTOKEN_SCHEMEDATA
                    || token == Tokens.XPTRTOKEN_SHORTHAND) {
                tokens.addToken(token);
                return;
            }
            throw new IllegalArgumentException("InvalidXPointerToken");
        }
    } // class Scanner
}
