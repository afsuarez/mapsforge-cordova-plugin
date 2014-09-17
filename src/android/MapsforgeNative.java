package com.suarez.cordova.mapsforge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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

public class MapsforgeNative {
	
	public static MapsforgeNative INSTANCE;
	
	public static void createInstance(Activity context, String mapFilePath, int width, int height){
		INSTANCE = new MapsforgeNative(context, mapFilePath, width, height);
	}
	
	private Activity context;

	private AndroidGraphicFactory graphicFactory;
	
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
	private boolean matchParent = false;
	private boolean online;
	private SparseArray<Layer> layersOnMap;
	
	private int layerCounter;
	
	private MapsforgeNative(Activity context, String mapFilePath, int width, int height){
		setContext(context);
		setGraphicFactory(AndroidGraphicFactory.INSTANCE);
		setMapFilePath(mapFilePath);
		setCacheName("mapcache");
		
		prepareMapThemes();
		
		setRenderThemePath(this.context.getFilesDir()+"/mapthemes/assets.xml");
		
		destroyCache = true;
		
		if(width == 0 || height == 0){
			matchParent = true;
		}else{
			this.width = width;
			this.height = height;
		}
		
		layersOnMap = new SparseArray<Layer>();
		layerCounter = 0;
		
		initializeMap();
	}
	
	public int addMarker(int drawableId, double lat, double lng){
		Drawable drawable = context.getResources().getDrawable(drawableId);
        Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
        Marker marker = new Marker(new LatLong(lat, lng), bitmap, 0, -bitmap
				.getHeight() / 2);
        
		layers.add(marker);
		
		layersOnMap.append(layerCounter, marker);
		return layerCounter++;
	}

	public int addPolyline(int color, int strokeWidth, List<Double> points){
		Paint paint = graphicFactory.createPaint();
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Style.STROKE);
        
        Polyline polyline = new Polyline(paint, graphicFactory);
        List<LatLong> latlongs = polyline.getLatLongs();
        
        for(int i=0;i<points.size();i+=2){
        	if((i+1)>=points.size()) break;
        	latlongs.add(new LatLong(points.get(i), points.get(i+1)));
        }

        layers.add(polyline);
        
