package io.github.khanr1.tedscraper

import cats.effect.Async
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}

import scala.util.{Try, Using}
import java.io.FileInputStream

object TedXmlRepository:

  enum TedFormat:
    case R208, R209, EForms

  case class TedRepos[F[_]](
    r208:   repositories.r208.NoticeRepository[F],
    r209:   repositories.r209.NoticeRepository[F],
    eforms: repositories.eforms.EFormsNoticeRepository[F]
  )

  private val eformsRoots =
    Set("ContractAwardNotice", "ContractNotice", "PriorInformationNotice")

  // Peek at the first 2 KB to identify format without full XML parse.
  // TED_EXPORT + VERSION attr → r209; TED_EXPORT without → r208; UBL root → eforms.
  private def detectFormat(path: Path): Option[TedFormat] =
    Try {
      val sample = Using(new FileInputStream(path.toNioPath.toFile)) { is =>
        new String(is.readNBytes(2048), "UTF-8")
      }.getOrElse("")
      if sample.contains("<TED_EXPORT") then
        if sample.contains("VERSION=\"") then Some(TedFormat.R209) else Some(TedFormat.R208)
      else if eformsRoots.exists(r => sample.contains(s"<$r ") || sample.contains(s"<$r>")) then
        Some(TedFormat.EForms)
      else None
    }.toOption.flatten

  /** Scan `path` recursively for XML files of all three TED formats and
    * expose them as three independent typed repositories.
    *
    * Format detection reads only the first 2 KB of each file (fast); full
    * parsing happens on demand in each repository's `getAll` stream.
    */
  def make[F[_]: Async: Files](
    path:        String,
    parallelism: Int = Runtime.getRuntime.availableProcessors * 2
  ): TedRepos[F] =
    val dir = Path(path)

    def allXmlPaths: Stream[F, Path] =
      Stream.eval(Files[F].exists(dir)).flatMap {
        case false => Stream.raiseError[F](new IllegalArgumentException(s"Directory not found: $path"))
        case true  => Files[F].walk(dir).filter(_.extName.equalsIgnoreCase(".xml"))
      }

    def forFormat(fmt: TedFormat): Stream[F, Path] =
      allXmlPaths.evalFilter(p => Async[F].blocking(detectFormat(p).contains(fmt)))

    TedRepos(
      r208 = new repositories.r208.NoticeRepository[F]:
        def getAll: Stream[F, r208.Notice] =
          forFormat(TedFormat.R208)
            .parEvalMapUnordered(parallelism) { p =>
              Async[F].blocking(r208.parser.XmlParser.parse(p.toNioPath.toFile))
                .flatMap {
                  case Right(n)  => n.some.pure[F]
                  case Left(err) =>
                    Async[F].delay(System.err.println(s"[WARN] r208 ${err.message}"))
                      .as(none[r208.Notice])
                }
            }
            .unNone,

      r209 = new repositories.r209.NoticeRepository[F]:
        def getAll: Stream[F, r209.Notice] =
          forFormat(TedFormat.R209)
            .parEvalMapUnordered(parallelism) { p =>
              Async[F].blocking(r209.parser.XmlParser.parse(p.toNioPath.toFile))
                .flatMap {
                  case Right(n)  => n.some.pure[F]
                  case Left(err) =>
                    Async[F].delay(System.err.println(s"[WARN] r209 ${err.message}"))
                      .as(none[r209.Notice])
                }
            }
            .unNone,

      eforms = new repositories.eforms.EFormsNoticeRepository[F]:
        def getAll: Stream[F, eforms.NoticeForm] =
          forFormat(TedFormat.EForms)
            .parEvalMapUnordered(parallelism) { p =>
              Async[F].blocking(eforms.parser.EFormsXmlParser.parse(p.toNioPath.toFile))
                .flatMap {
                  case Right(n)  => n.some.pure[F]
                  case Left(err) =>
                    Async[F].delay(System.err.println(s"[WARN] eforms ${err.message}"))
                      .as(none[eforms.NoticeForm])
                }
            }
            .unNone
    )
