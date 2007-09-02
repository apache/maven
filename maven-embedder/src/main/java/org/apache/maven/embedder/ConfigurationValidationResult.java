package org.apache.maven.embedder;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.settings.Settings;

/**
 * @author Jason van Zyl
 * TODO remove deprecated methods
 */
public interface ConfigurationValidationResult
{
    /**
     * Whether both user and global settings are valid
     */
    boolean isValid();

    /**
     * Parsed user settings. If there's any parse error, it can be retrieved through
     * {@link #getUserSettingsException()}
     */
    Settings getUserSettings();

    /**
     * Parsed global settings. If there's any parse error, it can be retrieved through
     * {@link #getGlobalSettingsException()}
     */
    Settings getGlobalSettings();

    /**
     * Any exception that happened during parsing user settings, or null if there were no errors.
     */
    Exception getUserSettingsException();

    /**
     * Any exception that happened during parsing global settings, or null if there were no errors.
     */
    Exception getGlobalSettingsException();

    /**
     * @deprecated
     */
    boolean isUserSettingsFilePresent();

    /**
     * @deprecated
     */
    void setUserSettingsFilePresent( boolean userSettingsFilePresent );

    /**
     * @deprecated
     */
    boolean isUserSettingsFileParses();

    /**
     * @deprecated
     */
    void setUserSettingsFileParses( boolean userSettingsFileParses );

    /**
     * @deprecated
     */
    boolean isGlobalSettingsFilePresent();

    /**
     * @deprecated
     */
    void setGlobalSettingsFilePresent( boolean globalSettingsFilePresent );

    /**
     * @deprecated
     */
    boolean isGlobalSettingsFileParses();

    /**
     * @deprecated
     */
    void setGlobalSettingsFileParses( boolean globalSettingsFileParses );

    /**
     * @deprecated
     */
    void display();
}
