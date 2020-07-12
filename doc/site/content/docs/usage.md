+++
title = "Usage"
draft = false
creator = "Emacs 26.3 (Org mode 9.1.9 + ox-hugo)"
weight = 30
+++

## Usage {#usage}

```text
main⟩ reno --help
Usage:
    reno export
    reno update
    reno dump

reading notes extractor

Options and flags:
    --help
        Display this help text.
    --version, -v
        Print the version number and exit.

Subcommands:
    export
        Export the annotations from the given Pdf
    update
        update the notes in the Org-file with new annotations from the Pdf
    dump
        for debugging / inspection (run without arguments for the usage)
```


## Export annotations to an org-file {#export-annotations-to-an-org-file}

Use the `export` subcommand to export your annotations.

```text
main⟩ reno export
Usage: reno export [--overwrite] [--pdf-engine <value>] [--mark-from <value>] <pdf> [<org>]

Export the annotations from the given Pdf

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


## Update new annotations {#update-new-annotations}

To update your org-file and include new annotations from your pdf, use the `update` subcommand.

```text
main⟩ reno update
Usage: reno update [--pdf-engine <value>] [--mark-from <value>] <pdf> [<src-org>] [<dst-org>]

update the notes in the Org-file with new annotations from the Pdf

Options and flags:
    --help
        Display this help text.
    --pdf-engine <value>
        Used pdf engine (itext / pdfbox)
    --mark-from <value>
        Extract mark from (boundingrect / quads)
```
