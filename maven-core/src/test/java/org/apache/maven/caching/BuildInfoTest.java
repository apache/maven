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

import com.google.common.collect.Lists;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.caching.hash.HashFactory;
import org.apache.maven.caching.xml.domain.ArtifactType;
import org.apache.maven.caching.xml.domain.BuildInfoType;
import org.apache.maven.caching.xml.domain.CompletedExecutionType;
import org.apache.maven.caching.xml.domain.DigestItemType;
import org.apache.maven.caching.xml.domain.ProjectsInputInfoType;
import org.apache.maven.caching.xml.domain.PropertyValueType;
import org.apache.maven.caching.xml.BuildInfo;
import org.apache.maven.caching.xml.XmlService;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class BuildInfoTest {

    @Test
    public void name() throws Exception {

        XmlService xmlService = new XmlService();

        ProjectsInputInfoType main = new ProjectsInputInfoType();
        main.setChecksum("dependencyChecksum");
        main.getItem().add(createItem("pom", "<project><modelVersion>4.0.0</modelVersion></project>", "hash1"));
        main.getItem().add(createItem("file", Paths.get(".").toString(), "hash2"));

        ArtifactType artifact = new ArtifactType();
        artifact.setGroupId("g");
        artifact.setArtifactId("a");
        artifact.setType("t");
        artifact.setClassifier("c");
        artifact.setScope("s");
        artifact.setFileName("f");
        artifact.setFileSize(123456);
        artifact.setFileHash("456L");

        BuildInfoType buildInfo = new BuildInfoType();
        buildInfo.setCacheImplementationVersion("cacheImplementationVersion");
        buildInfo.setBuildServer("server");
        buildInfo.setBuildTime(new Date());
        buildInfo.setArtifact(artifact);
        buildInfo.setHashFunction("SHA-256");
        buildInfo.setGoals(Lists.newArrayList("install"));
        final Artifact attachedArtifact = new DefaultArtifact("ag", "aa", "av", "as", "at", "ac", new DefaultArtifactHandler());
        buildInfo.setAttachedArtifacts(BuildInfo.createAttachedArtifacts(Lists.newArrayList(attachedArtifact), HashFactory.XX.createAlgorithm()));
        buildInfo.setProjectsInputInfo(main);
        buildInfo.setExecutions(createExecutions());

        byte[] bytes = xmlService.toBytes(buildInfo);
        System.out.println(new String(bytes, StandardCharsets.UTF_8));
        Path tempFilePath = Files.createTempFile("test", "test");
        File file = tempFilePath.toFile();
        file.deleteOnExit();
        Files.write(tempFilePath, bytes);

        BuildInfoType buildInfo1 = xmlService.fromFile(BuildInfoType.class, file);
        System.out.println(buildInfo1);
    }

    private List<CompletedExecutionType> createExecutions() {
        CompletedExecutionType execution = new CompletedExecutionType();
        execution.setExecutionKey("execkey");
        PropertyValueType property = new PropertyValueType();
        property.setValue("value");
        property.setName("key");
        execution.setConfiguration(new ArrayList<>(Arrays.asList(property)));
        return new ArrayList<>(Arrays.asList(execution));
    }

    private DigestItemType createItem(String pom, String s, String hash1) {
        final DigestItemType d1 = new DigestItemType();
        d1.setType(pom);
        d1.setHash(s);
        d1.setValue(hash1);
        return d1;
    }
}