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
package org.apache.maven.artifact.resolver.filter;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Exclusion;

/**
 * Filter to exclude from a list of artifact patterns.
 */
public class ExclusionArtifactFilter implements ArtifactFilter {

    private final List<Exclusion> exclusions;
    private final List<Predicate<Artifact>> predicates;

    public ExclusionArtifactFilter(List<Exclusion> exclusions) {
        this.exclusions = exclusions;
        this.predicates =
                exclusions.stream().map(ExclusionArtifactFilter::toPredicate).collect(Collectors.toList());
    }

    @Override
    public boolean include(Artifact artifact) {
        return predicates.stream().noneMatch(p -> p.test(artifact));
    }

    private static Predicate<Artifact> toPredicate(Exclusion exclusion) {
        Pattern groupId = Pattern.compile(convertGlobToRegex(exclusion.getGroupId()));
        Pattern artifactId = Pattern.compile(convertGlobToRegex(exclusion.getArtifactId()));
        Predicate<Artifact> predGroupId = a -> groupId.matcher(a.getGroupId()).matches();
        Predicate<Artifact> predArtifactId =
                a -> artifactId.matcher(a.getArtifactId()).matches();
        return predGroupId.and(predArtifactId);
    }

    /**
     * Converts a standard POSIX Shell globbing pattern into a regular expression
     * pattern. The result can be used with the standard {@link java.util.regex} API to
     * recognize strings which match the glob pattern.
     * <p/>
     * See also, the POSIX Shell language:
     * http://pubs.opengroup.org/onlinepubs/009695399/utilities/xcu_chap02.html#tag_02_13_01
     *
     * @param pattern A glob pattern.
     * @return A regex pattern to recognize the given glob pattern.
     */
    private static String convertGlobToRegex(String pattern) {
        StringBuilder sb = new StringBuilder(pattern.length());
        int inGroup = 0;
        int inClass = 0;
        int firstIndexInClass = -1;
        char[] arr = pattern.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            switch (ch) {
                case '\\':
                    if (++i >= arr.length) {
                        sb.append('\\');
                    } else {
                        char next = arr[i];
                        switch (next) {
                            case ',':
                                // escape not needed
                                break;
                            case 'Q':
                            case 'E':
                                // extra escape needed
                                sb.append('\\');
                                sb.append('\\');
                                break;
                            default:
                                sb.append('\\');
                                break;
                        }
                        sb.append(next);
                    }
                    break;
                case '*':
                    sb.append(inClass == 0 ? ".*" : "*");
                    break;
                case '?':
                    sb.append(inClass == 0 ? "." : "?");
                    break;
                case '[':
                    inClass++;
                    firstIndexInClass = i + 1;
                    sb.append('[');
                    break;
                case ']':
                    inClass--;
                    sb.append(']');
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    if (inClass == 0 || (firstIndexInClass == i && ch == '^')) {
                        sb.append('\\');
                    }
                    sb.append(ch);
                    break;
                case '!':
                    sb.append(firstIndexInClass == i ? '^' : '!');
                    break;
                case '{':
                    inGroup++;
                    sb.append('(');
                    break;
                case '}':
                    inGroup--;
                    sb.append(')');
                    break;
                case ',':
                    sb.append(inGroup > 0 ? '|' : ',');
                    break;
                default:
                    sb.append(ch);
                    break;
            }
        }
        return sb.toString();
    }
}
