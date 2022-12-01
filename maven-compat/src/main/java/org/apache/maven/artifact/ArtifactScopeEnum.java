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
package org.apache.maven.artifact;

/**
 * Type safe reincarnation of Artifact scope. Also supplies the {@code DEFAULT_SCOPE} as well
 * as convenience method to deal with scope relationships.
 *
 * @author <a href="oleg@codehaus.org">Oleg Gusakov</a>
 *
 */
public enum ArtifactScopeEnum {
    compile(1),
    test(2),
    runtime(3),
    provided(4),
    system(5),
    runtime_plus_system(6);

    public static final ArtifactScopeEnum DEFAULT_SCOPE = compile;

    private int id;

    // Constructor
    ArtifactScopeEnum(int id) {
        this.id = id;
    }

    int getId() {
        return id;
    }

    /**
     * Helper method to simplify null processing
     */
    public static ArtifactScopeEnum checkScope(ArtifactScopeEnum scope) {
        return scope == null ? DEFAULT_SCOPE : scope;
    }

    /**
     *
     * @return unsafe String representation of this scope.
     */
    public String getScope() {
        if (id == 1) {
            return Artifact.SCOPE_COMPILE;
        } else if (id == 2) {
            return Artifact.SCOPE_TEST;

        } else if (id == 3) {
            return Artifact.SCOPE_RUNTIME;

        } else if (id == 4) {
            return Artifact.SCOPE_PROVIDED;
        } else if (id == 5) {
            return Artifact.SCOPE_SYSTEM;
        } else {
            return Artifact.SCOPE_RUNTIME_PLUS_SYSTEM;
        }
    }

    private static final ArtifactScopeEnum[][][] COMPLIANCY_SETS = {
        {{compile}, {compile, provided, system}},
        {{test}, {compile, test, provided, system}},
        {{runtime}, {compile, runtime, system}},
        {{provided}, {compile, test, provided}}
    };

    /**
     * scope relationship function. Used by the graph conflict resolution policies
     *
     * @param scope
     * @return true is supplied scope is an inclusive sub-scope of current one.
     */
    public boolean encloses(ArtifactScopeEnum scope) {
        final ArtifactScopeEnum s = checkScope(scope);

        // system scope is historic only - and simple
        if (id == system.id) {
            return scope.id == system.id;
        }

        for (ArtifactScopeEnum[][] set : COMPLIANCY_SETS) {
            if (id == set[0][0].id) {
                for (ArtifactScopeEnum ase : set[1]) {
                    if (s.id == ase.id) {
                        return true;
                    }
                }
                break;
            }
        }
        return false;
    }
}
