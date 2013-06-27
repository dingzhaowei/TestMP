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
import java.util.HashSet;
import java.util.Set;

import org.codehaus.jackson.map.ObjectMapper;

@SuppressWarnings("serial")
public class Property implements Serializable {

    private Integer id;

    private String key;

    private Object value;

    private Set<Integer> relatedDataIds = new HashSet<Integer>();

    public Property() {

    }

    public Property(Integer id, String key, Object value) {
        this.id = id;
        this.key = key;
        this.value = value;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Set<Integer> getRelatedDataIds() {
        return relatedDataIds;
    }

    public void addRelatedDataId(int id) {
        relatedDataIds.add(id);
    }

    public void removeRelatedDataId(int id) {
        relatedDataIds.remove(id);
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
