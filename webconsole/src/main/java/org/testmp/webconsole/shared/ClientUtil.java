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
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.AnimationEffect;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.AnimationCallback;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

public class ClientUtil {

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
        window.setShowModalMask(true);
        window.centerInPage();
        window.addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(CloseClickEvent event) {
                window.destroy();
            }
        });
    }

    public static void unifyWindowLayoutStyle(final VLayout layout) {
        layout.setSize("100%", "100%");
        layout.setMargin(5);
        layout.setMembersMargin(5);
        layout.setAlign(Alignment.CENTER);
    }

    public static Label createLoadingLabel() {
        Label loading = new Label(ClientConfig.messages.sending() + "...");
        loading.setAlign(Alignment.CENTER);
        loading.setIcon("loading.gif");
        loading.setIconSize(16);
        return loading;
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
