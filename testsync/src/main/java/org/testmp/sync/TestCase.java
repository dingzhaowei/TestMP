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

package org.testmp.sync;

import java.util.Calendar;
import java.util.LinkedList;

import org.codehaus.jackson.map.ObjectMapper;

public class TestCase {

    public static final String QUALITY_STATUS_ALWAYSBAD = "alwaysBad";

    public static final String QUALITY_STATUS_DEGRADING = "degrading";

    public static final String QUALITY_STATUS_ALWAYSGOOD = "alwaysGood";

    public static final String QUALITY_STATUS_UPGRADING = "upgrading";

    private String project;

    private String name;

    private String description;

    private String automation;

    private LinkedList<RunRecord> runHistory = new LinkedList<RunRecord>();

    private String robustnessTrend;

    /**
     * record of once run of the test case
     * 
     */
    public static class RunRecord {

        private long recordTime;

        private long duration;

        private boolean passed;

        private boolean falseFailure;

        private String failureTrace;

        private String relatedBug;

        public boolean equals(Object o) {
            if (o == null || !(o instanceof RunRecord)) {
                return false;
            }
            RunRecord r = (RunRecord) o;
            return recordTime == r.recordTime
                    && duration == r.duration
                    && passed == r.passed
                    && falseFailure == r.falseFailure
                    && ((failureTrace == null && r.failureTrace == null) || (failureTrace != null && failureTrace
                            .equals(r.failureTrace)))
                    && ((relatedBug == null && r.relatedBug == null) || (relatedBug != null && relatedBug
                            .equals(r.relatedBug)));
        }

        public int hashCode() {
            StringBuilder sb = new StringBuilder();
            sb.append(recordTime).append(duration).append(passed).append(falseFailure).append(failureTrace)
                    .append(relatedBug);
            return sb.hashCode();
        }

        public String toString() {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.writeValueAsString(this);
            } catch (Exception e) {
                return super.toString();
            }
        }

        public long getRecordTime() {
            return recordTime;
        }

        public void setRecordTime(long recordTime) {
            this.recordTime = recordTime;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public boolean getPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public boolean getFalseFailure() {
            return falseFailure;
        }

        public void setFalseFailure(boolean falseFailure) {
            this.falseFailure = falseFailure;
        }

        public String getFailureTrace() {
            return failureTrace;
        }

        public void setFailureTrace(String failureTrace) {
            this.failureTrace = failureTrace;
        }

        public String getRelatedBug() {
            return relatedBug;
        }

        public void setRelatedBug(String relatedBug) {
            this.relatedBug = relatedBug;
        }

    }

    public TestCase() {

    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAutomation() {
        return automation;
    }

    public void setAutomation(String automation) {
        this.automation = automation;
    }

    public LinkedList<RunRecord> getRunHistory() {
        return runHistory;
    }

    public void setRunHistory(LinkedList<RunRecord> runHistory) {
        this.runHistory = runHistory;
    }

    public String getRobustnessTrend() {
        return robustnessTrend;
    }

    public void setRobustnessTrend(String robustnessTrend) {
        this.robustnessTrend = robustnessTrend;
    }

    /**
     * Evaluate the function robustness from the test cases's run history. r =
     * (1.0/(n-1))*sum(1-(now-ti)/sum(now-t) if record passed)
     * 
     * @return
     */
    public double evaluateRobustness() {
        if (runHistory.isEmpty()) {
            return 1.0;
        }

        if (runHistory.size() == 1) {
            return runHistory.get(0).getPassed() ? 1.0 : 0.0;
        }

        double sum = 0.0, rate = 0.0;
        double c = 1.0 / (runHistory.size() - 1.0);
        long now = Calendar.getInstance().getTimeInMillis();

        for (int i = 0; i < runHistory.size(); i++) {
            long recordTime = runHistory.get(i).getRecordTime();
            sum += now - recordTime;
        }

        for (int i = 0; i < runHistory.size(); i++) {
            if (runHistory.get(i).getPassed()) {
                long recordTime = runHistory.get(i).getRecordTime();
                rate += 1.0 - ((now - recordTime) / sum);
            }
        }

        rate *= c;
        return rate;
    }

    /**
     * Evaluate the average test time factored by the time delta between the
     * running day and today
     * 
     * @return
     */
    public double evaluateAverageTestTime() {
        if (runHistory.isEmpty()) {
            return 0.0;
        }

        if (runHistory.size() == 1) {
            return 1.0 * runHistory.get(0).getDuration();
        }

        double sum = 0.0, time = 0.0;
        double c = 1.0 / (runHistory.size() - 1.0);
        long now = Calendar.getInstance().getTimeInMillis();

        for (int i = 0; i < runHistory.size(); i++) {
            long recordTime = runHistory.get(i).getRecordTime();
            sum += now - recordTime;
        }

        for (int i = 0; i < runHistory.size(); i++) {
            long recordTime = runHistory.get(i).getRecordTime();
            long duration = runHistory.get(i).getDuration();
            time += (1.0 - ((now - recordTime) / sum)) * duration;
        }

        time *= c;
        return time;
    }

    /**
     * Evaluate the volatility of the test time from the test case's run history
     * 
     * @return
     */
    public double evaluateTimeVolatility() {
        if (runHistory.size() <= 1) {
            return 0.0;
        }

        double sum = 0.0, v = 0.0;
        for (int i = 0; i < runHistory.size(); i++) {
            sum += runHistory.get(i).getDuration();
        }
        double avg = sum / runHistory.size();
        for (int i = 0; i < runHistory.size(); i++) {
            double diff = runHistory.get(i).getDuration() - avg;
            v += diff * diff;
        }
        return Math.sqrt(v / runHistory.size()) / avg;
    }

    public boolean equals(Object o) {
        return o != null && automation != null && automation.equals(((TestCase) o).getAutomation());
    }

    public int hashCode() {
        return automation != null ? automation.hashCode() : 0;
    }

    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
