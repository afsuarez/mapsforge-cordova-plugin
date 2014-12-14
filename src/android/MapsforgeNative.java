package com.suarez.cordova.mapsforge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OnlineTileSource;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.SparseArray;
import android.view.WindowManager;
import android.widget.RelativeLayout;

/**
 * This class allows you to create a mapsforge map directly in the device by
 * adding the map view to the top of the current view content.
 * 
 * @author Adolfo Fernandez Suarez
 * @version 0.1
 */
public class MapsforgeNative {
	/**
	 * Remember to initialize this instance with
	 * {@code MapsforgeNative.createInstance} in order to avoid getting a null
	 * object, and therefore <code>NullPointerException</code> when invoking any
	 * method.
	 */
	public static MapsforgeNative INSTANCE;

	/**
	 * Creates a new instance of <code>MapsforgeNative</code>.
	 * 
	 * @param context
	 *            The context obtained from the main plugin class through
	 *            <code>this.cordova.getActivity()</code>
	 * @param mapFilePath
	 *            Absolute path to the map file that will be used. Set it up
	 *            even if you will change to an online tile provider
	 * @param width
	 *            MapView width. Set it to 0 in order to
	 *            <code>MATCH_PARENT</code>
	 * @param height
	 *            MapView height. Set it to 0 in order to
	 *            <code>MATCH_PARENT</code>
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public static void createInstance(Activity context, String mapFilePath,
			int width, int height) throws IllegalArgumentException, IOException {
		INSTANCE = new MapsforgeNative(context, mapFilePath, width, height);
	}

	// Application context
	private Activity context;
	// Graphic factory
	private AndroidGraphicFactory graphicFactory;
	// Needed objects
	private String cacheName;
	private MapView mapView;
	private TileCache tileCache;
	private TileRendererLayer tileRendererLayer;
	private TileDownloadLayer tileDownloadLayer;
	private OnlineTileSource onlineTileSource;

	private Layers layers;
	private String mapFilePath;
	private File mapFile;
	private String renderThemePath;

	private XmlRenderTheme renderTheme;

	private MapViewPosition mapViewPosition;

	private boolean destroyCache;
	private int width, height;
	private boolean online;
	// Array with the layers(markers and polylines added by the user)
	private SparseArray<Layer> layersOnMap;
	private int layerCounter;

	/**
	 * Creates a new instance of mapsforge with the specified map and layout
	 * size.
	 * 
	 * @param context
	 *            The application context that is obtained from the main plugin
	 *            class through <code>this.cordova.getActivity()</code>
	 * @param mapFilePath
	 *            The absolute path to the map file (.map)
	 * @param width
	 *            Width for the view. If you set this parameter to 0, the width
	 *            will <code>MATCH_PARENT</code>
	 * @param height
	 *            Height for the view. If you set this parameter to 0, the
	 *            height will <code>MATCH_PARENT</code>
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	private MapsforgeNative(Activity context, String mapFilePath, int width,
			int height) throws IllegalArgumentException, IOException {
		setContext(context);
		setGraphicFactory(AndroidGraphicFactory.INSTANCE);
		setMapFilePath(mapFilePath);
		setCacheName("mapcache");

		prepareMapThemes();

		setRenderThemePath(this.context.getFilesDir() + "/renderthemes/assets.xml");

		destroyCache = true;

		this.width = (width <= 0) ? WindowManager.LayoutParams.MATCH_PARENT
				: width;
		this.height = (height <= 0) ? WindowManager.LayoutParams.MATCH_PARENT
				: height;

		layersOnMap = new SparseArray<Layer>();
		layerCounter = 0;

		initializeMap();
	}

	/**
	 * Adds a new marker to the MapView at the specified location. The returned
	 * id is the one that has to be used in order to delete the marker from the
	 * MapView through the method <code>deleteLayer</code>.
	 * 
	 * @see #deleteLayer(int)
	 * 
	 * @param drawableId
	 *            Id of the image that will be placed as marker
	 * @param lat
	 *            Latitude
	 * @param lng
	 *            Longitude
	 * @return The identifier of the marker that has been created
	 */
	public int addMarker(int drawableId, double lat, double lng) {
		Drawable drawable = context.getResources().getDrawable(drawableId);
		Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
		Marker marker = new Marker(new LatLong(lat, lng), bitmap, 0,
				-bitmap.getHeight() / 2);

		layers.add(marker);

		layersOnMap.append(layerCounter, marker);
		return layerCounter++;
	}

