/**
 * This file is part of Kiama.
 *
 * Copyright (C) 2010-2012 Dominic R B Verity, Macquarie University.
 * Copyright (C) 2011-2012 Anthony M Sloane, Macquarie University.
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
package example.iswim.driver

/**
 * The main driver for compiling and executing ISWIM programs from the
 * command line.
 */

import org.kiama.example.iswim.compiler._
import org.kiama.example.iswim.secd._
import org.kiama.util.StdoutEmitter

object Main extends Parser with SemanticAnalysis with CodeGenerator with StdoutEmitter {

    import org.kiama.attribution.Attribution.initTree
    import org.kiama.example.iswim.driver.PrettyPrinter._
    import org.kiama.util.Emitter
    import org.kiama.util.IO.filereader
    import org.kiama.util.Messaging._
    import SECDBase._

    private def usageMessage =
    """|Usage: iswimc <options> <filename>
       |Options:
       |   -h   print this help message and stop
       |   -b   print the generated bytecode to standard out.
       |   -e   execute the generated bytecode and print value returned
       |        to standard out
       |   -d   turn on debugging output during execution (implies -e)""".stripMargin

    def main(args : Array[String]) {

        var executeFlag : Boolean = false
        var dumpFlag : Boolean = false
        var debugFlag : Boolean = false
        var helpFlag : Boolean = false
        var iswimFileName : String = null

        /**
         * Process command line arguments, setting appropriate
         * compilation flags as we go.
         */
        def processArgs(args : List[String]) : Boolean = args match {
            case Nil => iswimFileName != null
            case arg :: rest =>
                if (iswimFileName != null) false
                else if (arg.startsWith("-")) {
                    if (arg == "-h") { helpFlag = true; processArgs(rest) }
                    else if (arg == "-b") { dumpFlag = true; processArgs(rest) }
                    else if (arg == "-e") { executeFlag = true; processArgs(rest) }
                    else if (arg == "-d") { executeFlag = true; debugFlag = true; processArgs(rest) }
                    else false }
                else { iswimFileName = arg; processArgs(rest) }
        }

        if (processArgs(args.toList)) {
            val reader = filereader (iswimFileName)
            parseAll(start, reader) match {
                case Success(iswimcode,_) =>
                    initTree (iswimcode)
                    resetmessages
                    if (iswimcode->isSemanticallyCorrect) {
                        val bytecode = iswimcode->code
                        if (dumpFlag) {
                            emitter.emit("Generated bytecode:")
                            val d = (bytecode : CodeSegment).toDoc
                            emitter.emitln(pretty(nest(line <> d)))
                        }
                        if (executeFlag) {
                            val machine = new SECD(bytecode) {
                                override def debug : Boolean = debugFlag
                            }
                            if (debugFlag) emitter.emitln("Execution trace:")
                            machine.run
                            emitter.emitln("Returned value:")
                            machine.stack.value match {
                                case List(v) => emitter.emitln(v.toString)
                                case _ => emitter.emitln("** stack corrupted **")
                            }
                        }
                    } else report (new Emitter)
                case f => emitter.emitln(f)
            }
        } else emitter.emitln(usageMessage)
    }
}
