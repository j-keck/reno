+++
title = "reno"
type = "docs"
draft = false
creator = "Emacs 26.3 (Org mode 9.1.9 + ox-hugo)"
weight = 10
+++

`reno` extracts highlighted text from your pdf in an org-file.

The highlighted text will be placed in a `#+BEGIN_QUOTE` / `#+END_QUOTE` block
with a drawer with the id's from extracted text.

To update your notes and include new highlighted text, use the `update` subcommand.
This will insert new highlighted text in right position.

You can merge blocks or change the exported block to headings, source blocks, plain text or latex fragments.
When you keep the drawer with the id's, `reno` will recognize this blocks and keep it intact when you update your notes.

{{< hint warning >}}
****This project is in a early state!****
{{< /hint >}}
