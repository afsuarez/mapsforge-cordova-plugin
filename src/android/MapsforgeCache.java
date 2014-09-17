package com.suarez.cordova.mapsforge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import android.app.Activity;
import android.content.res.AssetManager;
import android.util.Log;

/**
 * This class creates a tile cache using mapsforge libraries. When a tile is not
 * available in cache, it will create it and store it in the cache (if there is
 * space available).
 * 
 * @author Adolfo Fernandez Suarez
 * @version 0.1
 */
public class MapsforgeCache {
	/**
	 * Remember to initialize this instance with
	 * {@code MapsforgeCache.createInstance} in order to avoid getting a null
	 * object, and therefore <code>NullPointerException</code> when invoking any
	 * method.
	 */
	public static MapsforgeCache INSTANCE;
	
	public static void createInstance(Activity context, String mapFilePath){
		INSTANCE = new MapsforgeCache(context, mapFilePath);
	}
	// Context
	private Activity context;
	// Graphic factory
	private AndroidGraphicFactory graphicFactory;
	// Cache config
	private int maxCacheSize;
	private int cleanCacheTrigger;
	private boolean cacheEnabled;
	private boolean externalCache;
	private String cacheName;
	// Additional config
	private int tileSize;
	private float overdrawFactor;
	private float screenRatio;
	// Check for remove cache on destroy
	private boolean cleanOnDestroy;
	//Map file to get tiles from
	private String mapFilePath;
	
	//Max time of an image in cache (minimum of 15 seconds)
	private long maxCacheAge;
	//Objects needed for tile rendering and cache control
	private Tile tile;
	private RendererJob rendererJob;
	private DatabaseRenderer renderer;
	private final MapDatabase mapDatabase = new MapDatabase();
	private XmlRenderTheme renderTheme;
	private final DisplayModel displayModel = new DisplayModel();
	private TileBitmap bitmap;
	private String relativeCachePath;
	private File cacheDir;
	private File mapFile;
	private long currentCacheSize;
	private long lastCleaning;

	private File tmpDir;
	
	/**
	 * Constructor that initializes the mapsforge cache with default values.
	 * Those values are as follows:
	 * <p>
	 * {@code maxCacheSize}: 25MB<br/>
	 * {@code cleanCacheTrigger}: 5MB<br/>
	 * {@code cacheEnabled}: true<br/>
	 * {@code externalCache}: true<br/>
	 * {@code cacheName}: mapcache<br/>
	 * <p>
	 * {@code tileSize}: 256<br/>
	 * {@code overdrawFactor}: 1.2<br/>
	 * {@code screenRatio}: 1.0<br/>
	 * <p>
	 * {@code cleanOnDestroy}: true<br/>
	 * <p>
	 * Please note that you may want to initialize an AndroidGraphicFactory
	 * instance ({@code AndroidGraphicFactory.createInstance}) before invoking
	 * this constructor, otherwise the instance will be automatically created
	 * (you can then access it through
	 * <code>AndroidGraphicFactory.INSTANCE</code>).
	 * <p>
	 * <strong>Use this constructor if</strong> you want to use most of the
	 * default configuration, and change the values you want with the
	 * appropriate setters.
	 * 
	 * @param context
	 *            The Android context (obtained from the cordova plugin through
	 *            <code><i>this.cordova.getActivity()</i></code>)
	 */
	private MapsforgeCache(Activity context, String mapFilePath) {
		// Context
		setContext(context);
		// Graphic factory
		setGraphicFactory(AndroidGraphicFactory.INSTANCE);
		// Cache default config
		setMaxCacheSize(25);
		setCleanCacheTrigger(5);
		setCacheEnabled(true);
		setExternalCache(true);
		setCacheName("mapcache");
		// Tiles default config
		setTileSize(256);
		setOverdrawFactor(1.2f);
		setScreenRatio(1f);
		// We'll delete the cache on destroy
		setCleanOnDestroy(true);
		//Setting the map path
		setMapFilePath(mapFilePath);
		
		createCacheDirectory();
		prepareMapThemes();
		
		try {
			renderTheme = new ExternalRenderTheme(new File(this.context.getFilesDir(),"/mapthemes/assets.xml"));
		} catch (FileNotFoundException e) {
			Log.e(MapsforgePlugin.TAG, e.getMessage());
		}
		
		this.tmpDir = new File(this.context.getCacheDir(),"/tmp");
		this.tmpDir.mkdirs();
	}
	
	private void checkCacheAvailability(){
		long usable = cacheDir.getUsableSpace();
		long maxSize = (maxCacheSize*1024*1024)-currentCacheSize;
		long cleanCache = cleanCacheTrigger*1024*1024;
		
		if(usable <= cleanCache){
			setCacheEnabled(false);
		}
		
		if(maxSize > usable){
			setMaxCacheSize((int)(usable+folderSize(cacheDir))/(1024*1024));
		}
		
	}
	
