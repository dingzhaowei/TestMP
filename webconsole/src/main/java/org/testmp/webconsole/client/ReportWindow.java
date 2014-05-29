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

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.AnimationEffect;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.AnimationCallback;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.IntegerItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.SectionItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

public class ReportWindow extends Window {

    private VLayout windowLayout;

    private String requestUrl;

    public enum ReportType {
        TEST_METRICS("Test Metrics"), ENVIRONMENT_STATUS("Environment Status");

        public String getTypeName() {
            return typeName;
        }

        private ReportType(String typeName) {
            this.typeName = typeName;
        }

        private String typeName;
    }

    public ReportWindow() {
        setWidth(950);
        setHeight("85%");
        setCanDragResize(true);
        ClientUtils.unifySimpleWindowStyle(this);

        windowLayout = new VLayout();
        windowLayout.setSize("100%", "100%");
        windowLayout.setAlign(Alignment.CENTER);
        windowLayout.setMembersMargin(5);
        addItem(windowLayout);

        show();
    }

    public void showReport(final ReportType type, Map<String, Object> params) {
        switch (type) {
        case TEST_METRICS:
            setTitle(ClientConfig.messages.testMetricsReport());
            break;
        case ENVIRONMENT_STATUS:
            setTitle(ClientConfig.messages.environmentStatusReport());
            break;
        default:
            setTitle(type.getTypeName() + " Report");
        }

        windowLayout.removeMembers(windowLayout.getMembers());
        final Label loading = new Label(ClientConfig.messages.loading());
        loading.setAlign(Alignment.CENTER);
        loading.setIcon("loading.gif");
        loading.setIconSize(16);
        windowLayout.addMember(loading);

        StringBuilder dataBuilder = new StringBuilder();
        dataBuilder.append("reportType=").append(URL.encode(type.getTypeName()));
        for (Map.Entry<String, Object> param : params.entrySet()) {
            String key = param.getKey();
            String value = param.getValue().toString();
            dataBuilder.append('&').append(key).append('=').append(URL.encode(value));
        }

        final String baseUrl = GWT.getModuleBaseURL();
        String servicePath = ClientConfig.constants.reportService();
        requestUrl = baseUrl + servicePath.substring(servicePath.lastIndexOf('/') + 1);

        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, requestUrl);
        try {
            builder.sendRequest(dataBuilder.toString(), new RequestCallback() {

                @Override
                public void onResponseReceived(Request request, Response response) {
                    if (response.getStatusCode() == Response.SC_OK) {
                        windowLayout.removeMember(loading);
                        HTMLFlow reportPane = new HTMLFlow();
                        reportPane.setWidth("99%");
                        reportPane.setEvalScriptBlocks(true);
                        final String filename = response.getText();
                        reportPane.setContentsURL(baseUrl + "reports/" + filename);

                        HLayout wrapperLayout = new HLayout();
                        wrapperLayout.setOverflow(Overflow.AUTO);
                        wrapperLayout.addMember(reportPane);
                        windowLayout.addMember(wrapperLayout);

                        HLayout controls = new HLayout();
                        controls.setSize("99%", "20");
                        controls.setMargin(5);
                        controls.setMembersMargin(5);
                        controls.setAlign(Alignment.CENTER);
                        windowLayout.addMember(controls);

                        IButton sendButton = new IButton(ClientConfig.messages.send());
                        sendButton.addClickHandler(new ClickHandler() {

                            @Override
                            public void onClick(ClickEvent event) {
                                SendWindow window = new SendWindow(type, filename);
                                window.show();
                            }

                        });
                        controls.addMember(sendButton);

                        IButton cancelButton = new IButton(ClientConfig.messages.cancel());
                        cancelButton.addClickHandler(new ClickHandler() {

                            @Override
                            public void onClick(ClickEvent event) {
                                ReportWindow.this.destroy();
                            }

                        });
                        controls.addMember(cancelButton);
                    } else {
                        SC.warn(response.getStatusCode() + " - " + response.getStatusText());
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    SC.warn(exception.getMessage());
                }

            });
        } catch (Exception e) {
            SC.warn(e.getMessage());
        }
    }

