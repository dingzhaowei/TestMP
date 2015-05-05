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

package org.testmp.webconsole.client;

import java.util.HashMap;
import java.util.Map;

import org.testmp.webconsole.shared.ClientConfig;
import org.testmp.webconsole.shared.ClientUtils;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceBooleanField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourcePasswordField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Side;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.IconButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

/**
 * Entry point for Web Console module.
 */
public class WebConsole implements EntryPoint {

    private Map<String, DataSource> dataSources;

    private IconButton logoutBtn;

    private IconButton settingBtn;

    private TabSet appTabSet;

    private VLayout loginView;

    private VLayout rootLayout;

    private boolean logined;

    /**
     * This is called when the browser loads Application.html.
     */
    public void onModuleLoad() {
        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            public void onUncaughtException(Throwable t) {
                System.err.println("--- UNCAUGHT EXCEPTION ---");
                t.printStackTrace();
            }
        });

        prepareDataSources();
        ClientConfig.currentUser = Cookies.getCookie("sid");
        Criteria criteria = new Criteria();
        criteria.setAttribute("sid", ClientConfig.currentUser);
        dataSources.get("userDS").fetchData(criteria, new DSCallback() {

            @Override
            public void execute(DSResponse dsResponse, Object data, DSRequest dsRequest) {
                logined = Boolean.parseBoolean(data.toString());
                initAppTabSet();
                initLoginView();
                initRootLayout();
            }

        });
    }

    private Canvas createHeader() {
        HLayout header = new HLayout();
        header.setWidth100();
        header.setHeight(50);
        header.setMargin(5);
        header.setBackgroundColor("lightBlue");
        header.setLayoutAlign(Alignment.CENTER);

        Label logo = new Label();
        logo.setIcon("testmp-logo.png");
        logo.setIconWidth(145);
        logo.setIconHeight(45);
        logo.setWidth("95%");
        logo.setValign(VerticalAlignment.TOP);
        logo.setLayoutAlign(Alignment.CENTER);
        header.addMember(logo);

        String logout = ClientConfig.messages.logout();
        String title = logined ? logout + " " + ClientConfig.currentUser : logout;
        logoutBtn = new IconButton(title);
        logoutBtn.setIcon("person.png");
        logoutBtn.setMargin(10);
        logoutBtn.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                ClientConfig.currentUser = null;
                Cookies.removeCookie("sid");
                afterLogout();
            }

        });
        logoutBtn.setVisible(logined);
        header.addMember(logoutBtn);

        settingBtn = new IconButton(ClientConfig.messages.settings());
        settingBtn.setIcon("setting.png");
        settingBtn.setMargin(10);
        settingBtn.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                SettingWindow window = new SettingWindow();
                window.show();
            }

        });
        settingBtn.setVisible(logined);
        header.addMember(settingBtn);
        return header;
    }

    private Canvas createFooter() {
        String copyright = "<span style=\"font-family:Arial;\">&copy;</span>2013-2015 Zhaowei Ding";
        String licenseLink = "http://opensource.org/licenses/MIT";
        String license = "Licensed under the <a href='" + licenseLink + "'>the MIT License</a>.";

        HTMLFlow footer = new HTMLFlow(copyright + ", " + license);
        footer.setWidth100();
        footer.setHeight(10);
        footer.setMargin(5);
        footer.setLayoutAlign(Alignment.CENTER);
        footer.setStyleName("footing");
        return footer;
    }

    private void initRootLayout() {
        rootLayout = new VLayout();
        rootLayout.setSize("100%", "100%");
        rootLayout.addMember(createHeader());
        rootLayout.addMember(loginView);
        rootLayout.addMember(appTabSet);
        rootLayout.addMember(createFooter());
        RootLayoutPanel.get().add(rootLayout);
    }

    private void initAppTabSet() {
        appTabSet = new TabSet();
        appTabSet.setTabBarPosition(Side.TOP);
        appTabSet.setTabBarAlign(Side.RIGHT);
        appTabSet.setWidth("95%");
        appTabSet.setLayoutAlign(Alignment.CENTER);
        appTabSet.setVisible(logined);

        Tab welcomeTab = new Tab(ClientConfig.messages.welcome());
        welcomeTab.setName("welcomeTab");
        welcomeTab.setIcon("welcome.png");
        welcomeTab.setWidth(120);
        welcomeTab.setPane(new MainPageView());
        appTabSet.addTab(welcomeTab);

        Tab testCaseTab = new Tab(ClientConfig.messages.testCase());
        testCaseTab.setName("testCaseTab");
        testCaseTab.setWidth(120);
        testCaseTab.setPane(new TestCaseView());
        appTabSet.addTab(testCaseTab);

        Tab testDataTab = new Tab(ClientConfig.messages.testData());
        testDataTab.setName("testDataTab");
        testDataTab.setWidth(120);
        testDataTab.setPane(new TestDataView());
        appTabSet.addTab(testDataTab);

        Tab testEnvTab = new Tab(ClientConfig.messages.testEnvironment());
        testEnvTab.setName("testEnvTab");
        testEnvTab.setWidth(120);
        testEnvTab.setPane(new TestEnvView());
        appTabSet.addTab(testEnvTab);
    }

    private void initLoginView() {
        final ComboBoxItem userNameItem = new ComboBoxItem("userName", ClientConfig.messages.user());
        final PasswordItem passwordItem = new PasswordItem("password", ClientConfig.messages.password());
        SubmitItem submitItem = new SubmitItem("login", ClientConfig.messages.login());

        userNameItem.setDisplayField("userName");
        submitItem.setAlign(Alignment.CENTER);
        submitItem.setColSpan(2);

        final DynamicForm loginForm = new DynamicForm();
        loginForm.addSubmitValuesHandler(new SubmitValuesHandler() {

            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                String userName = userNameItem.getValueAsString();
                String password = passwordItem.getValueAsString();
                Criteria criteria = new Criteria();
                criteria.setAttribute("userName", userName);
                criteria.setAttribute("password", password);
                loginForm.fetchData(criteria, new DSCallback() {

                    @Override
                    public void execute(DSResponse dsResponse, Object data, DSRequest dsRequest) {
                        if (dsResponse.getStatus() == DSResponse.STATUS_SUCCESS) {
                            afterLogin(data.toString());
                        }
                    }

                });
            }

        });
        loginForm.setCellPadding(10);
        loginForm.setSize("300", "150");
        loginForm.setBorder("1px dotted lightblue");
        loginForm.setLayoutAlign(Alignment.CENTER);
        loginForm.setDataSource(dataSources.get("userDS"));
        loginForm.setFields(userNameItem, passwordItem, submitItem);

        loginView = new VLayout();
        loginView.setAlign(Alignment.CENTER);
        loginView.addMember(loginForm);
        loginView.setVisible(!logined);
    }

    private void afterLogin(String user) {
        logined = true;
        ClientConfig.currentUser = user;
        Cookies.setCookie("sid", ClientConfig.currentUser);

        String logout = ClientConfig.messages.logout();
        logoutBtn.setTitle(logout + " " + ClientConfig.currentUser);
        logoutBtn.setVisible(true);
        settingBtn.setVisible(true);
        loginView.setVisible(false);
        appTabSet.setVisible(true);
    }

    private void afterLogout() {
        logined = false;
        ClientConfig.currentUser = null;
        Cookies.removeCookie("sid");

        com.google.gwt.user.client.Window.Location.reload();
    }

    private void prepareDataSources() {
        dataSources = new HashMap<String, DataSource>();

        DataSource userSource = ClientUtils.createDataSource("userDS", ClientConfig.constants.userService());
        DataSourceTextField userNameField = new DataSourceTextField("userName");
        DataSourceTextField passwordField = new DataSourceTextField("password");
        userNameField.setRequired(true);
        userNameField.setPrimaryKey(true);
        passwordField.setRequired(true);
        userSource.setFields(userNameField, passwordField);
        dataSources.put("userDS", userSource);

        DataSource tmrReportSettingsSource = ClientUtils.createDataSource("tmrReportSettingsDS",
                ClientConfig.constants.userService());
        DataSourceTextField tmrRecipientsField = new DataSourceTextField("tmrRecipients",
                ClientConfig.messages.recipients());
        DataSourceTextField tmrSubjectField = new DataSourceTextField("tmrSubject", ClientConfig.messages.subject());
        DataSourceTextField tmrHiddenField = new DataSourceTextField("userName");
        tmrHiddenField.setHidden(true);
        tmrReportSettingsSource.setFields(tmrHiddenField, tmrRecipientsField, tmrSubjectField);
        dataSources.put("tmrReportSettingsDS", tmrReportSettingsSource);

        DataSource esrReportSettingsSource = ClientUtils.createDataSource("esrReportSettingsDS",
                ClientConfig.constants.userService());
        DataSourceTextField esrRecipientsField = new DataSourceTextField("esrRecipients",
                ClientConfig.messages.recipients());
        DataSourceTextField esrSubjectField = new DataSourceTextField("esrSubject", ClientConfig.messages.subject());
        DataSourceTextField esrHiddenField = new DataSourceTextField("userName");
        esrHiddenField.setHidden(true);
        esrReportSettingsSource.setFields(esrHiddenField, esrRecipientsField, esrSubjectField);
        dataSources.put("esrReportSettingsDS", esrReportSettingsSource);

        DataSource mailboxSettingsSource = ClientUtils.createDataSource("mailboxSettingsDS",
                ClientConfig.constants.userService());
        DataSourceTextField smtpSettingUserField = new DataSourceTextField("smtpSettingUser",
                ClientConfig.messages.user());
        DataSourcePasswordField smtpSettingPassField = new DataSourcePasswordField("smtpSettingPass",
                ClientConfig.messages.password());
        DataSourceTextField smtpSettingHostField = new DataSourceTextField("smtpSettingHost",
                ClientConfig.messages.smtpHost());
        DataSourceIntegerField smtpSettingPortField = new DataSourceIntegerField("smtpSettingPort",
                ClientConfig.messages.smtpPort());
        DataSourceBooleanField smtpSettingSTARTTLSField = new DataSourceBooleanField("smtpSettingSTARTTLS",
                ClientConfig.messages.useStarttls());
        DataSourceTextField mailboxHiddenField = new DataSourceTextField("userName");
        mailboxHiddenField.setHidden(true);
        mailboxSettingsSource.setFields(mailboxHiddenField, smtpSettingUserField, smtpSettingPassField,
                smtpSettingHostField, smtpSettingPortField, smtpSettingSTARTTLSField);
        dataSources.put("mailboxSettingsDS", mailboxSettingsSource);

        DataSource automationSettingsSource = ClientUtils.createDataSource("automationSettingsDS",
                ClientConfig.constants.userService());
        DataSourceTextField automationServiceUrlField = new DataSourceTextField("automationServiceUrl",
                ClientConfig.messages.serviceUrl());
        automationSettingsSource.setFields(automationServiceUrlField);
        dataSources.put("automationSettingsDS", automationSettingsSource);
    }

    public class SettingWindow extends Window {

        private Map<String, DynamicForm> forms = new HashMap<String, DynamicForm>();

        public SettingWindow() {
            setWidth(500);
            setHeight(300);
            setTitle(ClientConfig.messages.settings());
            ClientUtils.unifySimpleWindowStyle(this);

            VLayout layout = new VLayout();
            ClientUtils.unifyWindowLayoutStyle(layout);
            addItem(layout);

            TabSet settingsTabSet = new TabSet();
            settingsTabSet.setWidth("99%");
            layout.addMember(settingsTabSet);

            Tab reportSettingsTab = new Tab();
            reportSettingsTab.setTitle(ClientConfig.messages.report());
            reportSettingsTab.setPane(createReportSettingsForm());
            settingsTabSet.addTab(reportSettingsTab);

            Tab mailboxSettingsTab = new Tab();
            mailboxSettingsTab.setTitle(ClientConfig.messages.mailbox());
            mailboxSettingsTab.setPane(createMailboxSettingsForm());
            settingsTabSet.addTab(mailboxSettingsTab);

            Tab automationSettingsTab = new Tab();
            automationSettingsTab.setTitle(ClientConfig.messages.automation());
            automationSettingsTab.setPane(createAutomationSettingsForm());
            settingsTabSet.addTab(automationSettingsTab);

            HLayout controls = new HLayout();
            ClientUtils.unifyControlsLayoutStyle(controls);
            layout.addMember(controls);

            IButton okButton = new IButton(ClientConfig.messages.ok());
            okButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    for (String formName : forms.keySet()) {
                        DynamicForm form = forms.get(formName);
                        if (form.getValues().isEmpty()) {
                            continue;
                        }
                        Record record = form.getValuesAsRecord();
                        dataSources.get(formName.replace("Form", "DS")).updateData(record);
                    }
                    SettingWindow.this.destroy();
                }

            });
            controls.addMember(okButton);

            IButton cancelButton = new IButton(ClientConfig.messages.cancel());
            cancelButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    SettingWindow.this.destroy();
                }

            });
            controls.addMember(cancelButton);
        }

        private Canvas createReportSettingsForm() {
            VLayout layout = new VLayout();
            layout.setWidth100();
            layout.setMargin(5);
            layout.setMembersMargin(10);

            DynamicForm tmrForm = new DynamicForm();
            forms.put("tmrReportSettingsForm", tmrForm);
            TextItem tmrRecipients = new TextItem("tmrRecipients", ClientConfig.messages.recipients());
            tmrRecipients.setWidth(200);
            TextItem tmrSubject = new TextItem("tmrSubject", ClientConfig.messages.subject());
            tmrSubject.setWidth(200);
            tmrForm.setFields(tmrRecipients, tmrSubject);
            tmrForm.setGroupTitle(ClientConfig.messages.testMetricsReport());
            tmrForm.setIsGroup(true);
            tmrForm.setSize("80%", "33%");
            tmrForm.setLayoutAlign(Alignment.CENTER);
            tmrForm.setDataSource(dataSources.get("tmrReportSettingsDS"));
            tmrForm.setAutoFetchData(true);
            layout.addMember(tmrForm);

            DynamicForm esrForm = new DynamicForm();
            forms.put("esrReportSettingsForm", esrForm);
            TextItem esrRecipients = new TextItem("esrRecipients", ClientConfig.messages.recipients());
            esrRecipients.setWidth(200);
            TextItem esrSubject = new TextItem("esrSubject", ClientConfig.messages.subject());
            esrSubject.setWidth(200);
            esrForm.setFields(esrRecipients, esrSubject);
            esrForm.setGroupTitle(ClientConfig.messages.environmentStatusReport());
            esrForm.setIsGroup(true);
            esrForm.setSize("80%", "33%");
            esrForm.setLayoutAlign(Alignment.CENTER);
            esrForm.setDataSource(dataSources.get("esrReportSettingsDS"));
            esrForm.setAutoFetchData(true);
            layout.addMember(esrForm);

            return layout;
        }

        private Canvas createMailboxSettingsForm() {
            VLayout layout = new VLayout();
            layout.setWidth100();
            layout.setMargin(5);
            layout.setMembersMargin(5);

            DynamicForm mailboxForm = new DynamicForm();
            forms.put("mailboxSettingsForm", mailboxForm);
            mailboxForm.setMargin(5);
            mailboxForm.setWidth("90%");
            mailboxForm.setDataSource(dataSources.get("mailboxSettingsDS"));
            mailboxForm.setAutoFetchData(true);
            layout.addMember(mailboxForm);

            return layout;
        }

        private Canvas createAutomationSettingsForm() {
            VLayout layout = new VLayout();
            layout.setWidth100();
            layout.setMargin(5);
            layout.setMembersMargin(5);

            DynamicForm automationForm = new DynamicForm();
            forms.put("automationSettingsForm", automationForm);
            TextItem automationServiceUrl = new TextItem("automationServiceUrl", ClientConfig.messages.serviceUrl());
            automationServiceUrl.setWidth(300);
            automationForm.setFields(automationServiceUrl);
            automationForm.setMargin(5);
            automationForm.setWidth("90%");
            automationForm.setDataSource(dataSources.get("automationSettingsDS"));
            automationForm.setAutoFetchData(true);
            layout.addMember(automationForm);

            return layout;
        }
    }
}
