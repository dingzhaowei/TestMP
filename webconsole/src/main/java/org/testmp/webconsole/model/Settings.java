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

        private String testMetricsReportRecipients;

        private String testMetricsReportSubject;

        private String dataAnalyticsReportRecipients;

        private String dataAnalyticsReportSubjet;

        private String envStatusReportRecipients;

        private String envStatusReportSubject;

        public String getTestMetricsReportRecipients() {
            return testMetricsReportRecipients;
        }

        public void setTestMetricsReportRecipients(String tmrRecipients) {
            this.testMetricsReportRecipients = tmrRecipients;
        }

        public String getTestMetricsReportSubject() {
            return testMetricsReportSubject;
        }

        public void setTestMetricsReportSubject(String tmrSubject) {
            this.testMetricsReportSubject = tmrSubject;
        }

        public String getDataAnalyticsReportRecipients() {
            return dataAnalyticsReportRecipients;
        }

        public void setDataAnalyticsReportRecipients(String darRecipients) {
            this.dataAnalyticsReportRecipients = darRecipients;
        }

        public String getDataAnalyticsReportSubjet() {
            return dataAnalyticsReportSubjet;
        }

        public void setDataAnalyticsReportSubjet(String darSubjet) {
            this.dataAnalyticsReportSubjet = darSubjet;
        }

        public String getEnvStatusReportRecipients() {
            return envStatusReportRecipients;
        }

        public void setEnvStatusReportRecipients(String esrRecipients) {
            this.envStatusReportRecipients = esrRecipients;
        }

        public String getEnvStatusReportSubject() {
            return envStatusReportSubject;
        }

        public void setEnvStatusReportSubject(String esrSubject) {
            this.envStatusReportSubject = esrSubject;
        }

    }

    public static class MailboxSettings {

        private String smtpSettingUser;

        private String smtpSettingHost;

        private Integer smtpSettingPort;

        private boolean smtpSettingSTARTTLS;

        public String getSmtpSettingUser() {
            return smtpSettingUser;
        }

        public void setSmtpSettingUser(String smtpSettingUser) {
            this.smtpSettingUser = smtpSettingUser;
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

        public boolean isSmtpSettingSTARTTLS() {
            return smtpSettingSTARTTLS;
        }

        public void setSmtpSettingSTARTTLS(boolean smtpSettingSTARTTLS) {
            this.smtpSettingSTARTTLS = smtpSettingSTARTTLS;
        }

    }

}
