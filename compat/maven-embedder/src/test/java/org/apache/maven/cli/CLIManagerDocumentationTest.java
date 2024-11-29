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
package org.apache.maven.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.cli.Option;
import org.junit.jupiter.api.Test;

import static java.util.Objects.nonNull;

/**
 * Pseudo test to generate documentation fragment about supported CLI options. TODO such documentation generation code
 * should not be necessary as unit test but should be run during site generation (Velocity? Doxia macro?)
 */
@Deprecated
class CLIManagerDocumentationTest {
    private static final String LS = System.lineSeparator();

    private static class OptionComparator implements Comparator<Option> {
        public int compare(Option opt1, Option opt2) {
            String s1 = opt1.getOpt() != null ? opt1.getOpt() : opt1.getLongOpt();
            String s2 = opt2.getOpt() != null ? opt2.getOpt() : opt2.getLongOpt();
            return s1.compareToIgnoreCase(s2);
        }
    }

    private static class CLIManagerExtension extends CLIManager {
        public Collection<Option> getOptions() {
            List<Option> optList = new ArrayList<>(options.getOptions());
            optList.sort(new OptionComparator());
            return optList;
        }
    }

    String getOptionsAsHtml() {
        StringBuilder sb = new StringBuilder(512);
        boolean odd = true;
        sb.append(
                "<table border='1' class='zebra-striped'><tr class='a'><th><b>Options</b></th><th><b>Description</b></th></tr>");
        for (Option option : new CLIManagerExtension().getOptions()) {
            odd = !odd;
            sb.append("<tr class='");
            sb.append(odd ? 'a' : 'b');
            sb.append("'>");

            sb.append("<td>");

            sb.append("<code>");

            if (nonNull(option.getOpt())) {
                sb.append("-<a name='");
                sb.append(option.getOpt());
                sb.append("'>");
                sb.append(option.getOpt());
                sb.append("</a>");
            }

            if (nonNull(option.getLongOpt())) {
                if (nonNull(option.getOpt())) {
                    sb.append(", ");
                }
                sb.append("--<a name='");
                sb.append(option.getLongOpt());
                sb.append("'>");
                sb.append(option.getLongOpt());
                sb.append("</a>");
            }

            if (option.hasArg()) {
                if (option.hasArgName()) {
                    sb.append(" &lt;").append(option.getArgName()).append("&gt;");
                } else {
                    sb.append(' ');
                }
            }
            sb.append("</code>");

            sb.append("</td>");
            sb.append("<td>");
            sb.append(option.getDescription());
            sb.append("</td>");

            sb.append("</tr>");
            sb.append(LS);
        }
        sb.append("</table>");
        return sb.toString();
    }

    @Test
    void testOptionsAsHtml() throws IOException {
        Path options = Paths.get("target/test-classes/options.html");
        Files.writeString(options, getOptionsAsHtml(), StandardCharsets.UTF_8);
    }
}
