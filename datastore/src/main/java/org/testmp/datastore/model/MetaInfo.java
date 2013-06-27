/*
 * TestMP (Test Management Platform)
 * Copyright 2013 and beyond, Zhaowei Ding.
 *
 * TestMP is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License (MIT).
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */

package org.testmp.datastore.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

@SuppressWarnings("serial")
public class MetaInfo implements Serializable {

    private Integer dataId;

    private Map<String, Object> metaInfo = new HashMap<String, Object>();

    public MetaInfo() {

    }

    public MetaInfo(Integer dataId, Map<String, Object> metaInfo) {
        this.dataId = dataId;
        this.metaInfo = metaInfo;
    }

    public Integer getDataId() {
        return dataId;
    }

    public void setDataId(Integer dataId) {
        this.dataId = dataId;
    }

    public Map<String, Object> getMetaInfo() {
        return metaInfo;
    }

    public void addMetaInfo(String key, Object value) {
        metaInfo.put(key, value);
    }

    public void removeMetaInfo(String key) {
        metaInfo.remove(key);
    }

    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
