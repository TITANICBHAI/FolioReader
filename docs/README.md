# Folio Reader website

This is the redesigned Folio Reader marketing/support website built around the actual Android UI screenshots.

## Screenshot treatment

The provided 720x1600 screenshots were cropped from y=64 to y=1515 to remove the Android system status/navigation areas. The remaining pixels are the original app UI, with only a very light sharpening pass. Layout, shadows, borders and responsive presentation are handled in CSS.

## Before publishing

1. Replace the `#` Google Play links with the real Play Store URL.
2. Replace `REPLACE_WITH_SUPPORT_EMAIL` in `support.html` and the legal pages with the real support/privacy address.
3. Review the Privacy Policy/Terms against the final release build and Play Console Data safety declaration.
4. Confirm whether Classroom mode, Gemini explanations, analytics, advertising, or any future server-side service are enabled in the release; update the legal text accordingly.

## Files

- `index.html` — product homepage
- `features.html` — detailed features
- `screenshots.html` — real product screenshots
- `about.html` — app/project overview
- `faq.html` — common questions
- `support.html` — support form
- `privacy.html` — privacy policy draft
- `terms.html` — terms draft
- `data-deletion.html` — data deletion instructions
- `style.css` — responsive editorial design
- `script.js` — mobile nav/year helper
- `assets/` — cleaned screenshot assets
