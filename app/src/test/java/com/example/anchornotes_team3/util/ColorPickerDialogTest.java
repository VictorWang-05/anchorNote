package com.example.anchornotes_team3.util;

import android.graphics.Color;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Utility Classes (8 tests) - Jeffrey
 *
 * 22. ColorPickerDialog - Test valid hex color conversion
 */
@RunWith(RobolectricTestRunner.class)
public class ColorPickerDialogTest {

	/**
	 * Test 22: ColorPickerDialog - Test valid hex color conversion
	 * White-box: invoke private toHex(int) via reflection and verify #RRGGBB format and value
	 */
	@Test
	public void testToHexConversion() throws Exception {
		Method toHex = ColorPickerDialog.class.getDeclaredMethod("toHex", int.class);
		toHex.setAccessible(true);

		// Exact colors
		assertEquals("#FF0000", toHex.invoke(null, Color.parseColor("#FF0000")));
		assertEquals("#00FF00", toHex.invoke(null, Color.parseColor("#00FF00")));
		assertEquals("#0000FF", toHex.invoke(null, Color.parseColor("#0000FF")));

		// Arbitrary color (alpha should be stripped)
		int argb = Color.argb(0x80, 0x12, 0x34, 0x56);
		String hex = (String) toHex.invoke(null, argb);
		assertTrue(hex.matches("^#[0-9A-F]{6}$"));
		assertEquals("#123456", hex);
	}
}

