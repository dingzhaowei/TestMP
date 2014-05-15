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
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.IconButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
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

    private IconButton loginBtn;

    private IconButton settingBtn;

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

        VLayout vLayout = new VLayout();
        vLayout.setMargin(5);
        vLayout.setLayoutMargin(5);
        vLayout.setAlign(Alignment.CENTER);
        vLayout.setSize("100%", "100%");

        HLayout header = new HLayout();
        header.setWidth("95%");
        header.setHeight(72);
        header.setMembersMargin(5);
        header.setLayoutAlign(Alignment.CENTER);

        Label logo = new Label();
        logo.setIcon("testmp-logo.png");
        logo.setIconWidth(265);
        logo.setIconHeight(82);
        logo.setWidth("95%");
        logo.setValign(VerticalAlignment.TOP);
        logo.setLayoutAlign(Alignment.CENTER);
        header.addMember(logo);

        ClientConfig.currentUser = Cookies.getCookie(ClientConfig.constants.currentUserCookie());
        if (ClientConfig.currentUser == null) {
            loginBtn = new IconButton(ClientConfig.messages.login());
        } else {
            loginBtn = new IconButton(ClientConfig.messages.hi() + ", " + ClientConfig.currentUser);
        }
        loginBtn.setIcon("person.png");
        loginBtn.setLayoutAlign(VerticalAlignment.TOP);
        loginBtn.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                LoginWindow window = new LoginWindow();
                window.show();
            }

        });
        header.addMember(loginBtn);

        settingBtn = new IconButton(ClientConfig.messages.settings());
        settingBtn.setIcon("setting.png");
        settingBtn.setLayoutAlign(VerticalAlignment.TOP);
        settingBtn.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                if (ClientConfig.currentUser != null) {
                    SettingWindow window = new SettingWindow();
                    window.show();
                } else {
                    SC.say(ClientConfig.messages.nullUser());
                }
            }

        });
        header.addMember(settingBtn);

        vLayout.addMember(header);

        TabSet appTabSet = new TabSet();
        appTabSet.setTabBarPosition(Side.TOP);
        appTabSet.setTabBarAlign(Side.RIGHT);
        appTabSet.setWidth("95%");
        appTabSet.setLayoutAlign(Alignment.CENTER);
        vLayout.addMember(appTabSet);

        String copyright = "<span style=\"font-family:Arial;\">&copy;</span>2013 Zhaowei Ding";
        String licenseLink = "http://opensource.org/licenses/MIT";
        String license = "Licensed under the <a href='" + licenseLink + "'>the MIT License</a>.";

        HTMLFlow footer = new HTMLFlow(copyright + ", " + license);
        footer.setWidth100();
        footer.setHeight(10);
        footer.setStyleName("footing");
        footer.setMargin(5);
        vLayout.addMember(footer);

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

        vLayout.draw();
    }

    private void prepareDataSources() {
        dataSources = new HashMap<String, DataSource>();

        DataSource userNameSource = ClientUtils.createDataSource("userNameDS", ClientConfig.constants.userService());
        DataSourceTextField userNameField = new DataSourceTextField("name");
        userNameField.setRequired(true);
        userNameField.setPrimaryKey(true);
        userNameSource.setFields(userNameField);
        dataSources.put("userNameDS", userNameSource);

        DataSource personalSettingsSource = ClientUtils.createDataSource("userSettingsDS",
                ClientConfig.constants.userService());
        DataSourceTextField fullNameField = new DataSourceTextField("fullName", ClientConfig.messages.fullName());
        DataSourceTextField emailField = new DataSourceTextField("email", ClientConfig.messages.email());
        DataSourceTextField personnalHiddenField = new DataSourceTextField("userName");
        personnalHiddenField.setHidden(true);
        personalSettingsSource.setFields(personnalHiddenField, fullNameField, emailField);
        dataSources.put("userSettingsDS", personalSettingsSource);

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

    public class LoginWindow extends Window {

        public LoginWindow() {
            setWidth(350);
            setHeight(80);
            setTitle(ClientConfig.messages.login());
            ClientUtils.unifySimpleWindowStyle(this);

            VLayout layout = new VLayout();
            ClientUtils.unifyWindowLayoutStyle(layout);
            addItem(layout);

            HLayout inline = new HLayout();
            inline.setLayoutAlign(VerticalAlignment.CENTER);
            inline.setAlign(Alignment.CENTER);
            inline.setMargin(5);
            inline.setMembersMargin(5);
            layout.addMember(inline);

            final DynamicForm form = new DynamicForm();
            form.setCellPadding(0);
            ComboBoxItem userNameItem = new ComboBoxItem("name");
            userNameItem.setTitle(ClientConfig.messages.user());
            userNameItem.setOptionDataSource(dataSources.get("userNameDS"));
            form.setFields(userNameItem);
            inline.addMember(form);

            IButton okButton = new IButton(ClientConfig.messages.ok());
            okButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    if (form.validate()) {
                        final String name = form.getValueAsString("name").trim();
                        LoginWindow.this.destroy();
                        Criteria criteria = new Criteria();
                        dataSources.get("userNameDS").fetchData(criteria, new DSCallback() {

                            @Override
                            public void execute(DSResponse response, Object rawData, DSRequest request) {
                                Record[] records = response.getData();
                                for (Record record : records) {
                                    if (record.getAttribute("name").equals(name)) {
                                        return;
                                    }
                                }
                                Record record = new Record();
                                record.setAttribute("name", name);
                                dataSources.get("userNameDS").addData(record);
                            }

                        });
                        ClientConfig.currentUser = name;
                        loginBtn.setTitle(ClientConfig.messages.hi() + ", " + ClientConfig.currentUser);
                        Cookies.setCookie(ClientConfig.constants.currentUserCookie(), ClientConfig.currentUser);
                    }
                }

            });
            inline.addMember(okButton);
        }
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

            Tab personalInfoTab = new Tab();
            personalInfoTab.setTitle(ClientConfig.messages.user());
            personalInfoTab.setPane(createUserSettingsForm());
            settingsTabSet.addTab(personalInfoTab);

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

        private Canvas createUserSettingsForm() {
            VLayout layout = new VLayout();
            layout.setWidth100();
            layout.setMargin(5);
            layout.setMembersMargin(5);

            DynamicForm userForm = new DynamicForm();
            forms.put("userSettingsForm", userForm);
            userForm.setWidth("90%");
            userForm.setDataSource(dataSources.get("userSettingsDS"));
            userForm.setAutoFetchData(true);
            layout.addMember(userForm);

            return layout;
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
