package io.github.khanr1
package tedscraper

import cats.effect.IO
import cats.syntax.all.*
import weaver.*
import fs2.Stream

import java.io.File
import java.nio.file.Files as JFiles

import r208.Notice
import r208.parser.XmlParser
import services.r208.r208Services

/** Runs XmlParser over every R2.0.8 XML file in the three test-resource
 *  directories, emits per-file results to stdout, and writes r208Notice.csv
 *  to the project root.  Each file is a Weaver pureTest so failures surface
 *  individually.
 */
object R208ParserSuite extends SimpleIOSuite:

  // ── Path helpers ──────────────────────────────────────────────────────────

  /** Walk up from the JVM working directory until we find build.sbt. */
  private lazy val projectRoot: File =
    var dir = new File(".").getAbsoluteFile
    while dir != null && !new File(dir, "build.sbt").exists() do
      dir = dir.getParentFile
    dir

  private val resourceDirs = List(
    "04-persistence/src/test/resources/TED_12-05-2026/R2.0.8.S02.E01",
    "04-persistence/src/test/resources/TED_12-05-2026/R2.0.8.S03.E01",
    "04-persistence/src/test/resources/TED_12-05-2026/R2.0.8.S04.E01"
  ).map(new File(projectRoot, _))

  private def xmlFilesIn(dir: File): List[File] =
    Option(dir.listFiles).toList.flatten
      .filter(_.getName.endsWith(".xml"))
      .sortBy(_.getName)

  private lazy val allXmlFiles: List[File] =
    resourceDirs.flatMap(xmlFilesIn)

  // ── Stub repository (toCSV does not use it) ───────────────────────────────

  private val stubRepo = new repositories.r208.NoticeRepository[IO]:
    def getAll: Stream[IO, Notice] = Stream.empty

  private val svc = r208Services.make[IO](stubRepo)

  // ── Tests ─────────────────────────────────────────────────────────────────

  test("parse all R2.0.8 XML files") {
    IO.blocking(allXmlFiles.map(f => f -> XmlParser.parse(f))).flatMap { results =>
      val errors  = results.collect { case (f, Left(e))  => f.getName -> e.message }
      val notices = results.collect { case (_, Right(n)) => n }

      for
        _ <- IO.println(s"\n══ R2.0.8 Parser  |  ${notices.size} ok  /  ${results.size} total ══")
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

  test("generate r208Notice.csv") {
    IO.blocking(allXmlFiles.flatMap(f => XmlParser.parse(f).toOption)).flatMap { notices =>
      for
        csvLines <- svc
                      .toCSV(Stream.emits[IO, Notice](notices))
                      .compile
                      .toList
        outFile   = new File(projectRoot, "r208Notice.csv")
        _        <- IO.blocking(JFiles.writeString(outFile.toPath, csvLines.mkString("\n") + "\n"))
        _        <- IO.println(s"\n══ CSV written → ${outFile.getAbsolutePath}")
        _        <- IO.println(s"   ${notices.size} notices  |  ${csvLines.size - 1} data rows  |  ${csvLines.head.split(",").length} columns\n")
      yield expect(csvLines.size > 1)
    }
  }
