package com.lagoda.mnemo.extraction

object ExtractionPrompts {
    fun buildExtractionPrompt(): String = """
        Analyze this screenshot and return a JSON object with exactly these fields.
        Infer the source from visual cues: LinkedIn has blue header/profile cards,
        Reddit has upvote arrows and subreddit names, Twitter/X has bird logo or @handles,
        Instagram has square images and heart/comment icons, email has To/From/Subject fields,
        chat apps have message bubbles, articles have large text blocks with no social chrome.

        Return ONLY valid JSON, no markdown, no explanation:
        {
          "source_type": "<linkedin|instagram|reddit|twitter|email|chat|article|other>",
          "title": "<inferred title, subject line, or post headline — max 100 chars>",
          "entities": ["<person name>", "<company name>", "<project name>", "<URL>"],
          "topics": ["<topic 1>", "<topic 2>"],
          "action_items": ["<action if any>"],
          "summary": "<2-3 sentence summary of the content>",
          "sentiment": "<positive|negative|neutral|mixed>",
          "urgency": <float 0.0 to 1.0>,
          "language": "<ISO 639-1 code, e.g. en>"
        }

        If a field has no meaningful value, use an empty array [] or empty string "".
        urgency: 1.0 = immediate action needed, 0.0 = purely informational.
    """.trimIndent()
}
