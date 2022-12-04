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
package org.apache.maven.artifact.versioning;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * <p>
 * Generic implementation of version comparison.
 * </p>
 *
 * Features:
 * <ul>
 * <li>mixing of '<code>-</code>' (hyphen) and '<code>.</code>' (dot) separators,</li>
 * <li>transition between characters and digits also constitutes a separator:
 *     <code>1.0alpha1 =&gt; [1, 0, alpha, 1]</code></li>
 * <li>unlimited number of version components,</li>
 * <li>version components in the text can be digits or strings,</li>
 * <li>
 *   String qualifiers are ordered lexically (case insensitive), with the following exceptions:
 *   <ul>
 *     <li> 'snapshot' &lt; '' &lt; 'sp' </li>
 *   </ul>
 *   and alias -&gt; replacement (all case insensitive):
 *   <ul>
 *     <li> 'a' -&gt; 'alpha' </li>
 *     <li> 'b' -&gt; 'beta' </li>
 *     <li> 'm' -&gt; 'milestone' </li>
 *     <li> 'cr' -&gt; 'rc' </li>
 *     <li> 'final' -&gt; '' </li>
 *     <li> 'final' -&gt; '' </li>
 *     <li> 'final' -&gt; '' </li>
 *   </ul>
 * </li>
 * <li>
 *   Following semver rules is encouraged, and some qualifiers are discouraged (no matter the case):
 *   <ul>
 *     <li> The usage of 'CR' qualifier is discouraged. Use 'RC' instead. </li>
 *     <li> The usage of 'final', 'ga', and 'release' qualifiers is discouraged. Use no qualifier instead. </li>
 *     <li> The usage of 'SP' qualifier is discouraged. Increment the patch version instead. </li>
 *   </ul>
 *   For other qualifiers, natural ordering is used (case insensitive):
 *   <ul>
 *     <li> alpha = a &lt; beta = b &lt; milestone = m &lt; rc = cr &lt; snapshot &lt; '' = final = ga = release &lt; sp </li>
 *   </ul>
 * </li>
 * <li>a hyphen usually precedes a qualifier, and is always less important than digits/number, for example
 *   1.0.RC2 &lt; 1.0-RC3 &lt; 1.0.1 ; but prefer '1.0.0-RC1' over '1.0.0.RC1' </li>
 * </ul>
 *
 * @see <a href="https://maven.apache.org/pom.html#Version_Order_Specification">Version Order Specification</a>
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 * @author <a href="mailto:hboutemy@apache.org">Hervé Boutemy</a>
 */
public class ComparableVersion implements Comparable<ComparableVersion> {
    private static final int MAX_INTITEM_LENGTH = 9;

    private static final int MAX_LONGITEM_LENGTH = 18;

    private String value;

    private String canonical;

    private ListItem items;

    private interface Item {
        int INT_ITEM = 3;
        int LONG_ITEM = 4;
        int BIGINTEGER_ITEM = 0;
        int STRING_ITEM = 1;
        int LIST_ITEM = 2;

        int compareTo(Item item);

        int getType();

        boolean isNull();
    }

    /**
     * Represents a numeric item in the version item list that can be represented with an int.
     */
    private static class IntItem implements Item {
        private final int value;

        public static final IntItem ZERO = new IntItem();

        private IntItem() {
            this.value = 0;
        }

        IntItem(String str) {
            this.value = Integer.parseInt(str);
        }

        @Override
        public int getType() {
            return INT_ITEM;
        }

        @Override
        public boolean isNull() {
            return value == 0;
        }

