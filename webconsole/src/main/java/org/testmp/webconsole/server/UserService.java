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
import org.testmp.webconsole.model.Settings.FilterSettings;
import org.testmp.webconsole.model.User;

@SuppressWarnings("serial")
public class UserService extends HttpServlet {

    private static Logger log = Logger.getLogger(UserService.class);

    private ObjectMapper mapper = new ObjectMapper();

    private DataStoreClient client;

    @Override
    public void init() throws ServletException {
        client = new DataStoreClient((String) getServletContext().getAttribute("testEnvStoreUrl"));
        super.init();
    }

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

        Map<String, Object> dsRequest = mapper.readValue(requestBody, new TypeReference<Map<String, Object>>() {
        });
        ObjectNode dsResponse = mapper.createObjectNode();
        ObjectNode responseBody = dsResponse.putObject("response");
        String dataSource = dsRequest.get("dataSource").toString();

        try {
            if (dataSource.equals("userNameDS")) {
                processRequestForUserName(dsRequest, responseBody);
            } else if (dataSource.endsWith("FilterDS")) {
                String filterType = capitailize(dataSource.substring(0, dataSource.length() - 8));
                processRequestForFilter(dsRequest, responseBody, filterType);
            } else if (dataSource.endsWith("SettingsDS")) {
                String settingType = capitailize(dataSource.substring(0, dataSource.length() - 10));
                processRequestForSettings(dsRequest, responseBody, settingType);
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

    @SuppressWarnings("unchecked")
    public void processRequestForUserName(Map<String, Object> dsRequest, ObjectNode responseBody) throws Exception {
        String operationType = dsRequest.get("operationType").toString();
        if (operationType.equals("fetch")) {
            List<String> nameList = client.getPropertyValues("name", "User");
            List<Object> dataList = new ArrayList<Object>();
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
    }

    @SuppressWarnings("unchecked")
    public void processRequestForFilter(Map<String, Object> dsRequest, ObjectNode responseBody, String filterType)
            throws Exception {
        Map<String, Object> data = (Map<String, Object>) dsRequest.get("data");
        String operationType = dsRequest.get("operationType").toString();
        if (operationType.equals("fetch")) {
            User user = getUser(data.get("userName").toString());
            FilterSettings filterSettigs = user.getFilterSettings();
            if (data.containsKey("isDefault")) {
                String filterName = filterSettigs.getDefaultFilter(filterType);
                if (filterName != null && !filterName.isEmpty()) {
                    responseBody.put("data", filterSettigs.getSavedFilters(filterType).get(filterName));
                } else {
                    responseBody.put("data", "");
                }
            } else if (data.containsKey("filterName")) {
                String filterName = data.get("filterName").toString();
                if (filterSettigs.getSavedFilters(filterType).containsKey(filterName)) {
                    responseBody.put("data", filterSettigs.getSavedFilters(filterType).get(filterName));
                } else {
                    responseBody.put("data", "");
                }
            } else {
                List<Object> dataList = getFilters(user, filterType);
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

    private List<Object> getFilters(User user, String type) {
        List<Object> dataList = new ArrayList<Object>();
        FilterSettings filterSettings = user.getFilterSettings();
        for (Map.Entry<String, String> f : filterSettings.getSavedFilters(type).entrySet()) {
            Map<String, Object> filterInfo = new HashMap<String, Object>();
            filterInfo.put("userName", user.getName());
            String filterName = f.getKey();
            String criteria = f.getValue();
            boolean isDefault = filterName.equals(filterSettings.getDefaultFilter(type));
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
        FilterSettings filterSettings = user.getFilterSettings();

        Map<String, String> savedFilters = filterSettings.getSavedFilters(type);
        savedFilters.put(filterName, criteria);
        filterSettings.setSavedFilters(type, savedFilters);
        if (isDefault.equalsIgnoreCase("true")) {
            filterSettings.setDefaultFilter(type, filterName);
        }
        user.setFilterSettings(filterSettings);
        client.addPropertyToData(userId, "filterSettings", filterSettings);
    }

    private void removeFilter(Map<String, Object> filterInfo, String type) throws DataStoreClientException {
        String userName = filterInfo.get("userName").toString();
        String filterName = filterInfo.get("filterName").toString();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", userName);
        DataInfo<User> userInfo = client.getDataByProperty(User.class, properties).get(0);
        Integer userId = userInfo.getId();
        User user = userInfo.getData();
        FilterSettings filterSettings = user.getFilterSettings();

        Map<String, String> savedFilters = filterSettings.getSavedFilters(type);
        savedFilters.remove(filterName);
        filterSettings.setSavedFilters(type, savedFilters);
        if (filterName.equals(filterSettings.getDefaultFilter(type))) {
            filterSettings.setDefaultFilter(type, null);
        }
        client.addPropertyToData(userId, "filterSettings", filterSettings);
    }

    @SuppressWarnings("unchecked")
    private void processRequestForSettings(Map<String, Object> dsRequest, ObjectNode responseBody, String settingType)
            throws Exception {
        Map<String, Object> data = (Map<String, Object>) dsRequest.get("data");
        String operationType = dsRequest.get("operationType").toString();

        if (settingType.equalsIgnoreCase("user")) {
            if (operationType.equals("fetch")) {
                User user = getUser(data.get("userName").toString());
                List<Object> dataList = new ArrayList<Object>();
                dataList.add(user.getUserSettings());
                populateResponseBody(responseBody, dataList);
            } else if (operationType.equals("add")) {

            }
        } else if (settingType.equalsIgnoreCase("tmrReport")) {

        } else if (settingType.equalsIgnoreCase("darReport")) {

        } else if (settingType.equalsIgnoreCase("esrReport")) {

        } else if (settingType.equalsIgnoreCase("mailbox")) {

        }
    }

    private User getUser(String userName) throws DataStoreClientException {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", userName);
        DataInfo<User> userInfo = client.getData(User.class, new String[] { "User" }, properties).get(0);
        User user = userInfo.getData();
        return user;
    }

    private void populateResponseBody(ObjectNode responseBody, List<Object> dataList) {
        responseBody.put("status", 0);
        responseBody.put("startRow", 0);
        responseBody.put("endRow", dataList.size());
        responseBody.put("totalRows", dataList.size());
        try {
            JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(dataList));
            responseBody.put("data", dataNode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String capitailize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

}
