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
package org.example;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.services.SettingsBuilderRequest;
import org.apache.maven.api.services.SettingsBuilderResult;
import org.apache.maven.api.services.ToolchainsBuilderRequest;
import org.apache.maven.api.services.ToolchainsBuilderResult;
import org.apache.maven.eventspy.EventSpy;

@Named("simple")
@Singleton
public class SimpleEventSpy implements EventSpy {
    private final List<Object> events = new ArrayList<>();

    @Override
    public void init(Context context) throws Exception {
        System.out.println("Initializing Simple Event Spy");
    }

    @Override
    public void onEvent(Object o) throws Exception {
        events.add(o);
    }

    @Override
    public void close() throws Exception {
        System.out.println("Closing Simple Event Spy, checking events");
        checkEvent(SettingsBuilderRequest.class);
        checkEvent(SettingsBuilderResult.class);
        checkEvent(ToolchainsBuilderRequest.class);
        checkEvent(ToolchainsBuilderResult.class);
    }

    private void checkEvent(Class<?> clazz) {
        if (!events.stream().anyMatch(e -> clazz.isAssignableFrom(e.getClass()))) {
            System.out.println(clazz.getSimpleName() + " event is absent");
        } else {
            System.out.println(clazz.getSimpleName() + " event is present");
        }
    }
}
