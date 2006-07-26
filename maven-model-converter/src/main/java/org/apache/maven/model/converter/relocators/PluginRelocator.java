package org.apache.maven.model.converter.relocators;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import org.apache.maven.model.Model;
import org.apache.maven.model.converter.ConverterListener;

import java.util.List;

/**
 * A plugin relocator handles a plugin that has changed its groupId and/or
 * artifactId between the Maven 1 version and the Maven 2 version. It changes
 * the appropriate values in the v4 pom.
 *
 * @author Dennis Lundberg
 * @version $Id$
 */
public interface PluginRelocator
{
    String ROLE = PluginRelocator.class.getName();

    /**
     * Relocate a plugin from one groupId/artifactId to another.
     *
     * @param v4Model The model where we look for the plugin
     */
    void relocate( Model v4Model );

    /**
     * Add a listener for all messages sended by the relocator.
     *
     * @param listener The listener that will receive messages
     */
    void addListener( ConverterListener listener );

    /**
     * Add a listeners list for all messages sended by the relocator.
     *
     * @param listeners The listeners list that will receive messages
     */
    void addListeners( List listeners );
}
