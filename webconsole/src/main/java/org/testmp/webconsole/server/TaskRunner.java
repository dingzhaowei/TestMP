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

package org.testmp.webconsole.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.testmp.webconsole.util.Commandline;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class TaskRunner extends Thread {

    private static Logger log = Logger.getLogger(TaskRunner.class);

    private long timeout;

    private String traceFileDir;

    private ExecutorService executor;

    private Vector<Execution> executions = new Vector<Execution>();

    public TaskRunner(int nThreads, long timeout, String traceFileDir) {
        executor = Executors.newFixedThreadPool(nThreads);
        this.timeout = timeout;
        this.traceFileDir = traceFileDir;
        setDaemon(true);
    }

    public static String getTraceFileName(String host, String workingDir, String command) {
        return "trace_" + host + "_" + String.valueOf(workingDir).hashCode() + "_" + command.hashCode();
    }

    public void run() {
        while (true) {
            cleanExecutions();
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // pass
            }
        }
    }

    public Future<Integer> addExecution(final Map<String, Object> info) {
        return executor.submit(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                Execution e = new Execution(info);
                synchronized (TaskRunner.this) {
                    executions.add(e);
                }
                return e.execute();
            }

        });
    }

    public void cancelExecution(Map<String, Object> info) {
        synchronized (this) {
            for (Execution e : executions) {
                if (e.host.equals(info.get("host")) && e.command.equals(info.get("command"))) {
                    e.cancel();
                    break;
                }
            }
        }
    }

    private void cleanExecutions() {
        List<Execution> completeExecutions = new ArrayList<Execution>();
        synchronized (this) {
            for (Execution e : executions) {
                long elapsedTime = e.getElapsedTime();
                if (!e.isComplete() && timeout > 0 && elapsedTime > timeout) {
                    e.cancel();
                }
                if (e.isComplete()) {
                    completeExecutions.add(e);
                }
            }
            executions.removeAll(completeExecutions);
        }
    }

    private class Execution {
        private String host;

        private String workingDir;

        private String command;

        private String username;

        private String password;

        private Process process;

        private Channel channel;

        private Session session;

        private long startTime;

        private boolean complete;

        public Execution(Map<String, Object> info) {
            host = info.get("host").toString();
            command = info.get("command").toString();
            workingDir = (String) info.get("workingDir");
            username = (String) info.get("username");
            password = (String) info.get("password");
        }

        public long getElapsedTime() {
            long elapsedTime = System.currentTimeMillis() - startTime;
            return elapsedTime;
        }

        public boolean isComplete() {
            return complete;
        }

        public Integer execute() {
            startTime = System.currentTimeMillis();
            try {
                if (host.equals("localhost") || host.equals("127.0.0.1")) {
                    return executeLocally();
                } else {
                    return executeRemotely();
                }
            } finally {
                complete = true;
            }
        }

        public void cancel() {
            try {
                if (process != null) {
                    process.destroy();
                }
                if (channel != null) {
                    channel.disconnect();
                }
            } finally {
                complete = true;
            }
        }

        private Integer executeLocally() {
            BufferedReader reader = null;
            PrintWriter writer = null;
            try {
                String filename = getTraceFileName(host, workingDir, command);
                writer = new PrintWriter(traceFileDir + File.separator + filename);

                Commandline cl = new Commandline(command);
                ProcessBuilder pb = new ProcessBuilder(cl.getCommandline());
                pb.redirectErrorStream(true);
                if (workingDir != null) {
                    pb.directory(new File(workingDir));
                }
                process = pb.start();
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    writer.println(line);
                    writer.flush();
                }

                return process.exitValue();
            } catch (Exception e) {
                try {
                    writer.println(e.getMessage());
                } catch (Exception e1) {
                    // pass
                }
                return -1;
            } finally {
                try {
                    reader.close();
                } catch (Exception e) {
                    // pass
                }
                try {
                    writer.close();
                } catch (Exception e) {
                    // pass
                }
            }
        }

        private Integer executeRemotely() {
            JSch.setLogger(new com.jcraft.jsch.Logger() {

                @Override
                public boolean isEnabled(int arg0) {
                    return true;
                }

                @Override
                public void log(int level, String message) {
                    switch (level) {
                    case DEBUG:
                        log.debug(message);
                        break;
                    case INFO:
                        log.info(message);
                        break;
                    case WARN:
                        log.warn(message);
                        break;
                    case ERROR:
                        log.error(message);
                        break;
                    case FATAL:
                        log.fatal(message);
                        break;
                    }
                }

            });
            JSch jsch = new JSch();
            String filename = getTraceFileName(host, workingDir, command);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(traceFileDir + File.separator + filename);
                session = jsch.getSession(username, host, 22);
                session.setUserInfo(new UserInfo() {

                    @Override
                    public String getPassphrase() {
                        return null;
                    }

                    @Override
                    public String getPassword() {
                        return password;
                    }

                    @Override
                    public boolean promptPassphrase(String arg0) {
                        return false;
                    }

                    @Override
                    public boolean promptPassword(String arg0) {
                        return true;
                    }

                    @Override
                    public boolean promptYesNo(String arg0) {
                        return false;
                    }

                    @Override
                    public void showMessage(String arg0) {
                    }

                });
                session.setConfig("StrictHostKeyChecking", "no");
                session.setConfig("PreferredAuthentications", "password");
                session.connect();

                channel = session.openChannel("exec");
                if (workingDir != null) {
                    String cd = "cd \"" + workingDir + "\"";
                    ((ChannelExec) channel).setCommand(cd + "&&" + command);
                } else {
                    ((ChannelExec) channel).setCommand(command);
                }
                channel.setOutputStream(output);
                ((ChannelExec) channel).setErrStream(output);
                channel.connect();

                while (!channel.isClosed()) {
                    Thread.sleep(1000);
                }
                return channel.getExitStatus();
            } catch (Exception e) {
                try {
                    output.write(e.getMessage().getBytes());
                } catch (Exception e1) {
                    // pass
                }
                return -1;
            } finally {
                try {
                    output.close();
                } catch (Exception e) {
                    // pass
                }
                try {
                    channel.disconnect();
                    session.disconnect();
                } catch (Exception e) {
                    // pass
                }
            }
        }
    }
}
