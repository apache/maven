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
package org.codehaus.modello.plugin.velocity;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
import org.codehaus.modello.ModelloException;
import org.codehaus.modello.ModelloParameterConstants;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.model.Version;
import org.codehaus.modello.plugin.AbstractModelloGenerator;
import org.codehaus.plexus.util.io.CachingWriter;

public class VelocityGenerator extends AbstractModelloGenerator {
    public static final String VELOCITY_TEMPLATES = "modello.velocity.template";

    public static final String VELOCITY_PARAMETERS = "modello.velocity.parameters";

    @Override
    public void generate(Model model, Properties parameters) throws ModelloException {
        try {
            Map<String, String> params = (Map) Objects.requireNonNull(parameters.get(VELOCITY_PARAMETERS));
            String templates = getParameter(parameters, VELOCITY_TEMPLATES);
            String output = getParameter(parameters, ModelloParameterConstants.OUTPUT_DIRECTORY);

            Properties props = new Properties();
            props.put("resource.loader.file.path", getParameter(parameters, "basedir"));
            RuntimeInstance velocity = new RuntimeInstance();
            velocity.init(props);

            VelocityContext context = new VelocityContext();
            for (Map.Entry<Object, Object> prop : parameters.entrySet()) {
                context.put(prop.getKey().toString(), prop.getValue());
            }
            for (Map.Entry<String, String> prop : params.entrySet()) {
                context.put(prop.getKey(), prop.getValue());
            }
            Version version = new Version(getParameter(parameters, ModelloParameterConstants.VERSION));
            context.put("version", version);
            context.put("model", model);
            context.put("Helper", new Helper(version));

            for (String templatePath : templates.split(",")) {
                Template template = velocity.getTemplate(templatePath);

                try (Writer w = new RedirectingWriter(Paths.get(output))) {
                    template.merge(context, w);
                }
            }
        } catch (Exception e) {
            throw new ModelloException("Unable to run velocity template", e);
        }
    }

    static class RedirectingWriter extends Writer {
        Path dir;
        StringBuilder sb = new StringBuilder();
        Writer current;

        RedirectingWriter(Path dir) {
            this.dir = dir;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            for (int i = 0; i < len; i++) {
                if (cbuf[off + i] == '\n') {
                    if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\r') {
                        sb.setLength(sb.length() - 1);
                    }
                    writeLine(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(cbuf[off + i]);
                }
            }
        }

        protected void writeLine(String line) throws IOException {
            if (line.startsWith("#MODELLO-VELOCITY#REDIRECT ")) {
                String file = line.substring("#MODELLO-VELOCITY#REDIRECT ".length());
                if (current != null) {
                    current.close();
                }
                Path out = dir.resolve(file);
                Files.createDirectories(out.getParent());
                current = new CachingWriter(out, StandardCharsets.UTF_8);
            } else if (current != null) {
                current.write(line);
                current.write("\n");
            }
        }

        @Override
        public void flush() throws IOException {
            if (current != null) {
                current.flush();
            }
        }

        @Override
        public void close() throws IOException {
            if (current != null) {
                current.close();
                current = null;
            }
        }
    }
}
