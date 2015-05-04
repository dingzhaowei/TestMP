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

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.testmp.webconsole.model.Settings.AutomationSettings;
import org.testmp.webconsole.model.Settings.FilterSettings;
import org.testmp.webconsole.model.Settings.MailboxSettings;
import org.testmp.webconsole.model.Settings.ReportSettings;

public class User {

    private String userName;

    private String password;

    private FilterSettings filterSettings = new FilterSettings();

    private ReportSettings tmrReportSettings = new ReportSettings();

    private ReportSettings esrReportSettings = new ReportSettings();

    private MailboxSettings mailboxSettings = new MailboxSettings();

    private AutomationSettings automationSettings = new AutomationSettings();

    public User() {

    }

    public User(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public FilterSettings getFilterSettings() {
        return filterSettings;
    }

    public void setFilterSettings(FilterSettings filterSettings) {
        this.filterSettings = filterSettings;
    }

    public ReportSettings getTmrReportSettings() {
        return tmrReportSettings;
    }

    public void setTmrReportSettings(ReportSettings reportSettings) {
        this.tmrReportSettings = reportSettings;
    }

    public ReportSettings getEsrReportSettings() {
        return esrReportSettings;
    }

    public void setEsrReportSettings(ReportSettings reportSettings) {
        this.esrReportSettings = reportSettings;
    }

    public MailboxSettings getMailboxSettings() {
        return mailboxSettings;
    }

    public void setMailboxSettings(MailboxSettings mailboxSettings) {
        this.mailboxSettings = mailboxSettings;
    }

    public AutomationSettings getAutomationSettings() {
        return automationSettings;
    }

    public void setAutomationSettings(AutomationSettings automationSettings) {
        this.automationSettings = automationSettings;
    }

    public Map<String, Object> toMap() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(this, new TypeReference<Map<String, Object>>() {
        });
    }

}
