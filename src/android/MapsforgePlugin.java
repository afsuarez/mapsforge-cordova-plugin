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

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.util.Log;

public class MapsforgePlugin extends CordovaPlugin {
	/**
	 * Plugin's tag for log purposes
	 */
	static final String TAG = "mapsforge-cordova-plugin";

	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callbackContext) throws JSONException {
		if ("global-status".equals(action)) {
			this.status();
			callbackContext.success();
			return true;
		} else if (action.contains("native-")) {
			if ("native-set-center".equals(action)) {
				try {
					MapsforgeNative.INSTANCE.setCenter(args.getDouble(0),
							args.getDouble(1));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("native-set-zoom".equals(action)) {
				try {
					MapsforgeNative.INSTANCE.setZoom(Byte.parseByte(args
							.getString(0)));
					callbackContext.success();
				} catch (NumberFormatException nfe) {
					callbackContext
							.error("Incorrect argument format. Should be: (byte zoom)");
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("native-show".equals(action)) {
				try {
					MapsforgeNative.INSTANCE.show();
					callbackContext.success();
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}
				return true;
			} else if ("native-hide".equals(action)) {
				MapsforgeNative.INSTANCE.hide();
				return true;
			} else if ("native-marker".equals(action)) {
				try {
					Activity context = this.cordova.getActivity();

					int markerId = context.getResources().getIdentifier(
							args.getString(0), "drawable",
							context.getPackageName());
					if (markerId == 0) {
						Log.i(MapsforgePlugin.TAG,
								"Marker not found...using default marker: marker_green");
						markerId = context.getResources().getIdentifier(
								"marker_green", "drawable",
								context.getPackageName());
					}

					int markerKey = MapsforgeNative.INSTANCE.addMarker(
							markerId, args.getDouble(1), args.getDouble(2));
					callbackContext.success(markerKey);
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("native-polyline".equals(action)) {

				try {
					JSONArray points = args.getJSONArray(2);

					if (points.length() % 2 != 0)
						throw new JSONException(
								"Invalid array of coordinates. Length should be multiple of 2");

					int polylineKey = MapsforgeNative.INSTANCE.addPolyline(
							args.getInt(0), args.getInt(1), points);

					callbackContext.success(polylineKey);
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("native-delete-layer".equals(action)) {

				try {
					MapsforgeNative.INSTANCE.deleteLayer(args.getInt(0));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("native-initialize".equals(action)) {

				try {

					MapsforgeNative.createInstance(this.cordova.getActivity(),
							args.getString(0), args.getInt(1), args.getInt(2));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (IllegalArgumentException e) {
					callbackContext.error(e.getMessage());
				} catch (IOException e) {
					callbackContext.error(e.getMessage());
				} catch(Exception e){
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("native-set-max-zoom".equals(action)) {

				try {
					MapsforgeNative.INSTANCE.setMaxZoom(Byte.parseByte(args
							.getString(0)));
					callbackContext.success();
				} catch (NumberFormatException nfe) {
					callbackContext
							.error("Incorrect argument format. Should be: (byte zoom)");
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("native-set-min-zoom".equals(action)) {

				try {
					MapsforgeNative.INSTANCE.setMinZoom(Byte.parseByte(args
							.getString(0)));
					callbackContext.success();
				} catch (NumberFormatException nfe) {
					callbackContext
							.error("Incorrect argument format. Should be: (byte zoom)");
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("native-show-controls".equals(action)) {

				try {
					MapsforgeNative.INSTANCE.setBuiltInZoomControls(args
							.getBoolean(0));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("native-clickable".equals(action)) {

				try {
					MapsforgeNative.INSTANCE.setClickable(args.getBoolean(0));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("native-show-scale".equals(action)) {

				try {
					MapsforgeNative.INSTANCE.showScaleBar(args.getBoolean(0));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("native-destroy-cache".equals(action)) {

				try {
					MapsforgeNative.INSTANCE.destroyCache(args.getBoolean(0));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				}

				return true;
			} else if ("native-map-path".equals(action)) {

				try {
					MapsforgeNative.INSTANCE.setMapFilePath(args.getString(0));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (IllegalArgumentException iae) {
					callbackContext.error(iae.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("native-cache-name".equals(action)) {

				try {
					MapsforgeNative.INSTANCE.setCacheName(args.getString(0));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("native-theme-path".equals(action)) {

				try {
					MapsforgeNative.INSTANCE.setRenderThemePath(args
							.getString(0));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (IllegalArgumentException e) {
					callbackContext.error(e.getMessage());
				} catch (IOException e) {
					callbackContext.error(e.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("native-stop".equals(action)) {
				try {
					MapsforgeNative.INSTANCE.onStop();
					callbackContext.success();
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("native-start".equals(action)) {
				try {
					MapsforgeNative.INSTANCE.onStart();
					callbackContext.success();
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("native-destroy".equals(action)) {
				try {
					MapsforgeNative.INSTANCE.onDestroy();
					callbackContext.success();
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("native-online".equals(action)) {

				try {
					MapsforgeNative.INSTANCE.setOnline(args.getString(0),
							args.getString(1), args.getString(2),
							args.getString(3), args.getInt(4));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("native-offline".equals(action)) {

				try {
					MapsforgeNative.INSTANCE.setOffline(args.getString(0),
							args.getString(1));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (IllegalArgumentException e) {
					callbackContext.error(e.getMessage());
				} catch (IOException e) {
					callbackContext.error(e.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			}
		} else if (action.contains("cache-")) {
			if ("cache-get-tile".equals(action)) {

				try {
					final long x = args.getLong(0);
					final long y = args.getLong(1);
					final byte z = Byte.parseByte(args.getString(2));
					final CallbackContext callbacks = callbackContext;

					cordova.getThreadPool().execute(new Runnable() {
						public void run() {
							try {
								String path = MapsforgeCache.INSTANCE
										.getTilePath(x, y, z);
								callbacks.success(path);
							} catch (IOException e) {
								callbacks.error(e.getMessage());
							} catch (Exception e) {
								callbacks.error(e.getMessage());
							}
						}
					});
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (NumberFormatException nfe) {
					callbackContext.error(nfe.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("cache-initialize".equals(action)) {

				try {
					MapsforgeCache.createInstance(cordova.getActivity(),
							args.getString(0));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (IllegalArgumentException e) {
					callbackContext.error(e.getMessage());
				} catch (IOException e) {
					callbackContext.error(e.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("cache-map-path".equals(action)) {

				try {
					final String mapFile = args.getString(0);
					final CallbackContext callbacks = callbackContext;

					cordova.getThreadPool().execute(new Runnable() {

						@Override
						public void run() {
							try {
								MapsforgeCache.INSTANCE.setMapFilePath(mapFile);
								callbacks.success();
							} catch (IllegalArgumentException e) {
								callbacks.error(e.getMessage());
							} catch (FileNotFoundException e) {
								callbacks.error(e.getMessage());
							} catch (Exception e) {
								callbacks.error(e.getMessage());
							}
						}
					});
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("cache-max-size".equals(action)) {

				try {
					MapsforgeCache.INSTANCE.setMaxCacheSize(args.getInt(0));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("cache-max-age".equals(action)) {

				try {
					MapsforgeCache.INSTANCE.setMaxCacheAge(args.getLong(0));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("cache-cleaning-trigger".equals(action)) {

				try {
					MapsforgeCache.INSTANCE
							.setCleanCacheTrigger(args.getInt(0));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("cache-enabled".equals(action)) {

				try {
					MapsforgeCache.INSTANCE.setCacheEnabled(args.getBoolean(0));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("cache-external".equals(action)) {

				try {
					MapsforgeCache.INSTANCE
							.setExternalCache(args.getBoolean(0));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("cache-name".equals(action)) {

				try {
					MapsforgeCache.INSTANCE.setCacheName(args.getString(0));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("cache-tile-size".equals(action)) {

				try {
					MapsforgeCache.INSTANCE.setTileSize(args.getInt(0));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("cache-clean-destroy".equals(action)) {

				try {
					MapsforgeCache.INSTANCE.setCleanOnDestroy(args
							.getBoolean(0));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("cache-theme-path".equals(action)) {

				try {
					final CallbackContext callbacks = callbackContext;
					final String themePath = args.getString(0);

					cordova.getThreadPool().execute(new Runnable() {

						@Override
						public void run() {
							try {
								MapsforgeCache.INSTANCE
										.setRenderTheme(themePath);
								callbacks.success();
							} catch (IllegalArgumentException e) {
								callbacks.error(e.getMessage());
							} catch (FileNotFoundException e) {
								callbacks.error(e.getMessage());
							} catch (Exception e) {
								callbacks.error(e.getMessage());
							}
						}
					});
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("cache-screen-ratio".equals(action)) {

				try {
					MapsforgeCache.INSTANCE.setScreenRatio(Float
							.parseFloat(args.getString(0)));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (NumberFormatException nfe) {
					callbackContext.error(nfe.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("cache-overdraw".equals(action)) {

				try {
					MapsforgeCache.INSTANCE.setOverdrawFactor(Float
							.parseFloat(args.getString(0)));
					callbackContext.success();
				} catch (JSONException je) {
					callbackContext.error(je.getMessage());
				} catch (NumberFormatException nfe) {
					callbackContext.error(nfe.getMessage());
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			} else if ("cache-destroy".equals(action)) {
				try {
					MapsforgeCache.INSTANCE.onDestroy();
					callbackContext.success();
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

				return true;
			}
		}
		return false; // Returning false results in a "MethodNotFound" error.
	}

	/**
	 * Check which one of the modes is active (or both), and log it with
	 * <i>INFO</i> level
	 */
	public void status() {
		String cacheCreated = (MapsforgeCache.INSTANCE == null) ? "FALSE"
				: "TRUE";
		String nativeCreated = (MapsforgeNative.INSTANCE == null) ? "FALSE"
				: "TRUE";

		Log.i(MapsforgePlugin.TAG,
				"[Status][1/2]: Mapsforge cache initialized..." + cacheCreated);
		Log.i(MapsforgePlugin.TAG,
				"[Status][2/2]: Mapsforge native initialized..."
						+ nativeCreated);
	}
}
