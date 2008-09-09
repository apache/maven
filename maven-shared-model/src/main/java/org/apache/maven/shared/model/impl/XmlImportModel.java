package org.apache.maven.shared.model.impl;

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

import org.apache.maven.shared.model.ImportModel;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.ModelMarshaller;

import java.util.List;
import java.io.InputStream;
import java.io.IOException;

public class XmlImportModel implements ImportModel {

    private String id;

    private List<ModelProperty> modelProperties;

    public XmlImportModel(String id, InputStream inputStream, String baseUri) throws IOException {
        if(id == null) {
            throw new IllegalArgumentException("id: null");
        }
        this.id = id;
        modelProperties = ModelMarshaller.marshallXmlToModelProperties(inputStream, baseUri, null);
    }

    public String getId() {
        return id;
    }

    public List<ModelProperty> getModelProperties() {
        return modelProperties;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XmlImportModel that = (XmlImportModel) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (modelProperties != null ? modelProperties.hashCode() : 0);
        return result;
    }
}
