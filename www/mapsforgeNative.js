var exec = require('cordova/exec');

function mapsforge_native(){
	
};

mapsforge_native.prototype = {
		COLOR_DKGRAY: -12303292,
		COLOR_CYAN: -16711681,
		COLOR_BLACK: -16777216,
		COLOR_BLUE: -16776961,
		COLOR_GREEN: -16711936,
		COLOR_RED: -65536,
		COLOR_WHITE: -1,
		COLOR_TRANSPARENT: 0,
		COLOR_YELLOW: -256,
		
		MARKER_RED: "marker_red",
		MARKER_GREEN: "marker_green",
		MARKER_BLUE: "marker_blue",
		MARKER_YELLOW: "marker_yellow",
		MARKER_BLACK: "marker_black",
		MARKER_WHITE: "marker_white",
		
		addMarker: function(params, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-marker", params);
		},
		
		addPolyline: function(params, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-polyline", params);
		},

		deleteLayer: function(key, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-delete-layer", [key]);
		},

		destroyCacheOnExit: function(destroyCache, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-destroy-cache", [destroyCache]);
		},
		
		hide : function(callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-hide", []);
		},
		
		initialize : function(params, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-initialize", params);
		},

		onDestroy: function(callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-destroy", []);
		},

		onStart: function(callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-start", []);
		},
		
		onStop: function(callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-stop", []);
		},
		
		setCacheName: function(cacheName, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-cache-name", [cacheName]);
		},
		
		setCenter: function(lat, lng, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-set-center", [lat,lng]);
		},

		setClickable: function(viewClickable, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-clickable", [viewClickable]);
		},

		setMapFile: function(absoluteMapFilePath, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-map-path", [absoluteMapFilePath]);
		},
		
		setMaxZoom: function(maxZoom, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-set-max-zoom", [maxZoom]);
		},

		setMinZoom: function(minZoom, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-set-min-zoom", [minZoom]);
		},

		setOfflineTileLayer: function(params, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-offline", params);
		},
		
		setOnlineTileLayer: function(params, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-online", params);
		},
		
		setThemePath: function(themePath, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-theme-path", [themePath]);
		},
		
		setZoom: function(zoomLevel, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-set-zoom", [zoomLevel]);
		},
	
		show : function(callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-show", []);
		},
		
		showBuiltInControls: function(showControls, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-show-controls", [showControls]);
		},
		
		showScaleBar: function(showScaleBar, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "native-show-scale", [showScaleBar]);
		}
};

mapsforge_native.doNothing = function(){};

module.exports = new mapsforge_native();