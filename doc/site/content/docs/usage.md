+++
title = "Usage"
draft = false
creator = "Emacs 26.3 (Org mode 9.1.9 + ox-hugo)"
weight = 30
+++

## Usage {#usage}

```text
Usage:
    reno dump
    reno update
    reno debug-show

reading notes extractor

Options and flags:
    --help
        Display this help text.
    --version, -v
        Print the version number and exit.

Subcommands:
    dump
        Dump the annotations from the given Pdf
    update
        update the notes in the Org-file with new annotations from the Pdf
    debug-show
        debug show each annotation with meta informations
```


## Dump annotations to an org-file {#dump-annotations-to-an-org-file}

```text
Usage: reno dump [--overwrite] [--pdf-engine <value>] [--mark-from <value>] <pdf> [<org>]

Dump the annotations from the given Pdf

Options and flags:
    --help
        Display this help text.
    --overwrite, -f
        Overwrite existing file
    --pdf-engine <value>
        Used pdf engine (itext / pdfbox)
    --mark-from <value>
        Extract mark from (boundingrect / quads)
```
