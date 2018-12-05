package com.ecar.ws.handler;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Immutable singleton map with WORM properties (Write-Once, Read-Many). This
 * allows an application to configure handlers behavior at start-up, but not
 * later. This protection is intended to prevent concurrent modification of
 * shared variables and keep handlers thread-safe. Each property has a name
 * (key) and a string value (not objects), because strings are immutable too.
 * 
 * More about enforcing a singleton with enum:
 * http://www.informit.com/articles/article.aspx?p=1216151&seqNum=3
 * 
 * More about immutable global state:
 * https://softwareengineering.stackexchange.com/a/148154/61334
 * 
 * More about thread-safe WORM map: https://stackoverflow.com/a/10152792/129497
 * 
 * @author Miguel Pardal
 *
 */
public enum HandlersConfig {

	/** The one and only instance. */
	INSTANCE;

	/**
	 * The map where properties are stored. A concurrent access implementation is
	 * used.
	 */
	private final Map<String, String> props = new ConcurrentHashMap<String, String>();

	/**
	 * Get property value. If property does not exist, null is returned.
	 * 
	 * @param key
	 *            Property name
	 */
	public String get(String key) {
		return props.get(key);
	}

	/**
	 * Get available property names.
	 * 
	 * @return unmodifiable set of property names
	 */
	public Set<String> keySet() {
		return Collections.unmodifiableSet(props.keySet());
	}

	/**
	 * Set property value. If property already exists (!= null) then an error is
	 * thrown. This method enforces WORM (Write-Once, Read-Many).
	 *
	 * @param key
	 *            Property name
	 * @param value
	 *            Property value
	 */
	public void putOnce(String key, String value) {
		String valueInMap = props.putIfAbsent(key, value);
		if (valueInMap != null) {
			throw new IllegalArgumentException("Properties can only be initialized (written) once!");
		}
	}

}
