package io.github.khanr1.tedscraper

import cats.effect.IO
import fs2.Stream
import weaver.*

import java.io.File

import io.github.khanr1.tedscraper.services.TedNoticeService
import io.github.khanr1.tedscraper.services.eforms.EFormsNoticeService
import io.github.khanr1.tedscraper.services.r208.r208Services
import io.github.khanr1.tedscraper.services.r209.r209Services
import io.github.khanr1.tedscraper.repositories.eforms.EFormsNoticeRepository
import io.github.khanr1.tedscraper.repositories.r208.{NoticeRepository as R208Repo}
import io.github.khanr1.tedscraper.repositories.r209.{NoticeRepository as R209Repo}
import io.github.khanr1.tedscraper.eforms.NoticeForm

object TedNoticeServiceSuite extends SimpleIOSuite:

  // ── Stub repositories (all empty) ─────────────────────────────────────────

  private val emptyR208: R208Repo[IO] = new R208Repo[IO]:
    def getAll: Stream[IO, r208.Notice] = Stream.empty

  private val emptyR209: R209Repo[IO] = new R209Repo[IO]:
    def getAll: Stream[IO, r209.Notice] = Stream.empty

  private val emptyEForms: EFormsNoticeRepository[IO] = new EFormsNoticeRepository[IO]:
    def getAll: Stream[IO, NoticeForm] = Stream.empty

  // ── Test-resource path ─────────────────────────────────────────────────────

  private lazy val projectRoot: File =
    var dir = new File(".").getAbsoluteFile
    while dir != null && !new File(dir, "build.sbt").exists() do dir = dir.getParentFile
    dir

  private lazy val resourceDir: String =
    new File(projectRoot, "04-persistence/src/test/resources/TED_12-05-2026").getAbsolutePath

  // ── Tests with all-empty stubs ─────────────────────────────────────────────

  test("all-empty repos produce exactly one line (the header)") {
    val svc = TedNoticeService.make[IO](emptyR208, emptyR209, emptyEForms)
    svc.toCSV.compile.toList.map { lines =>
      expect(lines.size == 1)
    }
  }

  test("header starts with schema_version") {
    val svc = TedNoticeService.make[IO](emptyR208, emptyR209, emptyEForms)
    svc.toCSV.compile.toList.map { lines =>
      expect(lines.head.startsWith("schema_version"))
    }
  }

  test("header has exactly 34 columns") {
    val svc = TedNoticeService.make[IO](emptyR208, emptyR209, emptyEForms)
    svc.toCSV.compile.toList.map { lines =>
      expect(lines.head.split(",", -1).length == 34)
    }
  }

  test("empty r209 and eforms repos produce only r208 header + r208 data rows") {

    val repos = TedXmlRepository.make[IO](resourceDir)
    val svc   = TedNoticeService.make[IO](repos.r208, emptyR209, emptyEForms)
    val r208Svc = r208Services.make[IO](repos.r208)
    for
      tedLines  <- svc.toCSV.compile.count
      r208Lines <- r208Svc.toCSV(repos.r208.getAll).compile.count
    yield expect(tedLines == r208Lines)
  }

  // ── Integration tests against real XML test resources ─────────────────────

  test("header appears exactly once when all three formats emit data") {

    val repos = TedXmlRepository.make[IO](resourceDir)
    val svc   = TedNoticeService.make[IO](repos.r208, repos.r209, repos.eforms)
    svc.toCSV.compile.toList.map { lines =>
      val header = lines.head
      expect(lines.size > 1) and
      expect(lines.count(_ == header) == 1)
    }
  }

  test("first line is the CSV header") {

    val repos = TedXmlRepository.make[IO](resourceDir)
    val svc   = TedNoticeService.make[IO](repos.r208, repos.r209, repos.eforms)
    svc.toCSV.compile.toList.map { lines =>
      expect(lines.head.startsWith("schema_version")) and
      expect(lines.head.split(",", -1).length == 34)
    }
  }

  test("all data rows are non-empty strings") {

    val repos = TedXmlRepository.make[IO](resourceDir)
    val svc   = TedNoticeService.make[IO](repos.r208, repos.r209, repos.eforms)
    svc.toCSV.compile.toList.map { lines =>
      val dataRows = lines.tail
      expect(dataRows.nonEmpty) and
      expect(dataRows.forall(_.nonEmpty))
    }
  }

  // Verifies the aggregation formula:
  // tedCount = r208Count + (r209Count - 1) + (eformsCount - 1)
  // because r209 and eforms each have their header dropped by .drop(1)
  test("total CSV lines = r208 + r209 + eforms output lines minus 2 dropped headers") {

    val repos     = TedXmlRepository.make[IO](resourceDir)
    val r208Svc   = r208Services.make[IO](repos.r208)
    val r209Svc   = r209Services.make[IO](repos.r209)
    val eformsSvc = EFormsNoticeService.make[IO](repos.eforms)
    val tedSvc    = TedNoticeService.make[IO](repos.r208, repos.r209, repos.eforms)
    for
      r208Count   <- r208Svc.toCSV(repos.r208.getAll).compile.count
      r209Count   <- r209Svc.toCSV(repos.r209.getAll).compile.count
      eformsCount <- eformsSvc.toCSV(repos.eforms.getAll).compile.count
      tedCount    <- tedSvc.toCSV.compile.count
    yield expect(tedCount == r208Count + r209Count + eformsCount - 2)
  }
