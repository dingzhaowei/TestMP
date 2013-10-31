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

                List<String> nameList = client.getPropertyValues("name", "Person");

                if (operationType.equals("fetch")) {
                    List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
                    for (String name : nameList) {
                        Map<String, Object> data = new HashMap<String, Object>();
                        data.put("name", name);
                        dataList.add(data);
                    }
                    responseBody.put("status", 0);
                    responseBody.put("startRow", 0);
                    responseBody.put("endRow", dataList.size());
                    responseBody.put("totalRows", dataList.size());
                    JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(dataList));
                    responseBody.put("data", dataNode);
                } else if (operationType.equals("add")) {
                    Map<String, Object> data = (Map<String, Object>) dsRequest.get("data");
                    String name = data.get("name").toString();
                    if (!nameList.contains(name)) {
                        DataInfo<User> userInfo = new DataInfo<User>();
                        userInfo.setData(new User(name));
                        userInfo.setTags(Arrays.asList(new String[] { "Person" }));
                        client.addData(userInfo);
                    }
                    responseBody.put("status", 0);
                    JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(data));
                    responseBody.put("data", dataNode);
                }
            } else if (dataSource.equals("userFilterDS")) {
                if (operationType.equals("fetch")) {

                } else if (operationType.equals("add")) {
                    Map<String, Object> data = (Map<String, Object>) dsRequest.get("data");
                    String filterName = data.get("filterName").toString();
                    String criteria = data.get("criteria").toString();
                    String isDefault = data.get("isDefault").toString();
                    String userName = data.get("userName").toString();

                    Map<String, Object> properties = new HashMap<String, Object>();
                    properties.put("name", userName);
                    DataInfo<User> userInfo = client.getDataByProperty(User.class, properties).get(0);
                    User user = userInfo.getData();

                    Map<String, String> savedFilters = user.getSavedFilters();
                    savedFilters.put(filterName, criteria);
                    user.setSavedFilters(savedFilters);

                    if (isDefault.equalsIgnoreCase("true")) {
                        user.setDefaultFilter(criteria);
                    }

                    userInfo.setData(user);
                    client.addData(userInfo);
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

}
