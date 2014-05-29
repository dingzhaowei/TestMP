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

public class Settings {

    public static class UserSettings {

        private String fullName;

        private String email;

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

    }

    public static class FilterSettings {

        private String defaultTestCaseFilter;

        private String defaultTestDataFilter;

        private Map<String, String> savedTestCaseFilters = new TreeMap<String, String>();

        private Map<String, String> savedTestDataFilters = new TreeMap<String, String>();

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
            if (type.equalsIgnoreCase("TestCase")) {
                return getDefaultTestCaseFilter();
            }
            if (type.equalsIgnoreCase("TestData")) {
                return getDefaultTestDataFilter();
            }
            return null;
        }

        public void setDefaultFilter(String type, String filterName) {
            if (type.equalsIgnoreCase("TestCase")) {
                setDefaultTestCaseFilter(filterName);
            } else if (type.equalsIgnoreCase("TestData")) {
                setDefaultTestDataFilter(filterName);
            }
        }

        public Map<String, String> getSavedFilters(String type) {
            if (type.equalsIgnoreCase("TestCase")) {
                return getSavedTestCaseFilters();
            }
            if (type.equalsIgnoreCase("TestData")) {
                return getSavedTestDataFilters();
            }
            return null;
        }

        public void setSavedFilters(String type, Map<String, String> savedFilters) {
            if (type.equalsIgnoreCase("TestCase")) {
                setSavedTestCaseFilters(savedFilters);
            } else if (type.equalsIgnoreCase("TestData")) {
                setSavedTestDataFilters(savedFilters);
            }
        }

    }

    public static class ReportSettings {

        private String recipients;

        private String subject;

        public String getRecipients() {
            return recipients;
        }

        public void setRecipients(String recipients) {
            this.recipients = recipients;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }
    }

    public static class MailboxSettings {

        private String smtpSettingUser;

        private String smtpSettingPass;

        private String smtpSettingHost;

        private Integer smtpSettingPort;

        private Boolean smtpSettingSTARTTLS;

        public String getSmtpSettingUser() {
            return smtpSettingUser;
        }

        public void setSmtpSettingUser(String smtpSettingUser) {
            this.smtpSettingUser = smtpSettingUser;
        }

        public String getSmtpSettingPass() {
            return smtpSettingPass;
        }

        public void setSmtpSettingPass(String smtpSettingPass) {
            this.smtpSettingPass = smtpSettingPass;
        }

        public String getSmtpSettingHost() {
            return smtpSettingHost;
        }

        public void setSmtpSettingHost(String smtpSettingHost) {
            this.smtpSettingHost = smtpSettingHost;
        }

        public Integer getSmtpSettingPort() {
            return smtpSettingPort;
        }

        public void setSmtpSettingPort(Integer smtpSettingPort) {
            this.smtpSettingPort = smtpSettingPort;
        }

        public Boolean getSmtpSettingSTARTTLS() {
            return smtpSettingSTARTTLS;
        }

        public void setSmtpSettingSTARTTLS(Boolean smtpSettingSTARTTLS) {
            this.smtpSettingSTARTTLS = smtpSettingSTARTTLS;
        }

    }

    public static class AutomationSettings {

        private String automationServiceUrl;

        public String getAutomationServiceUrl() {
            return automationServiceUrl;
        }

        public void setAutomationServiceUrl(String url) {
            this.automationServiceUrl = url;
        }

    }
}
