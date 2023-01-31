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
package org.apache.maven.toolchain.merge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 *
 * @author Robert Scholte
 * @since 3.2.4
 */
public class MavenToolchainMerger {

    public void merge(PersistedToolchains dominant, PersistedToolchains recessive, String recessiveSourceLevel) {
        if (dominant == null || recessive == null) {
            return;
        }

        recessive.setSourceLevel(recessiveSourceLevel);

        shallowMerge(dominant.getToolchains(), recessive.getToolchains(), recessiveSourceLevel);
    }

    private void shallowMerge(
            List<ToolchainModel> dominant, List<ToolchainModel> recessive, String recessiveSourceLevel) {
        Map<Object, ToolchainModel> merged = new LinkedHashMap<>();

        for (ToolchainModel dominantModel : dominant) {
            Object key = getToolchainModelKey(dominantModel);

            merged.put(key, dominantModel);
        }

        for (ToolchainModel recessiveModel : recessive) {
            Object key = getToolchainModelKey(recessiveModel);

            ToolchainModel dominantModel = merged.get(key);
            if (dominantModel == null) {
                recessiveModel.setSourceLevel(recessiveSourceLevel);
                dominant.add(recessiveModel);
            } else {
                mergeToolchainModelConfiguration(dominantModel, recessiveModel);
            }
        }
    }

    protected void mergeToolchainModelConfiguration(ToolchainModel target, ToolchainModel source) {
        Xpp3Dom src = (Xpp3Dom) source.getConfiguration();
        if (src != null) {
            Xpp3Dom tgt = (Xpp3Dom) target.getConfiguration();
            if (tgt == null) {
                tgt = Xpp3Dom.mergeXpp3Dom(new Xpp3Dom(src), tgt);
            } else {
                tgt = Xpp3Dom.mergeXpp3Dom(tgt, src);
            }
            target.setConfiguration(tgt);
        }
    }

    protected Object getToolchainModelKey(ToolchainModel model) {
        return model;
    }
}
