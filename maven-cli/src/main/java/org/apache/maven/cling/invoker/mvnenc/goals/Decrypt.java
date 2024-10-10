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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cling.invoker.mvnenc.DefaultEncryptInvoker;
import org.apache.maven.cling.invoker.mvnenc.Goal;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;

import static org.apache.maven.cling.invoker.mvnenc.DefaultEncryptInvoker.OK;

/**
 * The "decrypt" goal.
 */
@Singleton
@Named("decrypt")
public class Decrypt implements Goal {
    private final SecDispatcher secDispatcher;

    @Inject
    public Decrypt(SecDispatcher secDispatcher) {
        this.secDispatcher = secDispatcher;
    }

    @Override
    public int execute(DefaultEncryptInvoker.LocalContext context) throws Exception {
        String encrypted = context.reader.readLine("Enter the password to decrypt: ");
        System.out.println(secDispatcher.decrypt(encrypted));
        return OK;
    }
}
