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
package org.apache.maven.logging.slf4j;

import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.logging.impl.DefaultLoggerManager;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * LogFactory for Maven which creates SLF4J loggers that delegate level checks
 * and severity recording to the shared {@link DefaultLoggerManager}.
 */
public class MavenLoggerFactory implements ILoggerFactory {
    static volatile MavenLoggerFactory instance;

    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    final DefaultLoggerManager logManager;
    final ConcurrentMap<String, Logger> loggerMap = new ConcurrentHashMap<>();

    public MavenLoggerFactory() {
        instance = this;
        LoggerManager mgr = LoggerManager.get();
        if (mgr instanceof DefaultLoggerManager dlm) {
            logManager = dlm;
        } else {
            logManager = new DefaultLoggerManager();
            LoggerManager.set(logManager);
        }
        logManager.addReconfigurationListener(this::updateExistingLoggers);
    }

    @Override
    public Logger getLogger(String name) {
        // Always resolve pluginId first: a fast-path cache-by-name would incorrectly
        // return a non-plugin logger to a plugin caller that requests the same logger
        // name. The classloader cache in resolvePluginId() keeps the stack walk cheap
        // after the first call per classloader.
        String pluginId = resolvePluginId();
        String cacheKey = pluginId != null ? name + "@" + pluginId : name;
        return loggerMap.computeIfAbsent(cacheKey, k -> new MavenSimpleLogger(name, pluginId));
    }

    /**
     * Sentinel value meaning "this classloader is not associated with any plugin".
     * Stored in the cache to avoid repeated {@code LoggerManager.getPluginId()} calls.
     */
    private static final String NO_PLUGIN = "\0";

    /**
     * Cache of classloader → pluginId lookups. Uses weak keys so that plugin
     * classloaders can be GC-collected once their realm is discarded; without this
     * the map would retain a strong reference to each plugin classloader (and its
     * entire class graph) for the lifetime of the Maven JVM.
     *
     * <p>WeakHashMap is not thread-safe; all accesses are synchronized on this field.</p>
     */
    private final WeakHashMap<ClassLoader, String> classLoaderPluginIds = new WeakHashMap<>();

    private String resolvePluginId() {
        return STACK_WALKER.walk(frames -> frames.map(f -> {
                    ClassLoader cl = f.getDeclaringClass().getClassLoader();
                    return resolveClassLoaderPluginId(cl);
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null));
    }

    private String resolveClassLoaderPluginId(ClassLoader cl) {
        if (cl == null) {
            return null;
        }
        String cached;
        synchronized (classLoaderPluginIds) {
            cached = classLoaderPluginIds.get(cl);
        }
        if (cached != null) {
            return cached == NO_PLUGIN ? null : cached;
        }
        String resolved = LoggerManager.get().getPluginId(cl);
        synchronized (classLoaderPluginIds) {
            classLoaderPluginIds.put(cl, resolved != null ? resolved : NO_PLUGIN);
        }
        return resolved;
    }

    public void reconfigure() {
        LoggerManager.get().reconfigure();
    }

    private void updateExistingLoggers() {
        loggerMap.values().forEach(l -> {
            if (l instanceof MavenSimpleLogger msl) {
                msl.configure(logManager.getDefaultLogLevel());
            }
        });
    }
}