    public class SendWindow extends Window {

        public SendWindow(ReportType type, final String reportFileName) {
            setWidth(600);
            setHeight(450);
            setTitle(ClientConfig.messages.sendReport());
            ClientUtils.unifySimpleWindowStyle(this);

            final VLayout layout = new VLayout();
            layout.setSize("100%", "100%");
            layout.setMargin(5);
            layout.setMembersMargin(5);
            layout.setAlign(Alignment.CENTER);
            addItem(layout);

            VLayout sendInfoLayout = new VLayout();
            sendInfoLayout.setWidth("99%");
            sendInfoLayout.setMargin(5);
            layout.addMember(sendInfoLayout);

            final DynamicForm form = new DynamicForm();
            form.setWidth100();
            form.setHeight100();

            final TextItem recipientsItem = new TextItem();
            recipientsItem.setName("recipients");
            recipientsItem.setTitle(ClientConfig.messages.recipients());
            recipientsItem.setRequired(true);
            recipientsItem.setWidth(400);

            final TextItem subjectItem = new TextItem();
            subjectItem.setName("subject");
            subjectItem.setTitle(ClientConfig.messages.subject());
            subjectItem.setRequired(true);
            subjectItem.setWidth(400);

            TextAreaItem commentItem = new TextAreaItem();
            commentItem.setName("comment");
            commentItem.setTitle(ClientConfig.messages.comment());
            commentItem.setWidth(400);

            SectionItem emailInfoItem = new SectionItem();
            emailInfoItem.setDefaultValue(ClientConfig.messages.mailbox());
            emailInfoItem.setSectionExpanded(true);
            emailInfoItem.setItemIds("recipients", "subject", "comment");

            final TextItem smtphostItem = new TextItem();
            smtphostItem.setName("smtphost");
            smtphostItem.setTitle(ClientConfig.messages.smtpHost());
            smtphostItem.setRequired(true);

            final IntegerItem smtpportItem = new IntegerItem();
            smtpportItem.setName("smtpport");
            smtpportItem.setTitle(ClientConfig.messages.smtpPort());
            smtpportItem.setRequired(true);

            final CheckboxItem starttlsItem = new CheckboxItem();
            starttlsItem.setName("starttls");
            starttlsItem.setTitle(ClientConfig.messages.useStarttls());
            starttlsItem.setRequired(true);

            final TextItem usernameItem = new TextItem();
            usernameItem.setName("username");
            usernameItem.setTitle(ClientConfig.messages.user());
            usernameItem.setRequired(true);

            final PasswordItem passwordItem = new PasswordItem();
            passwordItem.setName("password");
            passwordItem.setTitle(ClientConfig.messages.password());
            passwordItem.setRequired(true);

            SectionItem smtpInfoItem = new SectionItem();
            smtpInfoItem.setDefaultValue(ClientConfig.messages.settings());
            smtpInfoItem.setSectionExpanded(true);
            smtpInfoItem.setItemIds("smtphost", "smtpport", "starttls", "username", "password");

            form.setFields(emailInfoItem, recipientsItem, subjectItem, commentItem, smtpInfoItem, smtphostItem,
                    smtpportItem, starttlsItem, usernameItem, passwordItem);
            sendInfoLayout.addMember(form);

            StringBuilder dataBuilder = new StringBuilder();
            dataBuilder.append("reportType=").append(URL.encode(type.getTypeName()));
            dataBuilder.append("&&action=getCustomSetting");
            if (ClientConfig.currentUser != null) {
                dataBuilder.append("&&userName=").append(URL.encode(ClientConfig.currentUser));
            }

            RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, requestUrl);
            try {
                builder.sendRequest(dataBuilder.toString(), new RequestCallback() {

                    @Override
                    public void onResponseReceived(Request request, Response response) {
                        if (response.getStatusCode() == Response.SC_OK) {
                            JSONObject customSettingObj = JSONParser.parseStrict(response.getText()).isObject();
                            recipientsItem.setValue(customSettingObj.get("recipients").isString().stringValue());
                            subjectItem.setValue(customSettingObj.get("subject").isString().stringValue());
                            smtphostItem.setValue(customSettingObj.get("smtphost").isString().stringValue());
                            String port = customSettingObj.get("smtpport").isString().stringValue();
                            try {
                                smtpportItem.setValue(Integer.parseInt(port));
                            } catch (NumberFormatException e) {
                                smtpportItem.setValue(25);
                            }
                            String starttls = customSettingObj.get("starttls").isString().stringValue();
                            starttlsItem.setValue(starttls.toLowerCase().equals("true"));
                            usernameItem.setValue(customSettingObj.get("username").isString().stringValue());
                            passwordItem.setValue(customSettingObj.get("password").isString().stringValue());
                        } else {
                            SC.warn(response.getStatusCode() + " - " + response.getStatusText());
                        }
                    }

                    @Override
                    public void onError(Request request, Throwable exception) {
                        SC.warn(exception.getMessage());
                    }

                });
            } catch (Exception e) {
                SC.warn(e.getMessage());
            }

