addCommandAlias("package-jar", "assembly")
addCommandAlias("package-native", "graalvm-native-image:packageBin")

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin, GitVersioning, GraalVMNativeImagePlugin)
  .settings(
    name := "reno",
    version := (git.gitDescribedVersion.value.getOrElse("_no-git-version-info_")),
    scalaVersion := "2.13.2",
    scalacOptions += "-target:jvm-1.8",
    maintainer := "code@j-keck.net",
    // git
    git.useGitDescribe := true,
    buildInfoPackage := "reno",
    buildInfoKeys := Seq[BuildInfoKey](name, scalaVersion, version, git.gitDescribedVersion, git.gitHeadCommit),
    // libs
    libraryDependencies ++= {
      val catsV           = "2.1.1"
      val catsEffectV     = "2.1.3"
      val kittensV        = "2.1.0"
      val pdfBoxV         = "2.0.19"
      val log4catsV       = "1.1.1"
      val logbackClassicV = "1.2.3"
      val declineV        = "1.0.0"
      val scalaCheckV     = "1.14.1"
      val scalacticV      = "3.1.2"
      val scalatestV      = "3.1.2"

      Seq(
        "org.typelevel"     %% "cats-core"       % catsV,
        "org.typelevel"     %% "cats-testkit"    % catsV,
        "org.typelevel"     %% "cats-effect"     % catsEffectV,
        "org.typelevel"     %% "kittens"         % kittensV,
        "org.apache.pdfbox"  % "pdfbox"          % pdfBoxV,
        "io.chrisdavenport" %% "log4cats-slf4j"  % log4catsV,
        "ch.qos.logback"     % "logback-classic" % logbackClassicV,
        "com.monovore"      %% "decline-effect"  % declineV,
        "org.scalacheck"    %% "scalacheck"      % scalaCheckV % "test",
        "org.scalactic"     %% "scalactic"       % scalacticV  % "test",
        "org.scalatest"     %% "scalatest"       % scalatestV  % "test"
      )
    },
    // sbt-assembly
    assemblyJarName := name.value + "-" + version.value + ".jar",
    // sbt-native-packager / graal
    graalVMNativeImageGraalVersion := scala.util.Properties
      .envOrNone("CI")
      .filter(_ == "true")
      .fold(Option("20.1.0-java11"))((_: String) => None),
    graalVMNativeImageOptions ++= {
      val options =  Seq(
        "--no-fallback",
        "--static",
        "--no-server",
        "--initialize-at-build-time",
        "--allow-incomplete-classpath",
        "-H:+TraceClassInitialization",
        "-H:+ReportExceptionStackTraces"
      ) ++ (System.getProperty("os.name").toLowerCase match {
        case name if name.startsWith("linux") =>
          List(
            "--static"
            , "--initialize-at-run-time=org.apache.pdfbox.pdmodel.encryption.PublicKeySecurityHandler"
          )
        case name if name.startsWith("mac")   =>
          List(
            "--initialize-at-run-time=org.apache.pdfbox.pdmodel.encryption.PublicKeySecurityHandler"+
              ",org.apache.fontbox.ttf.BufferedRandomAccessFile"+
              ",org.apache.pdfbox.pdmodel.font.PDType1Font"
          )
        case _                                => Nil
      })
      val log = sLog.value
      val os = System.getProperty("os.name")
      log.warn(s"native-image options on '${os}': ${options.mkString(" ")}")
      options
    }
  )

inThisBuild(
  Seq(
    organization := "net.j-keck",
    homepage := Some(url("https://j-keck.github.io/reno")),
    licenses := List("MIT" -> url("https://opensource.org/licenses/MIT")),
    developers := List(
      Developer(
        "j-keck",
        "Jürgen Keck",
        "code@j-keck.net",
        url("https://github.com/j-keck")
      )
    )
  )
)
