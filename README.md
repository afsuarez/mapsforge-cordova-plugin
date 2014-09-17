mapsforge-cordova-plugin
========================

Plugin for Apache Cordova (aka Phonegap) that wraps the [mapsforge](http://www.mapsforge.org) libraries(v 0.4).

Index
=====

1. [Overview](#overview)
2. [Installation](#installation)
3. [Mapsforge native](#mapsforge-native)
4. [Mapsforge offline tile layer](#mapsforge-offline-tile-layer)
5. [Contribute](#contribute)
6. [License](#license)

Overview
========

This plugin is available for Android(Amazon and FireOS as well) and allows you to use mapsforge in two modes: native, and as an offline tile layer (for use with other libraries like Leaflet).

Currently the wiki is under construction, so the main methods will be shown below as a quick usage guide, and if you need additional information you can contact me.

All the necessary libraries are imported with the plugin automatically.

Installation
============

Once you have your Cordova project created, you can add this plugin to it going to the project's folder and executing the following command:
```
cordova plugins add https://github.com/afsuarez/mapsforge-cordova-plugin.git
```

If you want to uninstall it, simply execute:
```
cordova plugins rm com.suarez.cordova.mapsforge
```

Mapsforge native
================
This mode will run mapsforge directly in the device, what means that it will add the map view directly on top on any other view. This may look unappropriately since you will not be able to manage it like a ``HTML div`` element, but there are methods to solve that problems and it renders the map faster which may be important for you. With this mode you can display offline maps from ``.map`` files that user has in his device, or you can display maps from an online tile provider.

In order to access this mode you will have to use the `mapsforge.embedded` object. Its main methods and constants are:

Constant values
---------------
```
mapsforge.embedded.COLOR_DKGRAY: -12303292;
mapsforge.embedded.COLOR_CYAN: -16711681;
mapsforge.embedded.COLOR_BLACK: -16777216;
mapsforge.embedded.COLOR_BLUE: -16776961;
mapsforge.embedded.COLOR_GREEN: -16711936;
mapsforge.embedded.COLOR_RED: -65536;
mapsforge.embedded.COLOR_WHITE: -1;
mapsforge.embedded.COLOR_TRANSPARENT: 0;
mapsforge.embedded.COLOR_YELLOW: -256;

mapsforge.embedded.MARKER_RED: "marker_red";
mapsforge.embedded.MARKER_GREEN: "marker_green";
mapsforge.embedded.MARKER_BLUE: "marker_blue";
mapsforge.embedded.MARKER_YELLOW: "marker_yellow";
mapsforge.embedded.MARKER_BLACK: "marker_black";
mapsforge.embedded.MARKER_WHITE: "marker_white";
```

Methods
-------

*Optional parameters (callbacks are always optional).

+ ``initialize([String mapFilePath, int viewWidth, int viewHeight], {onSuccess, onError}*)``: The map file path provided must be the absolute file path. You can specify the ``width`` and `height` values for the view that will be added, or you can set them to ``0`` for set the value to ``MATCH_PARENT``. You must call this method before any other method.
+ ``show({onSuccess, onError}*)``: To show the map view.
+ ``hide({onSuccess, onError}*)``: To hide the map view.
+ ``setCenter(double lat, double lng, {onSuccess, onError}*)``: Sets the center of the map to the given coordinates.
+ ``setZoom(byte zoomLevel, {onSuccess, onError}*)``: Sets the zoom to the specified value (if it is between the zoom limits).
+ ``setMaxZoom(byte maxZoom, {onSuccess, onError}*)``: Sets the maximum zoom level.
+ ``setMinZoom(byte minZoom, {onSuccess, onError}*)``: Sets the minimum zoom level.
+ ``setOfflineTileLayer([String mapFilePath, String renderThemePath], {onSuccess, onError}*)``: The path to the map ile is required, and the path to the render theme may be ``null`` in order to apply the default render theme.
+ ``setOnlineTileLayer([String providerName, String host, String baseUrl, String extension, int port], {onSuccess, onError}*)``: The best way to explain this function is through a call 
```
mapsforge.embedded.setOnlineTileLayer(['MapQuest', 'otile1.mqcdn.com', '/tiles/1.0.0/map/', 'png', 80]);
```
+ ``addMarker([String marker, double lat, double lng], {onSuccess, onError}*)``: Adds a marker to the map in the specified coordinates and returns the key for that marker to the ``onSuccess`` function. That key is the one you have to use if you want to delete it. The color of the marker should be one of the constants shown at the beginning of this section; if the marker doesn't exist a green marker will be used instead.
+ ``deleteLayer(int key, {onSuccess, onError}*)``: Deletes the layer with the specified key from the map.

Mapsforge offline tile layer
============================
The offline tile layer, or offline cache, uses a ``.map`` file to render the tiles in ``PNG`` format and stores them in the device's cache. Then it returns the path of the generated tiles, so any other library/plugin can use them.

In order to use this mode, you will have to use the ``mapsforge.cache`` object. Here go the main methods:

*Optional parameters (callbacks are always optional).

+ ``initialize(String mapFilePath, {onSuccess, onError}*)``: You should call this method before any other one, and provide it with the absolute map file path.
+ ``getTile([double lat, double lng, byte zoom], {onSuccess, onError}*)``: This method is the one that provides the tiles, generating them if their are not in the cache. Despite the ``onSuccess`` function is optional, you should provide a valid function since this method will return the tile's path to your ``onSuccess`` function.

As an example, a [Leaflet](http://www.leafletjs.com) code will be provided below:
```
var map = L.map('map-div');
	
L.OfflineTileLayer = L.TileLayer.extend({
	getTileUrl : function(tilePoint, tile) {
		var zoom = tilePoint.z, x = tilePoint.x, y = tilePoint.y;
		
		if (mapsforge.cache) {
			mapsforge.cache.getTile([x,y,zoom],{ onSuccess: function(result) {tile.src=result;}, 
			                                     onError: function() {tile.src = "path to an error image";}});
		}else{
			tile.src = "path to an error image";
		}
	},

	_loadTile: function (tile, tilePoint) {
    tile._layer = this;
    tile.onload = this._tileOnLoad;
    tile.onerror = this._tileOnError;

    this._adjustTilePoint(tilePoint);
    this.getTileUrl(tilePoint, tile);

    this.fire('tileloadstart', {
                tile: tile,
		            url: tile.src
		        });
	}
});

L.offlineTileLayer = function (options) {
  return new L.OfflineTileLayer(null, options);
};
		
L.offlineTileLayer({
    maxZoom: 18
}).addTo(map);
		
map.setView([43.360594,-5.849361],18);
```

Contribute
==========
Feel free to contribute, add issues about bringing more features to the plugin, detecting bugs...

License
=======
MIT license ;)
