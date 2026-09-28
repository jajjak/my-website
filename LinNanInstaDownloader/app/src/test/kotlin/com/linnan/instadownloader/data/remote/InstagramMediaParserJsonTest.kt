package com.linnan.instadownloader.data.remote

import com.linnan.instadownloader.data.model.MediaType
import com.linnan.instadownloader.data.model.PostType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * These fixtures mirror the field names/shape actually observed in Instagram's
 * private "items"-style response and its legacy public "graphql" response
 * (both captured by hand against real public posts while building the parser).
 * They exist to prove the parsing logic itself is correct; they are test
 * fixtures only and are never bundled into the shipped app or shown as if
 * they were a real network response.
 */
class InstagramMediaParserJsonTest {

    private val parser = InstagramMediaParser()

    @Test
    fun `parses a single photo from the private api shape`() {
        val json = JSONObject(
            """
            {
              "items": [{
                "id": "111_222",
                "code": "ABC123xyz",
                "media_type": 1,
                "user": {"username": "testuser"},
                "caption": {"text": "hello world"},
                "image_versions2": {
                  "candidates": [
                    {"url": "https://cdn.example/full.jpg", "width": 1080, "height": 1350},
                    {"url": "https://cdn.example/small.jpg", "width": 320, "height": 400}
                  ]
                }
              }]
            }
            """.trimIndent()
        )

        val post = parser.parsePostJson(json, "ABC123xyz")!!

        assertEquals(PostType.PHOTO, post.postType)
        assertEquals("testuser", post.ownerUsername)
        assertEquals("hello world", post.caption)
        assertEquals(1, post.items.size)
        val item = post.items.first()
        assertEquals(MediaType.IMAGE, item.type)
        assertEquals("https://cdn.example/full.jpg", item.downloadUrl)
        assertEquals(1080, item.width)
        assertEquals(1350, item.height)
    }

    @Test
    fun `parses a single video and picks the highest-width video candidate`() {
        val json = JSONObject(
            """
            {
              "items": [{
                "id": "111_222",
                "media_type": 2,
                "user": {"username": "videouser"},
                "image_versions2": {
                  "candidates": [{"url": "https://cdn.example/cover.jpg", "width": 720, "height": 1280}]
                },
                "video_versions": [
                  {"url": "https://cdn.example/low.mp4", "width": 480, "height": 852},
                  {"url": "https://cdn.example/high.mp4", "width": 1080, "height": 1920}
                ]
              }]
            }
            """.trimIndent()
        )

        val post = parser.parsePostJson(json, "VIDEOSHORTCODE")!!

        assertEquals(PostType.VIDEO, post.postType)
        assertEquals(1, post.items.size)
        val item = post.items.first()
        assertEquals(MediaType.VIDEO, item.type)
        assertEquals("https://cdn.example/high.mp4", item.downloadUrl)
        assertEquals("https://cdn.example/cover.jpg", item.thumbnailUrl)
        assertEquals(1080, item.width)
    }

    @Test
    fun `parses every item of a carousel, not just the first`() {
        val json = JSONObject(
            """
            {
              "items": [{
                "id": "999",
                "media_type": 8,
                "user": {"username": "carouseluser"},
                "carousel_media": [
                  {"media_type": 1, "image_versions2": {"candidates": [{"url": "https://cdn.example/1.jpg", "width": 1080, "height": 1080}]}},
                  {"media_type": 2, "image_versions2": {"candidates": [{"url": "https://cdn.example/2cover.jpg", "width": 720, "height": 1280}]}, "video_versions": [{"url": "https://cdn.example/2.mp4", "width": 1080, "height": 1920}]},
                  {"media_type": 1, "image_versions2": {"candidates": [{"url": "https://cdn.example/3.jpg", "width": 1080, "height": 1350}]}}
                ]
              }]
            }
            """.trimIndent()
        )

        val post = parser.parsePostJson(json, "CAROUSELSHORTCODE")!!

        assertEquals(PostType.CAROUSEL, post.postType)
        assertEquals(3, post.items.size)
        assertEquals(MediaType.IMAGE, post.items[0].type)
        assertEquals(MediaType.VIDEO, post.items[1].type)
        assertEquals(MediaType.IMAGE, post.items[2].type)
        assertEquals("https://cdn.example/2.mp4", post.items[1].downloadUrl)
        assertEquals("https://cdn.example/3.jpg", post.items[2].downloadUrl)
    }

    @Test
    fun `parses the legacy graphql sidecar shape used by the embed page`() {
        val json = JSONObject(
            """
            {
              "graphql": {
                "shortcode_media": {
                  "__typename": "GraphSidecar",
                  "owner": {"username": "owner1"},
                  "edge_media_to_caption": {"edges": [{"node": {"text": "caption text"}}]},
                  "edge_sidecar_to_children": {
                    "edges": [
                      {"node": {"__typename": "GraphImage", "display_url": "https://cdn.example/img1.jpg", "dimensions": {"width": 1080, "height": 1080}}},
                      {"node": {"__typename": "GraphVideo", "video_url": "https://cdn.example/vid1.mp4", "display_url": "https://cdn.example/cover1.jpg", "dimensions": {"width": 720, "height": 1280}}}
                    ]
                  }
                }
              }
            }
            """.trimIndent()
        )

        val post = parser.parsePostJson(json, "SIDECARSHORTCODE")!!

        assertEquals(PostType.CAROUSEL, post.postType)
        assertEquals("owner1", post.ownerUsername)
        assertEquals("caption text", post.caption)
        assertEquals(2, post.items.size)
        assertEquals(MediaType.IMAGE, post.items[0].type)
        assertEquals(MediaType.VIDEO, post.items[1].type)
        assertEquals("https://cdn.example/vid1.mp4", post.items[1].downloadUrl)
    }

    @Test
    fun `returns null when the response has no recognizable media shape`() {
        val json = JSONObject("""{"status": "fail", "message": "not found"}""")
        assertNull(parser.parsePostJson(json, "NONE"))
    }
}
