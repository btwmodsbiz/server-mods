package btwmod.livemap;

import java.util.LinkedHashMap;

public class MapImageCache<K> extends LinkedHashMap<K, MapImage> {
	
	private final mod_LiveMap mod;
	private final MapLayer layer;

	public MapImageCache(MapLayer layer) {
		this.layer = layer;
		this.mod = layer.map.mod;
	}
	
	@Override
	protected boolean removeEldestEntry(java.util.Map.Entry<K, MapImage> eldest) {
		if (size() > mod.mapImageCacheMax) {
			layer.onCacheRemoved(eldest.getValue());
			return true;
		}
		
		return super.removeEldestEntry(eldest);
	}
}
