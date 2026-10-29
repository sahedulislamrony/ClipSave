# HowItWorks

A pushed page (Settings › Other › How it works, and the button on Home): the whole flow at a glance.

- **Timeline** — one panel with four steps (Share · Or paste · Choose · Done). Each step is a 44dp iris-gradient badge with an icon; a thin iris rail runs down to the next badge, so the order reads without numbering. Title is Headline, body is Body in the muted ink.
- **Also handy** — a 2×2 grid of tiles (Playlists · Pause & resume · Your folder · Private): a 36dp iris-soft icon well, a title and one short line. Tiles in a row share the same height.
- **Works with** — glass pills for each supported site, ending with a soft-iris "1000+ more" pill.
- The rights line closes the page.
- On open, the blocks fade in and lift 22dp, 70ms apart, top to bottom (`Modifier.reveal`).

Back is the glass arrow at the top; the dock is hidden on pushed pages.
