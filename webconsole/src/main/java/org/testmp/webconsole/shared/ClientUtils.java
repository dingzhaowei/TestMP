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

package org.testmp.webconsole.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.OperationBinding;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RestDataSource;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.AnimationEffect;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.AnimationCallback;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.VLayout;

public class ClientUtils {

    public static void unifyControlsLayoutStyle(HLayout controls) {
        controls.setSize("99%", "20");
        controls.setMargin(5);
        controls.setMembersMargin(5);
        controls.setLayoutAlign(Alignment.CENTER);
        controls.setAlign(Alignment.CENTER);
    }

    public static void unifySimpleWindowStyle(final Window window) {
        window.setShowMinimizeButton(false);
        window.setIsModal(true);
        window.centerInPage();
        window.addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(CloseClickEvent event) {
                window.destroy();
            }
        });
    }

    public static void unifyWindowLayoutStyle(final Layout layout) {
        layout.setSize("100%", "100%");
        layout.setMargin(5);
        layout.setMembersMargin(5);
        layout.setAlign(Alignment.CENTER);
    }

    public static void unifyDataSourceSetting(DataSource ds, String dsName, String servicePath) {
        ds.setID(dsName);
        ds.setDataFormat(DSDataFormat.JSON);

        String baseUrl = GWT.getModuleBaseURL();
        String requestUrl = baseUrl + servicePath.substring(servicePath.lastIndexOf('/') + 1);
        ds.setDataURL(requestUrl);

        OperationBinding fetch = new OperationBinding();
        fetch.setOperationType(DSOperationType.FETCH);
        fetch.setDataProtocol(DSProtocol.POSTMESSAGE);
        fetch.setDataFormat(DSDataFormat.JSON);

        OperationBinding update = new OperationBinding();
        update.setOperationType(DSOperationType.UPDATE);
        update.setDataProtocol(DSProtocol.POSTMESSAGE);
        update.setDataFormat(DSDataFormat.JSON);

        OperationBinding add = new OperationBinding();
        add.setOperationType(DSOperationType.ADD);
        add.setDataProtocol(DSProtocol.POSTMESSAGE);
        add.setDataFormat(DSDataFormat.JSON);

        OperationBinding remove = new OperationBinding();
        remove.setOperationType(DSOperationType.REMOVE);
        remove.setDataProtocol(DSProtocol.POSTMESSAGE);
        remove.setDataFormat(DSDataFormat.JSON);

        ds.setOperationBindings(fetch, update, add, remove);
    }

    public static Label createLoadingLabel() {
        Label loading = new Label(ClientConfig.messages.sending() + "...");
        loading.setAlign(Alignment.CENTER);
        loading.setIcon("loading.gif");
        loading.setIconSize(16);
        return loading;
    }

    public static HoverCustomizer createHoverCustomizer() {
        HoverCustomizer hoverCustomizer = new HoverCustomizer() {

            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                Boolean isFolder = record.getAttributeAsBoolean("isFolder");
                Boolean isGridSummary = record.getIsGridSummary();
                Boolean isGroupSummary = record.getIsGroupSummary();
                if (value == null || (isFolder != null && isFolder.booleanValue())
                        || (isGridSummary != null && isGridSummary.booleanValue())
                        || (isGroupSummary != null && isGroupSummary.booleanValue())) {
                    return null;
                }
                String v = value.toString().replace("&nbsp;", "");
                return v.isEmpty() ? null : v;
            }

        };
        return hoverCustomizer;
    }

    public static DataSource createDataSource(String dsName, String servicePath) {
        DataSource ds = new RestDataSource() {

            @Override
            protected Object transformRequest(DSRequest dsRequest) {
                String operationType = dsRequest.getAttributeAsString("operationType");
                if (operationType.equals("fetch")) {
                    Criteria criteria = dsRequest.getCriteria();
                    if (criteria == null) {
                        criteria = new Criteria();
                    }
                    criteria.setAttribute("sid", ClientConfig.currentUser);
                    dsRequest.setCriteria(criteria);
                } else {
                    Record record = new Record(dsRequest.getData());
                    record.setAttribute("sid", ClientConfig.currentUser);
                    dsRequest.setData(record);
                }
                return super.transformRequest(dsRequest);
            }

        };
        unifyDataSourceSetting(ds, dsName, servicePath);
        return ds;
    }

    public static void sendDataFromWindow(final VLayout windowLayout, final Window window, final Label loadingLabel,
            String data, String servicePath) {
        final String baseUrl = GWT.getModuleBaseURL();
        String requestUrl = baseUrl + servicePath.substring(servicePath.lastIndexOf('/') + 1);
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, requestUrl);
        try {
            builder.sendRequest(data, new RequestCallback() {

                @Override
                public void onResponseReceived(Request request, Response response) {
                    if (response.getStatusCode() == Response.SC_OK) {
                        window.animateHide(AnimationEffect.FADE, new AnimationCallback() {

                            @Override
                            public void execute(boolean earlyFinish) {
                                window.destroy();
                            }

                        });
                    } else {
                        SC.warn(response.getStatusCode() + " - " + response.getStatusText());
                        window.removeItem(loadingLabel);
                        window.addItem(windowLayout);
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    SC.warn(exception.getMessage());
                    window.removeItem(loadingLabel);
                    window.addItem(windowLayout);
                }

            });
        } catch (Exception e) {
            SC.warn(e.getMessage());
            window.removeItem(loadingLabel);
            window.addItem(windowLayout);
        }
    }
}
