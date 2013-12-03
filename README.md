Auto Scaling the Cloud Platform - orchestrator and status publisher tools
=========================================================================

Copyright
---------

Copyright 2013 Google Inc. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


Disclaimer
----------

This sample application is not an official Google product.


Orchestrator tool
=================

Summary
-------

This application implements a tool for auto-scaling Google Compute Engine VMs via an orchestrator implemented on Google App Engine.

It is intended to be run in tandem with the status publisher tool that will publish information about Compute Engine VMs. See the README for the status publisher for more information.


Supported Components
--------------------

Languages:
<ul>
<li>Java</li>
</ul>

Google Cloud Platform:
<ul>
<li>App Engine</li>
<li>Compute Engine</li>
</ul>

Downloads
---------

Download the sample code. Untar/unzip the file into a directory of your choice.


Setting up the Application
--------------------------

This sample requires that you have projects on App Engine and the API Console.

Note: If you have not developed applications using App Engine before, it may be beneficial (though not strictly necessary) to skim through the [Getting Started Guide](https://developers.google.com/appengine/docs/java/gettingstarted/).

App Engine development is very quick and easy on Eclipse, because you get to use the nice Google Plugin. Download the following:

1. Make sure you have Java installed. You can install Open JDK from [here](http://openjdk.java.net/).
2. Download and set up Eclipse.
3. Download the [App Engine Java SDK](http://googleappengine.googlecode.com/files/appengine-java-sdk-1.8.7.zip)
4. Download the Google Plugin for Eclipse for your IDE version [here](https://developers.google.com/eclipse/docs/getting_started). Set it up using the instructions on that page.

After setting up the Google Plugin for Eclipse, import the orchestrator as an existing project in eclipse. To do this:

<ol>
<li> File > Import > Existing Projects into Workspace
<li> Select the root directory where you unzipped the orchestrator code.
<li> Click OK.
</ol>

You will have to add the AppEngine nature for this project. To do this, right-click on the newly created project. Under Google > App Engine Settings..., select the checkbox next to "Use Google App Engine". Make sure to enter you the ID of your AppEngine project as well as the version number. If this checkbox is already checked, unselect it, click OK, then follow the above steps again to select it and click OK again.

Finally, you will need to download gson-2.2.4.jar and add it to the war/WEB-INF/lib directory (you may have to create this directory).

Configuring the Application
---------------------------
#### appengine-web.xml

If you haven't done so, update the <application> tag to your own application ID as well as the version number.

#### Orchestration config file
Take a look at war/WEB-INF/config.xml. We have pre-populated this configuration file with some default values. You will need to provide the following:

1. The ID of the Compute Engine project where your application and the status publisher tool are running.
2. The zone your application is running in.
3. Your Compute Engine project's API key. In this sample, we use a simple API key. The API key can be found in the [API console](https://code.google.com/apis/console/). More information can be found [here](https://developers.google.com/console/help/#UsingKeys). This assumes that the orchestration app is separate project from the application running on Compute Engine.
4. The location (Cloud Storage bucket) and name of your startup script.

You should modify all the values in this file to suit your needs. Please refer to the paper "Auto Scaling on the Google Cloud Platform" for documentation for the parameters. Also refer to the README for the accompanying status publisher tool for more information.
Please note that if any of the scaling parameters result in a contradictory decision, no action is taken by the orchestrator tool. For instance, if the minimum-ave-cpu-load would dictate that an instance should be shut down, but the maximum-ave-cpu-load would dictate that a new instance should be created, no action is taken.

#### Other configuration
Your Compute Engine project (where the status publisher and your app are running) must add the orchestrator project's service account to its team.  The service account name can be found in the AppEngine console under Application Settings > Service Account Name.

Deploying and Running
---------------------

Deploy your App Engine application. Go to the landing page at your-app-id.appspot.com.

In order to see the orchestrator in action, you must have the accompanying status publisher running on a GCE instance. See the README for the status publisher for details.

To ensure that the orchestrator and status publisher tools are running properly together:

1. Set the maximum-ave-cpu-load to 0, the minimum-ave-cpu-load to 30 (both in config.xml) and then redeploy the orchestrator tool to App Engine.
2. On your Compute Engine instance, run
> $ sudo apt-get install stress  
> $ stress --cpu 10

Then verify that:

1. The CPU load on your instance is > 0 by directly querying http://&lt;VM Instance External IP&gt;:8080/StatusPublisher/status.
2. A new instance is spun up after the orchestrator tool gets called by the cron job. You can see this in AppEngine backend logs.

Important Assumptions
---------------------
The orchestrator pool currently assumes the following:

1. All instances with the specified prefix in the specified zone (both are specified in config.xml) will report status at http://&lt;VM Instance External IP&gt;:8080/StatusPublisher/status. The orchestrator further expects the JSON format that is described in the README for the status publisher.
2. When an instance is given the "prepare to shutdown" signal, it is responsible for either shutting itself down or publishing the following status: READY\_TO\_SHUT\_DOWN. The orchestrator will then shut it down. The orchestrator will wait for either of these to occur.

Status publisher tool
=====================

Summary
-------

This application implements a simple status publisher tool. It is a light-weight web application that collects and makes available the system statistics about the VM which it is running on, and optionally your application statistics. The data is used for auto-scaling Google Compute Engine VMs via an orchestrator tool implemented on Google App Engine. The orchestrator tool polls the data from this application at "http://&lt;VM Instance External IP&gt;:8080/StatusPublisher/status".

Here is a sample statistics published by this sample application in JSON format.

    {"currentStatus":"RUNNING",
      "systemStats":
        {"numProcessors":1.0,"cumSystemLoad":0.4,"aveSystemLoad":0.2,"memUsedRatio":0.15082616},
      "applicationStats":
        {"numTasksProcessing":0,"numTasksProcessedLastMin":0,"lastLeasedDate":"2013-08-14 21:48:29"}
    }

Please see the README for the orchestrator tool for more information of how these statistics are used.

The status publisher web application is designed to run in tandem with your application on Google Compute Engine. Every VM on Compute Engine that runs your application should also be running this status publisher application at the same time.

Supported Components
--------------------

Languages:

* Java

Google Cloud Platform:

* Compute Engine

Downloads
---------

Download the sample code. Create a directory and extract the archive file in this directory. You will see the following files and directories:

    build.xml
    common/
    LICENSE
    README.md
    src/
    startup.sh
    war/

Customizing
-----------

config.xml
----------

As mentioned in the summary, the status publisher tool will publish statistics that the orchestrator tool can use for scaling decisions. Each VM on Compute Engine will run both the status publisher tool as well as your own application. 

The status publisher tool will publish statistics about the VM, but it can also publish statistics about the status of your application. There are two choices on how to integrate your application with the status publisher tool.

1. poll - The status publisher tool polls your application at a regular interval. Your application will need to have a light-weight WEB server to handle the polling.
2. push - Your application publishes its statistics to the status publisher at "http://localhost:&lt;port&gt;/StatusPublisher/applicationStats".

The mode is configurable with the war/WEB-INF/config.xml file. If you do not need to use any application statistics for the scaling decision, just set the mode to push and don't push any statistics.

In either mode, the status publisher is expecting a JSON representation of the following Java class:
> com.google.cloud.solutions.sampleapps.orchestration.common.GceApplicationStats.

The following is the sample Java code showing how to convert GceApplicationStats to the JSON representation using the Gson library.

    Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    String jsonString = gson.toJson(gceApplicationStats);

Note that in order for the orchestrator tool to work, you must publish status in this format.

startup.sh
----------

When a new instance boots up, you need to supply a start-up script so that both the status publisher and your application will run on any new VM that is brought up. Here, we include a file (startup.sh) as an example. You will need to edit this file with any startup you need for your own application (in addition to the status publisher). Then upload the startup script to Google Cloud Storage. Then change the orchestrator tool configuration (which is also called config.xml) to use this start up script whenever a new instance is started.

Building 
--------

You will need to download the following jar files and add them to the war/WEB-INF/lib directory.

1. servlet-api-2.5.jar
2. gson-2.2.4.jar

> $ cp servlet-api-2.5.jar war/WEB-INF/lib

> $ cp gson-2.2.4.jar war/WEB-INF/lib

Use ant to build StatusPublisher.war. You may have to install ant. This build step requires Java 1.7.

> $ ant -f build.xml


Setting Up
----------

### Pre-requisites

1 If you don't yet have a [Cloud Console](https://cloud.google.com/console) project, create one with Compute Engine enabled.
2 Create a Compute Engine instance. 

> **Note: The name of the instance must have the same prefix as the instancePrefix you set in the orchestrator tool's configuration. It also must also be created in the same zone as the one you set in the orchestrator tool's configuration.**

Please refer to the [Google Compute Engine Developer Guide](https://developers.google.com/compute/) for information on how these are done.

### Setting Up

Install a servlet container such as [jetty](http://www.eclipse.org/jetty/), add the StatusPublisher.war file that you built above to the appropriate directory and start the servlet container. The following instructions are for jetty. The startup script provided with the orchestrator tool assumes jetty as well.

1. From the machine where you built StatusPublisher.war, push the war file to the Compute Engine instance. The following command will put the file in your home directory on the instance.
> gcutil --project=&lt;YOUR COMPUTE ENGINE PROJECT&gt; push &lt;YOUR INSTANCE&gt; StatusPublisher.war .

2. ssh into the instance
> $ gcutil --project=&lt;YOUR COMPUTE ENGINE PROJECT&gt; ssh &lt;YOUR INSTANCE&gt;

3. Install jetty if it is not installed already. Also install openjdk-7-jre, as the latest versions of jetty require it.
> $ sudo apt-get install jetty openjdk-7-jre -y

4. Copy StatusPublisher.war to the jetty's webapps directory
> $ sudo cp /home/&lt;user name&gt;/StatusPublisher.war /var/lib/jetty/webapps

5. Start jetty:
> $ cd /usr/share/jetty  
> $ sudo /usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java -jar start.jar &

Configuring Google Compute Engine Firewall
------------------------------------------

You need to setup a firewall to allow the orchestrator tool to access status publisher tool. The following command adds a firewall that allows http traffic to port 8080. This is sufficient for the status publisher (it will be able to respond to requests on port 8080), but you may have to add additional firewalls for your application.

> $ gcutil --project=&lt;YOUR COMPUTE ENGINE PROJECT&gt; addfirewall http2 --description="Incoming http allowed." --allowed="tcp:8080"

Your instance should now be running and publishing its status. Try it by navigating with your browser to http://&lt;VM Instance External IP&gt;:8080/StatusPublisher/status. You should now also make sure your application is running on the same instance.

Creating a Snapshot
-------------------

In order for the orchestrator tool to create new instances with your application, you will need to create a snapshot of your instance that is running both your application and the status publisher. This snapshot will be used as the boot source for the new instance.

1. Visit the [Cloud Console](https://cloud.google.com/console)
2. Select your Compute Engine Project, then click on Compute Engine
3. Select Snapshots
4. Click the NEW SNAPSHOT button
5. Specify a name and choose your instance's disk as the source disk.

Make sure that the name of the snapshot matches the name you set in the orchestrator tool's config.xml for the snapshotName. By default, this is statuspublisher-snapshot.


Important Assumptions
---------------------

1. It is the responsibility of the VM on which the status publisher runs to publish status at http://&lt;VM Instance External IP&gt;:8080/StatusPublisher/status.
2. When the orchestrator gives a VM the "shut down when ready" signal, it is the responsibility of the VM to either shut itself down or else publish the status READY\_TO\_SHUT\_DOWN.
3. Keep in mind that the orchestrator and status publisher tools will communicate based on matching configuration parameters. In particular, the following have to be true:

* If in the orchestrator tool you set the "instancePrefix", you must make sure that all your instances where status publisher is running have names that begin with that prefix.  
* When new instances are created, they will be created with a snapshot that you must create in your Compute Engine project, as well as with the startup script you provide.  
* The orchestrator tool will currently only poll instances in a specified zone (set in the orchestrator's config.xml). Your instances must be running in that zone.  
* The orchestrator tool must have access to your Compute Engine project ID, API key, zone, etc. Please see the orchestrator tool's README for further information.

