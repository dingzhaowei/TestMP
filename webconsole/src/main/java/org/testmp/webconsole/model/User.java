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

package org.testmp.webconsole.model;

import java.util.Map;
import java.util.TreeMap;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class User {

    public static final String FILTER_TYPE_TESTCASE = "TestCase";

    public static final String FILTER_TYPE_TESTDATA = "TestData";

    public static final String FILTER_TYPE_TESTENV = "TestEnv";

    private String name;

    private String defaultTestCaseFilter;

    private Map<String, String> savedTestCaseFilters = new TreeMap<String, String>();

    private String defaultTestDataFilter;

    private Map<String, String> savedTestDataFilters = new TreeMap<String, String>();

    public User() {

    }

    public User(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefaultTestCaseFilter() {
        return defaultTestCaseFilter;
    }

    public void setDefaultTestCaseFilter(String defaultTestCaseFilter) {
        this.defaultTestCaseFilter = defaultTestCaseFilter;
    }

    public Map<String, String> getSavedTestCaseFilters() {
        return savedTestCaseFilters;
    }

    public void setSavedTestCaseFilters(Map<String, String> savedTestCaseFilters) {
        this.savedTestCaseFilters = savedTestCaseFilters;
    }

    public String getDefaultTestDataFilter() {
        return defaultTestDataFilter;
    }

    public void setDefaultTestDataFilter(String defaultTestDataFilter) {
        this.defaultTestDataFilter = defaultTestDataFilter;
    }

    public Map<String, String> getSavedTestDataFilters() {
        return savedTestDataFilters;
    }

    public void setSavedTestDataFilters(Map<String, String> savedTestDataFilters) {
        this.savedTestDataFilters = savedTestDataFilters;
    }

    public String getDefaultFilter(String type) {
        if (type.equalsIgnoreCase(FILTER_TYPE_TESTCASE)) {
            return getDefaultTestCaseFilter();
        }
        if (type.equalsIgnoreCase(FILTER_TYPE_TESTDATA)) {
            return getDefaultTestDataFilter();
        }
        return null;
    }

    public Map<String, String> getSavedFilters(String type) {
        if (type.equalsIgnoreCase(FILTER_TYPE_TESTCASE)) {
            return getSavedTestCaseFilters();
        }
        if (type.equalsIgnoreCase(FILTER_TYPE_TESTDATA)) {
            return getSavedTestDataFilters();
        }
        return null;
    }

    public Map<String, Object> toMap() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(this, new TypeReference<Map<String, Object>>() {
        });
    }

}
