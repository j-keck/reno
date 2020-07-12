+++
title = "Example"
draft = false
creator = "Emacs 26.3 (Org mode 9.1.9 + ox-hugo)"
weight = 40
+++

## Reading notes {#reading-notes}

I use `reno` to create reading notes.

From this marked PDF Document:

{{< figure src="/market-text-example.png" >}}

i get (after some edits) this org-mode snippet:

{{< figure src="/market-text-example-notes-org.png" >}}

where i can

-   change some quotes to headers
-   edit exported text
-   add some notes for me


## Play with source code {#play-with-source-code}

I can mark some challenges:

{{< figure src="/market-challenges.png" >}}

and pack the source code in `#+begin_src` and `#+end_src` blocks to evaluate it with org-babel.

{{< figure src="/market-challenges-notes-org.png" >}}

Later i can export the notes from Emacs to a Pdf file to read it later again:

{{< figure src="/market-challenges-notes-pdf.png" >}}
