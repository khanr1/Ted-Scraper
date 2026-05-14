package io.github.khanr1.tedscraper
package r208

import parser.*

import cats.effect.Async
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}

object XMLRepository:

  /** Build a NoticeRepository[F] backed by FS2 streaming file IO.
    *
    * Constraints: Async[F] — needed to shift blocking SAX parsing to the
    * blocking pool. Files[F] — FS2 typeclass for file system operations.
    * Provided automatically for cats-effect IO; for any other F derive with
    * `given Files[F] = Files.forAsync`.
    *
    * @param path
    *   Directory containing TED XML files.
    * @param parallelism
    *   Max concurrent parse jobs on the blocking pool. Defaults to 2 ×
    *   available processors.
    */
  def make[F[_]: Async: Files](
      path: String,
      parallelism: Int = Runtime.getRuntime.availableProcessors * 2
  ): repositories.r208.NoticeRepository[F] =
    new repositories.r208.NoticeRepository[F]:

      private val dir: Path = Path(path)

      // ── Single-file parsing ───────────────────────────────────────────────

      /** Parse one file on the blocking pool. Returns None on failure after
        * logging — the stream keeps running.
        */
      private def attemptParse(p: Path): F[Option[Notice]] =
        Async[F]
          .blocking(XmlParser.parse(p.toNioPath.toFile))
          .flatMap {
            case Right(notice) =>
              notice.some.pure[F]

            case Left(ParseError.XmlError(file, cause)) =>
              Async[F]
                .delay(
                  System.err.println(
                    s"[WARN] XML error in '$file': ${cause.getMessage}"
                  )
                )
                .as(none[Notice])

            case Left(err) =>
              Async[F]
                .delay(System.err.println(s"[WARN] ${err.message}"))
                .as(none[Notice])
          }

      // ── Repository ────────────────────────────────────────────────────────

      /** Lazily stream every successfully parsed notice in the directory.
        *
        * The directory is checked for existence before listing begins; a
        * missing path raises an error in the stream effect so callers can
        * handle it with .handleErrorWith or .attempt.
        *
        * Files are parsed concurrently up to `parallelism`; results are emitted
        * as each parse completes (unordered). Swap parEvalMapUnordered for
        * parEvalMap if you need stable emission order.
        */
      def getAll: Stream[F, Notice] =
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
