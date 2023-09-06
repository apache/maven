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
 * This class represents the data type NCName use for XML non-colonized names.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class NCName {

    public static boolean isValid(String stValue) {
        for (int scan = 0; scan < stValue.length(); scan++) {
            char c = stValue.charAt(scan);
            boolean bValid = c != ':' && (scan == 0 ? is11NameStartChar(c, true) : is11NameChar(c, true));
            if (!bValid) {
                return false;
            }
        }
        return true;
    }

    public static boolean is11NameStartChar(int c, boolean ncname) {
        if (c <= 0x7A) { // 'z' or earlier
            if (c >= 0x61) { // 'a' - 'z' are ok
                return true;
            }
            if (c <= 0x5A) {
                if (c >= 0x41) { // 'A' - 'Z' ok too
                    return true;
                }
                // As are 0-9, '.' and '-'
                if ((c >= 0x30 && c <= 0x39) || (c == '.') || (c == '-')) {
                    return true;
                }
                // And finally, colon, in non-ns-aware mode
                if (c == ':' && !ncname) { // ':' == 0x3A
                    return true;
                }
            } else if (c == 0x5F) { // '_' is ok too
                return true;
            }
        }

        // Others are checked block-by-block:
        if (c <= 0x2FEF) {
            if (c < 0x300) {
                if (c < 0x00C0) { // 8-bit ctrl chars
                    return false;
                }
                // most of the rest are fine...
                return (c != 0xD7 && c != 0xF7);
            }
            if (c >= 0x2C00) {
                // 0x2C00 - 0x2FEF are ok
                return true;
            }
            if (c < 0x370 || c > 0x218F) {
                // 0x300 - 0x36F, 0x2190 - 0x2BFF invalid
                return false;
            }
            if (c < 0x2000) {
                // 0x370 - 0x37D, 0x37F - 0x1FFF are ok
                return (c != 0x37E);
            }
            if (c >= 0x2070) {
                // 0x2070 - 0x218F are ok; 0x218F+ was covered above
                return true;
            }
            // And finally, 0x200C - 0x200D
            return (c == 0x200C || c == 0x200D);
        }

        // 0x3000 and above:
        if (c >= 0x3001) {
            /* Hmmh, let's allow high surrogates here, without checking
             * that they are properly followed... crude basic support,
             * I know, but allows valid combinations, just doesn't catch
             * invalid ones
             */
            if (c <= 0xDBFF) { // 0x3001 - 0xD7FF (chars),
                // 0xD800 - 0xDBFF (high surrogate) are ok (unlike DC00-DFFF)
                return true;
            }
            if (c >= 0xF900 && c <= 0xFFFD) {
                /* Check above removes low surrogate (since one can not
                 * START an identifier), and byte-order markers..
                 */
                return (c <= 0xFDCF || c >= 0xFDF0);
            }
        }

        return false;
    }

    public static boolean is11NameChar(int c, boolean ncname) {
        if (c <= 0x7A) { // 'z' or earlier
            if (c >= 0x61) { // 'a' - 'z' are ok
                return true;
            }
            if (c <= 0x5A) {
                if (c >= 0x41) { // 'A' - 'Z' ok too
                    return true;
                }
                // As are 0-9, '.' and '-'
                if ((c >= 0x30 && c <= 0x39) || (c == '.') || (c == '-')) {
                    return true;
                }
                // And finally, colon, in non-ns-aware mode
                if (c == ':' && !ncname) { // ':' == 0x3A
                    return true;
                }
            } else if (c == 0x5F) { // '_' is ok too
                return true;
            }
        }

        // Others are checked block-by-block:
        if (c <= 0x2FEF) {
            if (c < 0x2000) { // only 8-bit ctrl chars and 0x37E to filter out
                return (c >= 0x00C0 && c != 0x37E) || (c == 0xB7);
            }
            if (c >= 0x2C00) {
                // 0x100 - 0x1FFF, 0x2C00 - 0x2FEF are ok
                return true;
            }
            if (c < 0x200C || c > 0x218F) {
                // 0x2000 - 0x200B, 0x2190 - 0x2BFF invalid
                return false;
            }
            if (c >= 0x2070) {
                // 0x2070 - 0x218F are ok
                return true;
            }
            // And finally, 0x200C - 0x200D, 0x203F - 0x2040 are ok
            return (c == 0x200C || c == 0x200D || c == 0x203F || c == 0x2040);
        }

        // 0x3000 and above:
        if (c >= 0x3001) {
            /* Hmmh, let's allow surrogate heres, without checking that
             * they have proper ordering. For non-first name chars, both are
             * ok, for valid names. Crude basic support,
             * I know, but allows valid combinations, just doesn't catch
             * invalid ones
             */
            if (c <= 0xDFFF) { // 0x3001 - 0xD7FF (chars),
                // 0xD800 - 0xDFFF (high, low surrogate) are ok:
                return true;
            }
            if (c >= 0xF900 && c <= 0xFFFD) {
                /* Check above removes other invalid chars (below valid
                 * range), and byte-order markers (0xFFFE, 0xFFFF).
                 */
                return (c <= 0xFDCF || c >= 0xFDF0);
            }
        }

        return false;
    }
}
