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
package org.codehaus.plexus.classworlds.launcher;

/*
 * Copyright 2001-2006 Codehaus Foundation.
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
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;

/**
 * Event based launcher configuration parser, delegating effective configuration handling to ConfigurationHandler.
 *
 * @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 * @author Jason van Zyl
 * @author Igor Fedorenko
 * @see ConfigurationHandler
 */
public class ConfigurationParser {
    public static final String MAIN_PREFIX = "main is";

    public static final String SET_PREFIX = "set";

    public static final String IMPORT_PREFIX = "import";

    public static final String LOAD_PREFIX = "load";

    /**
     * Optionally spec prefix.
     */
    public static final String OPTIONALLY_PREFIX = "optionally";

    protected static final String FROM_SEPARATOR = " from ";

    protected static final String USING_SEPARATOR = " using ";

    protected static final String DEFAULT_SEPARATOR = " default ";

    private final ConfigurationHandler handler;

    private final Properties systemProperties;

    public ConfigurationParser(ConfigurationHandler handler, Properties systemProperties) {
        this.handler = handler;
        this.systemProperties = systemProperties;
    }

    /**
     * Parse launcher configuration file and send events to the handler.
     *
     * @param is the inputstream
     * @throws IOException when IOException occurs
     * @throws ConfigurationException when ConfigurationException occurs
     * @throws DuplicateRealmException when realm already exists
     * @throws NoSuchRealmException when realm doesn't exist
     */
    public void parse(InputStream is)
            throws IOException, ConfigurationException, DuplicateRealmException, NoSuchRealmException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            int lineNo = 0;
            boolean mainSet = false;
            String curRealm = null;

