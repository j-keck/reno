+++
title = "reno"
type = "docs"
draft = false
creator = "Emacs 26.3 (Org mode 9.1.9 + ox-hugo)"
weight = 10
+++

`reno` extracts highlighted text from your pdf in an org-file.

The highlighted text be placed in a `#+BEGIN_QUOTE` / `#+END_QUOTE` block
and a drawer with the id's from extracted text.

You can later run an update and `reno` will recognize the exported blocks and
don't export it a second time / insert new highlighted text in right position.

Other text, headings, quotes and src blocks will keep intact.

{{< hint info >}}
More documentation / examples later.
{{< /hint >}}
