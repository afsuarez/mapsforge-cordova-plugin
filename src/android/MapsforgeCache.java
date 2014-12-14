package com.suarez.cordova.mapsforge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

	/**
	 * Creates a new instance for the tile cache using the map file provided.
	 * 
	 * @param context
	 *            Cordova activity. Get it from the main plugin class through
	 *            the <code>cordova</code> object.
	 * @param mapFilePath
	 *            Absolute path to the map file.
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public static void createInstance(Activity context, String mapFilePath)
			throws IllegalArgumentException, IOException {
		INSTANCE = new MapsforgeCache(context, mapFilePath);
	}

	// Context
	private Activity context;
	// Graphic factory
	private AndroidGraphicFactory graphicFactory;
	// Cache config
	private int maxCacheSize;
	// Trigger that cleans the cache if there is no space available on memory
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
	// Map file to get tiles from
	private String mapFilePath;

	// Max time of an image in cache (minimum of 15 seconds)
	private long maxCacheAge;
	// Objects needed for tile rendering and cache control
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
	// Directory for tiles when cache is not enabled
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
	 * @param mapFilePath
	 *            Absolute path to the map file
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	private MapsforgeCache(Activity context, String mapFilePath)
			throws IllegalArgumentException, IOException {
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
		// Setting the map path
		setMapFilePath(mapFilePath);

		createCacheDirectory();
		prepareMapThemes();

		renderTheme = new ExternalRenderTheme(new File(
				this.context.getFilesDir(), "/renderthemes/assets.xml"));

		this.tmpDir = new File(this.context.getCacheDir(), "/tmp");
		this.tmpDir.mkdirs();
	}

	// Check if there is enough space for a cache, and adjust its size if the
	// maxSize specified is higher than the usable space
	private void checkCacheAvailability() {
		long usable = cacheDir.getUsableSpace();
		long maxSize = (maxCacheSize * 1024 * 1024) - currentCacheSize;
		long cleanCache = cleanCacheTrigger * 1024 * 1024;

		if (usable <= cleanCache) {
			setCacheEnabled(false);
		}

		if (maxSize > usable) {
			setMaxCacheSize((int) (usable + folderSize(cacheDir))
					/ (1024 * 1024));
		}

	}

	// Checks the cache size and if it is necessary to clean it up
	private void checkCacheSize() {
		if (currentCacheSize >= (maxCacheSize * 1024 * 1024)
				|| cacheDir.getUsableSpace() <= (cleanCacheTrigger * 1024 * 1024)) {
			setCacheEnabled(false);
			cleanCache();
			currentCacheSize = folderSize(cacheDir);
			setCacheEnabled(true);

			for (File file : tmpDir.listFiles()) {
				file.delete();
			}
		}
	}

	// Checks if it is possible to set up a cache in the SD card
	private boolean checkExternalCache() {
		if (this.context != null) {
			return (this.context.getExternalCacheDir() == null) ? false : true;
		}

		return false;
	}

	// Cleans up the cache. Deletes all images with age > maxCacheAge. If the
	// last cleaning has ocurred in the last 5 seconds, the cache size is
	// increased in 5 MB to avoid be all the time cleaning it
	private void cleanCache() {
		long currentTime = System.currentTimeMillis();

		if (lastCleaning >= (currentTime - 5000)) {
			setMaxCacheSize(getMaxCacheSize() + 5);
			checkCacheAvailability();
		}

		for (File file : cacheDir.listFiles()) {
			deleteDirectory(file, currentTime - maxCacheAge);
		}
		lastCleaning = currentTime;
	}

	// Copy an inputStream in an outputStream
	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
	}

	// Creates the cache directory. If there is not possible to create it in the
	// SD card it will be created in the device's memory
	private void createCacheDirectory() {
		if (this.externalCache) {
			cacheDir = new File(this.context.getExternalCacheDir(),
					relativeCachePath);
			cacheDir.mkdirs();
		} else {
			cacheDir = new File(this.context.getCacheDir(), relativeCachePath);
			cacheDir.mkdirs();
		}

		this.currentCacheSize = folderSize(cacheDir);
		checkCacheSize();
		checkCacheAvailability();
	}

	// Delete all files in a directory (and sub-directories) with the
	// lastModified attribute < timestamp
	private void deleteDirectory(File directory, long timestamp) {
		for (File file : directory.listFiles()) {
			if (file.isDirectory()) {
				deleteDirectory(file, timestamp);
				if (file.list().length == 0)
					file.delete();
			} else if (file.lastModified() < timestamp) {
				file.delete();
			}
		}
	}

	// Returns the size in bytes of the file passed as argument. If it is a
	// directory, this function will return the size of it and its
	// sub-directories
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

	/**
	 * Returns the cache name. This is the name of the folder that contains the
	 * images.
	 * 
	 * @return The name of the cache
	 */
	public String getCacheName() {
		return cacheName;
	}

	/**
	 * Returns the cleanCacheTrigger property. This property avoids the cache to
	 * use all the space available in its partition.
	 * 
	 * @return The size(in MB) that should be preserved.
	 */
	public int getCleanCacheTrigger() {
		return cleanCacheTrigger;
	}

	/**
	 * Returns the context used when the cache was created.
	 * 
	 * @return The current context of this cache
	 */
	public Activity getContext() {
		return context;
	}

	/**
	 * Returns the graphic factory instance used in this cache.
	 * 
	 * @return The instance of AndroidGraphicFactory used in the cache
	 */
	public AndroidGraphicFactory getGraphicFactory() {
		return graphicFactory;
	}

	/**
	 * Returns the path of the map file that mapsforge is using
	 * 
	 * @return The path to the map file(.map).
	 */
	public String getMapFilePath() {
		return mapFilePath;
	}

	/**
	 * Returns the maxCacheAge. This is the attribute used for deleting the
	 * images from the cache when the cleaning process starts. This avoids the
	 * deleting of images that are being used in the UI.
	 * 
	 * @return The age, in milliseconds, from which the images will be deleted
	 */
	public long getMaxCacheAge() {
		return maxCacheAge;
	}

	/**
	 * Returns the maxCacheSize attribute. The default value for this attribute
	 * is 25MB, but if there is no enough space, it will be adjusted to fit the
	 * available space.
	 * 
	 * @return The maximum size for this cache(in MB)
	 */
	public int getMaxCacheSize() {
		return maxCacheSize;
	}

	/**
	 * @return The overdraw factor used for the tile rendering
	 */
	public float getOverdrawFactor() {
		return overdrawFactor;
	}

	/**
	 * @return Screen ratio for creating the tile's images. By default it is set
	 *         to 1f.
	 */
	public float getScreenRatio() {
		return screenRatio;
	}

	/**
	 * This method will return the path to the tile image that represents the
	 * given coordinates and zoom. If the cache is enabled, it will render the
	 * image iff it is not available in the cache. If the cache it is not
	 * enabled, it will render the image always, even if there is a previous one
	 * already stored in the device.
	 * 
	 * @param x
	 *            Latitude point
	 * @param y
	 *            Longitude point
	 * @param zoom
	 *            Zoom
	 * @return The absolute path to the tile image
	 * @throws IOException
	 */
	public synchronized String getTilePath(long x, long y, byte zoom)
			throws IOException {
		String path = null;
		if (!cacheDir.exists()) {
			if (externalCache) {
				checkExternalCache();
			}
			createCacheDirectory();
		}
		File tileFile = null;

		if (isCacheEnabled()) {
			tileFile = new File(cacheDir, "/" + zoom + "/" + x + "/" + y
					+ ".png");
		}

		if (tileFile != null && tileFile.exists()) {
			path = tileFile.getAbsolutePath();
			tileFile.setLastModified(System.currentTimeMillis());
		} else {
			tileFile = null;
			tile = new Tile(x, y, zoom);
			rendererJob = new RendererJob(tile, mapFile, renderTheme,
					displayModel, 1f, false);
			bitmap = renderer.executeJob(rendererJob);

			if (bitmap != null) {
				OutputStream outStream = null;

				try {
					if (isCacheEnabled()) {
						new File(cacheDir, "/" + zoom + "/" + x + "/").mkdirs();
						tileFile = new File(cacheDir, "/" + zoom + "/" + x
								+ "/" + y + ".png");
					} else {
						new File(tmpDir, "/" + zoom + "/" + x + "/").mkdirs();
						tileFile = new File(tmpDir, "/" + zoom + "/" + x + "/"
								+ y + ".png");
						if (tileFile.exists()) {
							tileFile.delete();
						}
					}
					outStream = new FileOutputStream(tileFile);
					bitmap.compress(outStream);

					path = tileFile.getAbsolutePath();
					tileFile.setLastModified(System.currentTimeMillis());
					if (isCacheEnabled()) {
						currentCacheSize += tileFile.length();
						checkCacheSize();
					}
				} finally {
					if (outStream != null) {
						outStream.close();
					}
				}

			} else {
				throw new IOException("Couldn't render tile, x: " + x + ", y: "
						+ y + ", zoom: " + zoom);
			}

		}

		return path;
	}

	/**
	 * Returns the tile size. This is the size of the images that will be
	 * generated, and by default its value is 256.
	 * 
	 * @return The tile image size
	 */
	public int getTileSize() {
		return tileSize;
	}

	/**
	 * Returns the flag that indicates whether or not the cache is enabled
	 * 
	 * @return True if the cache is enabled, false otherwise
	 */
	public boolean isCacheEnabled() {
		return cacheEnabled;
	}

	/**
	 * Returns the flag that indicates whether or not the cache should be
	 * deleted when executing the <code>onDestroy</code> method.
	 * 
	 * @return True if the cache will be deleted, false otherwise
	 */
	public boolean isCleanOnDestroy() {
		return cleanOnDestroy;
	}

	/**
	 * Returns the flag that indicates whether or not it's being used the
	 * external cache(SD card).
	 * 
	 * @return True if the cache is in the SD card, false otherwise
	 */
	public boolean isExternalCache() {
		return externalCache;
	}

	/**
	 * Destroys the temporal images that have been generated, and cleans up the
	 * cache if the flag for that is set up.
	 */
	public void onDestroy() {
		long currentTime = System.currentTimeMillis();
		deleteDirectory(tmpDir, currentTime);
		if (cleanOnDestroy) {
			deleteDirectory(cacheDir, currentTime);
		}
	}

	// Copies all XML files for the themes to the internal storage
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
	 * Enables or disables the cache. If the cache is not enabled, the images
	 * will be generated always from scratch, so it will be slower than with a
	 * cache.
	 * 
	 * @param cacheEnabled
	 *            True to enable cache, false to disable
	 */
	public void setCacheEnabled(boolean cacheEnabled) {
		this.cacheEnabled = cacheEnabled;
	}

	/**
	 * Set the name for the cache. Basically this is going to be the name of the
	 * folder within the cache directory. Changing it will destroy the previous
	 * cache (if exists).
	 * 
	 * @param cacheName
	 *            New name for the cache
	 */
	public void setCacheName(String cacheName) {
		if (cacheName != null && !cacheName.equals("")) {
			if (cacheDir != null && cacheDir.exists()
					&& !cacheName.equals(this.cacheName)) {
				deleteDirectory(cacheDir, System.currentTimeMillis());
			}
			this.cacheName = cacheName;
		}

		this.relativeCachePath = "/" + this.cacheName;

		createCacheDirectory();
	}

	/**
	 * Sets up the trigger to clean the cache. This means that the cache will
	 * always preserve free in memory the MB specified for the trigger. This may
	 * be useful in order to prevent the cache to use all available space.
	 * 
	 * @param cleanCacheTrigger
	 *            Size(in MB) that will be kept free in memory
	 */
	public void setCleanCacheTrigger(int cleanCacheTrigger) {
		if (cleanCacheTrigger >= maxCacheSize) {
			this.cleanCacheTrigger = this.maxCacheSize / 2;
		}
		this.cleanCacheTrigger = cleanCacheTrigger;
	}

	/**
	 * Enables or disables the deletion of the cache when the
	 * <code>onDestroy</code> method is called.
	 * 
	 * @param cleanOnDestroy
	 *            True to delete the cache, false to preserve it.
	 */
	public void setCleanOnDestroy(boolean cleanOnDestroy) {
		this.cleanOnDestroy = cleanOnDestroy;
	}

	/**
	 * Sets up the application context.
	 * 
	 * @param context
	 *            The new context
	 */
	public void setContext(Activity context) {
		this.context = context;
	}

	/**
	 * Enables or disables the external cache(SD card). If there is no SD card,
	 * the cache will be placed in the internal device memory.
	 * 
	 * @param externalCache
	 *            True to place the cache in the SD card, false to internal
	 *            storage
	 */
	public void setExternalCache(boolean externalCache) {
		boolean previousCache = this.externalCache;
		if (externalCache) {
			this.externalCache = checkExternalCache();
		} else {
			this.externalCache = externalCache;
		}

		if (this.externalCache != previousCache && cacheDir != null
				&& cacheDir.exists()) {
			deleteDirectory(cacheDir, System.currentTimeMillis());
			createCacheDirectory();
		}
	}

	/**
	 * Sets the <code>AndroidGraphicFactory</code> that will be used to generate
	 * tiles. If the factory has not been created yet, a new one will be
	 * created.
	 * 
	 * @param graphicFactory
	 *            The <code>AndroidGraphicFactory</code> instance or null to
	 *            create a new one
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
	 * Sets the map file that will be used for rendering. If the map file
	 * doesn't exist, or it has not the required extension(map) an exception
	 * will be logged with <code>ERROR</code> level.
	 * 
	 * @param mapFilePath
	 *            Absolute file path to the map file
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

			if (this.mapDatabase.hasOpenFile()) {
				this.mapDatabase.closeFile();
			}
			mapDatabase.openFile(mapFile);

			if (renderer != null) {
				renderer.destroy();
			}

			renderer = new DatabaseRenderer(this.mapDatabase,
					this.graphicFactory);
		} else {
			throw new IllegalArgumentException(
					"Incorrect map file path or incorrect file format (should be .map)");
		}
	}

	/**
	 * Sets the max age for the cached images. This means that when the cache is
	 * cleaned, every image that has not been used in the last
	 * <code>maxCacheAge</code> milliseconds will be deleted.
	 * 
	 * @param maxCacheAge
	 *            Maximum milliseconds to keep the images when the cache is
	 *            cleaned
	 */
	public void setMaxCacheAge(long maxCacheAge) {
		if (maxCacheAge < 15000) {
			this.maxCacheAge = 15000;
		} else {
			this.maxCacheAge = maxCacheAge;
		}
	}

	/**
	 * Sets up the maximum cache size. If the cache exceeds this size, it will
	 * be cleaned. This attribute has to be greater than
	 * <code>cleanCacheTrigger</code>, if not, the trigger will be set up to
	 * half the maximum cache size.
	 * 
	 * @param maxCacheSize
	 *            Maximum cache size(in MB)
	 */
	public void setMaxCacheSize(int maxCacheSize) {
		if (maxCacheSize <= this.cleanCacheTrigger) {
			this.maxCacheSize = maxCacheSize;
			this.cleanCacheTrigger = this.maxCacheSize / 2;
		}
		this.maxCacheSize = maxCacheSize;
	}

	/**
	 * Sets up the overdraw factor that will be used for rendering the tiles. By
	 * default its value is 1.2f.
	 * 
	 * @param overdrawFactor
	 *            New overdraw factor
	 */
	public void setOverdrawFactor(float overdrawFactor) {
		this.overdrawFactor = overdrawFactor;
	}

	/**
	 * Changes the render theme to the new one provided. If it doesn't exist,
	 * the theme will not be changed.
	 * 
	 * @param renderThemePath
	 *            Absolute path to the theme file
	 * @throws FileNotFoundException
	 */
	public void setRenderTheme(String renderThemePath)
			throws IllegalArgumentException, FileNotFoundException {
		if (renderThemePath != null
				&& renderThemePath.substring(renderThemePath.length() - 4,
						renderThemePath.length()).equals(".xml")) {

			if(!renderThemePath.contains("/")) 
				renderThemePath = this.context.getFilesDir() + "/renderthemes/"+renderThemePath;
			
			File newTheme = new File(renderThemePath);
			if (newTheme.exists()) {
				this.renderTheme = new ExternalRenderTheme(newTheme);
			} else {
				throw new FileNotFoundException("Render theme doesn't exist: "+ renderThemePath);
			}

		} else {
			throw new IllegalArgumentException(
					"Incorrect theme file path ("+renderThemePath+") or incorrect file format (should be .xml)");
		}
	}

	/**
	 * Changes the screen ratio used when the images are generated. By default
	 * its value is 1f.
	 * 
	 * @param screenRatio
	 *            New screen ratio
	 */
	public void setScreenRatio(float screenRatio) {
		this.screenRatio = screenRatio;
	}

	/**
	 * Sets the size of the images generated. By default its value is 256.
	 * 
	 * @param tileSize
	 *            New tile size
	 */
	public void setTileSize(int tileSize) {
		if (tileSize < 0) {
			this.tileSize = 256;
		} else {
			this.tileSize = tileSize;
		}

		if (displayModel != null) {
			displayModel.setFixedTileSize(this.tileSize);
		}
	}
}
