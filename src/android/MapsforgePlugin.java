/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.suarez.cordova.mapsforge;

import java.util.ArrayList;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.util.Log;

public class MapsforgePlugin extends CordovaPlugin 
{
static final String TAG = "mapsforge-cordova-plugin";
	
	@Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		if ("global-status".equals(action)) {
			this.status();
			callbackContext.success();
			return true;
		} else if (action.contains("native-")) {
			if ("native-set-center".equals(action)) {
				double lat, lng;

				try {
					lat = args.getDouble(0);
					lng = args.getDouble(1);
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
					return true;
				}

				MapsforgeNative.INSTANCE.setCenter(lat, lng);
				callbackContext.success();
				return true;
			} else if("native-set-zoom".equals(action)){
				byte zoom;
				
				try{
					zoom = Byte.parseByte(args.getString(0));
				}catch(NumberFormatException nfe){
					callbackContext.error("Incorrect argument format. Should be: (byte zoom)");
					return true;
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeNative.INSTANCE.setZoom(zoom);
				callbackContext.success();
				return true;
			} else if ("native-show".equals(action)) {
				MapsforgeNative.INSTANCE.show();
				callbackContext.success();
				return true;
			} else if ("native-hide".equals(action)) {
				MapsforgeNative.INSTANCE.hide();
				callbackContext.success();
				return true;
			} else if ("native-marker".equals(action)) {
				String marker;
				double lat, lng;
				int markerId;

				try {
					marker = args.getString(0);
					lat = args.getDouble(1);
					lng = args.getDouble(2);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				Activity context = this.cordova.getActivity();
				
				markerId = context.getResources().getIdentifier(marker, "drawable", context.getPackageName());
				if(markerId == 0){
					markerId = context.getResources().getIdentifier("marker_green", "drawable", context.getPackageName());
				}
				
				int key = MapsforgeNative.INSTANCE.addMarker(markerId, lat, lng);
				callbackContext.success(key);
				return true;
			} else if ("native-polyline".equals(action)) {
				List<Double> points = new ArrayList<Double>();
				int color, strokeWidth;

				try {
					color = args.getInt(0);
					strokeWidth = args.getInt(1);
					
					for(int i=2;i<args.length();i++){
						points.add(args.getDouble(i));
					}
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				int key = MapsforgeNative.INSTANCE.addPolyline(color, strokeWidth, points);
				callbackContext.success(key);
				return true;
			} else if ("native-delete-layer".equals(action)) {
				int key;

				try {
					key = args.getInt(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeNative.INSTANCE.deleteLayer(key);
				callbackContext.success();
				return true;
			} else if ("native-initialize".equals(action)) {
				String path;
				int width, height;
				
				try{
					path = args.getString(0);
					width = args.getInt(1);
					height = args.getInt(2);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				this.initializeNative(path, width, height);
				callbackContext.success();
				return true;
			} else if("native-set-max-zoom".equals(action)){
				byte zoom;
				
				try{
					zoom = Byte.parseByte(args.getString(0));
				}catch(NumberFormatException nfe){
					callbackContext.error("Incorrect argument format. Should be: (byte zoom)");
					return true;
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeNative.INSTANCE.setMaxZoom(zoom);
				callbackContext.success();
				return true;
			} else if("native-set-min-zoom".equals(action)){
				byte zoom;
				
				try{
					zoom = Byte.parseByte(args.getString(0));
				}catch(NumberFormatException nfe){
					callbackContext.error("Incorrect argument format. Should be: (byte zoom)");
					return true;
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeNative.INSTANCE.setMinZoom(zoom);
				callbackContext.success();
				return true;
			} else if("native-show-controls".equals(action)){
				boolean visible;
				
				try{
					visible = args.getBoolean(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeNative.INSTANCE.setBuiltInZoomControls(visible);
				callbackContext.success();
				return true;
			} else if("native-clickable".equals(action)){
				boolean clickable;
				
				try{
					clickable = args.getBoolean(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeNative.INSTANCE.setClickable(clickable);
				callbackContext.success();
				return true;
			} else if("native-show-scale".equals(action)){
				boolean visible;
				
				try{
					visible = args.getBoolean(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeNative.INSTANCE.showScaleBar(visible);
				callbackContext.success();
				return true;
			} else if("native-destroy-cache".equals(action)){
				boolean destroy;
				
				try{
					destroy = args.getBoolean(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeNative.INSTANCE.destroyCache(destroy);
				callbackContext.success();
				return true;
			} else if("native-map-path".equals(action)){
				String path;
				try{
					path = args.getString(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeNative.INSTANCE.setMapFilePath(path);
				callbackContext.success();
				return true;
			} else if("native-cache-name".equals(action)){
				String name;
				try{
					name = args.getString(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeNative.INSTANCE.setCacheName(name);
				callbackContext.success();
				return true;
			} else if("native-theme-path".equals(action)){
				String path;
				try{
					path = args.getString(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeNative.INSTANCE.setRenderThemePath(path);
				callbackContext.success();
				return true;
			} else if("native-stop".equals(action)){
				MapsforgeNative.INSTANCE.onStop();
				callbackContext.success();
				return true;
			} else if("native-start".equals(action)){
				MapsforgeNative.INSTANCE.onStart();
				callbackContext.success();
				return true;
			} else if("native-destroy".equals(action)){
				MapsforgeNative.INSTANCE.onDestroy();
				callbackContext.success();
				return true;
			} else if("native-online".equals(action)){
				String provider, host, baseUrl, extension;
				int port;
				try{
					provider = args.getString(0);
					host = args.getString(1);
					baseUrl = args.getString(2);
					extension = args.getString(3);
					port = args.getInt(4);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeNative.INSTANCE.setOnline(provider, host, baseUrl, extension, port);
				callbackContext.success();
				return true;
			} else if("native-offline".equals(action)){
				String mapPath, renderPath;
				try{
					mapPath = args.getString(0);
					renderPath = args.getString(1);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeNative.INSTANCE.setOffline(mapPath, renderPath);
				callbackContext.success();
				return true;
			}
		} else if (action.contains("cache-")) {
			if ("cache-get-tile".equals(action)) {
				long x, y;
				byte zoom;
				
				try{
					x = args.getLong(0);
					y = args.getLong(1);
					zoom = Byte.parseByte(args.getString(2));
				} catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				} catch(NumberFormatException nfe){
					callbackContext.error(nfe.getMessage());
					return true;
				}
				String path = MapsforgeCache.INSTANCE.getTilePath(x, y, zoom);

				callbackContext.success(path);
				return true;
			} else if("cache-initialize".equals(action)){
				String mapFilePath;
				
				try{
					mapFilePath = args.getString(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				this.initializeCache(mapFilePath);
				callbackContext.success();
				return true;
			} else if("cache-map-path".equals(action)){
				String mapFilePath;
				
				try{
					mapFilePath = args.getString(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeCache.INSTANCE.setMapFilePath(mapFilePath);
				callbackContext.success();
				return true;
			} else if("cache-max-size".equals(action)){
				int sizeInMB;
				
				try{
					sizeInMB = args.getInt(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeCache.INSTANCE.setMaxCacheSize(sizeInMB);
				callbackContext.success();
				return true;
			} else if("cache-max-age".equals(action)){
				long ageInMillis;
				
				try{
					ageInMillis = args.getLong(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeCache.INSTANCE.setMaxCacheAge(ageInMillis);
				callbackContext.success();
				return true;
			} else if("cache-cleaning-trigger".equals(action)){
				int sizeInMB;
				
				try{
					sizeInMB = args.getInt(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeCache.INSTANCE.setCleanCacheTrigger(sizeInMB);
				callbackContext.success();
				return true;
			} else if("cache-enabled".equals(action)){
				boolean cacheEnabled;
				
				try{
					cacheEnabled = args.getBoolean(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeCache.INSTANCE.setCacheEnabled(cacheEnabled);
				callbackContext.success();
				return true;
			} else if("cache-external".equals(action)){
				boolean externalCache;
				
				try{
					externalCache = args.getBoolean(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeCache.INSTANCE.setExternalCache(externalCache);
				callbackContext.success();
				return true;
			} else if("cache-name".equals(action)){
				String cacheName;
				
				try{
					cacheName = args.getString(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeCache.INSTANCE.setCacheName(cacheName);
				callbackContext.success();
				return true;
			} else if("cache-tile-size".equals(action)){
				int tileSize;
				
				try{
					tileSize = args.getInt(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeCache.INSTANCE.setTileSize(tileSize);
				callbackContext.success();
				return true;
			} else if("cache-clean-destroy".equals(action)){
				boolean cleanOnDestroy;
				
				try{
					cleanOnDestroy = args.getBoolean(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeCache.INSTANCE.setCleanOnDestroy(cleanOnDestroy);
				callbackContext.success();
				return true;
			} else if("cache-theme-path".equals(action)){
				String path;
				
				try{
					path = args.getString(0);
				}catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				}
				
				MapsforgeCache.INSTANCE.setRenderTheme(path);
				callbackContext.success();
				return true;
			} else if("cache-screen-ratio".equals(action)){
				Float ratio;
				
				try{
					ratio = Float.parseFloat(args.getString(0));
				} catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				} catch(NumberFormatException nfe){
					callbackContext.error(nfe.getMessage());
					return true;
				}
				
				MapsforgeCache.INSTANCE.setScreenRatio(ratio);
				callbackContext.success();
				return true;
			} else if("cache-overdraw".equals(action)){
				float overdrawFactor;
				
				try{
					overdrawFactor = Float.parseFloat(args.getString(0));
				} catch(JSONException je){
					callbackContext.error(je.getMessage());
					return true;
				} catch(NumberFormatException nfe){
					callbackContext.error(nfe.getMessage());
					return true;
				}
				
				MapsforgeCache.INSTANCE.setOverdrawFactor(overdrawFactor);
				callbackContext.success();
				return true;
			} else if("cache-destroy".equals(action)){
				MapsforgeCache.INSTANCE.onDestroy();
				callbackContext.success();
				return true;
			}
		}
		return false; // Returning false results in a "MethodNotFound" error.
	}

	public void initializeCache(String mapFilePath){
		MapsforgeCache.createInstance(this.cordova.getActivity(), mapFilePath);
	}

	public void initializeNative(String mapFilePath, int width, int height){
		MapsforgeNative.createInstance(this.cordova.getActivity(), mapFilePath, width, height);
	}

	public void status(){
		String cacheCreated = (MapsforgeCache.INSTANCE == null)?"FALSE":"TRUE";
		String nativeCreated = (MapsforgeNative.INSTANCE == null)?"FALSE":"TRUE";
		
		Log.i(MapsforgePlugin.TAG,"[Status][1/2]: Mapsforge cache initialized..."+ cacheCreated);
		Log.i(MapsforgePlugin.TAG,"[Status][2/2]: Mapsforge native initialized..."+ nativeCreated);
	}
}