            HLayout controls = new HLayout();
            ClientUtils.unifyControlsLayoutStyle(controls);
            layout.addMember(controls);

            IButton okButton = new IButton(ClientConfig.messages.ok());
            okButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    if (form.validate()) {
                        SendWindow.this.removeItem(layout);
                        final Label loading = new Label(ClientConfig.messages.sending() + "...");
                        loading.setAlign(Alignment.CENTER);
                        loading.setIcon("loading.gif");
                        loading.setIconSize(16);
                        SendWindow.this.addItem(loading);

                        String recipients = form.getValueAsString("recipients");
                        String subject = form.getValueAsString("subject");
                        String comment = form.getValueAsString("comment");
                        String smtphost = form.getValueAsString("smtphost");
                        String smtpport = form.getValueAsString("smtpport");
                        String starttls = form.getValueAsString("starttls");
                        String username = form.getValueAsString("username");
                        String password = form.getValueAsString("password");

                        Map<String, String> params = new HashMap<String, String>();
                        params.put("recipients", recipients.trim());
                        params.put("subject", subject.trim());
                        params.put("comment", comment == null ? "" : comment.trim());
                        params.put("smtphost", smtphost.trim());
                        params.put("smtpport", smtpport.trim());
                        params.put("starttls", starttls.trim());
                        params.put("username", username.trim());
                        params.put("password", password == null ? "" : password.trim());

                        StringBuilder dataBuilder = new StringBuilder();
                        dataBuilder.append("action=send");
                        dataBuilder.append("&filename=").append(URL.encode(reportFileName));
                        for (Map.Entry<String, String> param : params.entrySet()) {
                            String key = param.getKey();
                            String value = param.getValue();
                            dataBuilder.append('&').append(key).append('=').append(URL.encode(value));
                        }

                        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, requestUrl);
                        try {
                            builder.sendRequest(dataBuilder.toString(), new RequestCallback() {

                                @Override
                                public void onResponseReceived(Request request, Response response) {
                                    if (response.getStatusCode() == Response.SC_OK) {
                                        SendWindow.this.animateHide(AnimationEffect.FADE, new AnimationCallback() {

                                            @Override
                                            public void execute(boolean earlyFinish) {
                                                SendWindow.this.destroy();
                                            }

                                        });
                                    } else {
                                        SC.warn(response.getStatusCode() + " - " + response.getStatusText());
                                        SendWindow.this.removeItem(loading);
                                        SendWindow.this.addItem(layout);
                                    }
                                }

                                @Override
                                public void onError(Request request, Throwable exception) {
                                    SC.warn(exception.getMessage());
                                    SendWindow.this.removeItem(loading);
                                    SendWindow.this.addItem(layout);
                                }

                            });
                        } catch (Exception e) {
                            SC.warn(e.getMessage());
                            SendWindow.this.removeItem(loading);
                            SendWindow.this.addItem(layout);
                        }
                    }
                }

            });
            controls.addMember(okButton);

            IButton cancelButton = new IButton(ClientConfig.messages.cancel());
            cancelButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    SendWindow.this.destroy();
                }

            });
            controls.addMember(cancelButton);
        }

    }
}
