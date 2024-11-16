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
package org.apache.maven.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.apache.maven.shared.verifier.VerificationException;

public class Verifier extends org.apache.maven.shared.verifier.Verifier {
    public Verifier(String basedir) throws VerificationException {
        this(basedir, false);
    }

    public Verifier(String basedir, boolean debug) throws VerificationException {
        super(basedir, null, debug, defaultCliArguments());
    }

    static String[] defaultCliArguments() {
        return new String[] {
            "-e", "--batch-mode", "-Dmaven.repo.local.tail=" + System.getProperty("maven.repo.local.tail")
        };
    }

    public String loadLogContent() throws IOException {
        return Files.readString(Paths.get(getBasedir(), getLogFileName()));
    }

    public List<String> loadLogLines() throws IOException {
        return loadLines(getLogFileName());
    }

    public List<String> loadLines(String filename) throws IOException {
        return loadLines(filename, null);
    }

    @Override
    public List<String> loadFile(String basedir, String filename, boolean hasCommand) throws VerificationException {
        return super.loadFile(basedir, filename, hasCommand);
    }

    @Override
    public List<String> loadFile(File file, boolean hasCommand) throws VerificationException {
        return super.loadFile(file, hasCommand);
    }

    public File filterFile(String srcPath, String dstPath) throws IOException {
        return filterFile(srcPath, dstPath, (String) null);
    }

    public File filterFile(String srcPath, String dstPath, Map<String, String> filterMap) throws IOException {
        return super.filterFile(srcPath, dstPath, null, filterMap);
    }

    /**
     * Throws an exception if the text <strong>is</strong> present in the log.
     *
     * @param text the text to assert present
     * @throws VerificationException if text is not found in log
     */
    public void verifyTextNotInLog(String text) throws VerificationException, IOException {
        verifyTextNotInLog(loadLogLines(), text);
    }

    public long textOccurrencesInLog(String text) throws IOException {
        return textOccurencesInLog(loadLogLines(), text);
    }

    public static void verifyTextNotInLog(List<String> lines, String text) throws VerificationException {
        if (textOccurencesInLog(lines, text) > 0) {
            throw new VerificationException("Text found in log: " + text);
        }
    }

    public static void verifyTextInLog(List<String> lines, String text) throws VerificationException {
        if (textOccurencesInLog(lines, text) <= 0) {
            throw new VerificationException("Text not found in log: " + text);
        }
    }

    public static long textOccurencesInLog(List<String> lines, String text) {
        return lines.stream().filter(line -> stripAnsi(line).contains(text)).count();
    }

    public void execute() throws VerificationException {
        super.execute();
    }
}
