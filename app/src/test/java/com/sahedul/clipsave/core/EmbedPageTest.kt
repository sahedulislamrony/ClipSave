package com.sahedul.clipsave.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Fixtures are trimmed from the real embed pages (October 2026). */
class EmbedPageTest {

    @Test fun `instagram embed gives image, username and the caption's first line`() {
        val html = """
            <div class="Header"><a class="Username"><span class="UsernameText">instagram</span><i class="Sprite"></i></a></div>
            <img class="EmbeddedMediaImage" alt="Instagram post shared by &#064;instagram" src="https://instagram.fdac1-1.fna.fbcdn.net/v/t51/503_n.jpg?stp=dst-jpg&amp;_nc_ht=x&amp;oh=00_A" srcset="x" />
            <div class="Caption"><a class="CaptionUsername" href="https://www.instagram.com/instagram/?a=1&amp;b=2" target="_blank">instagram</a><br /><br />“Gingerton” &amp; friends<br /><br /><a href="/august/">&#064;august</a> loves dressing up</div>
        """.trimIndent()
        assertEquals(
            LinkPreview("“Gingerton” & friends", "instagram", "https://instagram.fdac1-1.fna.fbcdn.net/v/t51/503_n.jpg?stp=dst-jpg&_nc_ht=x&oh=00_A"),
            EmbedPage.instagram(html),
        )
    }

    @Test fun `instagram embed without a caption still gives the image`() {
        val html = """<span class="UsernameText">someone</span><img class="EmbeddedMediaImage" alt="" src="https://cdn.example/a.jpg" />"""
        assertEquals(LinkPreview(null, "someone", "https://cdn.example/a.jpg"), EmbedPage.instagram(html))
    }

    @Test fun `facebook embed gives the frame and the owner's name`() {
        val html = """
            <div><img class="_1p6f _1p6g img" src="https://scontent.fdac1-1.fna.fbcdn.net/v/t15.5256-10/315_n.jpg?_nc_cat=107&amp;oh=00_B" alt="" /></div>
            <img class="_2qgu img" src="https://scontent.fdac1-1.fna.fbcdn.net/v/t39.30808-1/463_n.jpg?stp=cp0" alt="" aria-label="Asif Nawab Butt" role="img" style="width:40px" />
        """.trimIndent()
        assertEquals(
            LinkPreview(null, "Asif Nawab Butt", "https://scontent.fdac1-1.fna.fbcdn.net/v/t15.5256-10/315_n.jpg?_nc_cat=107&oh=00_B"),
            EmbedPage.facebook(html),
        )
    }

    @Test fun `pages without the expected markup give no preview`() {
        assertNull(EmbedPage.instagram("<html><body>Sorry, this page isn't available.</body></html>"))
        assertNull(EmbedPage.facebook("<html><body>This video can't be embedded.</body></html>"))
    }

    @Test fun `unescape handles named and numeric entities and leaves unknown ones`() {
        assertEquals("a & b \"c\" 'd' <e> @ é", EmbedPage.unescape("a &amp; b &quot;c&quot; &apos;d&#039; &lt;e&gt; &#064; &#xe9;"))
        assertEquals("&nbsp; &#99999999;", EmbedPage.unescape("&nbsp; &#99999999;"))
    }
}
