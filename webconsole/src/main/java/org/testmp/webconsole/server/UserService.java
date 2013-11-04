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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;
import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.DataStoreClient;
import org.testmp.datastore.client.DataStoreClientException;
import org.testmp.webconsole.model.User;

@SuppressWarnings("serial")
public class UserService extends HttpServlet {

    private static Logger log = Logger.getLogger(ReportService.class);

    private DataStoreClient client;

    @Override
    public void init() throws ServletException {
        client = new DataStoreClient((String) getServletContext().getAttribute("testEnvStoreUrl"));
        super.init();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(req.getInputStream(), "ISO-8859-1"));
        StringBuilder sb = new StringBuilder();

        while (true) {
            String line = input.readLine();
            if (line == null) {
                break;
            }
            sb.append(line).append('\n');
        }

        String requestBody = new String(sb.toString().getBytes("ISO-8859-1"), "UTF-8");
        log("Received POST request: " + requestBody);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> dsRequest = mapper.readValue(requestBody, new TypeReference<Map<String, Object>>() {
        });

        ObjectNode dsResponse = mapper.createObjectNode();
        ObjectNode responseBody = dsResponse.putObject("response");
        String dataSource = dsRequest.get("dataSource").toString();
        String operationType = dsRequest.get("operationType").toString();
        try {
            if (dataSource.equals("userNameDS")) {
                if (operationType.equals("fetch")) {
                    List<String> nameList = client.getPropertyValues("name", "User");
                    List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
                    for (String name : nameList) {
                        Map<String, Object> data = new HashMap<String, Object>();
                        data.put("name", name);
                        dataList.add(data);
                    }
                    populateResponseBody(responseBody, dataList);
                } else if (operationType.equals("add")) {
                    Map<String, Object> data = (Map<String, Object>) dsRequest.get("data");
                    String name = data.get("name").toString();
                    DataInfo<User> userInfo = new DataInfo<User>();
                    userInfo.setData(new User(name));
                    userInfo.setTags(Arrays.asList(new String[] { "User" }));
                    Integer id = client.addData(userInfo).get(0);
                    User addedUser = client.getDataById(User.class, id).getData();
                    responseBody.put("status", 0);
                    JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(addedUser));
                    responseBody.put("data", dataNode);
                }
            } else if (dataSource.endsWith("FilterDS")) {
                String filterType = capitailize(dataSource.substring(0, dataSource.length() - 8));
                Map<String, Object> data = (Map<String, Object>) dsRequest.get("data");

                if (operationType.equals("fetch")) {
                    User user = getUser(data.get("userName").toString());
                    if (data.containsKey("isDefault")) {
                        String filterName = user.getDefaultTestCaseFilter();
                        if (filterName != null && !filterName.isEmpty()) {
                            responseBody.put("data", user.getSavedTestCaseFilters().get(filterName));
                        } else {
                            responseBody.put("data", "");
                        }
                    } else if (data.containsKey("filterName")) {
                        String filterName = data.get("filterName").toString();
                        if (user.getSavedTestCaseFilters().containsKey(filterName)) {
                            responseBody.put("data", user.getSavedTestCaseFilters().get(filterName));
                        } else {
                            responseBody.put("data", "");
                        }
                    } else {
                        List<Map<String, Object>> dataList = getFilters(user, filterType);
                        populateResponseBody(responseBody, dataList);
                    }
                } else if (operationType.equals("add")) {
                    addFilter(data, filterType);
                    responseBody.put("status", 0);
                    JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(data));
                    responseBody.put("data", dataNode);
                } else if (operationType.equals("remove")) {
                    removeFilter(data, filterType);
                    responseBody.put("status", 0);
                    JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(data));
                    responseBody.put("data", dataNode);
                }
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

    private User getUser(String userName) throws DataStoreClientException {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", userName);
        DataInfo<User> userInfo = client.getData(User.class, new String[] { "User" }, properties).get(0);
        User user = userInfo.getData();
        return user;
    }

    private void populateResponseBody(ObjectNode responseBody, List<Map<String, Object>> dataList) {
        responseBody.put("status", 0);
        responseBody.put("startRow", 0);
        responseBody.put("endRow", dataList.size());
        responseBody.put("totalRows", dataList.size());
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(dataList));
            responseBody.put("data", dataNode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Map<String, Object>> getFilters(User user, String type) {
        List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, String> f : user.getSavedFilters(type).entrySet()) {
            Map<String, Object> filterInfo = new HashMap<String, Object>();
            filterInfo.put("userName", user.getName());
            String filterName = f.getKey();
            String criteria = f.getValue();
            boolean isDefault = filterName.equals(user.getDefaultFilter(type));
            filterInfo.put("filterName", filterName);
            filterInfo.put("criteria", criteria);
            filterInfo.put("isDefault", String.valueOf(isDefault));
            dataList.add(filterInfo);
        }
        return dataList;
    }

    private void addFilter(Map<String, Object> filterInfo, String type) throws DataStoreClientException {
        String userName = filterInfo.get("userName").toString();
        String filterName = filterInfo.get("filterName").toString();
        String criteria = filterInfo.get("criteria").toString();
        String isDefault = filterInfo.get("isDefault").toString();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", userName);
        DataInfo<User> userInfo = client.getDataByProperty(User.class, properties).get(0);
        Integer userId = userInfo.getId();
        User user = userInfo.getData();

        Map<String, String> savedFilters = user.getSavedFilters(type);
        savedFilters.put(filterName, criteria);
        client.addPropertyToData(userId, "saved" + type + "Filters", savedFilters);
        if (isDefault.equalsIgnoreCase("true")) {
            client.addPropertyToData(userId, "default" + type + "Filter", filterName);
        }
    }

    private void removeFilter(Map<String, Object> filterInfo, String type) throws DataStoreClientException {
        String userName = filterInfo.get("userName").toString();
        String filterName = filterInfo.get("filterName").toString();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", userName);
        DataInfo<User> userInfo = client.getDataByProperty(User.class, properties).get(0);
        Integer userId = userInfo.getId();
        User user = userInfo.getData();

        Map<String, String> savedFilters = user.getSavedFilters(type);
        savedFilters.remove(filterName);
        client.addPropertyToData(userId, "saved" + type + "Filters", savedFilters);
    }

    private String capitailize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

}
