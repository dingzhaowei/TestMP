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

package org.testmp.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.DataStoreClient;
import org.testmp.datastore.client.DataStoreClientException;

public class TestData {

    private String name;

    private String parent;

    private Map<String, Object> properties;

    public static TestData get(String name) {
        DataStoreClient client = new DataStoreClient(TestConfig.getProperty("testDataStoreUrl"));

        HashMap<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put("name", name);
        String[] tags = new String[] { "TestData" };

        try {
            List<DataInfo<TestData>> dataInfoList = client.getData(TestData.class, tags, queryParams);
            TestData data = dataInfoList.size() == 0 ? null : dataInfoList.get(0).getData();
            return data.pullPropsFromParent();
        } catch (DataStoreClientException e) {
            return null;
        }
    }

    public static List<TestData> get(String[] includedTags) {
        DataStoreClient client = new DataStoreClient(TestConfig.getProperty("testDataStoreUrl"));

        List<String> tagList = new ArrayList<String>();
        tagList.add("TestData");
        for (String includedTag : includedTags) {
            tagList.add(includedTag);
        }
        String[] tags = tagList.toArray(new String[0]);

        try {
            List<DataInfo<TestData>> dataInfoList = client.getDataByTag(TestData.class, tags);
            List<TestData> dataList = new ArrayList<TestData>();
            for (DataInfo<TestData> dataInfo : dataInfoList) {
                TestData data = dataInfo.getData();
                dataList.add(data.pullPropsFromParent());
            }
            return dataList;
        } catch (DataStoreClientException e) {
            return new ArrayList<TestData>();
        }
    }

    public static List<TestData> get(String[] includedTags, String[] excludedTags) {
        DataStoreClient client = new DataStoreClient(TestConfig.getProperty("testDataStoreUrl"));

        List<String> tagList = new ArrayList<String>();
        tagList.add("TestData");
        for (String includedTag : includedTags) {
            tagList.add(includedTag);
        }
        String[] tags = tagList.toArray(new String[0]);

        try {
            List<DataInfo<TestData>> dataInfoList = client.getDataByTag(TestData.class, tags);
            List<TestData> dataList = new ArrayList<TestData>();
            for (DataInfo<TestData> dataInfo : dataInfoList) {
                List<String> curTags = dataInfo.getTags();

                boolean valid = true;
                for (String excludedTag : excludedTags) {
                    if (curTags.contains(excludedTag)) {
                        valid = false;
                        break;
                    }
                }

                if (valid) {
                    TestData data = dataInfo.getData();
                    data.pullPropsFromParent();
                    dataList.add(data);
                }
            }
            return dataList;
        } catch (DataStoreClientException e) {
            return new ArrayList<TestData>();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Object getProperty(String propName) {
        return properties.get(propName);
    }

    public <T> T convertTo(Class<T> type) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(properties);
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TestData pullPropsFromParent() {
        TestData curData = this;
        while (true) {
            String parent = curData.getParent();
            if (parent == null) {
                break;
            }

            TestData parentData = get(parent);
            if (parentData == null || parentData.getProperties() == null) {
                break;
            }

            Map<String, Object> parentProperties = parentData.getProperties();
            for (Map.Entry<String, Object> p : parentProperties.entrySet()) {
                if (!properties.containsKey(p.getKey())) {
                    properties.put(p.getKey(), p.getValue());
                }
            }
            curData = parentData;
        }
        return this;
    }
}
