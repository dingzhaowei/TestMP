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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.DataStoreClient;
import org.testmp.webconsole.model.User;

@SuppressWarnings("serial")
public class ServiceBase extends HttpServlet {

    protected String getRequestBody(HttpServletRequest req) throws UnsupportedEncodingException, IOException {
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
        return requestBody;
    }

    protected Map<String, Object> getDataSourceRequest(HttpServletRequest req) throws UnsupportedEncodingException,
            IOException {
        String requestBody = getRequestBody(req);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(requestBody, new TypeReference<Map<String, Object>>() {
        });
    }

    protected Object getSetting(String settingGroup, String settingName, String userName) throws Exception {
        Object value = null;
        if (userName != null) {
            String testEnvStoreUrl = (String) getServletContext().getAttribute("testEnvStoreUrl");
            DataStoreClient testEnvClient = new DataStoreClient(testEnvStoreUrl);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("userName", userName);
            List<DataInfo<User>> dataInfoList = testEnvClient.getData(User.class, new String[] { "User" }, properties);
            if (!dataInfoList.isEmpty()) {
                User user = dataInfoList.get(0).getData();
                String getSettingGroupMethodName = "get" + StringUtils.capitalize(settingGroup);
                Method getSettingGroupMethod = user.getClass().getMethod(getSettingGroupMethodName);
                if (getSettingGroupMethod != null) {
                    Object settingGroupObj = getSettingGroupMethod.invoke(user);
                    String getSettingMethodName = "get" + StringUtils.capitalize(settingName);
                    Method getSettingMethod = settingGroupObj.getClass().getMethod(getSettingMethodName);
                    if (getSettingMethod != null) {
                        value = getSettingMethod.invoke(settingGroupObj);
                    }
                }
            }
        }
        if (value == null) {
            value = getServletContext().getAttribute(settingName);
        }
        return value;
    }
}
