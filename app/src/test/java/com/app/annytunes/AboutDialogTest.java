package com.app.annytunes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.app.annytunes.ui.AboutDialog;

import org.junit.Test;

public class AboutDialogTest {

    @Test
    public void testGitUrlConfigured() {
        assertNotNull(AboutDialog.GIT_URL);
        assertTrue(AboutDialog.GIT_URL.startsWith("https://github.com/"));
        assertEquals("https://github.com/dirtybug/annytunes", AboutDialog.GIT_URL);
    }

    @Test
    public void testContributorsUrlConfigured() {
        assertNotNull(AboutDialog.CONTRIBUTORS_URL);
        assertTrue(AboutDialog.CONTRIBUTORS_URL.startsWith("https://github.com/"));
        assertEquals("https://github.com/dirtybug/annytunes/graphs/contributors", AboutDialog.CONTRIBUTORS_URL);
    }
}
