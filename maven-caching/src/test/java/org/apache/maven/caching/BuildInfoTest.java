package org.apache.maven.caching;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.caching.hash.HashFactory;
import org.apache.maven.caching.xml.Build;
import org.apache.maven.caching.xml.build.Artifact;
import org.apache.maven.caching.xml.build.CompletedExecution;
import org.apache.maven.caching.xml.build.DigestItem;
import org.apache.maven.caching.xml.build.ProjectsInputInfo;
import org.apache.maven.caching.xml.build.PropertyValue;
import org.apache.maven.caching.xml.XmlService;
import org.junit.jupiter.api.Test;

public class BuildInfoTest {

    @Test
    public void name() throws Exception {

        XmlService xmlService = new XmlService();

        ProjectsInputInfo main = new ProjectsInputInfo();
        main.setChecksum("dependencyChecksum");
        main.addItem(createItem("pom", "<project><modelVersion>4.0.0</modelVersion></project>", "hash1"));
        main.addItem(createItem("file", Paths.get(".").toString(), "hash2"));

        Artifact artifact = new Artifact();
        artifact.setGroupId("g");
        artifact.setArtifactId("a");
        artifact.setType("t");
        artifact.setClassifier("c");
        artifact.setScope("s");
        artifact.setFileName("f");
        artifact.setFileSize(123456);
        artifact.setFileHash("456L");

        org.apache.maven.caching.xml.build.Build buildInfo = new org.apache.maven.caching.xml.build.Build();
        buildInfo.setCacheImplementationVersion("cacheImplementationVersion");
        buildInfo.setBuildServer("server");
        buildInfo.setBuildTime(new Date());
        buildInfo.setArtifact(artifact);
        buildInfo.setHashFunction("SHA-256");
        buildInfo.setGoals(Lists.newArrayList("install"));
        final org.apache.maven.artifact.Artifact attachedArtifact = new DefaultArtifact("ag", "aa", "av", "as", "at", "ac", new DefaultArtifactHandler());
        buildInfo.setAttachedArtifacts(Build.createAttachedArtifacts(Lists.newArrayList(attachedArtifact), HashFactory.XX.createAlgorithm()));
        buildInfo.setProjectsInputInfo(main);
        buildInfo.setExecutions(createExecutions());

        byte[] bytes = xmlService.toBytes(buildInfo);
        System.out.println(new String(bytes, StandardCharsets.UTF_8));
        Path tempFilePath = Files.createTempFile("test", "test");
        File file = tempFilePath.toFile();
        file.deleteOnExit();
        Files.write(tempFilePath, bytes);

        org.apache.maven.caching.xml.build.Build buildInfo1 = xmlService.loadBuild(file);
        System.out.println(buildInfo1);
    }

    private List<CompletedExecution> createExecutions() {
        CompletedExecution execution = new CompletedExecution();
        execution.setExecutionKey("execkey");
        PropertyValue property = new PropertyValue();
        property.setValue("value");
        property.setName("key");
        execution.addProperty(property);
        return new ArrayList<>(Arrays.asList(execution));
    }

    private DigestItem createItem(String pom, String s, String hash1) {
        final DigestItem d1 = new DigestItem();
        d1.setType(pom);
        d1.setHash(s);
        d1.setValue(hash1);
        return d1;
    }
}