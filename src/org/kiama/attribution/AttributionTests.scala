/*
 * This file is part of Kiama.
 *
 * Copyright (C) 2008-2011 Anthony M Sloane, Macquarie University.
 *
 * Kiama is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Kiama is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Kiama.  (See files COPYING and COPYING.LESSER.)  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package org.kiama
package attribution

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
 * Tests of basic attribution.
 */
@RunWith(classOf[JUnitRunner])
class AttributionTests extends FunSuite {

    abstract class Tree extends Attributable
    case class Pair (left : Tree, right : Tree) extends Tree
    case class Leaf (value : Int) extends Tree
    case class Unused (b : Boolean) extends Tree

    test ("first child can be accessed") {
        val n = Pair (Leaf (1), Leaf (2))
        assert (n.left eq n.firstChild, "first child of pair")
    }

    test ("cached attributes are only evaluated once") {
        import Attribution._

        var count = 0

        lazy val maximum : Tree ==> Int =
            attr {
                case Pair (l,r) => count = count + 1; (l->maximum).max (r->maximum)
                case Leaf (v)   => v
            }

        val t = Pair (Leaf (3), Pair (Leaf (1), Leaf (10)))

        expect (10, "first value") (t->maximum)
        expect (10, "second value") (t->maximum)
        expect (2, "evaluation count") (count)
    }

    test ("cached attributes are distinct for nodes that are equal") {
        import Attribution._

        var count = 0

        lazy val maximum : Tree ==> Int =
            attr {
                case Pair (l,r) => count = count + 1; (l->maximum).max (r->maximum)
                case Leaf (v)   => v
            }

        val t = Pair (Leaf (3), Pair (Leaf (1), Leaf (10)))
        val s = Pair (Leaf (3), Pair (Leaf (1), Leaf (10)))

        expect (10, "first value") (t->maximum)
        expect (10, "second value") (s->maximum)
        expect (4, "evaluation count") (count)
    }
    
    test ("cached attributes are defined where they should be") {
        import Attribution._

        lazy val maximum : Tree ==> Int =
            attr {
                case Pair (l,r) => 0
                case Leaf (v)   => 0
            }
            
        expect (true, "isDefinedAt Leaf") (maximum.isDefinedAt (Leaf (1)))
        expect (true, "isDefinedAt Pair") (maximum.isDefinedAt (Pair (Leaf (1), Leaf (2))))
        expect (false, "isDefinedAt Unused") (maximum.isDefinedAt (Unused (false)))
        
    }

    test ("cached attributes can be reset") {
        import Attribution._

        var count = 0

        lazy val maximum : Tree ==> Int =
            attr {
                case Pair (l,r) => count = count + 1; (l->maximum).max (r->maximum)
                case Leaf (v)   => v
            }

        val t = Pair (Leaf (3), Pair (Leaf (1), Leaf (10)))

        expect (10, "first value") (t->maximum)
        resetMemo
        expect (10, "second value") (t->maximum)
        expect (4, "evaluation count") (count)
    }

    test ("uncached attributes are evaluated each time") {
        import UncachedAttribution._

        var count = 0

        lazy val maximum : Tree ==> Int =
            attr {
                case Pair (l,r) => count = count + 1; (l->maximum).max (r->maximum)
                case Leaf (v)   => v
            }

        val t = Pair (Leaf (3), Pair (Leaf (1), Leaf (10)))

        expect (10, "first value") (t->maximum)
        expect (10, "second value") (t->maximum)
        expect (4, "evaluation count") (count)
    }

    test ("uncached attributes are defined where they should be") {
        import UncachedAttribution._

        lazy val maximum : Tree ==> Int =
            attr {
                case Pair (l,r) => 0
                case Leaf (v)   => 0
            }
            
        expect (true, "isDefinedAt Leaf") (maximum.isDefinedAt (Leaf (1)))
        expect (true, "isDefinedAt Pair") (maximum.isDefinedAt (Pair (Leaf (1), Leaf (2))))
        expect (false, "isDefinedAt Unused") (maximum.isDefinedAt (Unused (false)))
        
    }

    test ("cached child attributes work") {
        import Attribution._

        lazy val cattr : Tree ==> Int =
            childAttr {
                case Pair (l, r) => {
                    case Pair (l, r) => 0
                    case Leaf (v)    => 1
                    case _           => 2
                }
                case Leaf (v) => {
                    case Pair (l, r) => 3
                    case Leaf (v)    => 4
                    case _           => 5
                }
                case _ => {
                    case _ => 6
                }
            }

        val f = Leaf (4)
        val e = Leaf (3)
        val d = Leaf (2)
        val c = Leaf (1)
        val b = Pair (d, e)
        val a = Pair (b, c)
        
        expect (0, "cached childAttr Pair Pair") (cattr (b))
        expect (2, "cached childAttr Pair top") (cattr (a))
        expect (3, "cached childAttr Leaf Pair") (cattr (c))
        expect (5, "cached childAttr Leaf top") (cattr (f))
    }

    test ("uncached child attributes work") {
        import UncachedAttribution._

        lazy val cattr : Tree ==> Int =
            childAttr {
                case Pair (l, r) => {
                    case Pair (l, r) => 0
                    case Leaf (v)    => 1
                    case _           => 2
                }
                case Leaf (v) => {
                    case Pair (l, r) => 3
                    case Leaf (v)    => 4
                    case _           => 5
                }
                case _ => {
                    case _ => 6
                }
            }

        val f = Leaf (4)
        val e = Leaf (3)
        val d = Leaf (2)
        val c = Leaf (1)
        val b = Pair (d, e)
        val a = Pair (b, c)
        
        expect (0, "uncached childAttr Pair Pair") (cattr (b))
        expect (2, "uncached childAttr Pair top") (cattr (a))
        expect (3, "uncached childAttr Leaf Pair") (cattr (c))
        expect (5, "uncached childAttr Leaf top") (cattr (f))
    }

