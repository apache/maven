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
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Loads classes and/or resources from a class loader and records the results in a properties file.
 *
 * @author Benjamin Bentmann
 *
 */
public abstract class AbstractLoadMojo extends AbstractMojo {

    /**
     * The comma separated set of classes to load. For each specified qualified class name <code>QCN</code> that was
     * successfully loaded, the generated properties files will contain a key named <code>QCN</code>. The value of this
     * key will be the hash code of the requested class. In addition, a key named <code>QCN.methods</code> holds the
     * comma separated list of all public methods declared directly in that class, in alphabetic order and possibly with
     * duplicates to account for overloaded methods.
     */
    @Parameter(property = "clsldr.classNames")
    protected String classNames;

    /**
     * The comma separated set of resources to load. For each specified absolute resource path <code>ARP</code> that was
     * successfully loaded, the generated properties files will contain a key named <code>ARP</code> whose value gives
     * the URL to the resource. In addition, the keys <code>ARP.count</code>, <code>ARP.0</code>, <code>ARP.1</code>
     * etc. will enumerate all URLs matching the resource name.
     */
    @Parameter(property = "clsldr.resourcePaths")
    protected String resourcePaths;

    /**
     * Loads the classes/resources.
     *
     * @param outputFile  The path to the properties file to generate, must not be <code>null</code>.
     * @param classLoader The class loader to use, must not be <code>null</code>.
     * @throws MojoExecutionException If the output file could not be created.
     */
    protected void execute(File outputFile, ClassLoader classLoader) throws MojoExecutionException {
        getLog().info("[MAVEN-CORE-IT-LOG] Using class loader " + classLoader);

        /*
         * NOTE: This one is a little subtle. For all properly implemented class loaders, loading a class/resource from
         * a child class loader (with the usual parent-first delegation and no additional search path) will deliver the
         * same result as loading directly from the parent class loader. However, Maven or better Plexus Classworlds
         * employs custom class loaders which - as history has shown (MNG-1898) - might not always be cleanly
         * implemented. To catch potential class loader defects, we check both the results from the original class
         * loader and a delegating child class loader for consistency. The key point is that querying the child class
         * loader will use a slightly different code path in the original class loader during parent delegation, thereby
         * increasing test coverage for its implementation.
         */
        ClassLoader childClassLoader = new URLClassLoader(new URL[0], classLoader);

        Properties loaderProperties = new Properties();

        if (classNames != null && classNames.length() > 0) {
            String[] names = classNames.split(",");
            for (String name : names) {
                getLog().info("[MAVEN-CORE-IT-LOG] Loading class " + name);

                // test ClassLoader.loadClass(String) and (indirectly) ClassLoader.loadClass(String, boolean)
                try {
                    Class type = classLoader.loadClass(name);
                    getLog().info("[MAVEN-CORE-IT-LOG]   Loaded class from " + type.getClassLoader());
                    try {
                        if (!type.equals(childClassLoader.loadClass(name))) {
                            throw new ClassNotFoundException(name);
                        }
                    } catch (ClassNotFoundException cnfe) {
                        getLog().error("[MAVEN-CORE-IT-LOG] Detected class loader defect while loading " + name);
                        throw cnfe;
                    }
                    loaderProperties.setProperty(name, "" + type.hashCode());

                    Method[] methods = type.getDeclaredMethods();
                    List methodNames = new ArrayList();
                    for (Method method : methods) {
                        if (Modifier.isPublic(method.getModifiers())) {
                            methodNames.add(method.getName());
                        }
                    }
                    Collections.sort(methodNames);
                    StringBuilder buffer = new StringBuilder(1024);
                    for (Object methodName : methodNames) {
                        if (buffer.length() > 0) {
                            buffer.append(',');
                        }
                        buffer.append(methodName);
                    }

                    loaderProperties.setProperty(name + ".methods", buffer.toString());
                } catch (ClassNotFoundException e) {
                    // ignore, will be reported by means of missing keys in the properties file
                    getLog().info("[MAVEN-CORE-IT-LOG]   Class not available");
                } catch (LinkageError e) {
                    // ignore, will be reported by means of missing keys in the properties file
                    getLog().info("[MAVEN-CORE-IT-LOG]   Class not linkable", e);
                }
            }
        }

        if (resourcePaths != null && resourcePaths.length() > 0) {
            String[] paths = resourcePaths.split(",");
            for (String path : paths) {
                getLog().info("[MAVEN-CORE-IT-LOG] Loading resource " + path);

                // test ClassLoader.getResource()
                URL url = classLoader.getResource(path);
                getLog().info("[MAVEN-CORE-IT-LOG]   Loaded resource from " + url);
                if (url != null && !url.equals(childClassLoader.getResource(path))) {
                    getLog().error("[MAVEN-CORE-IT-LOG] Detected class loader defect while getting " + path);
                    url = null;
                }
                if (url != null) {
                    loaderProperties.setProperty(path, url.toString());
                }

                // test ClassLoader.getResources()
                try {
                    List urls = Collections.list(classLoader.getResources(path));
                    if (!urls.equals(Collections.list(childClassLoader.getResources(path)))) {
                        getLog().error("[MAVEN-CORE-IT-LOG] Detected class loader defect while getting " + path);
                        urls = Collections.EMPTY_LIST;
                    }
                    loaderProperties.setProperty(path + ".count", "" + urls.size());
                    for (int j = 0; j < urls.size(); j++) {
                        loaderProperties.setProperty(path + "." + j, urls.get(j).toString());
                    }
                } catch (IOException e) {
                    throw new MojoExecutionException("Resources could not be enumerated: " + path, e);
                }
            }
        }

        getLog().info("[MAVEN-CORE-IT-LOG] Creating output file " + outputFile);

        PropertiesUtil.write(outputFile, loaderProperties);

        getLog().info("[MAVEN-CORE-IT-LOG] Created output file " + outputFile);
    }
}
