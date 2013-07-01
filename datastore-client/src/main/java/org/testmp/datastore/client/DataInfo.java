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

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

public class DataInfo<T> {

    private Integer id;

    private List<String> tags;

    private T data;

    public static <T> DataInfo<T> valueOf(String s, Class<T> type) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode dataInfoNode = mapper.readTree(s);
            int id = dataInfoNode.get("id").getIntValue();
            JsonNode tagsNode = dataInfoNode.get("tags");
            ArrayList<String> tags = new ArrayList<String>();
            for (int i = 0; i < tagsNode.size(); i++) {
                tags.add(tagsNode.get(i).getTextValue());
            }
            T data = mapper.readValue(dataInfoNode.get("data").toString(), type);
            return new DataInfo<T>(id, tags, data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DataInfo() {

    }

    public DataInfo(Integer id, List<String> tags, T data) {
        super();
        this.id = id;
        this.tags = tags;
        this.data = data;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
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