	/**
	 * Adds a new polyline to the MapView using the coordinates passed as
	 * <code>points</code>. That array uses the values in odd positions for the
	 * latitude, and the even positions for the longitude. The returned value is
	 * the identifier needed to delete the polyline from the view.
	 * 
	 * @see #deleteLayer(int)
	 * @param color
	 *            Color for the polyline
	 * @param strokeWidth
	 *            Width
	 * @param points
	 *            Array of doubles with the coordinates
	 * @return The identifier for delete the polyline in the future
	 * @throws JSONException
	 *             If the arguments passed as points can not be retrieved as
	 *             doubles
	 */
	public int addPolyline(int color, int strokeWidth, JSONArray points)
			throws JSONException {
		Paint paint = graphicFactory.createPaint();
		paint.setColor(color);
		paint.setStrokeWidth(strokeWidth);
		paint.setStyle(Style.STROKE);

		Polyline polyline = new Polyline(paint, graphicFactory);
		List<LatLong> latlongs = polyline.getLatLongs();

		for (int i = 0; i < points.length(); i += 2) {
			latlongs.add(new LatLong(points.getDouble(i), points
					.getDouble(i + 1)));
		}

		layers.add(polyline);

		layersOnMap.append(layerCounter, polyline);
		return layerCounter++;
	}

