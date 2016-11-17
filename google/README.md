# Google Calendar Integrated Time Off

> This module realizes integreation with Microsoft Office 365's APIs

## Install and Set Up the Application

### Prerequisites

- Have a Google account.
- Have the Google calendar time zone the same as your SuccessFactors account.

### Deploy and Set Up the SAP HANA Cloud Platform Extension Application in a HANA Cloud Platform Extension Account

To create a Google API Console project, you need a client ID and a client secret according to these steps:

1. Go to the [Google API Console](https://console.developers.google.com).
1. From the _Project_ drop-down menu, select an existing project, or create a new one by selecting _Create project_.
1. In the _API Manager_ menu, select _Library_, then select _Calendar API_ and _Gmail API_ and enable them.
1. In the _API Manager_ menu, select _Credentials_, then select the _OAuth consent screen_ tab.
1. Specify a product name and save the changes.
1. From the _Create credentials_ drop-down list, choose _OAuth client ID_.
1. Select _Web application_ as an application type.
1. Enter *https://[YOUR APPLICATION URL]/login/google* in the _Authorized redirect URIs_ field. Choose _Create_.
1. From the OAuth client dialog box that appears, copy the client ID and the client secret.
1. In the SAP HANA Cloud Platform Cockpit, create a new destination with name `google_oauth`.

	     Name: google_auth
	     Type: HTTP
	     URL: http://example.com
	     ProxyType: Internet
	     Authentication: BasicAuthentication

1. Add the following additional properties:

        client_id: <Client ID>
        client_secret: <Client Secret>
        TrustAll: true

## Use the Application

### Request a New Time Off

1. Create some events in the **Google Calendar** application that will conflict
   with the timeoff timeframe that you will request on the next step.
1. Request a new Timeoff in SuccessFactors (use the _Leave of Absence_ type
   beacuse it does not require an approval workflow and it takes effect
   immediately).
1. Go to **Google Calendar** and verify that a new _Out Of Office_ event
   coressponding to your request has been created.
1. In [Gmail](https://mail.google.com), navigate to _Settings_ and in the
   _General_ tab check _Vacation responder_ to verify it has been preset
   accordingly as well.
1. Go to SuccessFactors' home page and check the _Meetings to Reschedule_ tile.
   It should list the meetings that conflict with the timeoff you requested. The
   tile refreshes the list every 10 seconds. You can create some more conflicting
   meetings in the Calendar and see them listed in the tile as well. Click on
   a link in the list to go to the Google Calendar Web UI and reschedule or cancel
   the selected event.

### Cancel an Existing Time Off

1. Navigate to the **Google Calendar** and make sure there is an existing Out Of
   Office event.
1. Navigate to the Time Off tool in SuccessFactors and find the coressponding
   Employee Time Off that can be cancelled. (To see the effect immediately, select
   one that doesn't require an approval workflow). Choose _Cancel_.
1. Navigate back to the **Google Calendar** and verify that the Out-Of-Office
   event is gone.
