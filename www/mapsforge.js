var exec = require('cordova/exec');

function mapsforge_plugin(){
	
};

mapsforge_plugin.prototype.status = function(){
		exec(this.doNothing, this.doNothing, "MapsforgePlugin", "global-status", []);
};

mapsforge_plugin.doNothing = function(){return;};

module.exports = new mapsforge_plugin();