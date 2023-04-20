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
package org.apache.maven.model.root;

import javax.inject.Named;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@Named
public class DefaultRootLocator implements RootLocator {

    public boolean isRootDirectory(Path dir) {
        if (Files.isDirectory(dir.resolve(".mvn"))) {
            return true;
        }
        // we're too early to use the modelProcessor ...
        Path pom = dir.resolve("pom.xml");
        try (InputStream is = Files.newInputStream(pom)) {
            MXParser parser = new MXParser();
            parser.setInput(is, null);
            if (parser.nextTag() == MXParser.START_TAG && parser.getName().equals("project")) {
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    if ("root".equals(parser.getAttributeName(i))) {
                        return Boolean.parseBoolean(parser.getAttributeValue(i));
                    }
                }
            }
        } catch (IOException | XmlPullParserException e) {
            // The root locator can be used very early during the setup of Maven,
            // even before the arguments from the command line are parsed.  Any exception
            // that would happen here should cause the build to fail at a later stage
            // (when actually parsing the POM) and will lead to a better exception being
            // displayed to the user, so just bail out and return false.
        }
        return false;
    }
}