	private void checkCacheSize() {
		Log.d(MapsforgePlugin.TAG, "Current cache size (bytes): "+currentCacheSize);
		if (currentCacheSize >= (maxCacheSize * 1024 * 1024)
				|| cacheDir.getUsableSpace() <= (cleanCacheTrigger * 1024 * 1024)) {
			setCacheEnabled(false);
			cleanCache();
			currentCacheSize = folderSize(cacheDir);
			setCacheEnabled(true);

			for(File file : tmpDir.listFiles()){
				file.delete();
			}
		}
	}

	private boolean checkExternalCache(){
		if(this.context != null){
			return (this.context.getExternalCacheDir() == null)? false: true;
		}
		
		return false;
	}

	private void cleanCache() {
		long currentTime = System.currentTimeMillis();
		
		if(lastCleaning >= (currentTime-5000)){
			setMaxCacheSize(getMaxCacheSize()+5);
			checkCacheAvailability();
		}
		
		for(File file: cacheDir.listFiles()){
			deleteDirectory(file, currentTime-maxCacheAge);
		}
		lastCleaning = currentTime;
	}

	private void copyFile(InputStream in, OutputStream out) throws IOException {
	    byte[] buffer = new byte[1024];
	    int read;
	    while((read = in.read(buffer)) != -1){
	      out.write(buffer, 0, read);
	    }
	}

	private void createCacheDirectory() {
		if(this.externalCache){
			cacheDir = new File(this.context.getExternalCacheDir(), relativeCachePath);
			cacheDir.mkdirs();
		}else{
			cacheDir = new File(this.context.getCacheDir(), relativeCachePath);
			cacheDir.mkdirs();
		}
		
		this.currentCacheSize = folderSize(cacheDir);
		checkCacheSize();
		checkCacheAvailability();
	}

	private void deleteDirectory(File directory, long timestamp) {
		for(File file: directory.listFiles()){
			if(file.isDirectory()){
				deleteDirectory(file, timestamp);
				if(file.list().length == 0)
					file.delete();
			}else if(file.lastModified()<timestamp){
				file.delete();
			}
		}
	}

	private long folderSize(File directory) {
	    long size = 0;
	    for (File file : directory.listFiles()) {
	        if (file.isFile())
	        	size += file.length();
	        else
	        	size += folderSize(file);
	    }
	    return size;
	}

	public String getCacheName() {
		return cacheName;
	}

