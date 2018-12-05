package com.ecar.ws.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test suite for Immutable singleton map with WORM properties (Write-Once, Read-Many).
 * 
 * @author Miguel Pardal
 */
public class HandlersConfigTest {

	// static members

	/** Use a prefix for properties to avoid collisions with properties in other tests. */
	private static final String PREFIX = HandlersConfigTest.class.getName() + ".";
	/** Singleton instance. */
	private static HandlersConfig config = HandlersConfig.INSTANCE;

	
	// one-time initialization and clean-up

	@BeforeClass
	public static void oneTimeSetUp() {
		// write some properties once
		config.putOnce(PREFIX + "x", "10");
		config.putOnce(PREFIX + "y", "20");
	}

	@AfterClass
	public static void oneTimeTearDown() {

	}

	// members

	// initialization and clean-up for each test

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	// tests

	@Test
	public void testAddedProperties() {
		// assertEquals(expected, actual);
		// if the assert fails, the test fails
		assertEquals("10", config.get(PREFIX + "x"));
		assertEquals("20", config.get(PREFIX + "y"));
	}

	@Test
	public void testAddedPropertiesInKeySet() {
		Set<String> keys = config.keySet();
		assertTrue(keys.contains(PREFIX + "x"));
		assertTrue(keys.contains(PREFIX + "y"));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testUnmodifiableKeySet() throws Exception {
		// JUnit expects the exception declared in the annotation
		// if it is not thrown, the test fails
		config.keySet().addAll(Collections.emptyList());
		// reference:
		// https://stackoverflow.com/questions/8364856/how-to-test-if-a-list-extends-object-is-an-unmodifablelist
	}

	@Test
	public void testPutOnce() {
		config.putOnce(PREFIX + "z", "30");
		assertEquals("30", config.get(PREFIX + "z"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPutTwiceSameValues() throws Exception {
		config.putOnce(PREFIX + "w", "40");
		config.putOnce(PREFIX + "w", "40");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPutTwiceDifferentValues() throws Exception {
		config.putOnce(PREFIX + "v", "40");
		config.putOnce(PREFIX + "v", "45");
	}

}
