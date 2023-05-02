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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * <p>
 * Generic implementation of version comparison.
 * </p>
 * <p>
 * Features:
 * <ul>
 * <li>mixing of '<code>-</code>' (hyphen) and '<code>.</code>' (dot) separators,</li>
 * <li>transition between characters and digits also constitutes a separator:
 *     <code>1.0alpha1 =&gt; [1, [alpha, 1]]</code></li>
 * <li>unlimited number of version components,</li>
 * <li>version components in the text can be digits or strings,</li>
 * <li>strings are checked for well-known qualifiers and the qualifier ordering is used for version ordering.
 *     Well-known qualifiers (case insensitive) are:<ul>
 *     <li><code>alpha</code> or <code>a</code></li>
 *     <li><code>beta</code> or <code>b</code></li>
 *     <li><code>milestone</code> or <code>m</code></li>
 *     <li><code>rc</code> or <code>cr</code></li>
 *     <li><code>snapshot</code></li>
 *     <li><code>(the empty string)</code> or <code>ga</code> or <code>final</code></li>
 *     <li><code>sp</code></li>
 *     </ul>
 *     Unknown qualifiers are considered after known qualifiers, with lexical order (always case insensitive),
 *   </li>
 * <li>a hyphen usually precedes a qualifier, and is always less important than digits/number, for example
 *   {@code 1.0.RC2 < 1.0-RC3 < 1.0.1}; but prefer {@code 1.0.0-RC1} over {@code 1.0.0.RC1}, and more
 *   generally: {@code 1.0.X2 < 1.0-X3 < 1.0.1} for any string {@code X}; but prefer {@code 1.0.0-X1}
 *   over {@code 1.0.0.X1}.</li>
 * </ul>
 *
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 * @author <a href="mailto:hboutemy@apache.org">Hervé Boutemy</a>
 * @see <a href="https://maven.apache.org/pom.html#version-order-specification">"Versioning" in the POM reference</a>
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
        int COMBINATION_ITEM = 5;

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
                case COMBINATION_ITEM:
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
                case COMBINATION_ITEM:
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
                case COMBINATION_ITEM:
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
        private static final List<String> QUALIFIERS =
                Arrays.asList("alpha", "beta", "milestone", "rc", "snapshot", "", "sp");
        private static final List<String> RELEASE_QUALIFIERS = Arrays.asList("ga", "final", "release");

        private static final Properties ALIASES = new Properties();

        static {
            ALIASES.put("cr", "rc");
        }

        /**
         * A comparable value for the empty-string qualifier. This one is used to determine if a given qualifier makes
         * the version older than one without a qualifier, or more recent.
         */
        private static final String RELEASE_VERSION_INDEX = String.valueOf(QUALIFIERS.indexOf(""));

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
            this.value = ALIASES.getProperty(value, value);
        }

        @Override
        public int getType() {
            return STRING_ITEM;
        }

        @Override
        public boolean isNull() {
            return value == null || value.isEmpty();
        }

        /**
         * Returns a comparable value for a qualifier.
         * <p>
         * This method takes into account the ordering of known qualifiers then unknown qualifiers with lexical
         * ordering.
         * <p>
         * just returning an Integer with the index here is faster, but requires a lot of if/then/else to check for -1
         * or QUALIFIERS.size and then resort to lexical ordering. Most comparisons are decided by the first character,
         * so this is still fast. If more characters are needed then it requires a lexical sort anyway.
         *
         * @param qualifier
         * @return an equivalent value that can be used with lexical comparison
         */
        public static String comparableQualifier(String qualifier) {
            if (RELEASE_QUALIFIERS.contains(qualifier)) {
                return String.valueOf(QUALIFIERS.indexOf(""));
            }

            int i = QUALIFIERS.indexOf(qualifier);

            return i == -1 ? (QUALIFIERS.size() + "-" + qualifier) : String.valueOf(i);
        }

        @Override
        public int compareTo(Item item) {
            if (item == null) {
                // 1-rc < 1, 1-ga > 1
                return comparableQualifier(value).compareTo(RELEASE_VERSION_INDEX);
            }
            switch (item.getType()) {
                case INT_ITEM:
                case LONG_ITEM:
                case BIGINTEGER_ITEM:
                    return -1; // 1.any < 1.1 ?

                case STRING_ITEM:
                    return comparableQualifier(value).compareTo(comparableQualifier(((StringItem) item).value));

                case COMBINATION_ITEM:
                    int result = this.compareTo(((CombinationItem) item).getStringValue());
                    if (result == 0) {
                        return -1;
                    }
                    return result;

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
     * Represents a combination in the version item list.
     * It is usually a combination of a string and a number, with the string first and the number second
     */
    private static class CombinationItem implements Item {

        StringItem stringValue;

        Item digitValue;

        CombinationItem(String v) {
            int index = 0;
            for (int i = 0; i < v.length(); i++) {
                char c = v.charAt(i);
                if (Character.isDigit(c)) {
                    index = i;
                    break;
                }
            }

            stringValue = new StringItem(v.substring(0, index), true);
            digitValue = parseItem(true, v.substring(index));
        }

        @Override
        public int compareTo(Item item) {
            if (item == null) {
                // 1-rc1 < 1, 1-ga1 > 1
                return stringValue.compareTo(item);
            }
            int result = 0;
            switch (item.getType()) {
                case INT_ITEM:
                case LONG_ITEM:
                case BIGINTEGER_ITEM:
                    return -1;

                case STRING_ITEM:
                    result = stringValue.compareTo(item);
                    if (result == 0) {
                        // X1 > X
                        return 1;
                    }
                    return result;

                case LIST_ITEM:
                    return -1;

                case COMBINATION_ITEM:
                    result = stringValue.compareTo(((CombinationItem) item).getStringValue());
                    if (result == 0) {
                        return digitValue.compareTo(((CombinationItem) item).getDigitValue());
                    }
                    return result;
                default:
                    return 0;
            }
        }

        public StringItem getStringValue() {
            return stringValue;
        }

        public Item getDigitValue() {
            return digitValue;
        }

        @Override
        public int getType() {
            return COMBINATION_ITEM;
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CombinationItem that = (CombinationItem) o;
            return Objects.equals(stringValue, that.stringValue) && Objects.equals(digitValue, that.digitValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stringValue, digitValue);
        }

        @Override
        public String toString() {
            return stringValue.toString() + digitValue.toString();
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
                    if (i == size() - 1 || get(i + 1).getType() == STRING_ITEM) {
                        remove(i);
                    } else if (get(i + 1).getType() == LIST_ITEM) {
                        Item item = ((ListItem) get(i + 1)).get(0);
                        if (item.getType() == COMBINATION_ITEM || item.getType() == STRING_ITEM) {
                            remove(i);
                        }
                    }
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
                case COMBINATION_ITEM:
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

        boolean isCombination = false;

        int startIndex = 0;

        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);

            if (c == '.') {
                if (i == startIndex) {
                    list.add(IntItem.ZERO);
                } else {
                    list.add(parseItem(isCombination, isDigit, version.substring(startIndex, i)));
                }
                isCombination = false;
                startIndex = i + 1;
            } else if (c == '-') {
                if (i == startIndex) {
                    list.add(IntItem.ZERO);
                } else {
                    // X-1 is going to be treated as X1
                    if (!isDigit && i != version.length() - 1) {
                        char c1 = version.charAt(i + 1);
                        if (Character.isDigit(c1)) {
                            isCombination = true;
                            continue;
                        }
                    }
                    list.add(parseItem(isCombination, isDigit, version.substring(startIndex, i)));
                }
                startIndex = i + 1;

                if (!list.isEmpty()) {
                    list.add(list = new ListItem());
                    stack.push(list);
                }
                isCombination = false;
            } else if (Character.isDigit(c)) {
                if (!isDigit && i > startIndex) {
                    // X1
                    isCombination = true;

                    if (!list.isEmpty()) {
                        list.add(list = new ListItem());
                        stack.push(list);
                    }
                }

                isDigit = true;
            } else {
                if (isDigit && i > startIndex) {
                    list.add(parseItem(isCombination, true, version.substring(startIndex, i)));
                    startIndex = i;

                    list.add(list = new ListItem());
                    stack.push(list);
                    isCombination = false;
                }

                isDigit = false;
            }
        }

        if (version.length() > startIndex) {
            // 1.0.0.X1 < 1.0.0-X2
            // treat .X as -X for any string qualifier X
            if (!isDigit && !list.isEmpty()) {
                list.add(list = new ListItem());
                stack.push(list);
            }

            list.add(parseItem(isCombination, isDigit, version.substring(startIndex)));
        }

        while (!stack.isEmpty()) {
            list = (ListItem) stack.pop();
            list.normalize();
        }
    }

    private static Item parseItem(boolean isDigit, String buf) {
        return parseItem(false, isDigit, buf);
    }

    private static Item parseItem(boolean isCombination, boolean isDigit, String buf) {
        if (isCombination) {
            return new CombinationItem(buf.replace("-", ""));
        } else if (isDigit) {
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
     *             two adjacent will be compared
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
