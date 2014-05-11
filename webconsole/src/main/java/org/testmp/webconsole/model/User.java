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
import org.testmp.webconsole.model.Settings.FilterSettings;
import org.testmp.webconsole.model.Settings.MailboxSettings;
import org.testmp.webconsole.model.Settings.ReportSettings;
import org.testmp.webconsole.model.Settings.UserSettings;

public class User {

    private String name;

    private UserSettings userSettings = new UserSettings();

    private FilterSettings filterSettings = new FilterSettings();

    private ReportSettings tmrReportSettings = new ReportSettings();

    private ReportSettings darReportSettings = new ReportSettings();

    private ReportSettings esrReportSettings = new ReportSettings();

    private MailboxSettings mailboxSettings = new MailboxSettings();

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

    public UserSettings getUserSettings() {
        return userSettings;
    }

    public void setUserSettings(UserSettings userSettings) {
        this.userSettings = userSettings;
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

    public ReportSettings getDarReportSettings() {
        return darReportSettings;
    }

    public void setDarReportSettings(ReportSettings reportSettings) {
        this.darReportSettings = reportSettings;
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

    public Map<String, Object> toMap() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(this, new TypeReference<Map<String, Object>>() {
        });
    }

}
