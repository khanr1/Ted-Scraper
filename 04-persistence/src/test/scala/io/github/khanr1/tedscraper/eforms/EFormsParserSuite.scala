package io.github.khanr1.tedscraper.eforms

import cats.effect.IO
import cats.syntax.all.*
import fs2.Stream
import weaver.*

import java.io.File
import java.nio.file.Files as JFiles

import io.github.khanr1.tedscraper.services.eforms.EFormsNoticeService
import io.github.khanr1.tedscraper.repositories.eforms.EFormsNoticeRepository

object EFormsParserSuite extends SimpleIOSuite:

  // ── Project root discovery ────────────────────────────────────────────────────

  private lazy val projectRoot: File =
    var dir = new File(".").getAbsoluteFile
    while dir != null && !new File(dir, "build.sbt").exists() do dir = dir.getParentFile
    dir

  private val sdkVersions = List(
    "eforms-sdk-1.7",
    "eforms-sdk-1.8",
    "eforms-sdk-1.9",
    "eforms-sdk-1.10",
    "eforms-sdk-1.11",
    "eforms-sdk-1.12",
    "eforms-sdk-1.13"
  )

  private val resourceBase = "04-persistence/src/test/resources/TED_12-05-2026"

  private def xmlFilesIn(dir: File): List[File] =
    Option(dir.listFiles).toList.flatten
      .filter(_.getName.endsWith(".xml"))
      .sortBy(_.getName)

  private lazy val allEFormsFiles: List[File] =
    sdkVersions
      .map(v => new File(projectRoot, s"$resourceBase/$v"))
      .flatMap(xmlFilesIn)

  // ── Tests ─────────────────────────────────────────────────────────────────────

  test("all eforms XML files parse without error") {
    IO.blocking(allEFormsFiles.map(f => f -> parser.EFormsXmlParser.parse(f))).flatMap { results =>
      val errors  = results.collect { case (f, Left(e))  => f.getName -> e.message }
      val notices = results.collect { case (_, Right(n)) => n }

      for
        _ <- IO.println(s"\n══ eForms Parser  |  ${notices.size} ok  /  ${results.size} total ══")
        _ <- results.traverse_ {
               case (f, Right(n)) =>
                 val subtype = n.noticeSubtype.value
                 val nid     = n.data.metadata.noticeId.value
                 IO.println(s"  OK   ${f.getName.padTo(22, ' ')}  subtype=$subtype  noticeId=$nid")
               case (f, Left(e))  =>
                 IO.println(s"  FAIL ${f.getName.padTo(22, ' ')}  ${e.message}")
             }
      yield
        val baseExpect = expect(notices.nonEmpty)
        errors.foldLeft(baseExpect) { case (acc, (name, msg)) =>
          acc and failure(s"$name: $msg")
        }
    }
  }

  test("can-standard notices are wrapped as CanNotice") {
    IO.blocking {
      allEFormsFiles
        .map(f => parser.EFormsXmlParser.parse(f))
        .collect { case Right(n) if n.noticeSubtype.value == "can-standard" => n }
    }.map { canNotices =>
      canNotices.foldLeft(expect(true)) { (acc, n) =>
        acc and expect(n.isInstanceOf[CanNotice])
      }
    }
  }

  test("veat notices are wrapped as CanNotice") {
    IO.blocking {
      allEFormsFiles
        .map(f => parser.EFormsXmlParser.parse(f))
        .collect { case Right(n) if n.noticeSubtype.value == "veat" => n }
    }.map { veatNotices =>
      if veatNotices.isEmpty then expect(true)
      else
        veatNotices.foldLeft(expect(true)) { (acc, n) =>
          acc and expect(n.isInstanceOf[CanNotice])
        }
    }
  }

  test("noticeId is non-empty for every parsed notice") {
    IO.blocking(allEFormsFiles.flatMap(f => parser.EFormsXmlParser.parse(f).toOption)).map {
      notices =>
        notices.foldLeft(expect(notices.nonEmpty)) { (acc, n) =>
          acc and expect(n.data.metadata.noticeId.value.nonEmpty)
        }
    }
  }

  test("CAN notices with lot results have non-empty organization list") {
    IO.blocking(allEFormsFiles.flatMap(f => parser.EFormsXmlParser.parse(f).toOption)).map {
      notices =>
        val cans = notices.collect { case n: CanNotice => n }
        if cans.isEmpty then expect(true)
        else
          cans.foldLeft(expect(true)) { (acc, can) =>
            acc and expect(can.data.organizations.nonEmpty)
          }
    }
  }

  test("generate eformNotice.csv") {
    IO.blocking(allEFormsFiles.flatMap(f => parser.EFormsXmlParser.parse(f).toOption))
      .flatMap { notices =>
        val stubRepo = new EFormsNoticeRepository[IO]:
          def getAll: Stream[IO, NoticeForm] = Stream.empty

        val svc = EFormsNoticeService.make[IO](stubRepo)

        svc
          .toCSV(Stream.emits[IO, NoticeForm](notices))
          .compile
          .toList
          .flatMap { csvLines =>
            val outFile = new File(projectRoot, "eformNotice.csv")
            val content = csvLines.mkString("\n") + "\n"
            for
              _ <- IO.blocking(JFiles.writeString(outFile.toPath, content))
              _ <- IO.println(s"\n══ CSV written → ${outFile.getAbsolutePath}")
              _ <- IO.println(s"   ${notices.size} notices  |  ${csvLines.size - 1} data rows  |  ${csvLines.head.split(",").length} columns\n")
            yield expect(csvLines.size > 1)
          }
      }
  }