	// Copy an inputStream in an outputStream
	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
	}

	/**
	 * Deletes an element that has been added to the layer manager such markers
	 * and polylines.
	 * 
	 * @param key
	 *            The identifier for the layer to delete
	 */
	public void deleteLayer(int key) {
		layers.remove(layersOnMap.get(key, null));
		layersOnMap.delete(key);
	}

	/**
	 * Sets the flag to delete the cache when the <code>onDestroy</code> method
	 * is called.
	 * 
	 * @param destroy
	 *            True to delete the cache, false to keep it.
	 */
	public void destroyCache(boolean destroy) {
		destroyCache = destroy;
	}

	/**
	 * The cache name is the name of the folder that is created under the
	 * <i>cache</i> application directory.
	 * 
	 * @return The name of the cache
	 */
	public String getCacheName() {
		return cacheName;
	}

	/**
	 * The context is where the MapView will be added when it is created.
	 * 
	 * @return The context used for creating the MapView.
	 */
	public Activity getContext() {
		return context;
	}

	/**
	 * The AndroidGraphicFactory instance is used internally with mapsforge so
	 * it can render the tiles.
	 * 
	 * @return The instance of AndroidGraphicFactory used at the moment.
	 */
	public AndroidGraphicFactory getGraphicFactory() {
		return graphicFactory;
	}

	/**
	 * Gets the map file that is being used for rendering the offline map.
	 * 
	 * @return Absolute path to the map file.
	 */
	public String getMapFilePath() {
		return mapFilePath;
	}

	/**
	 * The render theme is used to render the tiles with different styles, such
	 * only the buildings, use different colors, etc.
	 * 
	 * @return Absolute path to the theme file.
	 */
	public String getRenderThemePath() {
		return renderThemePath;
	}

	/**
	 * Hides the MapView so you can show any other content. This is useful since
	 * the view is added directly to the application and otherwise it is not
	 * possible to change its visibility from the cordova application.
	 * 
	 * @see #show()
	 */
	public void hide() {
		mapView.setVisibility(MapView.GONE);
	}

	// Initializes the view to the default values.
	private void initializeMap() {
		mapView = new MapView(context);
		mapViewPosition = mapView.getModel().mapViewPosition;

		setClickable(true);
		showScaleBar(false);
		setBuiltInZoomControls(false);
		setMinZoom((byte) 8);
		setMaxZoom((byte) 20);
		setCenter(0d, 0d);
		setZoom((byte) 8);
		layers = mapView.getLayerManager().getLayers();

		// create a tile cache of suitable size
		this.tileCache = AndroidUtil.createTileCache(context, cacheName,
				mapView.getModel().displayModel.getTileSize(), 1f,
				mapView.getModel().frameBufferModel.getOverdrawFactor());

		// tile renderer layer using internal render theme
		this.tileRendererLayer = new TileRendererLayer(tileCache,
				mapViewPosition, false, graphicFactory);
		tileRendererLayer.setMapFile(mapFile);
		tileRendererLayer.setXmlRenderTheme(renderTheme);

		// only once a layer is associated with a mapView the rendering starts
		layers.add(tileRendererLayer);

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				width, height);

		context.addContentView(mapView, params);
	}

	/**
	 * Destroys the cache if the destroy flag is activated.
	 * 
	 * @see #destroyCache(boolean)
	 */
	public void onDestroy() {
		if (destroyCache) {
			tileCache.destroy();
		}
	}

	/**
	 * Initializes the renderer layer that was being used when the
	 * <code>onStop</code> method was called.
	 */
	public void onStart() {
		if (online) {
			tileDownloadLayer = new TileDownloadLayer(tileCache,
					mapViewPosition, onlineTileSource, graphicFactory);

			layers.add(tileDownloadLayer);
		} else {
			tileRendererLayer = new TileRendererLayer(tileCache,
					mapViewPosition, false, graphicFactory);
			tileRendererLayer.setMapFile(mapFile);
			tileRendererLayer.setXmlRenderTheme(renderTheme);

			layers.add(tileRendererLayer);
		}
	}

	/**
	 * Stops and cleans up the render layer.
	 */
	public void onStop() {
		if (online) {
			layers.remove(tileDownloadLayer);
			tileDownloadLayer.onDestroy();
		} else {
			layers.remove(tileRendererLayer);
			tileRendererLayer.onDestroy();
		}
	}

	// Copies the render themes from the application to the internal memory
	private void prepareMapThemes() throws IOException {
		copyFileOrDirInAssets("renderthemes");
	}
	
	private void copyFileOrDirInAssets(String path) throws IOException {
		AssetManager assetManager = context.getAssets();
		String assets[] = null;
		assets = assetManager.list(path);
		if (assets.length == 0) {
			copyFileInAssets(path);
		} else {
			String fullPath = this.context.getFilesDir() + "/" + path;

			File dir = new File(fullPath);
			if (!dir.exists())
				dir.mkdir();
			for (int i = 0; i < assets.length; ++i) {
				copyFileOrDirInAssets(path + "/" + assets[i]);
			}
		}
	}
	
	private void copyFileInAssets(String filename) throws IOException {
		AssetManager assetManager = context.getAssets();

		InputStream in = null;
		OutputStream out = null;
		in = assetManager.open(filename);
		File f = new File(this.context.getFilesDir() + "/" + filename);
		out = new FileOutputStream(f);

		copyFile(in, out);

		in.close();
		in = null;
		out.flush();
		out.close();
		out = null;
	}

	/**
	 * Sets the visibility to the built in zoom controls.
	 * 
	 * @param builtInZoomControls
	 *            True to show the controls, false to hide them.
	 */
	public void setBuiltInZoomControls(boolean builtInZoomControls) {
		mapView.setBuiltInZoomControls(builtInZoomControls);
	}

	/**
	 * Sets the cache name to the specified value. This name is the one that
	 * will be used for the folder in the cache directory.
	 * 
	 * @param cacheName
	 *            New name for the cache.
	 */
	public void setCacheName(String cacheName) {
		if (cacheName == null || cacheName.equals(""))
			return;

		this.cacheName = cacheName;

		layers.remove(tileRendererLayer);

		this.tileCache = AndroidUtil.createTileCache(context, this.cacheName,
				mapView.getModel().displayModel.getTileSize(), 1f,
				mapView.getModel().frameBufferModel.getOverdrawFactor());

		// tile renderer layer using internal render theme
		this.tileRendererLayer = new TileRendererLayer(tileCache,
				mapViewPosition, false, graphicFactory);
		tileRendererLayer.setMapFile(mapFile);
		tileRendererLayer.setXmlRenderTheme(renderTheme);

		layers.add(tileRendererLayer);
	}

	/**
	 * Sets the enter of the MapView to the specified coordinates.
	 * 
	 * @param lat
	 *            Latitude.
	 * @param lng
	 *            Longitude.
	 */
	public void setCenter(double lat, double lng) {
		mapViewPosition.setCenter(new LatLong(lat, lng));
	}

	/**
	 * Sets whether or not the view respond to touch events.
	 * 
	 * @param clickable
	 *            True to make the view touchable, false to reject touch events.
	 */
	public void setClickable(boolean clickable) {
		mapView.setClickable(clickable);
	}

	/**
	 * Sets the context where the view is going to be added.
	 * 
	 * @param context
	 *            New context.
	 */
	public void setContext(Activity context) {
		this.context = context;
	}

	/**
	 * Sets the AndroidGraphicFactory that is going to be used for render the
	 * tiles. If the instance does not exist, a new one will be created.
	 * 
	 * @param graphicFactory
	 *            Instance of AndroidGraphicFactory(
	 *            <code>AndroidGraphicFactory.INSTANCE;</code>).
	 */
	public void setGraphicFactory(AndroidGraphicFactory graphicFactory) {
		if (graphicFactory == null) {
			if (this.context != null) {
				AndroidGraphicFactory.createInstance(this.context
						.getApplication());
				this.graphicFactory = AndroidGraphicFactory.INSTANCE;
			}
		} else {
			this.graphicFactory = graphicFactory;
		}
	}

	/**
	 * Sets the map file for the offline rendering.
	 * 
	 * @param mapFilePath
	 *            Absolute path to the map file. This file must have <i>map</i>
	 *            extension.
	 * @throws FileNotFoundException
	 */
	public void setMapFilePath(String mapFilePath)
			throws IllegalArgumentException, FileNotFoundException {
		if (mapFilePath != null
				&& mapFilePath.substring(mapFilePath.length() - 4,
						mapFilePath.length()).equals(".map")) {
			this.mapFilePath = mapFilePath;

			mapFile = new File(mapFilePath);
			if (mapFile == null || !mapFile.exists()) {
				throw new FileNotFoundException("Map file not found.");
			}

			if (tileRendererLayer != null) {
				tileRendererLayer.setMapFile(mapFile);
			}
		} else {
			throw new IllegalArgumentException(
					"Incorrect map file path or incorrect file format (should be .map)");
		}
	}

	/**
	 * Sets the maximum zoom for the map. Tiles over it will not be rendered.
	 * 
	 * @param zoom
	 *            Maximum zoom.
	 */
	public void setMaxZoom(byte zoom) {
		mapViewPosition.setZoomLevelMax(zoom);
	}

	/**
	 * Sets the minimum zoom for the map. Tiles under that zoom will not be
	 * rendered.
	 * 
	 * @param zoom
	 *            Minimum zoom.
	 */
	public void setMinZoom(byte zoom) {
		mapViewPosition.setZoomLevelMin(zoom);
	}

	/**
	 * Changes the tile layer from online tile provider to the offline tile
	 * renderer.
	 * 
	 * @param mapFilePath
	 *            Absolute path to the map file that will be rendered.
	 * 
	 * @param renderThemePath
	 *            Absolute path to the theme file that will be used. If
	 *            <code>null</code> is passed, the default theme will be
	 *            applied.
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public void setOffline(String mapFilePath, String renderThemePath)
			throws IllegalArgumentException, IOException {
		setMapFilePath(mapFilePath);
		setRenderThemePath(renderThemePath);

		tileRendererLayer = new TileRendererLayer(tileCache, mapViewPosition,
				false, graphicFactory);
		tileRendererLayer.setMapFile(mapFile);
		tileRendererLayer.setXmlRenderTheme(renderTheme);

		layers.remove(tileDownloadLayer);
		tileDownloadLayer.onDestroy();

		layers.add(tileRendererLayer);

		online = false;
	}

	/**
	 * Changes the offline tile renderer to an online tile provider.
	 * 
	 * @param providerName
	 *            Name of the provider. Eg: MapQuest
	 * @param host
	 *            Host
	 * @param baseUrl
	 *            Base URL
	 * @param extension
	 *            Tile extension. Eg: png
	 * @param port
	 *            Port. Eg: 80
	 */
	public void setOnline(String providerName, String host, String baseUrl,
			String extension, int port) {
		onlineTileSource = new OnlineTileSource(new String[] { host }, port);
		onlineTileSource.setName(providerName).setAlpha(false)
				.setBaseUrl(baseUrl).setExtension(extension)
				.setParallelRequestsLimit(8).setProtocol("http")
				.setTileSize(256).setZoomLevelMax((byte) 18)
				.setZoomLevelMin((byte) 0);
		tileDownloadLayer = new TileDownloadLayer(this.tileCache,
				mapViewPosition, onlineTileSource,
				AndroidGraphicFactory.INSTANCE);

		layers.remove(tileRendererLayer);
		tileRendererLayer.onDestroy();

		layers.add(tileDownloadLayer);

		online = true;
	}

	/**
	 * Changes the render theme to the one specified in this method. If no one
	 * is provided, or it is not valid, the default theme will be applied.
	 * 
	 * @param renderThemePath
	 *            Absolute path to the theme file.
	 * @throws IOException
	 */
	public void setRenderThemePath(String renderThemePath) throws IOException,
			IllegalArgumentException {
		if (renderThemePath != null
				&& renderThemePath.substring(renderThemePath.length() - 4,
						renderThemePath.length()).equals(".xml")) {

			if(!renderThemePath.contains("/")) 
				renderThemePath = this.context.getFilesDir() + "/renderthemes/"+renderThemePath;
			
			File newTheme = new File(renderThemePath);
			if (newTheme.exists()) {
				this.renderThemePath = renderThemePath;
				this.renderTheme = new ExternalRenderTheme(newTheme);
			} else {
				prepareMapThemes();
				this.renderThemePath = this.context.getFilesDir()
						+ "/renderthemes/assets.xml";
				newTheme = new File(renderThemePath);
				this.renderTheme = new ExternalRenderTheme(newTheme);

				Log.w(MapsforgePlugin.TAG,
						"Render theme doesn't exist. Default theme applied.");
			}

			if (tileRendererLayer != null) {
				if (renderTheme == null) {
					throw new IllegalArgumentException(
							"Render theme is null, cannot asign it to the renderer layer.");
				} else {
					tileRendererLayer.setXmlRenderTheme(renderTheme);
				}
			}
		} else {
			throw new IllegalArgumentException(
					"Incorrect theme file path ("+renderThemePath+") or incorrect file format (should be .xml)");
		}
	}

	/**
	 * Changes the view to the specified zoom.
	 * 
	 * @param zoom
	 *            New zoom.
	 */
	public void setZoom(byte zoom) {
		mapViewPosition.setZoomLevel(zoom);
	}

	/**
	 * Shows the MapView. This is useful since it is not possible to change the
	 * visibility of the view from the cordova application.
	 * 
	 * @see #hide()
	 */
	public void show() {
		mapView.setVisibility(MapView.VISIBLE);
	}

	/**
	 * Sets the visibility of the scale bar in the MapView.
	 * 
	 * @param showScaleBar
	 *            True to show the scale bar, false to hide it.
	 */
	public void showScaleBar(boolean showScaleBar) {
		mapView.getMapScaleBar().setVisible(showScaleBar);
	}
}
