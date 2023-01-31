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
package org.apache.maven.settings.building;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.settings.io.SettingsParseException;

/**
 * Collects problems that are encountered during settings building.
 *
 * @author Benjamin Bentmann
 */
class DefaultSettingsProblemCollector implements SettingsProblemCollector {

    private List<SettingsProblem> problems;

    private String source;

    DefaultSettingsProblemCollector(List<SettingsProblem> problems) {
        this.problems = (problems != null) ? problems : new ArrayList<SettingsProblem>();
    }

    public List<SettingsProblem> getProblems() {
        return problems;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public void add(SettingsProblem.Severity severity, String message, int line, int column, Exception cause) {
        if (line <= 0 && column <= 0 && (cause instanceof SettingsParseException)) {
            SettingsParseException e = (SettingsParseException) cause;
            line = e.getLineNumber();
            column = e.getColumnNumber();
        }

        SettingsProblem problem = new DefaultSettingsProblem(message, severity, source, line, column, cause);

        problems.add(problem);
    }
}
