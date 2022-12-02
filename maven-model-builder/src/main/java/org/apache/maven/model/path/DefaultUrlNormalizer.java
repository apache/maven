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
package org.apache.maven.model.path;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Normalizes a URL.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultUrlNormalizer implements UrlNormalizer {

    @Override
    public String normalize(String url) {
        String result = url;

        if (result != null) {
            while (true) {
                int idx = result.indexOf("/../");
                if (idx < 0) {
                    break;
                } else if (idx == 0) {
                    result = result.substring(3);
                    continue;
                }
                int parent = idx - 1;
                while (parent >= 0 && result.charAt(parent) == '/') {
                    parent--;
                }
                parent = result.lastIndexOf('/', parent);
                if (parent < 0) {
                    result = result.substring(idx + 4);
                } else {
                    result = result.substring(0, parent) + result.substring(idx + 3);
                }
            }
        }

        return result;
    }
}
