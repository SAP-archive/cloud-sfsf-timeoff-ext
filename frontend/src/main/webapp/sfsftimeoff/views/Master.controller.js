sap.ui.define([
  "hcp/ext/hcm/timeoff/views/BaseController",
  "sap/ui/model/json/JSONModel",
  "sap/m/MessageToast",
], function(BaseController, JSONModel, MessageToast) {
  "use strict";
  return BaseController.extend("hcp.ext.hcm.timeoff.views.Master", {

    onInit: function() {
	 var thiz = this;
         // TODO: enable websocket
         this.timeout = setInterval(jQuery.proxy(this.getData, this), 10000);
         this.getView().setBusy(true);

         this.getData();

    },

    formatDate: function(input) {
      var date = new Date(input.dateTime);
      if (input.timeZone = 'UTC') {
        return date.toUTCString();
      }
      return date.toLocaleString();
    },

    onExit: function() {
      clearInterval(this.timeout);
    },

    formatAttendees: function(attendees) {
      if (!attendees) {
        return;
      }
      var users = attendees.slice(0, 3).map(function name(value) {
	  if(value.emailAddress){
		  return value.emailAddress.name;
	  } else{
	      return value;
	  }
      }).join(", ");
      var extra = attendees.length - 3;
      if (extra > 0) {
        var othersText = "others";
        if (extra === 1) {
          othersText = "other";
        }
        users = "With " + users + " and " + extra + " " + othersText;
      }
      return users;
    },

    formatTitle: function(attendees) {
      return attendees.length;
    },
    getData: function() {
      var thiz = this;
      jQuery.getJSON("api/v1/conflicting")
        .done(function(data) {
          thiz.getView().setModel(new JSONModel(data));
        })
        .fail(function(errObj, m, status) {
		if(errObj.status == 302){
			window.location.replace(errObj.responseText);
		} else{
                MessageToast.show(status, {
                  duration: 5000
                });
		}
        })
        .always(function() {
          thiz.getView().setBusy(false)
        });
    }
  });
});