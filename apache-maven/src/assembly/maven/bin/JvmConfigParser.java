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
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses .mvn/jvm.config file for Windows batch/Unix shell scripts.
 * This avoids the complexity of parsing special characters (pipes, quotes, etc.) in scripts.
 *
 * Usage: java JvmConfigParser.java <jvm.config-path> <maven-project-basedir> [output-file]
 *
 * If output-file is provided, writes result to that file (avoids Windows file locking issues).
 * Otherwise, outputs to stdout.
 *
 * Outputs: Single line with space-separated quoted arguments (safe for batch scripts)
 */
public class JvmConfigParser {
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.err.println("Usage: java JvmConfigParser.java <jvm.config-path> <maven-project-basedir> [output-file]");
            System.exit(1);
        }

        Path jvmConfigPath = Paths.get(args[0]);
        String mavenProjectBasedir = args[1];
        Path outputFile = args.length == 3 ? Paths.get(args[2]) : null;

        if (!Files.exists(jvmConfigPath)) {
            // No jvm.config file - output nothing (create empty file if output specified)
            if (outputFile != null) {
                try {
                    Files.writeString(outputFile, "", StandardCharsets.UTF_8);
                } catch (IOException e) {
                    System.err.println("ERROR: Failed to write output file: " + e.getMessage());
                    System.err.flush();
                    System.exit(1);
                }
            }
            return;
        }

        try {
            String result = parseJvmConfig(jvmConfigPath, mavenProjectBasedir);
            if (outputFile != null) {
                // Write directly to file - this ensures proper file handle cleanup on Windows
                // Add newline at end for Windows 'for /f' command compatibility
                try (Writer writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
                    writer.write(result);
                    if (!result.isEmpty()) {
                        writer.write(System.lineSeparator());
                    }
                }
            } else {
                System.out.print(result);
                System.out.flush();
            }
        } catch (IOException e) {
            // If jvm.config exists but can't be read, this is a configuration error
            // Print clear error and exit with error code to prevent Maven from running
            System.err.println("ERROR: Failed to read .mvn/jvm.config: " + e.getMessage());
            System.err.println("Please check file permissions and syntax.");
            System.err.flush();
            System.exit(1);
        }
    }

    /**
     * Parse jvm.config file and return formatted arguments.
     * Package-private for testing.
     */
    static String parseJvmConfig(Path jvmConfigPath, String mavenProjectBasedir) throws IOException {
        StringBuilder result = new StringBuilder();

        for (String line : Files.readAllLines(jvmConfigPath, StandardCharsets.UTF_8)) {
            line = processLine(line, mavenProjectBasedir);
            if (line.isEmpty()) {
                continue;
            }

            List<String> parsed = parseArguments(line);
            appendQuotedArguments(result, parsed);
        }

        return result.toString();
    }

    /**
     * Process a single line: remove comments, trim whitespace, and replace placeholders.
     */
    private static String processLine(String line, String mavenProjectBasedir) {
        // Remove comments
        int commentIndex = line.indexOf('#');
        if (commentIndex >= 0) {
            line = line.substring(0, commentIndex);
        }

        // Trim whitespace
        line = line.trim();

        // Replace MAVEN_PROJECTBASEDIR placeholders
        line = line.replace("${MAVEN_PROJECTBASEDIR}", mavenProjectBasedir);
        line = line.replace("$MAVEN_PROJECTBASEDIR", mavenProjectBasedir);

        return line;
    }

    /**
     * Append parsed arguments as quoted strings to the result builder.
     */
    private static void appendQuotedArguments(StringBuilder result, List<String> args) {
        for (String arg : args) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append('"').append(arg).append('"');
        }
    }

    /**
     * Parse a line into individual arguments, respecting quoted strings.
     * Quotes are stripped from the arguments.
     */
    private static List<String> parseArguments(String line) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
            } else if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
            } else if (c == ' ' && !inDoubleQuotes && !inSingleQuotes) {
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