        layersOnMap.append(layerCounter, polyline);
        return layerCounter++;
	}

	private void copyFile(InputStream in, OutputStream out) throws IOException {
	    byte[] buffer = new byte[1024];
	    int read;
	    while((read = in.read(buffer)) != -1){
	      out.write(buffer, 0, read);
	    }
	}

	public void deleteLayer(int key){
		layers.remove(layersOnMap.get(key,null));
		layersOnMap.delete(key);
	}

	public void destroyCache(boolean destroy){
		destroyCache = destroy;
	}

	public String getCacheName() {
		return cacheName;
	}

	public Activity getContext() {
		return context;
	}

	public AndroidGraphicFactory getGraphicFactory() {
		return graphicFactory;
	}

	public String getMapFilePath() {
		return mapFilePath;
	}

	public String getRenderThemePath() {
		return renderThemePath;
	}

	public void hide(){
		mapView.setVisibility(MapView.GONE);
	}
	
	public void initializeMap(){
		mapView = new MapView(context);
		mapViewPosition = mapView.getModel().mapViewPosition;

		setClickable(true);
		showScaleBar(false);
		setBuiltInZoomControls(false);
		setMinZoom((byte)8);
		setMaxZoom((byte)20);
		setCenter(0d, 0d);
		setZoom((byte)8);
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
		
		RelativeLayout.LayoutParams params = null;
		if(matchParent){
			params = new RelativeLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);
		}else{
			params = new RelativeLayout.LayoutParams(width, height);
		}
		
		context.addContentView(mapView, params);
	}
	
	public void onDestroy(){
		if(destroyCache){
			tileCache.destroy();
		}
	}
	
	public void onStart() {
		if(online){
			tileDownloadLayer = new TileDownloadLayer(tileCache,
					mapViewPosition, onlineTileSource, graphicFactory);
								
			layers.add(tileDownloadLayer);
		}else{
			tileRendererLayer = new TileRendererLayer(tileCache, mapViewPosition,
					false, graphicFactory);
			tileRendererLayer.setMapFile(mapFile);
			tileRendererLayer.setXmlRenderTheme(renderTheme);

			layers.add(tileRendererLayer);
		}
	}
	
	public void onStop(){
		if(online){
			layers.remove(tileDownloadLayer);
			tileDownloadLayer.onDestroy();
		}else{
			layers.remove(tileRendererLayer);
	        tileRendererLayer.onDestroy();
		}
	}
	
	private void prepareMapThemes() {
		List<String> themes = new ArrayList<String>();
		themes.add("assets.xml");
		themes.add("assetssvg.xml");
		themes.add("driving.xml");
		themes.add("onlybuildings.xml");
		themes.add("detailed.xml");
		themes.add("osmarendernopng.xml");
		
		File directory = new File(this.context.getFilesDir(),"/mapthemes");
		directory.mkdirs();
		
		for(String filename : directory.list()){
			themes.remove(filename);
		}
		
		AssetManager assetManager = this.context.getAssets();
		
		File file = null;
		InputStream inStream = null;
		OutputStream outStream = null;
		
		for (String filename : themes) {
			try {
				file = new File(directory, "/" + filename);

				inStream = assetManager.open("renderthemes/"+filename);
				outStream = new FileOutputStream(file);
				
				copyFile(inStream, outStream);
				
				if(inStream != null){
					inStream.close();
					inStream = null;
				}
				
				if(outStream != null){
					outStream.flush();
					outStream.close();
					outStream = null;
				}
			} catch (IOException e) {
				Log.e(MapsforgePlugin.TAG, "Failed to copy theme: " + filename, e);
			}
		}
	}
	
	public void setBuiltInZoomControls(boolean builtInZoomControls){
		mapView.setBuiltInZoomControls(builtInZoomControls);
	}
	
	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}
	
	public void setCenter(double lat, double lng){
		mapViewPosition.setCenter(new LatLong(lat, lng));
	}
	
	public void setClickable(boolean clickable){
		mapView.setClickable(clickable);
	}
	
	public void setContext(Activity context) {
		this.context = context;
	}
	
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
	
	public void setMapFilePath(String mapFilePath) {
		if (mapFilePath != null
				&& mapFilePath.substring(mapFilePath.length() - 4,
						mapFilePath.length()).equals(".map")){
			this.mapFilePath = mapFilePath;
			
			mapFile = new File(mapFilePath);
			if(mapFile==null || !mapFile.exists()){
				Log.e(MapsforgePlugin.TAG, "Map file not found.");
				return;
			}
			
			if(tileRendererLayer != null){
				tileRendererLayer.setMapFile(mapFile);
			}
		}else{
			Log.e(MapsforgePlugin.TAG, "Incorrect map file path or incorrect file format (should be .map)");
		}
	}
	
	public void setMaxZoom(byte zoom){
		mapViewPosition.setZoomLevelMax(zoom);
	}
	
	public void setMinZoom(byte zoom){
		mapViewPosition.setZoomLevelMin(zoom);
	}
	
	public void setOffline(String mapFilePath, String renderThemePath){
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
	
	public void setOnline(String providerName, String host, String baseUrl,
			String extension, int port) {
		onlineTileSource = new OnlineTileSource(new String[] {host}, port);
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
	
	public void setRenderThemePath(String renderThemePath) {
		if (renderThemePath != null
				&& renderThemePath.substring(renderThemePath.length() - 4,
						renderThemePath.length()).equals(".xml")){
			
			try {
				File newTheme = new File(renderThemePath);
				if(newTheme.exists()){
					this.renderThemePath = renderThemePath;
					this.renderTheme = new ExternalRenderTheme(newTheme);
				}else{
					this.renderThemePath = this.context.getFilesDir()+"/mapthemes/assets.xml";
					newTheme = new File(renderThemePath);
					this.renderTheme = new ExternalRenderTheme(newTheme);
					
					Log.e(MapsforgePlugin.TAG, "Render theme doesn't exist. Default theme applied.");
				}
			} catch (FileNotFoundException e) {
				Log.e(MapsforgePlugin.TAG, e.getMessage());
			}
			
			
			if(tileRendererLayer != null){
				if(renderTheme == null){
					Log.e(MapsforgePlugin.TAG, "Render theme is null, cannot asign it to the renderer layer.");
				}else{
					tileRendererLayer.setXmlRenderTheme(renderTheme);
				}
			}
		}else{
			Log.e(MapsforgePlugin.TAG, "Incorrect theme file path or incorrect file format (should be .xml)");
		}
	}
	
	public void setZoom(byte zoom){
		mapViewPosition.setZoomLevel(zoom);
	}
	
	public void show(){
		mapView.setVisibility(MapView.VISIBLE);
	}
	
	public void showScaleBar(boolean showScaleBar){
		mapView.getMapScaleBar().setVisible(showScaleBar);
	}
}
