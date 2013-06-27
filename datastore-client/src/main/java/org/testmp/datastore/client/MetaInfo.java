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

package org.testmp.datastore.client;

import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

public class MetaInfo {

    public static final String CREATE_TIME = "create_time";

    public static final String LAST_MODIFY_TIME = "last_modify_time";

    private Integer dataId;

    private Map<String, Object> metaInfo;

    public MetaInfo() {

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

    public void setMetaInfo(Map<String, Object> metaInfo) {
        this.metaInfo = metaInfo;
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
