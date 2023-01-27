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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.settings.IdentifiableBase;
import org.apache.maven.api.settings.Settings;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @author Benjamin Bentmann
 * @deprecated use {@link org.apache.maven.settings.v4.SettingsMerger}
 */
@Deprecated
public class MavenSettingsMerger {

    /**
     * @param dominant
     * @param recessive
     * @param recessiveSourceLevel
     */
    public Settings merge(Settings dominant, Settings recessive, String recessiveSourceLevel) {
        if (dominant == null) {
            return recessive;
        } else if (recessive == null) {
            return dominant;
        }

        recessive.setSourceLevel(recessiveSourceLevel);

        Settings.Builder merged = Settings.newBuilder(dominant);

        List<String> dominantActiveProfiles = dominant.getActiveProfiles();
        List<String> recessiveActiveProfiles = recessive.getActiveProfiles();
        List<String> mergedActiveProfiles = Stream.of(dominantActiveProfiles, recessiveActiveProfiles)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
        merged.activeProfiles(mergedActiveProfiles);

        List<String> dominantPluginGroupIds = dominant.getPluginGroups();
        List<String> recessivePluginGroupIds = recessive.getPluginGroups();
        List<String> mergedPluginGroupIds = Stream.of(dominantPluginGroupIds, recessivePluginGroupIds)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
        merged.pluginGroups(mergedPluginGroupIds);

        String localRepository = StringUtils.isEmpty(dominant.getLocalRepository())
                ? recessive.getLocalRepository()
                : dominant.getLocalRepository();
        merged.localRepository(localRepository);

        merged.mirrors(shallowMergeById(dominant.getMirrors(), recessive.getMirrors(), recessiveSourceLevel));
        merged.servers(shallowMergeById(dominant.getServers(), recessive.getServers(), recessiveSourceLevel));
        merged.proxies(shallowMergeById(dominant.getProxies(), recessive.getProxies(), recessiveSourceLevel));
        merged.profiles(shallowMergeById(dominant.getProfiles(), recessive.getProfiles(), recessiveSourceLevel));

        return merged.build();
    }

    /**
     * @param dominant
     * @param recessive
     * @param recessiveSourceLevel
     */
    private static <T extends IdentifiableBase> List<T> shallowMergeById(
            List<T> dominant, List<T> recessive, String recessiveSourceLevel) {
        Set<String> dominantIds = dominant.stream().map(IdentifiableBase::getId).collect(Collectors.toSet());
        final List<T> merged = new ArrayList<>(dominant.size() + recessive.size());
        merged.addAll(dominant);
        for (T identifiable : recessive) {
            if (!dominantIds.contains(identifiable.getId())) {
                identifiable.setSourceLevel(recessiveSourceLevel);
                merged.add(identifiable);
            }
        }
        return merged;
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
