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

import java.io.*;
import java.net.*;
import java.util.*;

public class Launcher {
    private static String testmpHome;

    private static Process webProc;

    private static List<Process> dsProcs = new ArrayList<Process>();

    private static Process launchDataStore(String storeName, Properties props, Set<String> launched) throws Exception {
        String u = props.getProperty(storeName + "Url");
        if (u == null) {
            throw new RuntimeException("No url is set for " + storeName);
        }
        u = u.replace("127.0.0.1", "localhost");
        if (launched.contains(u)) {
            System.out.println(storeName + " has been launched.");
            return null;
        }
        URL url = new URL(u);
        if (!url.getHost().equals("localhost")) {
            System.out.println(storeName + " should have been launched by yourself on " + u);
            return null;
        }

        int port = url.getPort();
        System.out.println("launching " + storeName + " on " + port);
        if (!isPortFree(port)) {
            throw new RuntimeException("Port is not free: " + port);
        }

        String executable = testmpHome + File.separator + "webapp" + File.separator + "datastore.war";
        String dbHome = testmpHome + File.separator + "data" + File.separator + storeName;
        String[] command = new String[] { "java", "-DhttpPort=" + port, "-Dtestmp.home=" + testmpHome,
                "-DdbHome=" + dbHome, "-jar", executable };
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(testmpHome)).redirectErrorStream(true);
        return pb.start();
    }

    private static Process launchWebConsole(int port) throws Exception {
        System.out.println("launching TestMP web console on " + port);
        if (!isPortFree(port)) {
            throw new RuntimeException("Port is not free: " + port);
        }

        String executable = testmpHome + File.separator + "webapp" + File.separator + "testmp.war";
        String[] command = new String[] { "java", "-DhttpPort=" + port, "-Dtestmp.home=" + testmpHome, "-jar",
                executable };
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(testmpHome)).redirectErrorStream(true);
        return pb.start();
    }

    private static boolean isPortFree(int port) {
        try {
            Socket s = new Socket("localhost", port);
            s.close();
            return false;
        } catch (IOException ex) {
            return true;
        }
    }

    public static void main(String[] args) throws Exception {
        testmpHome = System.getenv("TESTMP_HOME");
        if (testmpHome == null) {
            throw new RuntimeException("TESTMP_HOME is not set");
        }

        Properties props = new Properties();
        String confFile = testmpHome + File.separator + "conf" + File.separator + "testmp.properties";
        props.load(new FileInputStream(confFile));

        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Closing process...");

                for (Process p : dsProcs) {
                    p.destroy();
                }
                
                if (webProc != null) {
                    webProc.destroy();
                }
                System.out.println("TestMP is shutdown");
            }
        });

        HashSet<String> launched = new HashSet<String>();
        Process dsProc = null;
        if ((dsProc = launchDataStore("testCaseStore", props, launched)) != null) {
            dsProcs.add(dsProc);
        }
        if ((dsProc = launchDataStore("testDataStore", props, launched)) != null) {
            dsProcs.add(dsProc);
        }
        if ((dsProc = launchDataStore("testEnvStore", props, launched)) != null) {
            dsProcs.add(dsProc);
        }

        webProc = launchWebConsole(args.length > 0 ? Integer.parseInt(args[0]) : 10080);
        if (webProc == null) {
            return;
        }

        for (Process p : dsProcs) {
            final Process proc = p;
            Thread t = new Thread() {
                public void run() {
                    try {
                        InputStream trace = proc.getInputStream();
                        byte[] buffer = new byte[1024];
                        int n;
                        while ((n = trace.read(buffer)) > 0) {
                            synchronized (System.out) {
                                System.out.write(buffer, 0, n);
                                System.out.flush();
                            }
                        }
                        if (proc.waitFor() != 0) {
                            System.exit(-1);
                        }
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                        System.exit(-1);
                    }
                }
            };
            t.setDaemon(true);
            t.start();
        }

        InputStream trace = webProc.getInputStream();
        byte[] buffer = new byte[1024];
        int n;
        while ((n = trace.read(buffer)) > 0) {
            synchronized (System.out) {
                System.out.write(buffer, 0, n);
                System.out.flush();
            }
        }
        webProc.waitFor();
    }
}
