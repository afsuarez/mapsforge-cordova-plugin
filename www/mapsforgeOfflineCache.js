var exec = require('cordova/exec');

function mapsforge_cache(){
	
};

mapsforge_cache.prototype = {
		
		initialize: function(absoluteMapFilePath, success, error){
			exec(success || this.doNothing, error || this.doNothing, "MapsforgePlugin", "cache-initialize", [absoluteMapFilePath]);
		},
		
		destroyCacheOnExit: function(destroyCache, success, error){
			exec(success || this.doNothing, error || this.doNothing, "MapsforgePlugin", "cache-clean-destroy", [destroyCache]);
		},
		
		getTile: function(params, success, error){
			exec(success || this.doNothing, error || this.doNothing, "MapsforgePlugin", "cache-get-tile", params);
		},

		onDestroy: function(success, error){
			exec(success || this.doNothing, error || this.doNothing, "MapsforgePlugin", "cache-destroy", []);
		},
		
		setCacheCleaningTrigger: function(sizeInMegabytes, success, error){
			exec(success || this.doNothing, error || this.doNothing, "MapsforgePlugin", "cache-cleaning-trigger", [sizeInMegabytes]);
		},
		
		setCacheEnabled: function(enabled, success, error){
			exec(success || this.doNothing, error || this.doNothing, "MapsforgePlugin", "cache-enabled", [enabled]);
		},
		
		setCacheName: function(cacheName, success, error){
			exec(success || this.doNothing, error || this.doNothing, "MapsforgePlugin", "cache-name", [cacheName]);
		},

		setExternalCache: function(externalCache, success, error){
			exec(success || this.doNothing, error || this.doNothing, "MapsforgePlugin", "cache-external", [externalCache]);
		},

		setMapFile: function(absoluteMapFilePath, success, error){
			exec(success || this.doNothing, error || this.doNothing, "MapsforgePlugin", "cache-map-path", [absoluteMapFilePath]);
		},

		setMaxCacheAge: function(ageInMilliseconds, success, error){
			exec(success || this.doNothing, error || this.doNothing, "MapsforgePlugin", "cache-max-age", [ageInMilliseconds]);
		},
		
		setMaxCacheSize: function(sizeInMegaBytes, success, error){
			exec(success || this.doNothing, error || this.doNothing, "MapsforgePlugin", "cache-max-size", [sizeInMegaBytes]);
		},
		
		setOverdrawFactor: function(overdrawFactor, success, error){
			exec(success || this.doNothing, error || this.doNothing, "MapsforgePlugin", "cache-overdraw", [overdrawFactor]);
		},
		
		setScreenRatio: function(screenRatio, success, error){
			exec(success || this.doNothing, error || this.doNothing, "MapsforgePlugin", "cache-screen-ratio", [screenRatio]);
		},

		setThemePath: function(themePath, success, error){
			exec(success || this.doNothing, error || this.doNothing, "MapsforgePlugin", "cache-theme-path", [themePath]);
		},

		setTileSize: function(tileSize, success, error){
			exec(success || this.doNothing, error || this.doNothing, "MapsforgePlugin", "cache-tile-size", [tileSize]);
		}
};

mapsforge_cache.doNothing = function(){};

module.exports = new mapsforge_cache();