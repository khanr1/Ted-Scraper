package io.github.khanr1.tedscraper
package r209

import parser.*

import cats.effect.Async
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}

object XMLRepository:

  def make[F[_]: Async: Files](
      path: String,
      parallelism: Int = Runtime.getRuntime.availableProcessors * 2
  ): repositories.r209.NoticeRepository[F] =
    new repositories.r209.NoticeRepository[F]:

      private val dir: Path = Path(path)

      private def attemptParse(p: Path): F[Option[Notice]] =
        Async[F]
          .blocking(XmlParser.parse(p.toNioPath.toFile))
          .flatMap {
            case Right(notice) => notice.some.pure[F]
            case Left(ParseError.XmlError(file, cause)) =>
              Async[F]
                .delay(System.err.println(s"[WARN] XML error in '$file': ${cause.getMessage}"))
                .as(none[Notice])
            case Left(err) =>
              Async[F]
                .delay(System.err.println(s"[WARN] ${err.message}"))
                .as(none[Notice])
          }

      def getAll: Stream[F, Notice] =
        Stream
          .eval(Files[F].exists(dir))
          .flatMap {
            case false =>
              Stream.raiseError[F](new IllegalArgumentException(s"Directory not found: $path"))
            case true =>
              Files[F]
                .list(dir)
                .filter(_.extName.equalsIgnoreCase(".xml"))
                .parEvalMapUnordered(parallelism)(attemptParse)
                .unNone
          }
