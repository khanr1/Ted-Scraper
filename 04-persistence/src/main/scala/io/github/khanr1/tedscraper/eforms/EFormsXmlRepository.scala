package io.github.khanr1.tedscraper
package eforms

import parser.{EFormsXmlParser, ParseError}

import cats.effect.Async
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}

import io.github.khanr1.tedscraper.repositories.eforms.EFormsNoticeRepository

object EFormsXmlRepository:

  def make[F[_]: Async: Files](
      path: String,
      parallelism: Int = Runtime.getRuntime.availableProcessors * 2
  ): EFormsNoticeRepository[F] =
    new EFormsNoticeRepository[F]:

      private val dir: Path = Path(path)

      private def attemptParse(p: Path): F[Option[NoticeForm]] =
        Async[F]
          .blocking(EFormsXmlParser.parse(p.toNioPath.toFile))
          .flatMap {
            case Right(notice) =>
              notice.some.pure[F]

            case Left(ParseError.XmlLoadError(file, cause)) =>
              Async[F]
                .delay(System.err.println(s"[WARN] XML error in '$file': ${cause.getMessage}"))
                .as(none[NoticeForm])

            case Left(err) =>
              Async[F]
                .delay(System.err.println(s"[WARN] ${err.message}"))
                .as(none[NoticeForm])
          }

      def getAll: Stream[F, NoticeForm] =
        Stream
          .eval(Files[F].exists(dir))
          .flatMap {
            case false =>
              Stream.raiseError[F](
                new IllegalArgumentException(s"Directory not found: $path")
              )
            case true =>
              Files[F]
                .list(dir)
                .filter(_.extName.equalsIgnoreCase(".xml"))
                .parEvalMapUnordered(parallelism)(attemptParse)
                .unNone
          }
