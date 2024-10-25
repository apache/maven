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
package org.apache.maven.internal.impl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.maven.api.services.Prompter;
import org.apache.maven.api.services.PrompterException;
import org.codehaus.plexus.PlexusContainer;

@Named
@Singleton
public class DefaultPrompter implements Prompter {

    private static final String PROMPTER_CLASS = "org.codehaus.plexus.components.interactivity.Prompter";
    private final PlexusContainer container;

    @Inject
    public DefaultPrompter(PlexusContainer container) {
        this.container = container;
    }

    @Override
    public String prompt(String message, List<String> possibleValues, String defaultReply) throws PrompterException {
        try {
            Class<?> clazz = container.getContainerRealm().loadClass(PROMPTER_CLASS);
            Object instance = container.lookup(clazz);
            Method method = clazz.getMethod("prompt", String.class, List.class, String.class);
            return (String) method.invoke(instance, message, possibleValues, defaultReply);
        } catch (Exception e) {
            throw new PrompterException("Unable to call prompter", e);
        }
    }

    @Override
    public String promptForPassword(String message) throws PrompterException {
        try {
            Class<?> clazz = container.getContainerRealm().loadClass(PROMPTER_CLASS);
            Object instance = container.lookup(clazz);
            Method method = clazz.getMethod("promptForPassword", String.class);
            return (String) method.invoke(instance, message);
        } catch (Exception e) {
            throw new PrompterException("Unable to call prompter", e);
        }
    }

    @Override
    public void showMessage(String message) throws PrompterException {
        try {
            Class<?> clazz = container.getContainerRealm().loadClass(PROMPTER_CLASS);
            Object instance = container.lookup(clazz);
            Method method = clazz.getMethod("showMessage", String.class);
            method.invoke(instance, message);
        } catch (Exception e) {
            throw new PrompterException("Unable to call prompter", e);
        }
    }
}
