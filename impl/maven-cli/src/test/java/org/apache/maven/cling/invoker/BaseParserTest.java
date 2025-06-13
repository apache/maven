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

import java.nio.file.Path;
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
                subject.parseInvocation(ParserRequest.mvn(Arrays.asList("-e", "-X"), mock(MessageBuilderFactory.class))
                        .cwd(Path.of(System.getProperty("userDir")))
                        .userHome(Path.of(System.getProperty("userHome")))
                        .build());

        Assertions.assertTrue(invokerRequest.options().isPresent());
        Options options = invokerRequest.options().orElseThrow();
        Assertions.assertFalse(options.showVersion().orElse(false));
        Assertions.assertFalse(options.showVersionAndExit().orElse(false));
        Assertions.assertTrue(options.showErrors().orElse(false));
        Assertions.assertTrue(options.verbose().orElse(false));

        // user home
        Assertions.assertTrue(invokerRequest.userProperties().containsKey("user.property"));
        Assertions.assertEquals("yes it is", invokerRequest.userProperties().get("user.property"));

        // maven installation
        Assertions.assertTrue(invokerRequest.userProperties().containsKey("maven.property"));
        Assertions.assertEquals("yes it is", invokerRequest.userProperties().get("maven.property"));
    }

    @Test
    void notHappy() {
        InvokerRequest invokerRequest = subject.parseInvocation(
                ParserRequest.mvn(Arrays.asList("--what-is-this-option", "-X"), mock(MessageBuilderFactory.class))
                        .cwd(Path.of(System.getProperty("userDir")))
                        .userHome(Path.of(System.getProperty("userHome")))
                        .build());

        Assertions.assertFalse(invokerRequest.options().isPresent());
        Assertions.assertTrue(invokerRequest.parsingFailed());
    }

    @Test
    void specials() {
        InvokerRequest invokerRequest = subject.parseInvocation(ParserRequest.mvn(
                        Arrays.asList("-Dfoo=${session.rootDirectory}", "-X"), mock(MessageBuilderFactory.class))
                .cwd(Path.of(System.getProperty("userDir")))
                .userHome(Path.of(System.getProperty("userHome")))
                .build());

        Assertions.assertTrue(invokerRequest.options().isPresent());
        Assertions.assertTrue(invokerRequest.userProperties().containsKey("foo"));
        Assertions.assertNotEquals(
                "${session.rootDirectory}", invokerRequest.userProperties().get("foo"));
        Assertions.assertFalse(invokerRequest.userProperties().get("foo").trim().isEmpty());
    }
}
