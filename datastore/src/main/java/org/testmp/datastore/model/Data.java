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
public class Data implements Serializable {

    private Integer id;

    private Set<Integer> relatedPropertyIds = new HashSet<Integer>();

    private Set<String> relatedTags = new HashSet<String>();

    public Data() {

    }

    public Data(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Set<Integer> getRelatedPropertyIds() {
        return relatedPropertyIds;
    }

    public void addRelatedPropertyId(Integer propertyId) {
        relatedPropertyIds.add(propertyId);
    }

    public void removeRelatedPropertyId(Integer propertyId) {
        relatedPropertyIds.remove(propertyId);
    }

    public Set<String> getRelatedTags() {
        return relatedTags;
    }

    public void addRelatedTag(String tag) {
        relatedTags.add(tag);
    }

    public void removeRelatedTag(String tag) {
        relatedTags.remove(tag);
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
