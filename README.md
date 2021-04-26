![](https://img.shields.io/badge/STATUS-NOT%20CURRENTLY%20MAINTAINED-red.svg?longCache=true&style=flat)

# Important Notice
This public repository is read-only and no longer maintained.

# Integrated Time Off

> A SAP HANA Cloud Platform Extension of SuccessFactors Time Off requests process,
integrated with calendar providers.

[![Build Status](https://travis-ci.org/SAP/cloud-sfsf-timeoff-ext.svg?branch=master)](https://travis-ci.org/SAP/cloud-sfsf-timeoff-ext)

## Introduction

The focus of this example is to demonstrate the integration of SuccessFactors
and calendar providers  by means of an SAP HANA Cloud Platform (HCP) extension
application, consuming Intelligent Services (IS) _Employee Time_ events and
working with calendar providers to react to those events.

The example provides patterns for:

- Implementation of HCP extensions for event-driven scenarios leveraging
  SuccesFactors Intelligent Services.
- Orchestration of multiple SAP and non-SAP services in HCP: SuccessFactors
  OData API and third-party calendar APIs.
- Integration of HCP HTML5 UI.
- Maven project organization and build for extension applications.

This application builds on edge Java 8 technologies (lambda
expressions) and innovative programming paradigms (reactive programming).

The application source is organized for convenient insight and reuse into your
own proof of concept, that is not necessarily production ready.  Consider it as a
learning source and fast-track prototyping basis rather than a production ready
solution.

## Table of Contents

<!-- Use https://github.com/jonschlinkert/markdown-toc to generate the TOC. Run
     markdown-toc -i README.md -->

<!-- toc -->

- [Conventions used](#conventions-used)
- [Application Scenario Highlights](#application-scenario-highlights)
- [Technical components overview](#technical-components-overview)
  * [app](#app)
  * [notification-service](#notification-service)
  * [commons](#commons)
  * [google](#google)
  * [frontend](#frontend)
- [Get the Source, Build and Run locally](#get-the-source-build-and-run-locally)
  * [Source](#source)
  * [Build](#build)
  * [Run](#run)
- [Installation and setup](#installation-and-setup)
  * [Prerequisites](#prerequisites)
  * [Deploy and setup the HCP Extension Application in a HANA Cloud Platform Extension Account](#deploy-and-setup-the-hcp-extension-application-in-a-hana-cloud-platform-extension-account)
  * [Setup Intelligent Services event notifications on the SuccessFactors tenant](#setup-intelligent-services-event-notifications-on-the-successfactors-tenant)
  * [Setup the Meetings to Reschedule tile in the SuccessFactors tenant Home Page for all users](#setup-the-meetings-to-reschedule-tile-in-the-successfactors-tenant-home-page-for-all-users)
- [Using the application](#using-the-application)
  * [Requesting new Time Off](#requesting-new-time-off)
  * [Cancelling an existing Time Off](#cancelling-an-existing-time-off)
- [Operations Monitoring](#operations-monitoring)
- [Copyright and License](#copyright-and-license)

<!-- tocstop -->

## Conventions

The following conventions are used in this documentation:

*  **CALENDAR** - refres to provider's Calendar


## Application Scenario Highlights

The extension adds the following features to the normal SuccessFactors Time Off
request process:

- Time off requests managed in SuccessFactors are automatically managed in the
  **CALENDAR**.
- Conflicting meetings for all upcoming time off are listed in a dedicated Tile
  in SuccessFactors Home Page and feature link to the **CALENDAR**'s UI to
	reschedule.

## Technical Components Overview

### app

The server-side code for the timeoff application implementing the two main use
cases for handling `EmployeeTime` lifecycle events (actions) and queries for
conflicting meetings. This module largely acts as application logic controller.
The input triggering the logic is either the WebService payload for the events
raised from SuccessFactors Intelligent Services, or the end user requests from
the SAPUI5 application integrated in SuccessFactors Home Page for listing
conflicting events. Based on this input, the controlling logic resolves the
event and the handler for it and delegates further processing to it. Finally, it
wraps post-processes the response in a suitable for the requester form - for
events raised from SuccessFactors Intelligent Services this is a response
payload that will be send back from the WebService and for the UI application
this is JSON payload consumable by the SAPUI5 client code.

### notification-service

Implements a SOAP WebService that listens for push notifications on
`EmployeeTime` events changes raised from SuccessFactors Intelligent Services.  A
notification handler is invoked upon reciept of an event to process the payload
from the WebService action request and return a valid response.  The handler
implementation is dynamically loaded, using Spring injection mechanism, where
Spring will be looking for any implementations of the `NotificationHandler`
interface and resolve them as eligible notification handlers. Theoretically, the
mechanism is generic enough to imply and be capable of orchestrating multiple
implementations, but in this application we supply a single
`TimeoffNotificaitonHandler`.

### commons

Implementations of (normally domain-agnostic) routines commonly used throughout
the code.

### google

Implements the `CalendarServiceProvider` interface for a Google
subscription. The implemented interface is the contract between the application
controller logic implemented in [app](#app) and concrete implementations,
in this case Google GMail and Calendar. It declares the actions and queries on
which the application business logic is built.

For more information, see the [documentation](google/README.md).

### frontend

Java EE Web application module. Aggregates all dependencies and provisions the
SAPUI5 client code and the Spring Boot Application class.  One of the products
of building this module is the WAR file comprising the server-side services
and the user interfaces that should be integrated in the SuccessFactors Home
Page as a custom tile. Another product of the build is the HCP HTML5 app archive
that can be imported either in the WEbIDE for further development and/or
deployed as an HTML5 application directly in a HANA Cloud Platform account.
This deployment scenario is suitable for:
1) managing/developing the UI completely decoupled from the application services
2)

## Get the Source, Build and Run Locally

### Source

The source is available on GitHub. Use Git's functionality to clone the
repository or download it as a ZIP file.

### Build

The source uses Maven. Navigate to the root folder and
invoke the command `mvn clean package`. The build results can be found in the
webapp project's target directory `ROOT.war` that contains both the server-side
code and the user interface and is ready to be deployed in an HCP account and
`timeoff-tile-html5.zip` should you decide to decouple and manage the UI artifact
as an HTML5 application.

### Run

To run locally, install the SAP HANA Cloud Platform Tools in Eclipse and run a local
Java Web Tomcat 7 server. Then deploy the `ROOT.war` in it.

## Installation and Setup

### Prerequisites

- Eclipse installed with SAP HANA Cloud Platform Tools plugins.
- JDK 1.8 set as an Installed JRE in
  *Windows > Preferences > Java > Installed JREs*.
- Java Web Tomcat 7 set as a runtime environment in
  *Windows > Preferences > Server > Runtime Environments*.
- All classes must be compiled with --parameter option.
  **Windows > Prefrerences > Java > Compiler -> Store information about
  method parameters (usable via reflection)**.
- A valid HCP extension account with trust settings set up for a corresponding
  SuccessFactors tenant.
- A SuccessFactors user that has an administrative access to the Admin Center and the
  Event Notification Subscription tool.
- The SuccessFactors and **CALENDAR**'s user accounts are harmonized to use
  the same timezone and emails.

> Note: Refer to the documentation of invididual modules for prerequisites requirements.

### Deploy and Set Up the HCP Extension Application in a SAP HANA Cloud Platform Extension Account

1. Login to your HCP account in the cockpit.
1. Go to _Java Applications_ and choose _Deploy Application_.
1. Change the following properties in the form:
	 * **WAR File Location**: Choose _Browse..._ and select the application war file
	 * **Application Name**: _timeoff_
	 * **Runtime Name**: _Java Web Tomcat 7_
	 * **JVM Version**: _JRE8_

   and choose _Deploy_ and as soon the application is deployed
   you can also start it.

1. Navigate to the application _timeoff_ dashboard and click _Destinations_ in
   the navigation on the left
1. Follow the same process to import the destination `sap_hcmcloud_core_odata`
   from the same location and set it up for your SuccessFactors tenant accordingly.
   Use BasicAuthentication and a dedicated system user for this destination. The
	 applicaiton will use these to receive events from Intelligent Services and send
   back status responses.
1. The last step is to register the SAP HANA Cloud Platform Extension
   Application as an authorized Assertion Consumer Service in SuccessFactors
   Provisioning. If you do this manually, use the following url template to fetch
   the metadata that features the data reqired for the setup:
   https://{host}.successfactors.com/idp/samlmetadata?company={company-name}.

> Note: Refer to the documentation of invididual modules for setup requirements.

### Set Up Intelligent Services Event Notifications on the SuccessFactors Tenant

1. Login to `https://<host>.successfactors.com` with admin user
1. Go to **Admin Center** and in the **Tool Search** box type _Event
   Notification Subscription_ and select the search hit.
1. In the **Event Notification Subscription** tool, select the **Subscriber** tab
1. Click the **+ Add Subscriber** button and type the following details in the row for edit:
	 *  **Subscriber Id**: _HCPSubscriber_
	 *  **Name**: _HCP Subscriber_
	 *  **Group**: _HCP_
	 *  **Client Id**: _HCP_
1. In the **Event Notification Subscription** tool, select the **SEB External Event** tab
1. Click the **+Add Topic** button and choose **Employee Time** from the select control
1. Click the **+ Add Subscription** button and type the following details in the row for edit:
   *  **Endpoint URL**: <https://timeoff{account-id}.{dc}.hana.ondemand.com/services/NotifyImplPort>
		 > Note: You can find the HCP application URL in the application dashboard in the cockpit

   *  **Subscriber**: _HCPSubscriber_
	 *  **Protocol**: _SOAP over HTTP/HTTPS_
	 *  **Authentication**: leave as it is (defaults to _No authentication_)

### Setup the Meetings to Reschedule tile in the SuccessFactors tenant Home Page for all users

1. Go to **Admin center** (still logged in with the credentials from section B)
   and use the **Manage Home Page** tool
1. Click **Add Custom Tile** button and follow the wizard
1. Provide the following details in the wizard pages:

  *  **Properties wizard page:**
	  *  **Name**: _Meetings to Reschedule_
	  *  **Description**: _This tile shows all the conflicting meetings due to
	     the approved time off request. It also has a link to navigate to a
         conflicting **CALENDAR**'s Web UI event and reschedule or cancel it._

  *  **Tile wizard page:**
	  *  **Title**: _Meetings to Reschedule_
	  *  **Size**: _1x1_
	  *  **Icon**: _appointment_

  *  **Navigation wizard page:**
     *  **Tile Target**: _Popover_

        In the content editor click the _Source_ button and type

        ```
<p><iframe border="0" frameborder="0"	src="https://timeoff{account-id}.{dc}.hana.ondemand.com/" style="width:	622px;height:600px"></iframe></p>
        ```

        > Note: Change the _src_ value to fit your own application URL details.
				  You can find the HCP application URL in the application dashboard in the cockpit.


  *  **Assignments wizard page:**
     *  **Assign to section**: _My Info_

1. Finally click **Finish** in the wizard toolbar and **Save** in the **Home
   Page Management** tool.
1. Navigate to the **Home Page** to verify the tile is available and pops up
   when clicked on the user Home Page.


## Using the application

### Requesting new Time Off

1. Create some events in **CALENDAR** that will conflict with the
   time off timeframe that you will request on the next step.
1. Request a new Timeoff in SuccessFactors (use Leave of Absence type beacuse it
   doesn't require approval workflow and is effective immediately)
1. Go to **CALENDAR** and verify that a new Out Of Office event
   coressponding to your request has been created.
1. In **CALENDAR**, navigate to Mailbox settings and check Autoreply to
   verify it has been preset accordingly too.
1. Go to SuccessFactors **Home page** and check the **Meetings to Reschedule**
   tile – it should list the meetings that conflict with the timeoff you request.
   The tile refreshes the list every 10 seconds. You can create some more
   conflicting meetings in the Calendar and check them listed in the tile too.
   Click on a link in the list to go to the **CALENDAR**'s Web UI and reschedule or
   cancel the selected event.

### Cancelling an existing Time Off

1. Navigate to the **CALENDAR** and make sure there is an existing
   Out Of Office event.
1. Navigate to the Time Off tool in SuccessFactors and find the coressponding
   Employee Time Off that can be cancelled. (To see the effect immediately seelct
   one that doesn't require approval workflow). Click **Cancel** on it.
1. Navigate back the **CALENDAR** and verify that the Out-Of-Office
   event is gone.

## Operations Monitoring

Navigate to SuccessFactors **Admin Center** > **Event Notifications Audit Log**
and filter by your subsciber id (_HCPSubscriber_ in this setup). The list that
is retrieved for you presents the events sent to this subscriber and the
responses from the HCP application event handling service that was received for
each. The response details will either inform for a successfull processing of
the event or will present an error and some helpful insight on its context for
further problem tracing.

## Copyright and License

See [LICENSE](LICENSE)
