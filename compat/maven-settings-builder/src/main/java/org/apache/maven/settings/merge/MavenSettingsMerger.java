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
package org.apache.maven.settings.merge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.settings.IdentifiableBase;
import org.apache.maven.settings.Settings;

/**
 * @deprecated since 4.0.0, use {@link org.apache.maven.settings.v4.SettingsMerger} instead
 */
@Deprecated(since = "4.0.0")
public class MavenSettingsMerger {

    /**
     * @param dominant
     * @param recessive
     * @param recessiveSourceLevel
     */
    public void merge(Settings dominant, Settings recessive, String recessiveSourceLevel) {
        if (dominant == null || recessive == null) {
            return;
        }

        recessive.setSourceLevel(recessiveSourceLevel);

        List<String> dominantActiveProfiles = dominant.getActiveProfiles();
        List<String> recessiveActiveProfiles = recessive.getActiveProfiles();

        if (recessiveActiveProfiles != null) {
            if (dominantActiveProfiles == null) {
                dominantActiveProfiles = new ArrayList<>();
                dominant.setActiveProfiles(dominantActiveProfiles);
            }

            for (String profileId : recessiveActiveProfiles) {
                if (!dominantActiveProfiles.contains(profileId)) {
                    dominantActiveProfiles.add(profileId);
                }
            }
        }

        List<String> dominantPluginGroupIds = dominant.getPluginGroups();

        List<String> recessivePluginGroupIds = recessive.getPluginGroups();

        if (recessivePluginGroupIds != null) {
            if (dominantPluginGroupIds == null) {
                dominantPluginGroupIds = new ArrayList<>();
                dominant.setPluginGroups(dominantPluginGroupIds);
            }

            for (String pluginGroupId : recessivePluginGroupIds) {
                if (!dominantPluginGroupIds.contains(pluginGroupId)) {
                    dominantPluginGroupIds.add(pluginGroupId);
                }
            }
        }

        if (dominant.getLocalRepository() == null
                || dominant.getLocalRepository().isEmpty()) {
            dominant.setLocalRepository(recessive.getLocalRepository());
        }

        shallowMergeById(dominant.getMirrors(), recessive.getMirrors(), recessiveSourceLevel);
        shallowMergeById(dominant.getServers(), recessive.getServers(), recessiveSourceLevel);
        shallowMergeById(dominant.getProxies(), recessive.getProxies(), recessiveSourceLevel);
        shallowMergeById(dominant.getProfiles(), recessive.getProfiles(), recessiveSourceLevel);
        shallowMergeById(dominant.getRepositories(), recessive.getRepositories(), recessiveSourceLevel);
        shallowMergeById(dominant.getPluginRepositories(), recessive.getPluginRepositories(), recessiveSourceLevel);
    }

    /**
     * @param dominant
     * @param recessive
     * @param recessiveSourceLevel
     */
    private static <T extends IdentifiableBase> void shallowMergeById(
            List<T> dominant, List<T> recessive, String recessiveSourceLevel) {
        Map<String, T> dominantById = mapById(dominant);
        final List<T> identifiables = new ArrayList<>(recessive.size());

        for (T identifiable : recessive) {
            if (!dominantById.containsKey(identifiable.getId())) {
                identifiable.setSourceLevel(recessiveSourceLevel);

                identifiables.add(identifiable);
            }
        }

        dominant.addAll(0, identifiables);
    }

    /**
     * @param identifiables
     * @return a map
     */
    private static <T extends IdentifiableBase> Map<String, T> mapById(List<T> identifiables) {
        Map<String, T> byId = new HashMap<>();

        for (T identifiable : identifiables) {
            byId.put(identifiable.getId(), identifiable);
        }

        return byId;
    }
}