        @Override
        public int compareTo(Item item) {
            if (item == null) {
                return (value == 0) ? 0 : 1; // 1.0 == 1, 1.1 > 1
            }

            switch (item.getType()) {
                case INT_ITEM:
                    int itemValue = ((IntItem) item).value;
                    return Integer.compare(value, itemValue);
                case LONG_ITEM:
                case BIGINTEGER_ITEM:
                    return -1;

                case STRING_ITEM:
                    return 1; // 1.1 > 1-sp

                case LIST_ITEM:
                    return 1; // 1.1 > 1-1

                default:
                    throw new IllegalStateException("invalid item: " + item.getClass());
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            IntItem intItem = (IntItem) o;

            return value == intItem.value;
        }

        @Override
        public int hashCode() {
            return value;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }

    /**
     * Represents a numeric item in the version item list that can be represented with a long.
     */
    private static class LongItem implements Item {
        private final long value;

        LongItem(String str) {
            this.value = Long.parseLong(str);
        }

        @Override
        public int getType() {
            return LONG_ITEM;
        }

        @Override
        public boolean isNull() {
            return value == 0;
        }

        @Override
        public int compareTo(Item item) {
            if (item == null) {
                return (value == 0) ? 0 : 1; // 1.0 == 1, 1.1 > 1
            }

            switch (item.getType()) {
                case INT_ITEM:
                    return 1;
                case LONG_ITEM:
                    long itemValue = ((LongItem) item).value;
                    return Long.compare(value, itemValue);
                case BIGINTEGER_ITEM:
                    return -1;

                case STRING_ITEM:
                    return 1; // 1.1 > 1-sp

                case LIST_ITEM:
                    return 1; // 1.1 > 1-1

                default:
                    throw new IllegalStateException("invalid item: " + item.getClass());
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            LongItem longItem = (LongItem) o;

            return value == longItem.value;
        }

        @Override
        public int hashCode() {
            return (int) (value ^ (value >>> 32));
        }

        @Override
        public String toString() {
            return Long.toString(value);
        }
    }

    /**
     * Represents a numeric item in the version item list.
     */
    private static class BigIntegerItem implements Item {
        private final BigInteger value;

        BigIntegerItem(String str) {
            this.value = new BigInteger(str);
        }

        @Override
        public int getType() {
            return BIGINTEGER_ITEM;
        }

        @Override
        public boolean isNull() {
            return BigInteger.ZERO.equals(value);
        }

        @Override
        public int compareTo(Item item) {
            if (item == null) {
                return BigInteger.ZERO.equals(value) ? 0 : 1; // 1.0 == 1, 1.1 > 1
            }

            switch (item.getType()) {
                case INT_ITEM:
                case LONG_ITEM:
                    return 1;

                case BIGINTEGER_ITEM:
                    return value.compareTo(((BigIntegerItem) item).value);

                case STRING_ITEM:
                    return 1; // 1.1 > 1-sp

                case LIST_ITEM:
                    return 1; // 1.1 > 1-1

                default:
                    throw new IllegalStateException("invalid item: " + item.getClass());
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            BigIntegerItem that = (BigIntegerItem) o;

            return value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        public String toString() {
            return value.toString();
        }
    }

    /**
     * Represents a string in the version item list, usually a qualifier.
     */
    private static class StringItem implements Item {
        private static final List<String> QUALIFIERS = Arrays.asList("snapshot", "", "sp");

        private static final Map<String, String> ALIASES = new HashMap<>(4);

        static {
            ALIASES.put("cr", "rc");
            ALIASES.put("final", "");
            ALIASES.put("ga", "");
            ALIASES.put("release", "");
        }

        /**
         * An index value for the empty-string qualifier. This one is used to determine if a given qualifier makes
         * the version older than one without a qualifier, or more recent.
         */
        private static final int RELEASE_VERSION_INDEX = QUALIFIERS.indexOf("");

        private final String value;

        StringItem(String value, boolean followedByDigit) {
            if (followedByDigit && value.length() == 1) {
                // a1 = alpha-1, b1 = beta-1, m1 = milestone-1
                switch (value.charAt(0)) {
                    case 'a':
                        value = "alpha";
                        break;
                    case 'b':
                        value = "beta";
                        break;
                    case 'm':
                        value = "milestone";
                        break;
                    default:
                }
            }
            this.value = ALIASES.getOrDefault(value, value);
        }

        @Override
        public int getType() {
            return STRING_ITEM;
        }

        @Override
        public boolean isNull() {
            return QUALIFIERS.indexOf(value) == RELEASE_VERSION_INDEX;
        }

        /**
         * Returns a comparable value for a qualifier.
         *
         * This method takes into account the ordering of known qualifiers then unknown qualifiers with lexical
         * ordering.
         *
         * just returning an Integer with the index here is faster, but requires a lot of if/then/else to check for -1
         * or QUALIFIERS.size and then resort to lexical ordering. Most comparisons are decided by the first character,
         * so this is still fast. If more characters are needed then it requires a lexical sort anyway.
         *
         * @param qualifier
         * @return an equivalent value that can be used with lexical comparison
         * @deprecated Use {@link #compareQualifiers(String, String)} instead
         */
        @Deprecated
        public static String comparableQualifier(String qualifier) {
            int index = QUALIFIERS.indexOf(qualifier) + 1;

            return index == 0 ? ("0-" + qualifier) : String.valueOf(index);
        }

        /**
         * Compare the qualifiers of two artifact versions.
         *
         * @param qualifier1 qualifier of first artifact
         * @param qualifier2 qualifier of second artifact
         * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or
         * greater than the second
         */
        public static int compareQualifiers(String qualifier1, String qualifier2) {
            int i1 = QUALIFIERS.indexOf(qualifier1);
            int i2 = QUALIFIERS.indexOf(qualifier2);

            // if both pre-release, then use natural lexical ordering
            if (i1 == -1 && i2 == -1) {
                // alpha < beta < ea < milestone < preview < rc
                return qualifier1.compareTo(qualifier2);
            }

            // 'other qualifier' < 'snapshot' < '' < 'sp'
            return Integer.compare(i1, i2);
        }

        @Override
        public int compareTo(Item item) {
            if (item == null) {
                // 1-rc < 1, 1-ga > 1
                return Integer.compare(QUALIFIERS.indexOf(value), RELEASE_VERSION_INDEX);
            }
            switch (item.getType()) {
                case INT_ITEM:
                case LONG_ITEM:
                case BIGINTEGER_ITEM:
                    return -1; // 1.any < 1.1 ?

                case STRING_ITEM:
                    return compareQualifiers(value, ((StringItem) item).value);

                case LIST_ITEM:
                    return -1; // 1.any < 1-1

                default:
                    throw new IllegalStateException("invalid item: " + item.getClass());
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            StringItem that = (StringItem) o;

            return value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        public String toString() {
            return value;
        }
    }

    /**
     * Represents a version list item. This class is used both for the global item list and for sub-lists (which start
     * with '-(number)' in the version specification).
     */
    private static class ListItem extends ArrayList<Item> implements Item {
        @Override
        public int getType() {
            return LIST_ITEM;
        }

        @Override
        public boolean isNull() {
            return (size() == 0);
        }

        void normalize() {
            for (int i = size() - 1; i >= 0; i--) {
                Item lastItem = get(i);

                if (lastItem.isNull()) {
                    // remove null trailing items: 0, "", empty list
                    remove(i);
                } else if (!(lastItem instanceof ListItem)) {
                    break;
                }
            }
        }

        @Override
        public int compareTo(Item item) {
            if (item == null) {
                if (size() == 0) {
                    return 0; // 1-0 = 1- (normalize) = 1
                }
                // Compare the entire list of items with null - not just the first one, MNG-6964
                for (Item i : this) {
                    int result = i.compareTo(null);
                    if (result != 0) {
                        return result;
                    }
                }
                return 0;
            }
            switch (item.getType()) {
                case INT_ITEM:
                case LONG_ITEM:
                case BIGINTEGER_ITEM:
                    return -1; // 1-1 < 1.0.x

                case STRING_ITEM:
                    return 1; // 1-1 > 1-sp

                case LIST_ITEM:
                    Iterator<Item> left = iterator();
                    Iterator<Item> right = ((ListItem) item).iterator();

                    while (left.hasNext() || right.hasNext()) {
                        Item l = left.hasNext() ? left.next() : null;
                        Item r = right.hasNext() ? right.next() : null;

                        // if this is shorter, then invert the compare and mul with -1
                        int result = l == null ? (r == null ? 0 : -1 * r.compareTo(l)) : l.compareTo(r);

                        if (result != 0) {
                            return result;
                        }
                    }

                    return 0;

                default:
                    throw new IllegalStateException("invalid item: " + item.getClass());
            }
        }

        @Override
        public String toString() {
            StringBuilder buffer = new StringBuilder();
            for (Item item : this) {
                if (buffer.length() > 0) {
                    buffer.append((item instanceof ListItem) ? '-' : '.');
                }
                buffer.append(item);
            }
            return buffer.toString();
        }

        /**
         * Return the contents in the same format that is used when you call toString() on a List.
         */
        private String toListString() {
            StringBuilder buffer = new StringBuilder();
            buffer.append("[");
            for (Item item : this) {
                if (buffer.length() > 1) {
                    buffer.append(", ");
                }
                if (item instanceof ListItem) {
                    buffer.append(((ListItem) item).toListString());
                } else {
                    buffer.append(item);
                }
            }
            buffer.append("]");
            return buffer.toString();
        }
    }

    public ComparableVersion(String version) {
        parseVersion(version);
    }

    @SuppressWarnings("checkstyle:innerassignment")
    public final void parseVersion(String version) {
        this.value = version;

        items = new ListItem();

        version = version.toLowerCase(Locale.ENGLISH);

        ListItem list = items;

        Deque<Item> stack = new ArrayDeque<>();
        stack.push(list);

        boolean isDigit = false;

        int startIndex = 0;

        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);

            if (c == '.' || c=='-' || c=='+') {
                if (i == startIndex) {
                    list.add(IntItem.ZERO);
                } else {
                    list.add(parseItem(isDigit, version.substring(startIndex, i)));
                }
                startIndex = i + 1;

                if (c != '.' || !isDigit) {
                    list.add(list = new ListItem());
                    stack.push(list);
                }
            } else if (Character.isDigit(c)) {
                if (!isDigit && i > startIndex) {
                    // 1.0.0.RC1 < 1.0.0-RC2
                    // treat .RC as -RC
                    if (!list.isEmpty()) {
                        list.add(list = new ListItem());
                        stack.push(list);
                    }

                    list.add(new StringItem(version.substring(startIndex, i), true));
                    startIndex = i;

                    list.add(list = new ListItem());
                    stack.push(list);
                }

                isDigit = true;
            } else {
                if (isDigit && i > startIndex) {
                    list.add(parseItem(true, version.substring(startIndex, i)));
                    startIndex = i;

                    list.add(list = new ListItem());
                    stack.push(list);
                }

                isDigit = false;
            }
        }

        if (version.length() > startIndex) {
            // 1.0.0.RC1 < 1.0.0-RC2
            // treat .RC as -RC
            if (!isDigit && !list.isEmpty()) {
                list.add(list = new ListItem());
                stack.push(list);
            }

            list.add(parseItem(isDigit, version.substring(startIndex)));
        }

        while (!stack.isEmpty()) {
            list = (ListItem) stack.pop();
            list.normalize();
        }
    }

    private static Item parseItem(boolean isDigit, String buf) {
        if (isDigit) {
            buf = stripLeadingZeroes(buf);
            if (buf.length() <= MAX_INTITEM_LENGTH) {
                // lower than 2^31
                return new IntItem(buf);
            } else if (buf.length() <= MAX_LONGITEM_LENGTH) {
                // lower than 2^63
                return new LongItem(buf);
            }
            return new BigIntegerItem(buf);
        }
        return new StringItem(buf, false);
    }

    private static String stripLeadingZeroes(String buf) {
        if (buf == null || buf.isEmpty()) {
            return "0";
        }
        for (int i = 0; i < buf.length(); ++i) {
            char c = buf.charAt(i);
            if (c != '0') {
                return buf.substring(i);
            }
        }
        return buf;
    }

    @Override
    public int compareTo(ComparableVersion o) {
        return items.compareTo(o.items);
    }

    @Override
    public String toString() {
        return value;
    }

    public String getCanonical() {
        if (canonical == null) {
            canonical = items.toString();
        }
        return canonical;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ComparableVersion) && items.equals(((ComparableVersion) o).items);
    }

    @Override
    public int hashCode() {
        return items.hashCode();
    }

    // CHECKSTYLE_OFF: LineLength
    /**
     * Main to test version parsing and comparison.
     * <p>
     * To check how "1.2.7" compares to "1.2-SNAPSHOT", for example, you can issue
     * <pre>java -jar ${maven.repo.local}/org/apache/maven/maven-artifact/${maven.version}/maven-artifact-${maven.version}.jar "1.2.7" "1.2-SNAPSHOT"</pre>
     * command to command line. Result of given command will be something like this:
     * <pre>
     * Display parameters as parsed by Maven (in canonical form) and comparison result:
     * 1. 1.2.7 == 1.2.7
     *    1.2.7 &gt; 1.2-SNAPSHOT
     * 2. 1.2-SNAPSHOT == 1.2-snapshot
     * </pre>
     *
     * @param args the version strings to parse and compare. You can pass arbitrary number of version strings and always
     * two adjacent will be compared
     */
    // CHECKSTYLE_ON: LineLength
    public static void main(String... args) {
        System.out.println("Display parameters as parsed by Maven (in canonical form and as a list of tokens) and"
                + " comparison result:");
        if (args.length == 0) {
            return;
        }

        ComparableVersion prev = null;
        int i = 1;
        for (String version : args) {
            ComparableVersion c = new ComparableVersion(version);

            if (prev != null) {
                int compare = prev.compareTo(c);
                System.out.println("   " + prev.toString() + ' ' + ((compare == 0) ? "==" : ((compare < 0) ? "<" : ">"))
                        + ' ' + version);
            }

            System.out.println(
                    (i++) + ". " + version + " -> " + c.getCanonical() + "; tokens: " + c.items.toListString());

            prev = c;
        }
    }
}
