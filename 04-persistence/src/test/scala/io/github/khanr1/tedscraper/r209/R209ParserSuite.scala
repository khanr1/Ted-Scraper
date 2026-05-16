package io.github.khanr1.tedscraper.r209

import cats.effect.IO
import cats.syntax.all.*
import fs2.Stream
import weaver.*

import java.io.File
import java.nio.file.Files as JFiles

import io.github.khanr1.tedscraper.repositories.r209.NoticeRepository
import io.github.khanr1.tedscraper.services.r209.r209Services

/** Runs XmlParser over every R2.0.9 XML file in the five test-resource
 *  directories, emits per-file results to stdout, and writes r209Notice.csv
 *  to the project root.
 */
object R209ParserSuite extends SimpleIOSuite:

  // ── Path helpers ──────────────────────────────────────────────────────────

  private lazy val projectRoot: File =
    var dir = new File(".").getAbsoluteFile
    while dir != null && !new File(dir, "build.sbt").exists() do dir = dir.getParentFile
    dir

  private val resourceDirs = List(
    "04-persistence/src/test/resources/TED_12-05-2026/R2.0.9.S01.E01",
    "04-persistence/src/test/resources/TED_12-05-2026/R2.0.9.S02.E01",
    "04-persistence/src/test/resources/TED_12-05-2026/R2.0.9.S03.E01",
    "04-persistence/src/test/resources/TED_12-05-2026/R2.0.9.S04.E01",
    "04-persistence/src/test/resources/TED_12-05-2026/R2.0.9.S05.E01"
  ).map(new File(projectRoot, _))

  private def xmlFilesIn(dir: File): List[File] =
    Option(dir.listFiles).toList.flatten
      .filter(_.getName.endsWith(".xml"))
      .sortBy(_.getName)

  private lazy val allXmlFiles: List[File] =
    resourceDirs.flatMap(xmlFilesIn)

  // ── Stub repository ───────────────────────────────────────────────────────

  private val stubRepo = new NoticeRepository[IO]:
    def getAll: Stream[IO, Notice] = Stream.empty

  private val svc = r209Services.make[IO](stubRepo)

  // ── Tests ─────────────────────────────────────────────────────────────────

  test("parse all R2.0.9 XML files") {
    IO.blocking(allXmlFiles.map(f => f -> parser.XmlParser.parse(f))).flatMap { results =>
      val errors  = results.collect { case (f, Left(e))  => f.getName -> e.message }
      val notices = results.collect { case (_, Right(n)) => n }

      for
        _ <- IO.println(s"\n══ R2.0.9 Parser  |  ${notices.size} ok  /  ${results.size} total ══")
        _ <- results.traverse_ {
               case (f, Right(n)) =>
                 val forms = n.formSection.forms
                   .map(fb => fb.getClass.getSimpleName.stripSuffix("Form"))
                   .mkString(", ")
                 IO.println(s"  OK   ${f.getName.padTo(20, ' ')}  docId=${n.docId.value}  forms=[$forms]")
               case (f, Left(e))  =>
                 IO.println(s"  FAIL ${f.getName.padTo(20, ' ')}  ${e.message}")
             }
      yield
        errors.foldLeft(expect(notices.nonEmpty)) { case (acc, (name, msg)) =>
          acc and failure(s"$name: $msg")
        }
    }
  }

  test("generate r209Notice.csv") {
    IO.blocking(allXmlFiles.flatMap(f => parser.XmlParser.parse(f).toOption)).flatMap { notices =>
      for
        csvLines <- svc
                      .toCSV(Stream.emits[IO, Notice](notices))
                      .compile
                      .toList
        outFile   = new File(projectRoot, "r209Notice.csv")
        _        <- IO.blocking(JFiles.writeString(outFile.toPath, csvLines.mkString("\n") + "\n"))
        _        <- IO.println(s"\n══ CSV written → ${outFile.getAbsolutePath}")
        _        <- IO.println(s"   ${notices.size} notices  |  ${csvLines.size - 1} data rows  |  ${csvLines.head.split(",").length} columns\n")
      yield expect(csvLines.size > 1)
    }
  }
