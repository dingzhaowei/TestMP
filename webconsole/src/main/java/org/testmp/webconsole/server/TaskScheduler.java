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

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.DataStoreClient;
import org.testmp.webconsole.model.Task;
import org.testmp.webconsole.util.CronExpression;

public class TaskScheduler extends TimerTask {

    private static Logger log = Logger.getLogger(TaskScheduler.class);

    private DataStoreClient client;

    private TaskRunner taskRunner;

    private long lastRefreshingTime;

    private long refreshGap;

    private long triggerLatency;

    private Map<Integer, TaskScheduleInfo> schedule;

    public TaskScheduler(String taskStoreUrl, long refreshingGap, TaskRunner taskRunner, long triggerLatency) {
        client = new DataStoreClient(taskStoreUrl);
        schedule = new HashMap<Integer, TaskScheduleInfo>();
        this.refreshGap = refreshingGap;
        this.triggerLatency = triggerLatency;
        this.taskRunner = taskRunner;
    }

    @Override
    public void run() {
        try {
            long currentTime = System.currentTimeMillis();
            if (lastRefreshingTime == 0 || currentTime - lastRefreshingTime > refreshGap) {
                refreshTaskSchedule();
                lastRefreshingTime = currentTime;
            }
            poll();
        } catch (Exception e) {
            log.error("TaskScheduler confronted exception.", e);
        }
    }

    private void refreshTaskSchedule() throws Exception {
        List<DataInfo<Task>> dataInfoList = client.getDataByTag(Task.class, "Task");
        for (DataInfo<Task> dataInfo : dataInfoList) {
            Task task = dataInfo.getData();
            String cronExp = task.getSchedule();
            Integer taskId = dataInfo.getId();
            if (!schedule.containsKey(taskId) && cronExp != null) {
                schedule.put(taskId, new TaskScheduleInfo(cronExp));
            } else {
                if (cronExp == null) {
                    schedule.remove(taskId);
                } else {
                    schedule.get(taskId).updateCron(cronExp);
                }
            }
        }
    }

    private void poll() throws Exception {
        for (Map.Entry<Integer, TaskScheduleInfo> entry : schedule.entrySet()) {
            final Integer taskId = entry.getKey();
            TaskScheduleInfo info = entry.getValue();
            CronExpression cron = info.getCron();
            Date lastRunTime = info.getLastRunTime();

            Date nextValidTime = cron.getNextValidTimeAfter(lastRunTime);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(cron.getTimeZone());
            Date currentTime = calendar.getTime();

            if (currentTime.after(nextValidTime)) {
                long diff = currentTime.getTime() - nextValidTime.getTime();
                if (diff <= triggerLatency) {
                    new Thread() {
                        public void run() {
                            try {
                                TaskService.runTask(taskId, client, taskRunner);
                            } catch (Exception e) {
                                log.error("Running task confronted exception.", e);
                            }
                        }
                    }.start();
                }
                info.markTaskRun();
            }
        }
    }

    private class TaskScheduleInfo {

        private CronExpression cron;

        private Date lastRunTime;

        TaskScheduleInfo(String cronExp) throws ParseException {
            cron = new CronExpression(cronExp);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(cron.getTimeZone());
            lastRunTime = calendar.getTime();
        }

        CronExpression getCron() {
            return cron;
        }

        void updateCron(String cronExp) throws ParseException {
            cron = new CronExpression(cronExp);
        }

        Date getLastRunTime() {
            return lastRunTime;
        }

        void markTaskRun() {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(cron.getTimeZone());
            lastRunTime = calendar.getTime();
        }

    }
}
