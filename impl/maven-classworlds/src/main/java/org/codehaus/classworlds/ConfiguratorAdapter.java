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
package org.codehaus.classworlds;

/*
 * Copyright 2001-2010 Codehaus Foundation.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

/**
 * An adapter for Configurators.
 *
 * <p><b>Note:</b> This is a legacy internal class provided for backward compatibility with Maven 2.
 * New code should avoid using this adapter.</p>
 *
 * @deprecated This is a legacy internal class.
 */
@Deprecated
public class ConfiguratorAdapter extends Configurator {

    public static ConfiguratorAdapter getInstance(
            org.codehaus.plexus.classworlds.launcher.Configurator newConfig, Launcher launcher) {
        return new ConfiguratorAdapter(newConfig, launcher);
    }

    public static ConfiguratorAdapter getInstance(
            org.codehaus.plexus.classworlds.launcher.Configurator newConfig, ClassWorld world) {
        return new ConfiguratorAdapter(newConfig, world);
    }

    private final org.codehaus.plexus.classworlds.launcher.Configurator config;

    private ConfiguratorAdapter(org.codehaus.plexus.classworlds.launcher.Configurator config, Launcher launcher) {
        super(launcher);
        this.config = config;
    }

    private ConfiguratorAdapter(org.codehaus.plexus.classworlds.launcher.Configurator config, ClassWorld world) {
        super(world);
        this.config = config;
    }

    public void associateRealms() {
        config.associateRealms();
    }

    @SuppressWarnings("DuplicateThrows")
    public void configureAdapter(InputStream is)
            throws IOException, MalformedURLException, ConfigurationException, DuplicateRealmException,
                    NoSuchRealmException {
        try {
            config.configure(is);
        } catch (org.codehaus.plexus.classworlds.launcher.ConfigurationException e) {
            throw new ConfigurationException(e.getMessage());
        } catch (org.codehaus.plexus.classworlds.realm.DuplicateRealmException e) {
            throw new DuplicateRealmException(ClassWorldAdapter.getInstance(e.getWorld()), e.getId());
        } catch (org.codehaus.plexus.classworlds.realm.NoSuchRealmException e) {
            throw new NoSuchRealmException(ClassWorldAdapter.getInstance(e.getWorld()), e.getId());
        }
    }
}
