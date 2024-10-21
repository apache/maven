///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 14+
//DEPS guru.nidi:graphviz-java:0.18.1
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

import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.*;
import guru.nidi.graphviz.parse.Parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import static guru.nidi.graphviz.model.Factory.*;

public class ReactorGraph {
    private static final LinkedHashMap<String, Pattern> CLUSTER_PATTERNS = new LinkedHashMap<>();
    static {
        CLUSTER_PATTERNS.put("JLine", Pattern.compile("^org\\.jline:.*"));
        CLUSTER_PATTERNS.put("Maven API", Pattern.compile("^org\\.apache\\.maven:maven-api-(?!impl).*"));
        CLUSTER_PATTERNS.put("Maven Resolver", Pattern.compile("^org\\.apache\\.maven\\.resolver:.*"));
        CLUSTER_PATTERNS.put("Maven Implementation", Pattern.compile("^org\\.apache\\.maven:maven-(api-impl|di|core|cli|xml-impl|jline|logging):.*"));
        CLUSTER_PATTERNS.put("Maven Compatibility", Pattern.compile("^org\\.apache\\.maven:maven-(artifact|builder-support|compat|embedder|model|model-builder|plugin-api|repository-metadata|resolver-provider|settings|settings-builder|toolchain-builder|toolchain-model):.*"));
        CLUSTER_PATTERNS.put("Sisu", Pattern.compile("(^org\\.eclipse\\.sisu:.*)|(.*:guice:.*)|(.*:javax.inject:.*)|(.*:javax.annotation-api:.*)"));
        CLUSTER_PATTERNS.put("Plexus", Pattern.compile("^org\\.codehaus\\.plexus:.*"));
        CLUSTER_PATTERNS.put("XML Parsing", Pattern.compile("(.*:woodstox-core:.*)|(.*:stax2-api:.*)"));
        CLUSTER_PATTERNS.put("Wagon", Pattern.compile("^org\\.apache\\.maven\\.wagon:.*"));
        CLUSTER_PATTERNS.put("SLF4j", Pattern.compile("^org\\.slf4j:.*"));
        CLUSTER_PATTERNS.put("Commons", Pattern.compile("^commons-cli:.*"));
    }
    private static final Pattern HIDDEN_NODES = Pattern.compile(".*:(maven-docgen|roaster-api|roaster-jdt|velocity-engine-core|commons-lang3|asm|logback-classic|slf4j-simple):.*");

