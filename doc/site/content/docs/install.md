+++
title = "Installation"
draft = false
creator = "Emacs 26.3 (Org mode 9.1.9 + ox-hugo)"
weight = 20
+++

## Binary packages {#binary-packages}

You can download the latest binary package from ****here**** or from the [GitHub release page](https://github.com/j-keck/reno/releases).

{{<tabs "install">}}
{{< tab "Linux (x64)" >}}
  1.) ****Download**** the latest version: [reno-linux-x64.zip](https://github.com/j-keck/reno/releases/download/v0.3.0/reno-linux-x64.zip)

  2.) Run it:  `./reno`
{{< /tab >}}

{{< tab "Generic (Java)" >}}
  1.) ****Download**** the latest version: [reno-0.3.0.jar](https://github.com/j-keck/reno/releases/download/v0.3.0/reno-0.3.0.jar)

  2.) Run it:  `java -jar reno-0.3.0.jar`
{{< /tab >}}

{{< /tabs >}}

{{< hint warning >}}
The `--pdf-backend pdfbox` flag gives better results, but doesn't work with `native-image` ([see PDFBOX-4548](https://issues.apache.org/jira/browse/PDFBOX-4548)).

So if you would use the [PDFBox](https://pdfbox.apache.org/) backend, use the "Generic (Java)" version.
{{< /hint >}}