            while (true) {
                line = reader.readLine();

                if (line == null) {
                    break;
                }

                ++lineNo;
                line = line.trim();

                if (canIgnore(line)) {
                    continue;
                }

                char lineFirstChar = line.charAt(0);
                switch (lineFirstChar) {
                    case 'm':
                        mainSet = handleMainConfiguration(line, lineNo, mainSet);
                        break;
                    case 's':
                        if (handleSetConfiguration(line, lineNo)) {
                            continue;
                        }
                        break;
                    case '[':
                        curRealm = handleRealmConfiguration(line, lineNo);
                        break;
                    case 'i':
                        handleImportConfiguration(line, lineNo, curRealm);
                        break;
                    case 'l':
                        handleLoadConfiguration(line, lineNo);
                        break;
                    case 'o':
                        handleOptionallyConfiguration(line, lineNo);
                        break;
                    default:
                        throw new ConfigurationException("Unhandled configuration", lineNo, line);
                }
            }
        }
    }

    /**
     * Load a glob into the specified classloader.
     *
     * @param line       The path configuration line.
     * @param optionally Whether the path is optional or required
     * @throws MalformedURLException If the line does not represent
     *                               a valid path element.
     * @throws FileNotFoundException If the line does not represent
     *                               a valid path element in the filesystem.
     * @throws ConfigurationException will never occur (thrown for backwards compatibility)
     */
    protected void loadGlob(String line, boolean optionally)
            throws MalformedURLException, FileNotFoundException, ConfigurationException {
        File globFile = new File(line);

        File dir = globFile.getParentFile();
        if (!dir.exists()) {
            if (optionally) {
                return;
            } else {
                throw new FileNotFoundException(dir.toString());
            }
        }

        String localName = globFile.getName();

        int starLoc = localName.indexOf("*");

        final String prefix = localName.substring(0, starLoc);

        final String suffix = localName.substring(starLoc + 1);

        File[] matches = dir.listFiles((dir1, name) -> {
            if (!name.startsWith(prefix)) {
                return false;
            }

            if (!name.endsWith(suffix)) {
                return false;
            }

            return true;
        });

        for (File match : matches) {
            handler.addLoadFile(match);
        }
    }

    /**
     * Filter a string for system properties.
     *
     * @param text The text to filter.
     * @return The filtered text.
     * @throws ConfigurationException If the property does not
     *                                exist or if there is a syntax error.
     */
    protected String filter(String text) throws ConfigurationException {
        StringBuilder result = new StringBuilder();

        int cur = 0;
        int textLen = text.length();

        int propStart;
        int propStop;

        String propName;
        String propValue;

        while (cur < textLen) {
            propStart = text.indexOf("${", cur);

            if (propStart < 0) {
                break;
            }

            result.append(text, cur, propStart);

            propStop = text.indexOf("}", propStart);

            if (propStop < 0) {
                throw new ConfigurationException("Unterminated property: " + text.substring(propStart));
            }

            propName = text.substring(propStart + 2, propStop);

            propValue = systemProperties.getProperty(propName);

            /* do our best if we are not running from surefire */
            if (propName.equals("basedir") && (propValue == null || propValue.equals(""))) {
                propValue = (new File("")).getAbsolutePath();
            }

            if (propValue == null) {
                throw new ConfigurationException("No such property: " + propName);
            }
            result.append(propValue);

            cur = propStop + 1;
        }

        result.append(text.substring(cur));

        return result.toString();
    }

    /**
     * Determine if a line can be ignored because it is
     * a comment or simply blank.
     *
     * @param line The line to test.
     * @return <code>true</code> if the line is ignorable,
     *         otherwise <code>false</code>.
     */
    private boolean canIgnore(String line) {
        return (line.isEmpty() || line.startsWith("#"));
    }

    private boolean handleMainConfiguration(String line, int lineNo, boolean mainSet) throws ConfigurationException {
        if (line.startsWith(MAIN_PREFIX)) {
            if (mainSet) {
                throw new ConfigurationException("Duplicate main configuration", lineNo, line);
            }

            int fromLoc = line.indexOf(FROM_SEPARATOR, MAIN_PREFIX.length());

            if (fromLoc < 0) {
                throw new ConfigurationException("Missing from clause", lineNo, line);
            }

            String mainClassName =
                    filter(line.substring(MAIN_PREFIX.length(), fromLoc).trim());

            String mainRealmName =
                    filter(line.substring(fromLoc + FROM_SEPARATOR.length()).trim());

            this.handler.setAppMain(mainClassName, mainRealmName);

            return true;
        }
        throw new ConfigurationException("Unhandled configuration", lineNo, line);
    }

    private boolean handleSetConfiguration(String line, int lineNo) throws ConfigurationException {
        if (line.startsWith(SET_PREFIX)) {
            String conf = line.substring(SET_PREFIX.length()).trim();

            int usingLoc = conf.indexOf(USING_SEPARATOR);

            String property = null;
            String propertiesFileName = null;

            if (usingLoc >= 0) {
                property = conf.substring(0, usingLoc).trim();
                propertiesFileName = filter(
                        conf.substring(usingLoc + USING_SEPARATOR.length()).trim());
                conf = propertiesFileName;
            }

            String defaultValue = null;
            int defaultLoc = conf.indexOf(DEFAULT_SEPARATOR);

            if (defaultLoc >= 0) {
                defaultValue = filter(
                        conf.substring(defaultLoc + DEFAULT_SEPARATOR.length()).trim());

                if (property == null) {
                    property = conf.substring(0, defaultLoc).trim();
                } else {
                    propertiesFileName = conf.substring(0, defaultLoc).trim();
                }
            }

            String value = systemProperties.getProperty(property);

            if (value != null) {
                return true;
            }

            if (propertiesFileName != null) {
                File propertiesFile = new File(propertiesFileName);

                if (propertiesFile.exists()) {
                    Properties properties = new Properties();

                    try (InputStream inputStream = Files.newInputStream(Paths.get(propertiesFileName))) {
                        properties.load(inputStream);
                        value = properties.getProperty(property);
                    } catch (Exception e) {
                        // do nothing
                    }
                }
            }

            if (value == null && defaultValue != null) {
                value = defaultValue;
            }

            if (value != null) {
                value = filter(value);
                systemProperties.setProperty(property, value);
            }

            return false;
        }
        throw new ConfigurationException("Unhandled configuration", lineNo, line);
    }

    private String handleRealmConfiguration(String line, int lineNo)
            throws ConfigurationException, DuplicateRealmException {
        int rbrack = line.indexOf("]");

        if (rbrack < 0) {
            throw new ConfigurationException("Invalid realm specifier", lineNo, line);
        }

        String realmName = line.substring(1, rbrack);
        handler.addRealm(realmName);
        return realmName;
    }

    private void handleImportConfiguration(String line, int lineNo, String curRealm)
            throws ConfigurationException, NoSuchRealmException {
        if (line.startsWith(IMPORT_PREFIX)) {
            if (curRealm == null) {
                throw new ConfigurationException("Unhandled import", lineNo, line);
            }
            int fromLoc = line.indexOf(FROM_SEPARATOR, IMPORT_PREFIX.length());

            if (fromLoc < 0) {
                throw new ConfigurationException("Missing from clause", lineNo, line);
            }

            String importSpec = line.substring(IMPORT_PREFIX.length(), fromLoc).trim();
            String realmName = line.substring(fromLoc + FROM_SEPARATOR.length()).trim();

            handler.addImportFrom(realmName, importSpec);
            return;
        }
        throw new ConfigurationException("Unhandled configuration", lineNo, line);
    }

    private void handleLoadConfiguration(String line, int lineNo)
            throws ConfigurationException, FileNotFoundException, MalformedURLException {
        if (line.startsWith(LOAD_PREFIX)) {
            String constituent = line.substring(LOAD_PREFIX.length()).trim();
            constituent = filter(constituent);

            if (constituent.contains("*")) {
                loadGlob(constituent, false /*not optionally*/);
            } else {
                File file = new File(constituent);

                if (file.exists()) {
                    handler.addLoadFile(file);
                } else {
                    try {
                        handler.addLoadURL(new URL(constituent));
                    } catch (MalformedURLException e) {
                        throw new FileNotFoundException(constituent);
                    }
                }
            }
            return;
        }
        throw new ConfigurationException("Unhandled configuration", lineNo, line);
    }

    private void handleOptionallyConfiguration(String line, int lineNo)
            throws ConfigurationException, FileNotFoundException, MalformedURLException {
        if (line.startsWith(OPTIONALLY_PREFIX)) {
            String constituent = line.substring(OPTIONALLY_PREFIX.length()).trim();
            constituent = filter(constituent);

            if (constituent.contains("*")) {
                loadGlob(constituent, true /*optionally*/);
            } else {
                File file = new File(constituent);

                if (file.exists()) {
                    handler.addLoadFile(file);
                } else {
                    try {
                        handler.addLoadURL(new URL(constituent));
                    } catch (MalformedURLException e) {
                        // swallow
                    }
                }
            }
            return;
        }
        throw new ConfigurationException("Unhandled configuration", lineNo, line);
    }
}
