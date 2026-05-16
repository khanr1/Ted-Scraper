package io.github.khanr1.tedscraper

import cats.effect.IO
import weaver.*

import java.io.File
import java.nio.file.Files as JFiles

import io.github.khanr1.tedscraper.services.TedNoticeService

object TedXmlRepositorySuite extends SimpleIOSuite:

  private lazy val projectRoot: File =
    var dir = new File(".").getAbsoluteFile
    while dir != null && !new File(dir, "build.sbt").exists() do dir = dir.getParentFile
    dir

  private val resourceBase = "04-persistence/src/test/resources/TED_12-05-2026"

  private lazy val resourceDir = new File(projectRoot, resourceBase).getAbsolutePath

  test("non-existent directory raises an error on getAll") {
    val repos = TedXmlRepository.make[IO]("/does/not/exist/at/all")
    repos.r208.getAll.compile.drain.attempt.map { result =>
      expect(result.isLeft)
    }
  }

  test("XML file with unknown format is silently ignored by every repo") {
    IO.blocking {
      val tmp = JFiles.createTempDirectory("ted-test-unknown").toFile
      val xml = new File(tmp, "unknown.xml")
      JFiles.writeString(xml.toPath, "<Foo><Bar/></Foo>")
      tmp.getAbsolutePath
    }.flatMap { tmpDir =>
      val repos = TedXmlRepository.make[IO](tmpDir)
      for
        r208Count   <- repos.r208.getAll.compile.count
        r209Count   <- repos.r209.getAll.compile.count
        eformsCount <- repos.eforms.getAll.compile.count
      yield
        expect(r208Count   == 0) and
        expect(r209Count   == 0) and
        expect(eformsCount == 0)
    }
  }

  test("TedXmlRepository detects and routes all three formats") {
    val repos = TedXmlRepository.make[IO](resourceDir)
    for
      r208Count   <- repos.r208.getAll.compile.count
      r209Count   <- repos.r209.getAll.compile.count
      eformsCount <- repos.eforms.getAll.compile.count
      _           <- IO.println(s"\n══ TedXmlRepository  |  r208=$r208Count  r209=$r209Count  eforms=$eformsCount ══")
    yield
      expect(r208Count > 0) and
      expect(r209Count > 0) and
      expect(eformsCount > 0)
  }

  test("toCSV emits one shared header then rows from all formats") {
    val repos = TedXmlRepository.make[IO](resourceDir)
    val svc   = TedNoticeService.make[IO](repos.r208, repos.r209, repos.eforms)
    svc.toCSV.compile.toList.flatMap { lines =>
      val header      = lines.headOption.getOrElse("")
      val dataRows    = lines.tail
      val headerCount = lines.count(_ == header)
      IO.println(s"\n══ TedNoticeService.toCSV  |  ${dataRows.size} data rows  |  ${header.split(",").length} columns ══")
        .as(
          expect(lines.size > 1)                       and
          expect(header.startsWith("schema_version"))  and
          expect(header.split(",").length == 34)       and
          expect(headerCount == 1)                     and
          expect(dataRows.forall(_.nonEmpty))
        )
    }
  }

  test("generate tedNotice.csv from all formats") {
    val repos = TedXmlRepository.make[IO](resourceDir)
    val svc   = TedNoticeService.make[IO](repos.r208, repos.r209, repos.eforms)
    svc.toCSV.compile.toList.flatMap { csvLines =>
      val outFile = new File(projectRoot, "tedNotice.csv")
      for
        _ <- IO.blocking(JFiles.writeString(outFile.toPath, csvLines.mkString("\n") + "\n"))
        _ <- IO.println(s"\n══ CSV written → ${outFile.getAbsolutePath}")
        _ <- IO.println(s"   ${csvLines.size - 1} data rows  |  ${csvLines.head.split(",").length} columns\n")
      yield expect(csvLines.size > 1)
    }
  }