	public int getCleanCacheTrigger() {
		return cleanCacheTrigger;
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

	public long getMaxCacheAge() {
		return maxCacheAge;
	}

	public int getMaxCacheSize() {
		return maxCacheSize;
	}

	public float getOverdrawFactor() {
		return overdrawFactor;
	}

	public float getScreenRatio() {
		return screenRatio;
	}

	public synchronized String getTilePath(long x, long y, byte zoom){
		String path = null;
		try{
			if(!cacheDir.exists()){
				if(externalCache){
					checkExternalCache();
				}
				createCacheDirectory();
			}
			File tileFile = null;
			
			if(isCacheEnabled()){
				tileFile = new File(cacheDir,"/"+zoom+"/"+x+"/"+y+".png");
			}
			
			if(tileFile != null && tileFile.exists()){
				path = tileFile.getAbsolutePath();
				tileFile.setLastModified(System.currentTimeMillis());
			}else{
				tileFile = null;
				tile = new Tile(x, y, zoom);
				rendererJob = new RendererJob(tile, mapFile, renderTheme, displayModel, 1f, false);
				bitmap = renderer.executeJob(rendererJob);
				
				if(bitmap != null){
					OutputStream outStream = null;
					
					
					try {
						if(isCacheEnabled()){
							new File(cacheDir, "/" + zoom + "/" + x + "/").mkdirs();
							tileFile = new File(cacheDir, "/" + zoom + "/" + x + "/" + y + ".png");
						}else{
							new File(tmpDir, "/" + zoom + "/" + x + "/").mkdirs();
							tileFile = new File(tmpDir, "/" + zoom + "/" + x + "/" + y + ".png");
							if(tileFile.exists()){
								tileFile.delete();
							}
						}
						outStream = new FileOutputStream(tileFile);
						bitmap.compress(outStream);
						
						path = tileFile.getAbsolutePath();
						tileFile.setLastModified(System.currentTimeMillis());
						if(isCacheEnabled()){
							currentCacheSize += tileFile.length();
							checkCacheSize();
						}
					} catch (FileNotFoundException e) {
						Log.e(MapsforgePlugin.TAG, e.getMessage());
					} catch (IOException e) {
						Log.e(MapsforgePlugin.TAG, e.getMessage());
					}finally{
						if(outStream != null){
							try {
								outStream.close();
							} catch (IOException e) {
								Log.e(MapsforgePlugin.TAG, e.getMessage());
							}
						}
					}
					
				}else{
					// Exception couldn't render tile
					Log.e(MapsforgePlugin.TAG, "Couldn't render tile");
				}
			}
			
		}catch(NullPointerException npe){
			Log.e(MapsforgePlugin.TAG, npe.getMessage());
		}
		return path;
	}

	public int getTileSize() {
		return tileSize;
	}

	public boolean isCacheEnabled() {
		return cacheEnabled;
	}

	public boolean isCleanOnDestroy() {
		return cleanOnDestroy;
	}

	public boolean isExternalCache() {
		return externalCache;
	}

	public void onDestroy(){
		long currentTime = System.currentTimeMillis();
		deleteDirectory(tmpDir, currentTime);
		if(cleanOnDestroy){
			deleteDirectory(cacheDir, currentTime);
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

	public void setCacheEnabled(boolean cacheEnabled) {
		this.cacheEnabled = cacheEnabled;
	}

	public void setCacheName(String cacheName) {
		if(cacheName != null && !cacheName.equals("")){
			if(cacheDir != null && cacheDir.exists() && !cacheName.equals(this.cacheName)){
				deleteDirectory(cacheDir, System.currentTimeMillis());
			}
			this.cacheName = cacheName;
		}
		
		this.relativeCachePath = "/" + this.cacheName;
		
		createCacheDirectory();
	}

	public void setCleanCacheTrigger(int cleanCacheTrigger) {
		if(cleanCacheTrigger >= maxCacheSize){
			this.cleanCacheTrigger = this.maxCacheSize/2;
		}
		this.cleanCacheTrigger = cleanCacheTrigger;
	}

	public void setCleanOnDestroy(boolean cleanOnDestroy) {
		this.cleanOnDestroy = cleanOnDestroy;
	}
	
	public void setContext(Activity context) {
		this.context = context;
	}

	public void setExternalCache(boolean externalCache) {
		boolean previousCache = this.externalCache;
		if(externalCache){
			this.externalCache = checkExternalCache();
		}else{
			this.externalCache = externalCache;
		}
		
		if(this.externalCache != previousCache && cacheDir != null && cacheDir.exists()){
			deleteDirectory(cacheDir, System.currentTimeMillis());
			createCacheDirectory();
		}
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
			
			if(this.mapDatabase.hasOpenFile()){
				this.mapDatabase.closeFile();
			}
			mapDatabase.openFile(mapFile);
			
			if(renderer != null){
				renderer.destroy();
			}
			
			renderer = new DatabaseRenderer(this.mapDatabase, this.graphicFactory);
		}else{
			Log.e(MapsforgePlugin.TAG, "Incorrect map file path or incorrect file format (should be .map)");
		}
	}

	public void setMaxCacheAge(long maxCacheAge) {
		if(maxCacheAge < 15000){
			this.maxCacheAge = 15000;
		}else{
			this.maxCacheAge = maxCacheAge;
		}
	}
	
	public void setMaxCacheSize(int maxCacheSize) {
		if(maxCacheSize <= this.cleanCacheTrigger){
			this.maxCacheSize = maxCacheSize;
			this.cleanCacheTrigger = this.maxCacheSize/2;
		}
		this.maxCacheSize = maxCacheSize;
	}
	
	public void setOverdrawFactor(float overdrawFactor) {
		this.overdrawFactor = overdrawFactor;
	}
	
	public void setRenderTheme(String renderThemePath){
		if (renderThemePath != null
				&& renderThemePath.substring(renderThemePath.length() - 4,
						renderThemePath.length()).equals(".xml")){
			
			try {
				File newTheme = new File(renderThemePath);
				if(newTheme.exists()){
					this.renderTheme = new ExternalRenderTheme(newTheme);
				}else{
					Log.e(MapsforgePlugin.TAG, "Render theme doesn't exist.");
				}
			} catch (FileNotFoundException e) {
				Log.e(MapsforgePlugin.TAG, e.getMessage());
			}
		}else{
			Log.e(MapsforgePlugin.TAG, "Incorrect theme file path or incorrect file format (should be .xml)");
		}
	}
	
	public void setScreenRatio(float screenRatio) {
		this.screenRatio = screenRatio;
	}
	
	public void setTileSize(int tileSize) {
		if(tileSize < 0){
			this.tileSize = 256;
		}else{
			this.tileSize = tileSize;
		}
		
		if(displayModel != null){
			displayModel.setFixedTileSize(this.tileSize);
		}
	}
}