    public static void main(String[] args) {
        try {
            // Parse DOT file
            MutableGraph originalGraph = new Parser().read(new File("target/graph/reactor-graph.dot"));

            // Create final graph
            MutableGraph clusteredGraph = mutGraph("G").setDirected(true);
            clusteredGraph.graphAttrs().add(GraphAttr.COMPOUND);
            clusteredGraph.graphAttrs().add(Label.of("Reactor Graph"));

            // Create clusters
            Map<String, MutableGraph> clusters = new HashMap<>();
            for (String clusterName : CLUSTER_PATTERNS.keySet()) {
                String key = "cluster_" + clusterName.replaceAll("\\s+", "");
                MutableGraph cluster = mutGraph(key).setDirected(true);
                cluster.graphAttrs().add(Label.of(clusterName));
                clusters.put(clusterName, cluster);
                clusteredGraph.add(cluster);
            }

            // Map to store new nodes by node name
            Map<String, MutableNode> nodeMap = new HashMap<>();
            Map<String, String> nodeToCluster = new HashMap<>();
            Map<String, String> newNames = new HashMap<>();

            // First pass: Create nodes and organize them into clusters
            for (MutableNode originalNode : originalGraph.nodes()) {
                String oldNodeName = originalNode.name().toString();
                if (HIDDEN_NODES.matcher(oldNodeName).matches()) {
                    continue;
                }
                String nodeName = oldNodeName;
                if (originalNode.get("label") instanceof Label l) {
                    nodeName = l.value();
                }
                MutableNode newNode = mutNode(nodeName);
                nodeMap.put(nodeName, newNode);
                newNames.put(oldNodeName, nodeName);

                boolean added = false;
                for (Map.Entry<String, Pattern> entry : CLUSTER_PATTERNS.entrySet()) {
                    if (entry.getValue().matcher(oldNodeName).matches()) {
                        clusters.get(entry.getKey()).add(newNode);
                        nodeToCluster.put(nodeName, entry.getKey());
                        added = true;
                        break;
                    }
                }

                if (!added) {
                    clusteredGraph.add(newNode);
                }
            }

            // Second pass: Add links to the clustered graph
            Map<String, MutableNode> substitutes = new HashMap<>();
            Set<Pair> existingLinks = new HashSet<>();
            for (MutableNode node : originalGraph.nodes()) {
                for (Link link : node.links()) {
                    String sourceName = newNames.get(link.from().name().toString());
                    String targetName = newNames.get(link.to().name().toString());
                    String sourceCluster = nodeToCluster.get(sourceName);
                    String targetCluster = nodeToCluster.get(targetName);
                    MutableNode sourceNode = nodeMap.get(sourceName);
                    MutableNode targetNode = nodeMap.get(targetName);
                    if (sourceNode != null && targetNode != null ) {
                        if (!Objects.equals(sourceCluster, targetCluster)) {
                            // Inter-cluster link
                            if (sourceCluster != null) {
                                sourceName = "cluster_" + sourceCluster.replaceAll("\\s+", "");
                            }
                            if (targetCluster != null) {
                                targetName = "cluster_" + targetCluster.replaceAll("\\s+", "");
                            }
                            sourceNode = substitutes.computeIfAbsent(sourceName, n -> createNode(n, clusteredGraph));
                            targetNode = substitutes.computeIfAbsent(targetName, n -> createNode(n, clusteredGraph));
                        }
                        if (existingLinks.add(new Pair(sourceName, targetName))) {
                            sourceNode.addLink(targetNode);
                        }
                    }
                }
            }

            // Write intermediary graph to DOT file
            String dotContent = Graphviz.fromGraph(clusteredGraph).render(Format.DOT).toString();
            Files.write(Paths.get("target/graph/intermediary_graph.dot"), dotContent.getBytes());
            System.out.println("Intermediary graph written to intermediary_graph.dot");

            // Render graph to SVF
            Graphviz.fromGraph(clusteredGraph)
                    .engine(Engine.FDP)
                    .render(Format.SVG).toFile(new File("target/graph/intermediary_graph.svg"));
            System.out.println("Final graph rendered to intermediary_graph.svg");

            // Generate and render the high-level graph
            MutableGraph highLevelGraph = generateHighLevelGraph(clusteredGraph, clusters, nodeToCluster, nodeMap);

            // Write high-level graph to DOT file
            String highLevelDotContent = Graphviz.fromGraph(highLevelGraph).render(Format.DOT).toString();
            Files.write(Paths.get("target/graph/high_level_graph.dot"), highLevelDotContent.getBytes());
            System.out.println("High-level graph written to high_level_graph.dot");

            // Render high-level graph to SVG
            Graphviz.fromGraph(highLevelGraph)
                    .engine(Engine.DOT)
                    .render(Format.SVG).toFile(new File("target/site/images/maven-deps.svg"));
            System.out.println("High-level graph rendered to high_level_graph.svg");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static MutableGraph generateHighLevelGraph(MutableGraph clusteredGraph, Map<String, MutableGraph> clusters,
                                                       Map<String, String> nodeToCluster, Map<String, MutableNode> nodeMap) {
        MutableGraph highLevelGraph = mutGraph("HighLevelGraph").setDirected(true);
        highLevelGraph.graphAttrs().add(GraphAttr.COMPOUND);
        highLevelGraph.graphAttrs().add(Label.of("High-Level Reactor Graph"));

        Map<String, MutableNode> highLevelNodes = new HashMap<>();

        // Create nodes for each cluster
        for (Map.Entry<String, MutableGraph> entry : clusters.entrySet()) {
            String key = entry.getKey();
            String clusterName = key.replaceAll("\\s+", "");
            MutableGraph cluster = entry.getValue();

            String headerColor = clusterName.startsWith("Maven") ? "black" : "#808080";  // #808080 is a middle gray
            StringBuilder labelBuilder = new StringBuilder();
            labelBuilder.append("<table border='0' cellborder='0' cellspacing='0'>");
            labelBuilder.append("<tr><td bgcolor='")
                    .append(headerColor)
                    .append("'><font color='white'>")
                    .append(key)
                    .append("</font></td></tr>");
            cluster.nodes().stream().map(MutableNode::name).map(Label::toString).sorted()
                    .forEach(nodeName -> labelBuilder.append("<tr><td>").append(nodeName).append("</td></tr>"));
            labelBuilder.append("</table>");

            MutableNode clusterNode = mutNode(clusterName).add(Label.html(labelBuilder.toString()))
                    .add("shape", "rectangle");
            highLevelNodes.put(clusterName, clusterNode);
            highLevelGraph.add(clusterNode);
        }

        // Add individual nodes for unclustered nodes
        for (MutableNode node : clusteredGraph.nodes()) {
            String nodeName = node.name().toString();
            if (!nodeToCluster.containsKey(nodeName) && !nodeName.startsWith("cluster_")) {
                throw new IllegalStateException("All nodes should be in a cluster: " + node.name());
            }
        }

        // Add edges
        Set<Pair> existingLinks = new HashSet<>();
        for (MutableNode node : clusteredGraph.nodes()) {
            String sourceName = node.name().toString().replace("cluster_", "");
            String sourceCluster = nodeToCluster.getOrDefault(sourceName, sourceName);

            for (Link link : node.links()) {
                String targetName = link.to().name().toString().replace("cluster_", "");
                String targetCluster = nodeToCluster.getOrDefault(targetName, targetName);

                Pair linkPair = new Pair(sourceCluster, targetCluster);
                if (existingLinks.add(linkPair)) {
                    MutableNode sourceNode = highLevelNodes.get(sourceCluster);
                    MutableNode targetNode = highLevelNodes.get(targetCluster);
                    if (sourceNode != null && targetNode != null && sourceNode != targetNode) {
                        sourceNode.addLink(targetNode);
                    }
                }
            }
        }

        return highLevelGraph;
    }

    private static MutableNode createNode(String n, MutableGraph clusteredGraph) {
        MutableNode t = mutNode(n);
        clusteredGraph.add(t);
        return t;
    }

    record Pair(String from, String to) {};
}