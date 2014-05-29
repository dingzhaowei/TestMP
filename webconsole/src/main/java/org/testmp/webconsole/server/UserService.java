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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;
import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.DataStoreClient;
import org.testmp.datastore.client.DataStoreClientException;
import org.testmp.webconsole.model.Settings.AutomationSettings;
import org.testmp.webconsole.model.Settings.FilterSettings;
import org.testmp.webconsole.model.Settings.MailboxSettings;
import org.testmp.webconsole.model.Settings.ReportSettings;
import org.testmp.webconsole.model.Settings.UserSettings;
import org.testmp.webconsole.model.User;

@SuppressWarnings("serial")
public class UserService extends ServiceBase {

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
        String requestBody = getRequestBody(req);
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
            Collections.sort(nameList);
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
            User user = getUserInfo(data.get("userName").toString()).getData();
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

    private void addFilter(Map<String, Object> data, String type) throws DataStoreClientException {
        String userName = data.get("userName").toString();
        String filterName = data.get("filterName").toString();
        String criteria = data.get("criteria").toString();
        String isDefault = data.get("isDefault").toString();

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

    private void removeFilter(Map<String, Object> data, String type) throws DataStoreClientException {
        String userName = data.get("userName").toString();
        String filterName = data.get("filterName").toString();

        DataInfo<User> userInfo = getUserInfo(userName);
        FilterSettings filterSettings = userInfo.getData().getFilterSettings();

        Map<String, String> savedFilters = filterSettings.getSavedFilters(type);
        savedFilters.remove(filterName);
        filterSettings.setSavedFilters(type, savedFilters);
        if (filterName.equals(filterSettings.getDefaultFilter(type))) {
            filterSettings.setDefaultFilter(type, null);
        }
        client.addPropertyToData(userInfo.getId(), "filterSettings", filterSettings);
    }

    @SuppressWarnings("unchecked")
    private void processRequestForSettings(Map<String, Object> dsRequest, ObjectNode responseBody, String settingType)
            throws Exception {
        Map<String, Object> data = (Map<String, Object>) dsRequest.get("data");
        String operationType = dsRequest.get("operationType").toString();

        if (settingType.equalsIgnoreCase("user")) {
            if (operationType.equals("fetch")) {
                String userName = data.get("userName").toString();
                List<Object> dataList = getUserSettings(userName);
                populateResponseBody(responseBody, dataList);
            } else if (operationType.equals("update")) {
                updateUserSettings(data);
                responseBody.put("status", 0);
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(data));
                responseBody.put("data", dataNode);
            }
        } else if (settingType.equalsIgnoreCase("tmrReport")) {
            if (operationType.equals("fetch")) {
                String userName = data.get("userName").toString();
                List<Object> dataList = getTmrReportSettings(userName);
                populateResponseBody(responseBody, dataList);
            } else if (operationType.equals("update")) {
                updateTmrReportSettings(data);
                responseBody.put("status", 0);
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(data));
                responseBody.put("data", dataNode);
            }
        } else if (settingType.equalsIgnoreCase("esrReport")) {
            if (operationType.equals("fetch")) {
                String userName = data.get("userName").toString();
                List<Object> dataList = getEsrReportSettings(userName);
                populateResponseBody(responseBody, dataList);
            } else if (operationType.equals("update")) {
                updateEsrReportSettings(data);
                responseBody.put("status", 0);
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(data));
                responseBody.put("data", dataNode);
            }
        } else if (settingType.equalsIgnoreCase("mailbox")) {
            if (operationType.equals("fetch")) {
                String userName = data.get("userName").toString();
                List<Object> dataList = getMailboxSettings(userName);
                populateResponseBody(responseBody, dataList);
            } else if (operationType.equals("update")) {
                updateMailboxSettings(data);
                responseBody.put("status", 0);
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(data));
                responseBody.put("data", dataNode);
            }
        } else if (settingType.equalsIgnoreCase("automation")) {
            if (operationType.equals("fetch")) {
                String userName = data.get("userName").toString();
                List<Object> dataList = getAutomationSettings(userName);
                populateResponseBody(responseBody, dataList);
            } else if (operationType.equals("update")) {
                updateAutomationSettings(data);
                responseBody.put("status", 0);
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(data));
                responseBody.put("data", dataNode);
            }
        }
    }

    private List<Object> getUserSettings(String userName) throws DataStoreClientException {
        User user = getUserInfo(userName).getData();
        List<Object> dataList = new ArrayList<Object>();
        UserSettings userSettings = user.getUserSettings();
        Map<String, Object> settings = new HashMap<String, Object>();
        settings.put("fullName", userSettings.getFullName());
        settings.put("email", userSettings.getEmail());
        settings.put("userName", user.getName());
        dataList.add(settings);
        return dataList;
    }

    private void updateUserSettings(Map<String, Object> data) throws DataStoreClientException {
        String userName = data.get("userName").toString();
        String fullName = (String) data.get("fullName");
        String email = (String) data.get("email");
        DataInfo<User> userInfo = getUserInfo(userName);
        UserSettings userSettings = userInfo.getData().getUserSettings();
        userSettings.setFullName(fullName);
        userSettings.setEmail(email);
        client.addPropertyToData(userInfo.getId(), "userSettings", userSettings);
    }

    private List<Object> getTmrReportSettings(String userName) throws DataStoreClientException {
        User user = getUserInfo(userName).getData();
        List<Object> dataList = new ArrayList<Object>();
        ReportSettings reportSettings = user.getTmrReportSettings();
        String recipients = reportSettings.getRecipients();
        if (StringUtils.isBlank(recipients)) {
            recipients = (String) getServletContext().getAttribute("testMetricsReportRecipients");
        }
        String subject = reportSettings.getSubject();
        if (StringUtils.isBlank(subject)) {
            subject = (String) getServletContext().getAttribute("testMetricsReportSubject");
        }
        Map<String, Object> settings = new HashMap<String, Object>();
        settings.put("tmrRecipients", recipients);
        settings.put("tmrSubject", subject);
        settings.put("userName", user.getName());
        dataList.add(settings);
        return dataList;
    }

    private void updateTmrReportSettings(Map<String, Object> data) throws DataStoreClientException {
        String userName = data.get("userName").toString();
        String tmrRecipients = (String) data.get("tmrRecipients");
        String tmrSubject = (String) data.get("tmrSubject");
        DataInfo<User> userInfo = getUserInfo(userName);
        ReportSettings reportSettings = userInfo.getData().getTmrReportSettings();
        reportSettings.setRecipients(tmrRecipients);
        reportSettings.setSubject(tmrSubject);
        client.addPropertyToData(userInfo.getId(), "tmrReportSettings", reportSettings);
    }

    private List<Object> getEsrReportSettings(String userName) throws DataStoreClientException {
        User user = getUserInfo(userName).getData();
        List<Object> dataList = new ArrayList<Object>();
        ReportSettings reportSettings = user.getEsrReportSettings();
        String recipients = reportSettings.getRecipients();
        if (StringUtils.isBlank(recipients)) {
            recipients = (String) getServletContext().getAttribute("envStatusReportRecipients");
        }
        String subject = reportSettings.getSubject();
        if (StringUtils.isBlank(subject)) {
            subject = (String) getServletContext().getAttribute("envStatusReportSubject");
        }
        Map<String, Object> settings = new HashMap<String, Object>();
        settings.put("esrRecipients", recipients);
        settings.put("esrSubject", subject);
        settings.put("userName", user.getName());
        dataList.add(settings);
        return dataList;
    }

    private void updateEsrReportSettings(Map<String, Object> data) throws DataStoreClientException {
        String userName = data.get("userName").toString();
        String esrRecipients = (String) data.get("esrRecipients");
        String esrSubject = (String) data.get("esrSubject");
        DataInfo<User> userInfo = getUserInfo(userName);
        ReportSettings reportSettings = userInfo.getData().getEsrReportSettings();
        reportSettings.setRecipients(esrRecipients);
        reportSettings.setSubject(esrSubject);
        client.addPropertyToData(userInfo.getId(), "esrReportSettings", reportSettings);
    }

    private List<Object> getMailboxSettings(String userName) throws DataStoreClientException {
        User user = getUserInfo(userName).getData();
        List<Object> dataList = new ArrayList<Object>();
        MailboxSettings mailboxSettings = user.getMailboxSettings();
        String smtpSettingHost = mailboxSettings.getSmtpSettingHost();
        if (StringUtils.isBlank(smtpSettingHost)) {
            smtpSettingHost = (String) getServletContext().getAttribute("smtpSettingHost");
        }
        Integer smtpSettingPort = mailboxSettings.getSmtpSettingPort();
        if (smtpSettingPort == null) {
            Object v = getServletContext().getAttribute("smtpSettingPort");
            smtpSettingPort = v == null ? 25 : Integer.valueOf(v.toString());
        }
        String smtpSettingUser = mailboxSettings.getSmtpSettingUser();
        if (smtpSettingUser == null) {
            smtpSettingUser = (String) getServletContext().getAttribute("smtpSettingUser");
        }
        String smtpSettingPass = mailboxSettings.getSmtpSettingPass();
        if (smtpSettingPass == null) {
            smtpSettingPass = (String) getServletContext().getAttribute("smtpSettingPass");
        }
        Boolean smtpSettingSTARTTLS = mailboxSettings.getSmtpSettingSTARTTLS();
        if (smtpSettingSTARTTLS == null) {
            Object v = getServletContext().getAttribute("smtpSettingSTARTTLS");
            smtpSettingSTARTTLS = v == null ? false : Boolean.valueOf(v.toString());
        }
        Map<String, Object> settings = new HashMap<String, Object>();
        settings.put("smtpSettingUser", smtpSettingUser);
        settings.put("smtpSettingPass", smtpSettingPass);
        settings.put("smtpSettingHost", smtpSettingHost);
        settings.put("smtpSettingPort", smtpSettingPort);
        settings.put("smtpSettingSTARTTLS", smtpSettingSTARTTLS);
        settings.put("userName", user.getName());
        dataList.add(settings);
        return dataList;
    }

    private void updateMailboxSettings(Map<String, Object> data) throws DataStoreClientException {
        String userName = data.get("userName").toString();
        DataInfo<User> userInfo = getUserInfo(userName);
        MailboxSettings mailboxSettings = userInfo.getData().getMailboxSettings();
        mailboxSettings.setSmtpSettingUser((String) data.get("smtpSettingUser"));
        mailboxSettings.setSmtpSettingPass((String) data.get("smtpSettingPass"));
        mailboxSettings.setSmtpSettingHost((String) data.get("smtpSettingHost"));
        try {
            String v = data.get("smtpSettingPort").toString();
            mailboxSettings.setSmtpSettingPort(Integer.valueOf(v));
        } catch (Exception e) {
            // ignore
        }
        try {
            String v = data.get("smtpSettingSTARTTLS").toString();
            mailboxSettings.setSmtpSettingSTARTTLS(Boolean.valueOf(v));
        } catch (Exception e) {
            // ignore
        }
        client.addPropertyToData(userInfo.getId(), "mailboxSettings", mailboxSettings);
    }

    private List<Object> getAutomationSettings(String userName) throws DataStoreClientException {
        User user = getUserInfo(userName).getData();
        List<Object> dataList = new ArrayList<Object>();
        AutomationSettings automationSettings = user.getAutomationSettings();
        String automationServiceUrl = automationSettings.getAutomationServiceUrl();
        if (StringUtils.isBlank(automationServiceUrl)) {
            automationServiceUrl = (String) getServletContext().getAttribute("automationServiceUrl");
        }
        Map<String, Object> settings = new HashMap<String, Object>();
        settings.put("automationServiceUrl", automationServiceUrl);
        settings.put("userName", user.getName());
        dataList.add(settings);
        return dataList;
    }

    private void updateAutomationSettings(Map<String, Object> data) throws DataStoreClientException {
        String userName = data.get("userName").toString();
        DataInfo<User> userInfo = getUserInfo(userName);
        AutomationSettings automationSettings = userInfo.getData().getAutomationSettings();
        automationSettings.setAutomationServiceUrl((String) data.get("automationServiceUrl"));
        client.addPropertyToData(userInfo.getId(), "automationSettings", automationSettings);
    }

    private DataInfo<User> getUserInfo(String userName) throws DataStoreClientException {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", userName);
        List<DataInfo<User>> userInfoList = client.getData(User.class, new String[] { "User" }, properties);
        return userInfoList.isEmpty() ? null : userInfoList.get(0);
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
