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

    private static Process launchDataStore(String storeName, String addr, Set<String> launched) throws Exception {
        System.out.println("Launching " + storeName + ": " + addr);

        if (addr == null) {
            throw new RuntimeException("DataStore address is null");
        }

        addr = addr.replace("127.0.0.1", "localhost");
        if (launched.contains(addr)) {
            return null;
        }

        URL url = new URL(addr);
        if (!url.getHost().equals("localhost")) {
            return null;
        }

        int port = url.getPort();
        if (!isPortFree(port)) {
            throw new RuntimeException("Port is not free: " + port);
        }

        String war = testmpHome + File.separator + "webapp" + File.separator + "datastore.war";
        String dbHome = testmpHome + File.separator + "data" + File.separator + storeName;
        String logFile = testmpHome + File.separator + "log" + File.separator + "testmp.log";
        String[] command = new String[] { getJavaExecutable(), "-DhttpPort=" + port, "-Ddatastore.log=" + logFile,
                "-DdbHome=" + dbHome, "-jar", war };
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(testmpHome)).redirectErrorStream(true);
        return pb.start();
    }

    private static Process launchWebConsole(int port) throws Exception {
        System.out.println("Launching TestMP web console: http://localhost:" + port);
        if (!isPortFree(port)) {
            throw new RuntimeException("Port is not free: " + port);
        }

        String war = testmpHome + File.separator + "webapp" + File.separator + "testmp.war";
        String logFile = testmpHome + File.separator + "log" + File.separator + "testmp.log";
        String[] command = new String[] { getJavaExecutable(), "-DhttpPort=" + port, "-Dwebconsole.log=" + logFile,
                "-jar", war };
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

    private static String getJavaExecutable() {
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            return javaHome + File.separator + "bin" + File.separator + "java";
        }
        return "java";
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

        String tcAddr = props.getProperty("testCaseStoreUrl");
        if ((dsProc = launchDataStore("testCaseStore", tcAddr, launched)) != null) {
            dsProcs.add(dsProc);
            launched.add(tcAddr);
        }

        String tdAddr = props.getProperty("testDataStoreUrl");
        if ((dsProc = launchDataStore("testDataStore", tdAddr, launched)) != null) {
            dsProcs.add(dsProc);
            launched.add(tdAddr);
        }

        String teAddr = props.getProperty("testEnvStoreUrl");
        if ((dsProc = launchDataStore("testEnvStore", teAddr, launched)) != null) {
            dsProcs.add(dsProc);
            launched.add(teAddr);
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
