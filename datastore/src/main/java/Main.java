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

import java.net.URL;
import java.security.ProtectionDomain;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;

public class Main {

    public static void main(String[] args) {
        String httpPort = System.getProperty("httpPort");
        Connector connector = new SelectChannelConnector();
        connector.setPort(httpPort == null ? 8080 : Integer.parseInt(httpPort));

        Server server = new Server();
        server.setConnectors(new Connector[] { connector });

        WebAppContext context = new WebAppContext();
        context.setServer(server);
        context.setContextPath("/");

        ProtectionDomain protectionDomain = Main.class.getProtectionDomain();
        URL location = protectionDomain.getCodeSource().getLocation();
        context.setWar(location.toExternalForm());

        server.setStopAtShutdown(true);
        server.setHandler(context);

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
