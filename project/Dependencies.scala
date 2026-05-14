import sbt.*

import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport.*

object Dependencies {
  object Version {
    val cats = "2.13.0"
    val catsEffect = "3.6.1"
    val iron = "3.0.2"
    val kitten = "3.5.0"
    val monocle = "3.3.0"
    val skunk = "0.6.4"
    val circe = "0.14.14"
    val squants = "1.8.3"
    val http4s = "0.23.30"
    val log4cats = "2.7.1"
    val laminar = "17.2.1"
    val chimney = "1.8.1"
    val xml = "2.4.0"
    val fs2IO = "3.12.0"

    // config
    val ciris = "3.9.0"

    // test
    val scalacheck = "1.18.1"
    val weaver = "0.8.4"

  }

  object Library {

    val cats = Def.setting("org.typelevel" %%% "cats-core" % Version.cats)
    val catsEffect =
      Def.setting("org.typelevel" %%% "cats-effect" % Version.catsEffect)

    val circe = Def.setting("io.circe" %%% "circe-core" % Version.circe)
    val circeGeneric =
      Def.setting("io.circe" %%% "circe-generic" % Version.circe)
    val circeParser = Def.setting("io.circe" %%% "circe-parser" % Version.circe)
    val fs2IO = Def.setting("co.fs2" %%% "fs2-io" % Version.fs2IO)

    val htt4sCirce =
      Def.setting("org.http4s" %%% "http4s-circe" % Version.http4s)
    val http4sClient =
      Def.setting("org.http4s" %%% "http4s-client" % Version.http4s)
    val htt4sDsl = Def.setting("org.http4s" %%% "http4s-dsl" % Version.http4s)
    val htt4sEmberServer =
      Def.setting("org.http4s" %%% "http4s-ember-server" % Version.http4s)
    val htt4sEmberClient =
      Def.setting("org.http4s" %% "http4s-ember-client" % Version.http4s)

    val chimney = Def.setting("io.scalaland" %% "chimney" % Version.chimney)

    val iron = Def.setting("io.github.iltotore" %%% "iron" % Version.iron)
    val ironCat =
      Def.setting("io.github.iltotore" %%% "iron-cats" % Version.iron)
    val ironCirce =
      Def.setting("io.github.iltotore" %%% "iron-circe" % Version.iron)
    val ironScalaC =
      Def.setting("io.github.iltotore" %%% "iron-scalacheck" % Version.iron)
    val ironSkunk =
      Def.setting("io.github.iltotore" %% "iron-skunk" % Version.iron)
    val kitten = Def.setting("org.typelevel" %% "kittens" % Version.kitten)
    val log4cats = "org.typelevel" %% "log4cats-core" % Version.log4cats
    val log4catslf4j = "org.typelevel" %% "log4cats-slf4j" % Version.log4cats
    val monocle = Def.setting("dev.optics" %% "monocle-core" % Version.monocle)
    val skunkCirce = "org.tpolecat" %% "skunk-circe" % Version.skunk
    val skunkCore = "org.tpolecat" %% "skunk-core" % Version.skunk
    val squants = Def.setting("org.typelevel" %%% "squants" % Version.squants)
    val xml =
      Def.setting("org.scala-lang.modules" %%% "scala-xml" % Version.xml)

    // config library
    val ciris = "is.cir" %% "ciris" % Version.ciris

    // Testing library
    val weaverCats = "com.disneystreaming" %% "weaver-cats" % Version.weaver
    val weaverDiscipline =
      "com.disneystreaming" %% "weaver-discipline" % Version.weaver
    val weaverScalaCheck =
      "com.disneystreaming" %% "weaver-scalacheck" % Version.weaver

    // UI Library
    val laminar = Def.setting("com.raquo" %%% "laminar" % Version.laminar)

  }
}
