TestMP
======

Test Management Platform for Automation

------

# Setup #
Once downloaded, unpack the compressed folder to see the structure of (the compiled) TestMP. You'll see a structure like this:

	testmp/
	|-- bin/      # contains the scripts for startup & maintenace.
	|-- conf/     # contains the configuration file.
	|-- data/     # contains the database files.
	|-- lib/      # contains the TestMP client libraries. 
	|-- log/      # contains the generated log files.
	|-- webapp/   # contains the wars of WebConsole and DataStore.
    |-- README.md

It's not forced but recommended that you set the TESTMP_HOME environment variable, of which the value is the path to the *testmp/*, if you hope to launch TestMP outside such directory.

Before running TestMP, you also need to have Java Runtime Environment (Version 6 or the above) installed, and make sure the JAVA_HOME is set correctly.

# Configuration #
You will find most TestMP configurations in the *conf/testmp.properties*:

	# The launching url of the DataStore each for test case, data, and environment.
	testCaseStoreUrl=http://localhost:10081/DataStore.do
	testDataStoreUrl=http://localhost:10082/DataStore.do
	testEnvStoreUrl=http://localhost:10083/DataStore.do

	# The number of threads for concurrently running task.
	executionThreadNum=10
	# The timeout for task execution. 0 means no timeout.
	executionTimeout=0

	# The time gap (in seconds) between two refreshings of the task schedule.
	scheduleRefreshingTimeGap=1800
	# The maximum latency (in seconds) to trigger a scheduled task, or ignore it.
	taskTriggerMaxLatency=600

	# The default recipient list and subject of the test metrics report. 
	testMetricsReportRecipients=
	testMetricsReportSubject=

	# The default recipient list and subject of the test env status report.
	envStatusReportRecipients=
	envStatusReportSubject=

	# The default SMTP settings to sending the report.
	smtpSettingHost=
	smtpSettingPort=25
	smtpSettingUser=
	smtpSettingPass=
	smtpSettingSTARTTLS=true

While most settings can be left to its default value, *testCaseStoreUrl*, *testDataUrl*, *testEnvUrl* may need to be modified if the default listening ports have been occupied or they are launched remotely.

It's also possible to make these urls the same to share only one datastore. But in practice it will not be efficient and may cause confusion.

The settings of *\*ReportRecipients*, *\*ReportSubject*, and *smtpSetting\** are for your convenience. You can always input/modify them manually on the TestMP Web Console.

# Launch the TestMP #
Now we're ready to startup the TestMP!

* On Windows

		cd TESTMP_HOME/bin
		startup.bat [port]

* On Linux / Mac

		cd TESTMP_HOME/bin
		./startup.sh [port]

The argument *port* is optional. If it is not given, the Web Console will defaultly use 10080 as the listening port. Then you may see the output like this:

	launching testCaseStore on 10081
	launching testDataStore on 10082
	launching testEnvStore on 10083
	launching TestMP web console on 10080
	...
	2013-07-04 14:26:55.634:INFO:oejs.AbstractConnector:Started SelectChannelConnector@0.0.0.0:10080

which meanse the datastores for test case, data, and environment, and the web console are successfully and fully launched.

Open your favorite browser, enter "http://yourhost:10080" in the address bar and click Go. You see the the welcome page on the TestMP Web Console? Congratulations!

# Features #

### Instant Update of Test Case Doc & Status ###
**Everything in automation code**

Document test cases just in code, and no longer bother to maintain them in another place.

The web console will instantly update test document and measures upon test run.

### Auto-generation of Metrics Report to Signoff ###
**Evaluate the automation test**

Test robustness, effectiveness, and efficiency will be evaluated into a clean but meaningful test report.

You can signoff the report by marking "Accept" or "Refuse" and send it to the stakeholders!

### Object-based Test Data Storage & Service ###
**Get test data object via service**

You can add test data into the data store, categorize it by tags, and use it by API in automation.

The data is kept as object and can be complex. Different data can be merged together to be new.

### Centralized Test Environment Management ###
**Driven by Task Engine**

Environment tasks are defined, scheduled, and bounded to scripts on local/remote hosts.

Task executes concurrently, which can be monitored and traced from the Web Console.

------

Copyright © 2013 Zhaowei Ding
