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

import org.testmp.webconsole.shared.WebConsoleClientConfig;

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
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.AnimationCallback;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
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
        setWidth("950");
        setHeight("95%");
        setShowMinimizeButton(false);
        setIsModal(true);
        setShowModalMask(true);
        centerInPage();
        addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(CloseClickEvent event) {
                ReportWindow.this.destroy();
            }
        });

        windowLayout = new VLayout();
        windowLayout.setSize("100%", "100%");
        windowLayout.setAlign(Alignment.CENTER);
        windowLayout.setMembersMargin(5);
        addItem(windowLayout);

        show();
    }

    public void showReport(final ReportType type, Map<String, Object> params) {
        setTitle(type.getTypeName() + " Report");

        windowLayout.removeMembers(windowLayout.getMembers());
        final Label loading = new Label("Loading report...");
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
        String servicePath = WebConsoleClientConfig.constants.reportService();
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
                        windowLayout.addMember(reportPane);
                        HLayout controls = new HLayout();
                        controls.setSize("99%", "20");
                        controls.setMargin(5);
                        controls.setMembersMargin(5);
                        controls.setAlign(Alignment.CENTER);
                        IButton sendButton = new IButton("Send");
                        sendButton.addClickHandler(new ClickHandler() {

                            @Override
                            public void onClick(ClickEvent event) {
                                SendWindow window = new SendWindow(type, filename);
                                window.show();
                            }

                        });
                        controls.addMember(sendButton);
                        windowLayout.addMember(controls);
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
            setTitle("Send Report");
            setShowMinimizeButton(false);
            setIsModal(true);
            setShowModalMask(true);
            centerInPage();
            addCloseClickHandler(new CloseClickHandler() {
                public void onCloseClick(CloseClickEvent event) {
                    SendWindow.this.destroy();
                }
            });

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
            recipientsItem.setTitle("Recipients");
            recipientsItem.setRequired(true);
            recipientsItem.setWidth(400);

            final TextItem subjectItem = new TextItem();
            subjectItem.setName("subject");
            subjectItem.setTitle("Subject");
            subjectItem.setRequired(true);
            subjectItem.setWidth(400);

            TextAreaItem commentItem = new TextAreaItem();
            commentItem.setName("comment");
            commentItem.setTitle("Comment");
            commentItem.setWidth(400);

            SectionItem emailInfoItem = new SectionItem();
            emailInfoItem.setDefaultValue("EMail");
            emailInfoItem.setSectionExpanded(true);
            emailInfoItem.setItemIds("recipients", "subject", "comment");

            final TextItem smtphostItem = new TextItem();
            smtphostItem.setName("smtphost");
            smtphostItem.setTitle("SMTP host");
            smtphostItem.setRequired(true);

            final IntegerItem smtpportItem = new IntegerItem();
            smtpportItem.setName("smtpport");
            smtpportItem.setTitle("SMTP port");
            smtpportItem.setRequired(true);

            final CheckboxItem starttlsItem = new CheckboxItem();
            starttlsItem.setName("starttls");
            starttlsItem.setTitle("Use STARTTLS");
            starttlsItem.setRequired(true);

            final TextItem usernameItem = new TextItem();
            usernameItem.setName("username");
            usernameItem.setTitle("User");
            usernameItem.setRequired(true);

            final PasswordItem passwordItem = new PasswordItem();
            passwordItem.setName("password");
            passwordItem.setTitle("Password");
            passwordItem.setRequired(true);

            SectionItem smtpInfoItem = new SectionItem();
            smtpInfoItem.setDefaultValue("Setting");
            smtpInfoItem.setSectionExpanded(true);
            smtpInfoItem.setItemIds("smtphost", "smtpport", "starttls", "username", "password");

            form.setFields(emailInfoItem, recipientsItem, subjectItem, commentItem, smtpInfoItem, smtphostItem,
                    smtpportItem, starttlsItem, usernameItem, passwordItem);
            sendInfoLayout.addMember(form);

            StringBuilder dataBuilder = new StringBuilder();
            dataBuilder.append("reportType=").append(URL.encode(type.getTypeName()));
            dataBuilder.append("&&action=getCustomSetting");

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
            controls.setSize("99%", "20");
            controls.setMargin(5);
            controls.setMembersMargin(5);
            controls.setLayoutAlign(Alignment.CENTER);
            controls.setAlign(Alignment.CENTER);
            layout.addMember(controls);

            IButton okButton = new IButton("OK");
            okButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    if (form.validate()) {
                        SendWindow.this.removeItem(layout);
                        final Label loading = new Label("Sending report...");
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

            IButton cancelButton = new IButton("Cancel");
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
