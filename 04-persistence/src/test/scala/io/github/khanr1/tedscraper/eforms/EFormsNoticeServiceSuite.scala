package io.github.khanr1.tedscraper.eforms

import cats.effect.IO
import fs2.Stream
import weaver.*

import io.github.khanr1.tedscraper.repositories.eforms.EFormsNoticeRepository
import io.github.khanr1.tedscraper.services.eforms.EFormsNoticeService

object EFormsNoticeServiceSuite extends SimpleIOSuite:

  // ── Minimal test-fixture builders ──────────────────────────────────────────

  private def meta(id: String, subtype: String): NoticeMetadata =
    NoticeMetadata(
      noticeId            = NoticeId.unsafe(id),
      sdkVersion          = SdkVersion.unsafe("eforms-sdk-1.10"),
      contractFolderId    = None,
      issueDate           = None,
      issueTime           = None,
      noticeTypeCode      = NoticeSubtypeCode.unsafe(subtype),
      language            = None,
      regulatoryDomain    = None,
      versionId           = None,
      noticePublicationId = None,
      gazetteId           = None
    )

  private def notice(
    id:      String,
    subtype: String,
    buyers:  List[BuyerRef]     = Nil,
    orgs:    List[Organization] = Nil,
    lots:    List[Lot]          = Nil,
    result:  Option[NoticeResult] = None
  ): Notice =
    Notice(
      metadata              = meta(id, subtype),
      buyers                = buyers,
      organizations         = orgs,
      lots                  = lots,
      lotsGroups            = Nil,
      parts                 = Nil,
      noticeResult          = result,
      procedureScope        = None,
      procedureCode         = None,
      procedureJustification= None
    )

  private def repo(notices: NoticeForm*): EFormsNoticeRepository[IO] =
    new EFormsNoticeRepository[IO]:
      def getAll: Stream[IO, NoticeForm] = Stream.emits(notices.toList)

  private val pinSubtype = NoticeSubtypeCode.unsafe("pin-buyer")
  private val cnSubtype  = NoticeSubtypeCode.unsafe("cn-standard")
  private val canSubtype = NoticeSubtypeCode.unsafe("can-standard")

  private val pin = PinNotice(pinSubtype, notice("n1", "pin-buyer"))
  private val cn  = CnNotice(cnSubtype,   notice("n2", "cn-standard"))
  private val can = CanNotice(canSubtype,  notice("n3", "can-standard"))

  // ── toCSV: header ──────────────────────────────────────────────────────────

  test("toCSV on empty stream yields only the header row") {
    val svc = EFormsNoticeService.make[IO](repo())
    svc.toCSV(Stream.empty).compile.toList.map { lines =>
      expect(lines.size == 1) and
      expect(lines.head.startsWith("schema_version"))
    }
  }

  test("CSV header has exactly 34 columns") {
    val svc = EFormsNoticeService.make[IO](repo())
    svc.toCSV(Stream.empty).compile.toList.map { lines =>
      expect(lines.head.split(",", -1).length == 34)
    }
  }

  // ── toCSV: data row counts ─────────────────────────────────────────────────

  test("PIN notice produces exactly one data row") {
    val svc = EFormsNoticeService.make[IO](repo())
    svc.toCSV(Stream.emit(pin)).compile.toList.map { lines =>
      expect(lines.size == 2)
    }
  }

  test("CN notice produces exactly one data row") {
    val svc = EFormsNoticeService.make[IO](repo())
    svc.toCSV(Stream.emit(cn)).compile.toList.map { lines =>
      expect(lines.size == 2)
    }
  }

  test("CAN notice with no lot results produces exactly one data row") {
    val svc = EFormsNoticeService.make[IO](repo())
    svc.toCSV(Stream.emit(can)).compile.toList.map { lines =>
      expect(lines.size == 2)
    }
  }

  test("CAN notice with two lot results produces two data rows") {
    val lotResult1 = LotResult(
      resultId           = ResultId.unsafe("r1"),
      tenderResultCode   = WinnerSelectionStatus.SelecW,
      lowerTenderAmount  = None,
      higherTenderAmount = None,
      tenderRef          = None,
      contractRef        = None,
      lotRef             = Some(LotId.unsafe("LOT-0001")),
      offersReceived     = Some(3)
    )
    val lotResult2 = LotResult(
      resultId           = ResultId.unsafe("r2"),
      tenderResultCode   = WinnerSelectionStatus.NoAwa,
      lowerTenderAmount  = None,
      higherTenderAmount = None,
      tenderRef          = None,
      contractRef        = None,
      lotRef             = Some(LotId.unsafe("LOT-0002")),
      offersReceived     = None
    )
    val nr  = NoticeResult(None, None, List(lotResult1, lotResult2), Nil, Nil, Nil)
    val svc = EFormsNoticeService.make[IO](repo())
    svc.toCSV(Stream.emit(CanNotice(canSubtype, notice("n-lots", "can-standard", result = Some(nr))))).compile.toList.map {
      lines => expect(lines.size == 3) // header + 2 rows
    }
  }

  test("multiple notices produce header + one data row each") {
    val svc = EFormsNoticeService.make[IO](repo())
    svc.toCSV(Stream.emits[IO, NoticeForm](List(pin, cn, can))).compile.toList.map { lines =>
      expect(lines.size == 4) // header + 3 rows
    }
  }

  // ── toCSV: column count per data row ──────────────────────────────────────

  test("every data row has 34 fields") {
    val svc     = EFormsNoticeService.make[IO](repo())
    val notices = Stream.emits[IO, NoticeForm](List(pin, cn, can))
    svc.toCSV(notices).compile.toList.map { lines =>
      val dataRows = lines.tail
      expect(dataRows.nonEmpty) and
        dataRows.foldLeft(expect(true)) { (acc, row) =>
          // split(",", -1) preserves trailing empty fields
          acc and expect(row.split(",", -1).length == 34)
        }
    }
  }

  // ── toCSV: field content ───────────────────────────────────────────────────

  test("schema_version field reflects sdkVersion from metadata") {
    val svc = EFormsNoticeService.make[IO](repo())
    svc.toCSV(Stream.emit(pin)).compile.toList.map { lines =>
      expect(lines.last.startsWith("eforms-sdk-1.10"))
    }
  }

  test("notice_type for pin-buyer is 'Prior Information Notice (Buyer Profile)'") {
    val svc = EFormsNoticeService.make[IO](repo())
    svc.toCSV(Stream.emit(pin)).compile.toList.map { lines =>
      val cols = lines.last.split(",", -1)
      expect(cols(2) == "Prior Information Notice (Buyer Profile)")
    }
  }

  test("notice_type for cn-standard is 'Contract Notice'") {
    val svc = EFormsNoticeService.make[IO](repo())
    svc.toCSV(Stream.emit(cn)).compile.toList.map { lines =>
      val cols = lines.last.split(",", -1)
      expect(cols(2) == "Contract Notice")
    }
  }

  test("notice_type for can-standard is 'Contract Award Notice'") {
    val svc = EFormsNoticeService.make[IO](repo())
    svc.toCSV(Stream.emit(can)).compile.toList.map { lines =>
      val cols = lines.last.split(",", -1)
      expect(cols(2) == "Contract Award Notice")
    }
  }

  // ── CSV escaping ───────────────────────────────────────────────────────────

  test("ca_name containing a comma is double-quoted in CSV output") {
    val orgId = OrganizationId.unsafe("org-comma")
    val co    = Company(orgId, Some(PlainText.unsafe("Acme, Inc.")), None, None, None, None, None)
    val org   = Organization(co, None, None, None, None, None)
    val n     = notice("n-comma", "pin-buyer", buyers = List(BuyerRef(orgId, None, None)), orgs = List(org))
    val svc   = EFormsNoticeService.make[IO](repo())
    svc.toCSV(Stream.emit(PinNotice(pinSubtype, n))).compile.toList.map { lines =>
      expect(lines.last.contains("\"Acme, Inc.\""))
    }
  }

  test("ca_name containing a double-quote uses doubled-quote escaping") {
    val orgId = OrganizationId.unsafe("org-quote")
    val co    = Company(orgId, Some(PlainText.unsafe("Acme \"Best\" Ltd")), None, None, None, None, None)
    val org   = Organization(co, None, None, None, None, None)
    val n     = notice("n-quote", "pin-buyer", buyers = List(BuyerRef(orgId, None, None)), orgs = List(org))
    val svc   = EFormsNoticeService.make[IO](repo())
    svc.toCSV(Stream.emit(PinNotice(pinSubtype, n))).compile.toList.map { lines =>
      expect(lines.last.contains("\"Acme \"\"Best\"\" Ltd\""))
    }
  }

  // ── getAll ─────────────────────────────────────────────────────────────────

  test("getAll returns every notice from the repository") {
    val svc = EFormsNoticeService.make[IO](repo(pin, cn, can))
    svc.getAll.compile.toList.map { result =>
      expect(result.size == 3)
    }
  }

  test("getAll preserves notice identity") {
    val svc = EFormsNoticeService.make[IO](repo(pin, cn))
    svc.getAll.compile.toList.map { result =>
      expect(result.contains(pin)) and expect(result.contains(cn))
    }
  }

  // ── getBySubtype ───────────────────────────────────────────────────────────

  test("getBySubtype keeps only notices with the matching subtype") {
    val svc = EFormsNoticeService.make[IO](repo(pin, cn, can))
    svc.getBySubtype(cnSubtype).compile.toList.map { result =>
      expect(result.size == 1) and
      expect(result.head.noticeSubtype.value == "cn-standard")
    }
  }

  test("getBySubtype returns empty stream when no notices match") {
    val svc = EFormsNoticeService.make[IO](repo(pin, cn, can))
    svc.getBySubtype(NoticeSubtypeCode.unsafe("veat")).compile.toList.map { result =>
      expect(result.isEmpty)
    }
  }

  // ── getContractAwards / getPriorInformation / getContractNotices ───────────

  test("getContractAwards returns only CanNotice instances") {
    val svc = EFormsNoticeService.make[IO](repo(pin, cn, can))
    svc.getContractAwards.compile.toList.map { result =>
      expect(result.size == 1) and
      expect(result.forall(_.isInstanceOf[CanNotice]))
    }
  }

  test("getContractAwards returns empty stream from all-PIN repo") {
    val svc = EFormsNoticeService.make[IO](repo(pin, pin))
    svc.getContractAwards.compile.toList.map { result =>
      expect(result.isEmpty)
    }
  }

  test("getPriorInformation returns only PinNotice instances") {
    val svc = EFormsNoticeService.make[IO](repo(pin, cn, can))
    svc.getPriorInformation.compile.toList.map { result =>
      expect(result.size == 1) and
      expect(result.forall(_.isInstanceOf[PinNotice]))
    }
  }

  test("getContractNotices returns only CnNotice instances") {
    val svc = EFormsNoticeService.make[IO](repo(pin, cn, can))
    svc.getContractNotices.compile.toList.map { result =>
      expect(result.size == 1) and
      expect(result.forall(_.isInstanceOf[CnNotice]))
    }
  }
