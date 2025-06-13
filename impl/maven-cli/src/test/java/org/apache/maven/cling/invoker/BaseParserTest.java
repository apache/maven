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
package org.apache.maven.cling.invoker;

import java.util.Arrays;

import org.apache.commons.cli.ParseException;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

public class BaseParserTest {
    private final BaseParser subject = new BaseParser() {
        @Override
        protected Options parseCliOptions(LocalContext context) {
            try {
                return CommonsCliOptions.parse(
                        "test", context.parserRequest.args().toArray(new String[0]));
            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
        }
    };

    @Test
    void happy() {
        InvokerRequest invokerRequest =
                subject.parseInvocation(ParserRequest.mvn(Arrays.asList("-v", "-X"), mock(MessageBuilderFactory.class))
                        .build());

        Assertions.assertTrue(invokerRequest.options().isPresent());
        Options options = invokerRequest.options().orElseThrow();
        Assertions.assertTrue(options.showVersionAndExit().orElse(false));
        Assertions.assertTrue(options.verbose().orElse(false));
    }

    @Test
    void notHappy() {
        InvokerRequest invokerRequest = subject.parseInvocation(
                ParserRequest.mvn(Arrays.asList("--what-is-this-option", "-X"), mock(MessageBuilderFactory.class))
                        .build());

        Assertions.assertFalse(invokerRequest.options().isPresent());
        Assertions.assertTrue(invokerRequest.parsingFailed());
    }

    @Test
    void specials() {
        InvokerRequest invokerRequest = subject.parseInvocation(ParserRequest.mvn(
                        Arrays.asList("-Dfoo=${session.rootDirectory}", "-X"), mock(MessageBuilderFactory.class))
                .build());

        Assertions.assertTrue(invokerRequest.options().isPresent());
        Assertions.assertNotEquals(
                "${session.rootDirectory}", invokerRequest.userProperties().get("foo"));
    }
}
