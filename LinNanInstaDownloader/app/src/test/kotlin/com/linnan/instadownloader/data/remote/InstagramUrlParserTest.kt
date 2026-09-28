package com.linnan.instadownloader.data.remote

import com.linnan.instadownloader.data.model.PostType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstagramUrlParserTest {

    @Test
    fun `parses a plain post url`() {
        val result = InstagramUrlParser.parse("https://www.instagram.com/p/DW1nTDiDvnF/")
        assertEquals("DW1nTDiDvnF", result?.shortcode)
        assertEquals(PostType.PHOTO, result?.hintedPostType)
    }

    @Test
    fun `parses a reel url`() {
        val result = InstagramUrlParser.parse("https://instagram.com/reel/AbC123-_XY/")
        assertEquals("AbC123-_XY", result?.shortcode)
        assertEquals(PostType.REEL, result?.hintedPostType)
    }

    @Test
    fun `parses a reels url with share query params`() {
        val result = InstagramUrlParser.parse("https://www.instagram.com/reels/XyZ789/?igsh=abc123")
        assertEquals("XyZ789", result?.shortcode)
        assertEquals(PostType.REEL, result?.hintedPostType)
    }

    @Test
    fun `parses a url with a leading username segment`() {
        val result = InstagramUrlParser.parse("https://www.instagram.com/nasa/p/DW1nTDiDvnF/")
        assertEquals("DW1nTDiDvnF", result?.shortcode)
    }

    @Test
    fun `rejects a non-instagram url`() {
        assertNull(InstagramUrlParser.parse("https://example.com/p/DW1nTDiDvnF/"))
    }

    @Test
    fun `rejects blank input`() {
        assertNull(InstagramUrlParser.parse(""))
        assertNull(InstagramUrlParser.parse("   "))
    }

    @Test
    fun `rejects arbitrary text`() {
        assertNull(InstagramUrlParser.parse("this is not a url"))
    }
}
