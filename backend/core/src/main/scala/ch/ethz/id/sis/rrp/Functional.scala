package ch.ethz.id.sis.rrp

import scala.util.Failure
import scala.util.Success
import scala.util.Try

type Kleisli[-I, +F[+_], +A] = I => F[A]

trait Functor[F[_]]:
  def map[A, B](fa: F[A])(f: A => B): F[B]

trait Applicative[F[_]] extends Functor[F]:
  def unit[A](a: => A): F[A]
  def map2[A, B, C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C]
  override def map[A, B](fa: F[A])(f: A => B): F[B] = map2(fa, fa)((a, _) => f(a))

trait Monad[F[_]] extends Applicative[F]:
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
  override def map[A, B](fa: F[A])(f: A => B): F[B] = flatMap(fa)(a => unit(f(a)))
  override def map2[A, B, C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C] =
    flatMap(fa)(a => map(fb)(b => f(a, b)))

trait Traversable[F[_]] extends Functor[F]:
  def traverse[G[_]: Applicative, A, B](fa: F[A])(f: A => G[B]): G[F[B]] =
    sequence(map(fa)(f))
  def sequence[G[_]: Applicative, A](fga: F[G[A]]): G[F[A]]
//  traverse(fga)(ga => ga)

val setMonad: Monad[Set] = new Monad[Set]:
  override def unit[A](a: => A)                                  = Set(a)
  override def flatMap[A, B](as: Set[A])(f: A => Set[B]): Set[B] = as flatMap f

val setTraversable: Traversable[Set] = new Traversable[Set]:
  override def map[A, B](as: Set[A])(f: A => B): Set[B] = as map f
  override def sequence[G[_], A](fga: Set[G[A]])(using app: Applicative[G]): G[Set[A]] =
    fga.foldLeft(app.unit(Set[A]()))((acc, ga) => app.map2(acc, ga)(_ + _))

type AbstractOperation[-Env, +A] = Kleisli[Env, [X] =>> Try[Either[Error, X]], A]
type Operation[A]                = AbstractOperation[ExecutionEnvironment, A]

extension [Env, A](op: AbstractOperation[Env, A]) def withFilter(p: A => Boolean): AbstractOperation[Env, A] = op

def operationMonad[Env] = new Monad[[X] =>> AbstractOperation[Env, X]]:
  def unit[A](a: => A): AbstractOperation[Env, A] = e => Try { Right(a) }
  def flatMap[A, B](op: AbstractOperation[Env, A])(f: A => AbstractOperation[Env, B]): AbstractOperation[Env, B] =
    e =>
      Try {
        op(e) match {
          case Success(Right(a)) => f(a)(e)
          case Success(Left(e))  => Success(Left(e))
          case Failure(err)      => Failure(err)
        }
      }.flatten

given Monad[Operation] = operationMonad[ExecutionEnvironment]

extension [X[_], F[_], A](a: X[A])(using mon: Monad[F], ev: X[A] => F[A])
  def flatMap[B](f: A => F[B]): F[B] = mon.flatMap(ev(a))(f)
  def map[B](f: A => B): F[B]        = mon.map(ev(a))(f)

extension [F[_], G[A], A, App <: Applicative[G]](fga: F[G[A]])(using trav: Traversable[F], app: App)
  def sequence = trav.sequence(fga)

given eitherConversion[A]: Conversion[Either[Error, A], Operation[A]] = either => env => Success(either)

trait Monoid[A]:
  val zero: A
  def op(x: A, y: A): A

extension [A](x: A)(using m: Monoid[A]) def |+|(y: A): A = m.op(x, y)

given [A]: Monoid[Set[A]] =
  new Monoid[Set[A]]:
    override val zero                             = Set()
    override def op(x: Set[A], y: Set[A]): Set[A] = x ++ y

given [K, V](using m: Monoid[V]): Monoid[Map[K, V]] = new Monoid[Map[K, V]]:
  val zero = Map()
  def op(a: Map[K, V], b: Map[K, V]): Map[K, V] =
    b.foldLeft(a) { case (acc, (k, v)) =>
      acc.get(k) match {
        case None           => acc + (k -> v)
        case Some(existing) => acc + (k -> (existing |+| v))
      }
    }

extension [A](op: Operation[A])
  def failWith[B](handler: Operation[B]): Operation[A] =
    e =>
      op(e) match {
        case res @ Success(Right(ok))   => res
        case Success(res @ Left(fails)) => handler(e).map(_ => res)
        case Failure(exception)         => Failure(exception)
      }
