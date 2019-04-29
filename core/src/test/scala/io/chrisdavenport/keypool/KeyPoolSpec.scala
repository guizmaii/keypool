package io.chrisdavenport.keypool

import org.specs2._
import cats.implicits._
import cats.effect._
import cats.effect.concurrent._
import scala.concurrent.ExecutionContext.global
class KeypoolSpec extends mutable.Specification with ScalaCheck {

  import _root_.io.chrisdavenport.keypool.KeyPool
  "Keypool" should {
    "Keep Resources marked to be kept" in {
      def nothing(i: Int, ref: Ref[IO, Int]): IO[Unit] = {
        val _ = i
        ref.get.void
      }
      implicit val CS = IO.contextShift(global)
      implicit val T = IO.timer(global)
      KeyPool.create(
        {i: Int => Ref.of[IO, Int](i)},
        nothing,
        Reuse,
        Long.MaxValue,
        10,
        10,
        {_: Throwable => IO.unit}
      ).use( k => 

        k.take(1)
          .use(_ => IO.unit) >>
        k.state.map(_._1)
      ).unsafeRunSync() must_=== (1)
    }

    "Delete Resources marked to be deleted" in {
      def nothing(i: Int, ref: Ref[IO, Int]): IO[Unit] = {
        val _ = i
        ref.get.void
      }
      implicit val CS = IO.contextShift(global)
      implicit val T = IO.timer(global)
      KeyPool.create(
        {i: Int => Ref.of[IO, Int](i)},
        nothing,
        DontReuse,
        Long.MaxValue,
        10,
        10,
        {_: Throwable => IO.unit}
      ).use( k =>

        k.take(1)
          .use(_ => IO.unit) >>
        k.state.map(_._1)
      ).unsafeRunSync() must_=== (0)
    }

    "Delete Resource when pool is full" in {
      def nothing(i: Int, ref: Ref[IO, Int]): IO[Unit] = {
        val _ = i
        ref.get.void
      }
      implicit val CS = IO.contextShift(global)
      implicit val T = IO.timer(global)
      KeyPool.create(
        {i: Int => Ref.of[IO, Int](i)},
        nothing,
        Reuse,
        Long.MaxValue,
        1,
        1,
        {_: Throwable => IO.unit}
      ).use{ k =>

        val action = k.take(1)
        .use(_ => IO.unit)

        (action, action).parMapN{case (_, _) => IO.unit} >>
        k.state.map(_._1)
      }.unsafeRunSync() must_=== (1)
    }
  }
}