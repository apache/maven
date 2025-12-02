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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Parses .mvn/jvm.config file for Windows batch scripts.
 * This avoids the complexity of parsing special characters (pipes, quotes, etc.) in batch scripts.
 *
 * Usage: java JvmConfigParser.java <jvm.config-path> <maven-project-basedir>
 *
 * Outputs: Single line with space-separated quoted arguments (safe for batch scripts)
 */
public class JvmConfigParser {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java JvmConfigParser.java <jvm.config-path> <maven-project-basedir>");
            System.exit(1);
        }

        Path jvmConfigPath = Paths.get(args[0]);
        String mavenProjectBasedir = args[1];

        if (!Files.exists(jvmConfigPath)) {
            // No jvm.config file - output nothing
            return;
        }

        try (Stream<String> lines = Files.lines(jvmConfigPath, StandardCharsets.UTF_8)) {
            StringBuilder result = new StringBuilder();

            lines.forEach(line -> {
                // Remove comments
                int commentIndex = line.indexOf('#');
                if (commentIndex >= 0) {
                    line = line.substring(0, commentIndex);
                }

                // Trim whitespace
                line = line.trim();

                // Skip empty lines
                if (line.isEmpty()) {
                    return;
                }

                // Replace MAVEN_PROJECTBASEDIR placeholders
                line = line.replace("${MAVEN_PROJECTBASEDIR}", mavenProjectBasedir);
                line = line.replace("$MAVEN_PROJECTBASEDIR", mavenProjectBasedir);

                // Parse line into individual arguments (split on spaces, respecting quotes)
                List<String> parsed = parseArguments(line);

                // Append each argument quoted
                for (String arg : parsed) {
                    if (result.length() > 0) {
                        result.append(' ');
                    }
                    result.append('"').append(arg).append('"');
                }
            });

            System.out.print(result.toString());
            System.out.flush(); // Ensure output is flushed before exit (important on Windows)
        } catch (IOException e) {
            System.err.println("Error reading jvm.config: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Parse a line into individual arguments, respecting quoted strings.
     * Quotes are stripped from the arguments.
     */
    private static List<String> parseArguments(String line) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean inSingleQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"' && !inSingleQuotes) {
                inQuotes = !inQuotes;
                // Don't include the quote character itself
            } else if (c == '\'' && !inQuotes) {
                inSingleQuotes = !inSingleQuotes;
                // Don't include the quote character itself
            } else if (c == ' ' && !inQuotes && !inSingleQuotes) {
                // Space outside quotes - end of argument
                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        // Add last argument
        if (current.length() > 0) {
            args.add(current.toString());
        }

        return args;
    }
}

