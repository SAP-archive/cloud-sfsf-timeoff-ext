sap.ui.define([
  "sap/m/routing/RouteMatchedHandler",
  "sap/ui/model/resource/ResourceModel",
  "sap/ui/core/UIComponent"
], function(RouteMatchedHandler, ResourceModel, UIComponent) {
  "use strict";
  return UIComponent.extend('hcp.ext.hcm.timeoff.Component', {
    metadata: {
      name: 'SFSFTimeoff',
      version: '1.0',
      includes: ['css/theme.css'],
      dependencies: {
        libs: [
          'sap.m',
          'sap.ui.layout',
          'sap.ui.commons',
          'sap.suite.ui.commons'
        ]
      },
      rootView: 'hcp.ext.hcm.timeoff.App',
      config: {
        resourceBundle: 'i18n/i18n.properties'
      },
      routing: {
        config: {
          routerClass: "sap.m.routing.Router",
          viewType: 'XML',
          viewPath: 'hcp.ext.hcm.timeoff.views',
          controlId: "AppContainter",
          controlAggregation: "pages",
          bypassed: {
            "target": "notfound"
          }
        },
        routes: [{
          name: 'home',
          pattern: '',
          target: 'master'
        }, {
            name: 'details',
            pattern: '{id}',
            target: 'details'
          }],
        targets: {
          master: {
            viewName: "Master",
            viewLevel: 1
          },
          notfound: {
            viewName: "NotFound",
            transition: "show"
          },
          details: {
            viewName: "Details",
            viewLevel: 2
          }
        }
      }
    },

    init: function() {
      UIComponent.prototype.init.apply(this, arguments);
      var mConfig = this.getMetadata().getConfig();
      var rootPath = jQuery.sap.getModulePath('hcp.ext.hcm.timeoff');
      var i18nModel = new ResourceModel({
        bundleUrl: [
          rootPath,
          mConfig.resourceBundle
        ].join('/')
      });
      this.setModel(i18nModel, 'i18n');

      var router = this.getRouter();
      this.routeHandler = new RouteMatchedHandler(router);
      router.initialize();
    },

    destroy: function() {
      if (this.routeHandler) {
        this.routeHandler.destroy();
      }
      this.getModel('i18n').destroy();
      sap.ui.getCore().getModel().destroy();
      UIComponent.destroy.apply(this, arguments);
    }
  });
});