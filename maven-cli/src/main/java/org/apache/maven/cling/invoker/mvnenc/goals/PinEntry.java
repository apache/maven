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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * Inspired by <a href="https://velvetcache.org/2023/03/26/a-peek-inside-pinentry/">A peek inside pinentry</a>
 */
public class PinEntry {
    public enum Outcome {
        SUCCESS,
        TIMEOUT,
        NOT_CONFIRMED,
        CANCELED,
        FAILED;
    }

    public record Result<T>(Outcome outcome, T payload) {}

    private final String cmd;
    private final LinkedHashMap<String, String> commands;

    public PinEntry(String cmd) {
        this.cmd = requireNonNull(cmd);
        this.commands = new LinkedHashMap<>();
    }

    public PinEntry setKeyInfo(String keyInfo) {
        requireNonNull(keyInfo);
        commands.put("SETKEYINFO", keyInfo);
        commands.put("OPTION", "allow-external-password-cache");
        return this;
    }

    public PinEntry setOk(String msg) {
        requireNonNull(msg);
        commands.put("SETOK", msg);
        return this;
    }

    public PinEntry setCancel(String msg) {
        requireNonNull(msg);
        commands.put("SETCANCEL", msg);
        return this;
    }

    public PinEntry setTitle(String title) {
        requireNonNull(title);
        commands.put("SETTITLE", title);
        return this;
    }

    public PinEntry setDescription(String desc) {
        requireNonNull(desc);
        commands.put("SETDESC", desc);
        return this;
    }

    public PinEntry setPrompt(String prompt) {
        requireNonNull(prompt);
        commands.put("SETPROMPT", prompt);
        return this;
    }

    public PinEntry confirmPin() {
        commands.put("SETREPEAT", cmd);
        return this;
    }

    public PinEntry setTimeout(Duration timeout) {
        long seconds = timeout.toSeconds();
        if (seconds < 0) {
            throw new IllegalArgumentException("Set timeout is 0 seconds");
        }
        commands.put("SETTIMEOUT", String.valueOf(seconds));
        return this;
    }

    public Result<String> getPin() throws IOException {
        commands.put("GETPIN", null);
        return execute();
    }

    public Result<String> confirm() throws IOException {
        commands.put("CONFIRM", null);
        return execute();
    }

    public Result<String> message() throws IOException {
        commands.put("MESSAGE", null);
        return execute();
    }

    private Result<String> execute() throws IOException {
        Process process = new ProcessBuilder(cmd).start();
        BufferedReader reader = process.inputReader();
        BufferedWriter writer = process.outputWriter();
        expectOK(process.inputReader());
        Map.Entry<String, String> lastEntry = commands.entrySet().iterator().next();
        for (Map.Entry<String, String> entry : commands.entrySet()) {
            String cmd;
            if (entry.getValue() != null) {
                cmd = entry.getKey() + " " + entry.getValue();
            } else {
                cmd = entry.getKey();
            }
            writer.write(cmd);
            writer.newLine();
            writer.flush();
            if (entry != lastEntry) {
                expectOK(reader);
            }
        }
        Result<String> result = lastExpect(reader);
        writer.write("BYE");
        writer.newLine();
        writer.flush();
        try {
            process.waitFor(5, TimeUnit.SECONDS);
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return new Result<>(Outcome.FAILED, "Exit code: " + exitCode);
            } else {
                return result;
            }
        } catch (Exception e) {
            return new Result<>(Outcome.FAILED, e.getMessage());
        }
    }

    private void expectOK(BufferedReader in) throws IOException {
        String response = in.readLine();
        if (!response.startsWith("OK")) {
            throw new IOException("Expected OK but got this instead: " + response);
        }
    }

    private Result<String> lastExpect(BufferedReader in) throws IOException {
        while (true) {
            String response = in.readLine();
            if (response.startsWith("#")) {
                continue;
            }
            if (response.startsWith("S")) {
                continue;
            }
            if (response.startsWith("ERR")) {
                if (response.contains("83886142")) {
                    return new Result<>(Outcome.TIMEOUT, response);
                }
                if (response.contains("83886179")) {
                    return new Result<>(Outcome.CANCELED, response);
                }
                if (response.contains("83886194")) {
                    return new Result<>(Outcome.NOT_CONFIRMED, response);
                }
            }
            if (response.startsWith("D")) {
                return new Result<>(Outcome.SUCCESS, response.substring(2));
            }
            if (response.startsWith("OK")) {
                return new Result<>(Outcome.SUCCESS, response);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Result<String> pinResult = new PinEntry("/usr/bin/pinentry-gnome3")
                .setTimeout(Duration.ofSeconds(5))
                .setKeyInfo("maven:masterPassword")
                .setTitle("Maven Master Password")
                .setDescription("Please enter the Maven master password")
                .setPrompt("Master password")
                .setOk("Her you go!")
                .setCancel("Uh oh, rather not")
                // .confirmPin() (will not let you through if you cannot type same thing twice)
                .getPin();
        if (pinResult.outcome() == Outcome.SUCCESS) {
            Result<String> confirmResult = new PinEntry("/usr/bin/pinentry-gnome3")
                    .setTitle("Password confirmation")
                    .setDescription("Please confirm that the password you entered is correct")
                    .setPrompt("Is the password '" + pinResult.payload() + "' the one you want?")
                    .confirm();
            if (confirmResult.outcome() == Outcome.SUCCESS) {
                new PinEntry("/usr/bin/pinentry-gnome3")
                        .setTitle("Password confirmed")
                        .setDescription("You confirmed your password")
                        .setPrompt("The password '" + pinResult.payload() + "' is confirmed.")
                        .confirm();
            } else {
                System.out.println(confirmResult);
            }
        } else {
            System.out.println(pinResult);
        }
    }
}
