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
package org.apache.maven.plugin.coreit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Assists in handling properties.
 *
 * @author Benjamin Bentmann
 */
class PropertiesUtil {

    public static Properties read(File inputFile) throws MojoExecutionException {
        Properties props = new Properties();

        if (inputFile.exists()) {
            InputStream is = null;
            try {
                is = new FileInputStream(inputFile);
                props.load(is);
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Input file " + inputFile + " could not be read: " + e.getMessage(), e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // just ignore
                    }
                }
            }
        }

        return props;
    }

    public static void write(File outputFile, Properties props) throws MojoExecutionException {
        OutputStream os = null;
        try {
            outputFile.getParentFile().mkdirs();
            os = new FileOutputStream(outputFile);
            props.store(os, "MAVEN-CORE-IT-LOG");
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Output file " + outputFile + " could not be created: " + e.getMessage(), e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // just ignore
                }
            }
        }
    }

    public static void serialize(Properties props, String key, Object value) {
        if (value != null && value.getClass().isArray()) {
            props.setProperty(key, Integer.toString(Array.getLength(value)));
            for (int i = Array.getLength(value) - 1; i >= 0; i--) {
                serialize(props, key + "." + i, Array.get(value, i));
            }
        } else if (value instanceof Collection) {
            Collection collection = (Collection) value;
            props.setProperty(key, Integer.toString(collection.size()));
            int i = 0;
            for (Iterator it = collection.iterator(); it.hasNext(); i++) {
                serialize(props, key + "." + i, it.next());
            }
        } else if (value instanceof Map) {
            Map map = (Map) value;
            props.setProperty(key, Integer.toString(map.size()));
            int i = 0;
            for (Iterator it = map.keySet().iterator(); it.hasNext(); i++) {
                Object k = it.next();
                Object v = map.get(k);
                serialize(props, key + "." + k, v);
            }
        } else if (value instanceof PlexusConfiguration) {
            PlexusConfiguration config = (PlexusConfiguration) value;

            String val = config.getValue(null);
            if (val != null) {
                props.setProperty(key + ".value", val);
            }

            String[] attributes = config.getAttributeNames();
            props.setProperty(key + ".attributes", Integer.toString(attributes.length));
            for (int i = attributes.length - 1; i >= 0; i--) {
                props.setProperty(key + ".attributes." + attributes[i], config.getAttribute(attributes[i], ""));
            }

            PlexusConfiguration children[] = config.getChildren();
            props.setProperty(key + ".children", Integer.toString(children.length));
            Map<String, Integer> indices = new HashMap<>();
            for (PlexusConfiguration child : children) {
                String name = child.getName();
                Integer index = indices.get(name);
                if (index == null) {
                    index = 0;
                }
                serialize(props, key + ".children." + name + "." + index, child);
                indices.put(name, index + 1);
            }
        } else if (value instanceof Date) {
            props.setProperty(key, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) value));
        } else if (value != null) {
            props.setProperty(key, value.toString());
        }
    }
}
