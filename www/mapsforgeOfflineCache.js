var exec = require('cordova/exec');

function mapsforge_cache(){
	
};

mapsforge_cache.prototype = {
		
		initialize: function(absoluteMapFilePath, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "cache-initialize", [absoluteMapFilePath]);
		},
		
		destroyCacheOnExit: function(destroyCache, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "cache-clean-destroy", [destroyCache]);
		},
		
		getTile: function(params, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "cache-get-tile", params);
		},

		onDestroy: function(callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "cache-destroy", []);
		},
		
		setCacheCleaningTrigger: function(sizeInMegabytes, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "cache-cleaning-trigger", [sizeInMegabytes]);
		},
		
		setCacheEnabled: function(enabled, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "cache-enabled", [enabled]);
		},
		
		setCacheName: function(cacheName, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "cache-name", [cacheName]);
		},

		setExternalCache: function(externalCache, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "cache-external", [externalCache]);
		},

		setMapFile: function(absoluteMapFilePath, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "cache-map-path", [absoluteMapFilePath]);
		},

		setMaxCacheAge: function(ageInMilliseconds, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "cache-max-age", [ageInMilliseconds]);
		},
		
		setMaxCacheSize: function(sizeInMegaBytes, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "cache-max-size", [sizeInMegaBytes]);
		},
		
		setOverdrawFactor: function(overdrawFactor, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "cache-overdraw", [overdrawFactor]);
		},
		
		setScreenRatio: function(screenRatio, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "cache-screen-ratio", [screenRatio]);
		},

		setThemePath: function(themePath, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "cache-theme-path", [themePath]);
		},

		setTileSize: function(tileSize, callbacks){
			var callbacks = callbacks || {};
			exec(callbacks.onSuccess || this.doNothing, callbacks.onError || this.doNothing, "MapsforgePlugin", "cache-tile-size", [tileSize]);
		}
};

mapsforge_cache.doNothing = function(){};

module.exports = new mapsforge_cache();