import scala.sys.process._

val scalaVersions = Seq("2.13.5", "2.12.13", "2.11.12")
val macrosParadiseVersion = "2.1.1"

// version is derived from latest git tag
organization in ThisBuild := "com.github.duhemm"
scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-feature",
  // "-Xfatal-warnings",
  "-Xlint"
)
licenses in ThisBuild := Seq(("BSD New", url("http://opensource.org/licenses/BSD-3-Clause")))
homepage in ThisBuild := Some(url("https://github.com/olafurpg/sbt-ci-release"))
developers in ThisBuild := List(
  Developer(
    "jodersky",
    "Jakob Odersky",
    "jakob@odersky.com",
    url("https://jakob.odersky.com")
  )
)

lazy val root = (project in file("."))
  .aggregate(macros, plugin)
  .settings(
    publish := {},
    publishLocal := {},
    // make sbt-pgp happy
    publishTo := Some(Resolver.file("Unused transient repository", target.value / "unusedrepo")),
    addCommandAlias("test-plugin", ";+macros/publishLocal;scripted")
  )

lazy val macros = (project in file("macros"))
  .disablePlugins(ScriptedPlugin)
  .settings(
    name := "sbt-jni-macros",
    crossScalaVersions := scalaVersions,
    libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value % Provided,
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,

    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => Seq()
        case _ => Seq(
          compilerPlugin("org.scalamacros" % "paradise" % macrosParadiseVersion cross CrossVersion.full)
        )
      }
    },
    Compile / scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => Seq("-Ymacro-annotations")
        case _ => Seq()
      }
    }
  )

lazy val plugin = (project in file("plugin"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-jni",
    libraryDependencies += "org.ow2.asm" % "asm" % "6.2.1",
    // make project settings available to source
    sourceGenerators in Compile += Def.task {
      val src = s"""|/* Generated by sbt */
                    |package ch.jodersky.sbt.jni
                    |
                    |private[jni] object ProjectVersion {
                    |  final val Organization = "${organization.value}"
                    |  final val MacrosParadise = "${macrosParadiseVersion}"
                    |  final val Macros = "${version.value}"
                    |}
                    |""".stripMargin
      val file = sourceManaged.value / "ch" / "jodersky" / "sbt" / "jni" / "ProjectVersion.scala"
      IO.write(file, src)
      Seq(file)
    }.taskValue,
    scriptedLaunchOpts := Seq(
      "-Dplugin.organization=" + organization.value,
      "-Dplugin.version=" + version.value,
      "-Xmx2g", "-Xss2m"
    )
  )
