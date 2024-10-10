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
package org.apache.maven.cling.invoker.mvnenc.goals;

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

import org.codehaus.plexus.components.secdispatcher.MasterSource;
import org.codehaus.plexus.components.secdispatcher.MasterSourceMeta;
import org.codehaus.plexus.components.secdispatcher.SecDispatcherException;
import org.codehaus.plexus.components.secdispatcher.internal.sources.PrefixMasterSourceSupport;

/**
 * Inspired by <a href="https://velvetcache.org/2023/03/26/a-peek-inside-pinentry/">A peek inside pinentry</a>
 */
@Singleton
@Named(PinEntryPrompt.NAME)
public class PinEntryPrompt extends PrefixMasterSourceSupport implements MasterSource, MasterSourceMeta {
    public static final String NAME = "pinentry-prompt";

    public PinEntryPrompt() {
        super(NAME + ":");
    }

    @Override
    public String description() {
        return "Secure PinEntry prompt";
    }

    @Override
    public Optional<String> configTemplate() {
        return Optional.of(NAME + ":" + "$pinentryPath");
    }

    @Override
    public String doHandle(String s) throws SecDispatcherException {
        try {
            PinEntry.Result<String> result = new PinEntry(s)
                    .setTimeout(Duration.ofSeconds(30))
                    .setKeyInfo("maven:masterPassword")
                    .setTitle("Maven Master Password")
                    .setDescription("Please enter the Maven master password")
                    .setPrompt("Master password")
                    .setOk("Ok")
                    .setCancel("Cancel")
                    .getPin();
            if (result.outcome() == PinEntry.Outcome.SUCCESS) {
                return result.payload();
            } else if (result.outcome() == PinEntry.Outcome.CANCELED) {
                throw new SecDispatcherException("User canceled the operation");
            } else if (result.outcome() == PinEntry.Outcome.TIMEOUT) {
                throw new SecDispatcherException("Timeout");
            } else {
                throw new SecDispatcherException("Failure: " + result.payload());
            }
        } catch (IOException e) {
            throw new SecDispatcherException("Could not collect the password", e);
        }
    }
}