    test ("cached parameterised attributes work") {
        import Attribution._

        lazy val pattr : String => Tree ==> Int =
            paramAttr {
                case "hello" => {
                    case Pair (l, r) => 0
                    case Leaf (v)    => 1
                    case _           => 2
                }
                case "goodbye" => {
                    case _ => 3
                }
            }
        
        expect (0, "cached paramAttr Pair hello") (
            pattr ("hello") (Pair (Leaf (1), Leaf (2)))
        )
        expect (3, "cached paramAttr Pair goodbye") (
            pattr ("goodbye") (Pair (Leaf (1), Leaf (2)))
        )
        expect (1, "cached paramAttr Leaf hello") (pattr ("hello") (Leaf (1)))
        expect (3, "cached paramAttr Leaf goodbye") (pattr ("goodbye") (Leaf (1)))
    }

    test ("uncached parameterised attributes work") {
        import UncachedAttribution._

        lazy val pattr : String => Tree ==> Int =
            paramAttr {
                case "hello" => {
                    case Pair (l, r) => 0
                    case Leaf (v)    => 1
                    case _           => 2
                }
                case "goodbye" => {
                    case _ => 3
                }
            }
        
        expect (0, "uncached paramAttr Pair hello") (
            pattr ("hello") (Pair (Leaf (1), Leaf (2)))
        )
        expect (3, "uncached paramAttr Pair goodbye") (
            pattr ("goodbye") (Pair (Leaf (1), Leaf (2)))
        )
        expect (1, "uncached paramAttr Leaf hello") (pattr ("hello") (Leaf (1)))
        expect (3, "uncached paramAttr Leaf goodbye") (pattr ("goodbye") (Leaf (1)))
    }

    test ("circularities are detected for cached attributes") {
        import Attribution._

        lazy val direct : Tree ==> Int =
            attr {
                case t => t->direct
            }
        lazy val indirect : Tree ==> Int =
            attr {
                case t => t->indirect2
            }
        lazy val indirect2 : Tree ==> Int =
            attr {
                case t => t->indirect
            }

        val t = Pair (Leaf (3), Pair (Leaf (1), Leaf (10)))

        intercept[IllegalStateException] {
            t->direct
        }

        intercept[IllegalStateException] {
            t->indirect
        }
    }

    test ("circularities are detected for uncached attributes") {
        import UncachedAttribution._

        lazy val direct : Tree ==> Int =
            attr {
                case t => t->direct
            }
        lazy val indirect : Tree ==> Int =
            attr {
                case t => t->indirect2
            }
        lazy val indirect2 : Tree ==> Int =
            attr {
                case t => t->indirect
            }

        val t = Pair (Leaf (3), Pair (Leaf (1), Leaf (10)))

        intercept[IllegalStateException] {
            t->direct
        }

        intercept[IllegalStateException] {
            t->indirect
        }
    }

    test ("parameterised attribute keys compare correctly") {

        object Base extends AttributionBase {
            val n = Leaf (1)
            val k1 = new ParamAttributeKey ("hello", n)
            val k2 = new ParamAttributeKey ("hello", n)
            val k3 = new ParamAttributeKey ("hello", Leaf (1))
            val k4 = new ParamAttributeKey ("goodbye", n)
            val k5 = new ParamAttributeKey ("goodbye", Leaf (1))
            val k6 = new ParamAttributeKey ("hello", null)
            val k7 = new ParamAttributeKey ("hello", null)
            val k8 = new ParamAttributeKey ("goodbye", null)
            expect (true) (k1 equals k2)
            expect (true) (k2 equals k1)
            expect (false) (k1 equals k3)
            expect (false) (k3 equals k1)
            expect (false) (k1 equals k4)
            expect (false) (k4 equals k1)
            expect (false) (k1 equals k5)
            expect (false) (k5 equals k1)
            expect (false) (k1 equals k6)
            expect (false) (k6 equals k1)
            expect (false) (k1 equals k7)
            expect (false) (k7 equals k1)
            expect (false) (k1 equals k8)
            expect (false) (k8 equals k1)
            expect (false) (k1 equals Leaf (1))
            expect (false) (Leaf (1) equals k1)
            expect (true) (k6 equals k7)
            expect (true) (k7 equals k6)
            expect (false) (k6 equals k8)
            expect (false) (k8 equals k6)
        }

        Base

    }
    
}

/**
 * Tests of collection attributes.
 */
// @RunWith(classOf[JUnitRunner])
// class CollectionAttributionTests extends FunSuite
//                                  with org.kiama.example.lambda2.Parser {
// 
//     import Attribution._
//     import org.kiama.example.lambda2.AST._
//     import org.kiama.example.lambda2.Analysis._
// 
//     def process (s : String, r : Set[(Int,Int)]) {
//         parseAll (start, s) match {
//             case Success (e : Lam, in) if in.atEnd =>
//                 expect (r, "uses for " + e) (e->uses)
//             case Success (e, _) =>
//                 fail ("non-Lam " + e + " parsed in test input '" + s + "'")
//             case _ =>
//                 fail ("can't parse test input '" + s + "'")
//         }
//     }

//    test ("collection attribute: no collected node") {
//
//
//    }

//    test ("collection attribute: single collected node") {
//        process ("""\x : Int . x""", Set ((1,12)))
//    }

//    test ("collection attribute: multiple collected nodes") {
//        process ("""\\x : Int . x + (\\y : Int . x + y) 5""")
//    }

// }
