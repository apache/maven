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
package org.apache.maven.cling.invoker.mvnup.goals;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.cling.invoker.mvnup.Goal;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;

/**
 * The "help" goal implementation.
 */
@Named("help")
@Singleton
public class Help implements Goal {

    @Override
    public int execute(UpgradeContext context) throws Exception {
        context.info("Maven Upgrade Tool - Help");
        context.println();
        context.info("Upgrades Maven projects to be compatible with Maven 4.");
        context.println();
        context.info("Available goals:");
        context.indent();
        context.info("help  - display this help message");
        context.info("check - check for available upgrades");
        context.info("apply - apply available upgrades");
        context.unindent();
        context.println();
        context.info("Usage: mvnup [options] <goal>");
        context.println();
        context.info("Options:");
        context.indent();
        context.info("-m, --model <version> Target POM model version (4.0.0 or 4.1.0)");
        context.info("-d, --directory <path> Directory to use as starting point for POM discovery");
        context.info("-i, --infer           Remove redundant information that can be inferred by Maven");
        context.info("    --fix-model       Fix Maven 4 compatibility issues in POM files");
        context.info("    --plugins         Upgrade plugins known to fail with Maven 4");
        context.info(
                "-a, --all             Apply all upgrades (equivalent to --model 4.1.0 --infer --fix-model --plugins)");
        context.info("-f, --force           Overwrite files without asking for confirmation");
        context.info("-y, --yes             Answer \"yes\" to all prompts automatically");
        context.unindent();
        context.println();
        context.info("Default behavior: --fix-model and --plugins are applied if no other options are specified");
        context.println();

        return 0;
    }
}
