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
package org.apache.maven.internal.impl.secdispatcher;

import java.io.IOException;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;
import org.codehaus.plexus.components.secdispatcher.SecDispatcherException;
import org.codehaus.plexus.components.secdispatcher.internal.DefaultSecDispatcher;

/**
 * Delegate that offers just the minimal surface needed to decrypt settings.
 */
@Named
public class MavenSecDispatcher {
    private final SecDispatcher delegate;

    @Inject
    public MavenSecDispatcher(@Nullable DefaultSecDispatcher delegate) {
        this.delegate = delegate;
    }

    public String decrypt(String s) throws SecDispatcherException, IOException {
        if (delegate == null) {
            return s;
        }
        return delegate.decrypt(s);
    }

    public boolean isLegacyEncryptedString(String s) {
        if (delegate == null) {
            return false;
        }
        return delegate.isLegacyEncryptedString(s);
    }

    public boolean isAnyEncryptedString(String s) {
        if (delegate == null) {
            return false;
        }
        return delegate.isAnyEncryptedString(s);
    }
}
