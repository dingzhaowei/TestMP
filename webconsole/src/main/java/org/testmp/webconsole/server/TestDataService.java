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

package org.testmp.webconsole.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;
import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.DataStoreClient;
import org.testmp.datastore.client.MetaInfo;
import org.testmp.sync.TestData;
import org.testmp.webconsole.server.Filter.Criteria;

@SuppressWarnings("serial")
public class TestDataService extends ServiceBase {

    private static Logger log = Logger.getLogger(TestDataService.class);

    private DataStoreClient client;

    private DataAssemblyStrategy strategy;

    private DataLoader<TestData> loader;

    @Override
    public void init() throws ServletException {
        String testDataStoreUrl = (String) getServletContext().getAttribute("testDataStoreUrl");
        client = new DataStoreClient(testDataStoreUrl);
        strategy = new TestDataAssemblyStrategy();
        loader = new DataLoader<TestData>(testDataStoreUrl, TestData.class, strategy);
        super.init();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestBody = getRequestBody(req);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> dsRequest = mapper.readValue(requestBody, new TypeReference<Map<String, Object>>() {
        });

        ObjectNode dsResponse = mapper.createObjectNode();
        ObjectNode responseBody = dsResponse.putObject("response");
        String dataSource = dsRequest.get("dataSource").toString();
        String operationType = dsRequest.get("operationType").toString();
        Map<String, Object> data = (Map<String, Object>) dsRequest.get("data");
        try {
            if (operationType.equals("fetch")) {
                if (dataSource.equals("testDataNameDS")) {
                    List<Map<String, Object>> dataList = getTestDataNames();
                    JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(dataList));
                    responseBody.put("data", dataNode);
                } else {
                    // TODO: filter by userName
                    data.remove("userName");
                    Criteria criteria = Criteria.valueOf(mapper.writeValueAsString(data));
                    List<String> sortBy = (List<String>) dsRequest.get("sortBy");
                    List<Map<String, Object>> dataList = getTestData(criteria, sortBy);
                    int startRow = Integer.parseInt(dsRequest.get("startRow").toString());
                    int endRow = Integer.parseInt(dsRequest.get("endRow").toString());
                    int actualEndRow = endRow > dataList.size() ? dataList.size() : endRow;
                    int totalRows = dataList.size();
                    dataList = dataList.subList(startRow, actualEndRow);
                    responseBody.put("status", 0);
                    responseBody.put("startRow", startRow);
                    responseBody.put("endRow", actualEndRow);
                    responseBody.put("totalRows", totalRows);
                    JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(dataList));
                    responseBody.put("data", dataNode);
                }
            } else if (operationType.equals("update")) {
                Map<String, Object> oldValues = (Map<String, Object>) dsRequest.get("oldValues");
                Map<String, Object> updatedData = updateTestData(data, oldValues);
                responseBody.put("status", 0);
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(updatedData));
                responseBody.put("data", dataNode);
            } else if (operationType.equals("add")) {
                Map<String, Object> addedData = addTestData(data);
                responseBody.put("status", 0);
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(addedData));
                responseBody.put("data", dataNode);
            } else if (operationType.equals("remove")) {
                Map<String, Object> removedData = removeTestData(data);
                responseBody.put("status", 0);
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(removedData));
                responseBody.put("data", dataNode);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            responseBody.put("status", -1);
            responseBody.put("data", e.getMessage());
        }

        resp.setCharacterEncoding("UTF-8");
        PrintWriter output = resp.getWriter();
        output.print(dsResponse.toString());
        output.flush();
    }

    private List<Map<String, Object>> getTestDataNames() throws Exception {
        List<String> names = client.getPropertyValues("name", "TestData");
        Collections.sort(names);
        List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
        for (String name : names) {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("name", name);
            dataList.add(data);
        }
        return dataList;
    }

    private List<Map<String, Object>> getTestData(Criteria criteria, final List<String> sortBy) throws Exception {
        List<Map<String, Object>> dataList = loader.load("TestData");
        if (criteria != null) {
            Filter filter = new Filter(criteria);
            dataList = filter.doFilter(dataList);
        }
        if (sortBy != null) {
            Collections.sort(dataList, new Comparator<Map<String, Object>>() {

                @Override
                public int compare(Map<String, Object> data1, Map<String, Object> data2) {
                    for (String fieldName : sortBy) {
                        boolean descending = fieldName.startsWith("-");
                        fieldName = descending ? fieldName.substring(1) : fieldName;
                        Object o1 = data1.get(fieldName);
                        Object o2 = data2.get(fieldName);
                        if (o1 == null || o2 == null || o1.equals(o2)) {
                            continue;
                        }
                        if (NumberUtils.isNumber(o1.toString())) {
                            Double d1 = Double.valueOf(o1.toString());
                            Double d2 = Double.valueOf(o2.toString());
                            return descending ? d2.compareTo(d1) : d1.compareTo(d2);
                        } else {
                            String s1 = o1.toString();
                            String s2 = o2.toString();
                            return descending ? s2.compareTo(s1) : s1.compareTo(s2);
                        }
                    }
                    return 0;
                }

            });
        }
        return dataList;
    }

    private Map<String, Object> updateTestData(Map<String, Object> data, Map<String, Object> oldValues)
            throws Exception {
        Integer id = (Integer) data.get("id");

        for (Object key : data.keySet()) {
            if (key.equals("tags")) {
                String[] oldTags = oldValues.get("tags").toString().split("\\s*,\\s*");
                Set<String> oldTagsSet = new HashSet<String>(Arrays.asList(oldTags));

                String[] newTags = data.get("tags").toString().split("\\s*,\\s*");
                Set<String> newTagsSet = new HashSet<String>(Arrays.asList(newTags));

                Set<String> tagsToDelete = new HashSet<String>(oldTagsSet);
                tagsToDelete.removeAll(newTagsSet);

                Set<String> tagsToAdd = new HashSet<String>(newTagsSet);
                tagsToAdd.removeAll(oldTagsSet);

                for (String tag : tagsToDelete) {
                    client.deleteTagFromData(id, tag);
                }

                for (String tag : tagsToAdd) {
                    client.addTagToData(id, tag);
                }
            } else if (key.equals("properties")) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> properties = mapper.readValue(data.get(key).toString(),
                        new TypeReference<Map<String, Object>>() {
                        });
                client.addPropertyToData(id, "properties", properties);
            } else if (key.equals("parent")) {
                String parent = (String) data.get("parent");
                if (parent != null) {
                    HashMap<String, Object> queryParams = new HashMap<String, Object>();
                    queryParams.put("name", parent);
                    if (client.findData(new String[] { "TestData" }, queryParams).size() == 0) {
                        throw new RuntimeException("No such parent named: " + parent);
                    }
                }
                client.addPropertyToData(id, "parent", parent);
            }
        }

        DataInfo<TestData> dataInfo = client.getDataById(TestData.class, id);
        MetaInfo metaInfo = client.getMetaInfo(id).get(0);
        Map<String, Object> updatedData = strategy.assemble(dataInfo, metaInfo);
        return updatedData;
    }

    private Map<String, Object> addTestData(Map<String, Object> data) throws Exception {
        String name = data.get("name").toString().trim();
        HashMap<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put("name", name);
        if (client.findData(new String[] { "TestData" }, queryParams).size() > 0) {
            throw new RuntimeException("Duplicate data name: " + name);
        }

        String parent = (String) data.get("parent");
        if (parent != null) {
            parent = parent.trim();
            queryParams = new HashMap<String, Object>();
            queryParams.put("name", parent);
            if (client.findData(new String[] { "TestData" }, queryParams).size() == 0) {
                throw new RuntimeException("No such parent named: " + parent);
            }
        }

        DataInfo<TestData> dataInfo = new DataInfo<TestData>();
        TestData td = new TestData();
        td.setName(name);
        td.setParent(parent);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> properties = mapper.readValue(data.get("properties").toString(),
                new TypeReference<Map<String, Object>>() {
                });
        td.setProperties(properties);
        dataInfo.setData(td);

        ArrayList<String> tags = new ArrayList<String>();
        tags.add("TestData");
        String s = (String) data.get("tags");
        if (s != null && !s.isEmpty()) {
            for (String t : s.trim().split(",")) {
                tags.add(t.trim());
            }
        }
        dataInfo.setTags(tags);

        Integer id = client.addData(dataInfo).get(0);
        dataInfo = client.getDataById(TestData.class, id);
        MetaInfo metaInfo = client.getMetaInfo(id).get(0);
        Map<String, Object> addedData = strategy.assemble(dataInfo, metaInfo);
        return addedData;
    }

    private Map<String, Object> removeTestData(Map<String, Object> data) throws Exception {
        Integer id = (Integer) data.get("id");
        if (client.deleteData(id)) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("id", id);
            return m;
        } else {
            throw new RuntimeException("Cannot remove the data");
        }
    }
}
