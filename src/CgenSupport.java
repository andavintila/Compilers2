/*
Copyright (c) 2000 The Regents of the University of California.
All rights reserved.
Permission to use, copy, modify, and distribute this software for any
purpose, without fee, and without written agreement is hereby granted,
provided that the above copyright notice and the following two
paragraphs appear in all copies of this software.
IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */

// This is a project skeleton file
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/** This class aggregates all kinds of support routines and constants
for the code generator; all routines are statics, so no instance of
this class is even created. */
class CgenSupport {

    final static int WORD_SIZE = 4;

    // Global names
    final static String CLASSNAMETAB = "class_nameTab";
    final static String CLASSOBJTAB = "class_objTab";

    // Naming conventions
    final static String DISPTAB_SUFFIX = "_dispatch";
    final static String METHOD_SEP = ".";
    final static String CLASSINIT_SUFFIX = "_init";
    final static String PROTOBJ_SUFFIX = "_protObj";
    final static String OBJECTPROTOBJ = "Object" + PROTOBJ_SUFFIX;
    final static String INTCONST_PREFIX = "int_const";
    final static String STRCONST_PREFIX = "str_const";
    final static String BOOLCONST_PREFIX = "bool_const";

    final static int EMPTYSLOT = 0;
    final static String LABEL = ":\n";

    // information about object headers
    final static int DEFAULT_OBJFIELDS = 3;
    final static int TAG_OFFSET = 0;
    final static int SIZE_OFFSET = 1;
    final static int DISPTABLE_OFFSET = 2;

    final static int STRING_SLOTS = 1;
    final static int INT_SLOTS = 1;
    final static int BOOL_SLOTS = 1;

    // opcodes
    final static String DWORD = "\tDW\t";
    final static String DLABEL = "\tDL\t";
    final static String LOAD = "\tload\t";
    final static String STORE = "\tstore\t";
    final static String JUMPF = "\tjumpf\t";
    final static String JUMPT = "\tjumpt\t";
    final static String JUMP = "\tjump\t";
    final static String CALL = "\tcall";
    final static String VR = "VR";

    static void emitIRLoad(String dest_reg, int offset, String source_reg, PrintStream s) {
        s.println(LOAD + dest_reg + " " + "[" + source_reg + ", " + offset * WORD_SIZE + "]");
    }

    static void emitIRStore(String source_reg, int offset, String dest_reg, PrintStream s) {
        s.println(STORE + source_reg + " " + "[" + dest_reg + ", " + offset * WORD_SIZE + "]");
    }

    static void emitIRLoadAddress(String dest_reg, String address, PrintStream s) {
        s.println("\t" + dest_reg + "\t<-\t" + address);
    }

    static void emitIRLoadBool(String dest_reg, BoolConst b, PrintStream s) {
        s.println("\t" + dest_reg + "\t<-\t" + b.IRRef());
    }

    static void emitIRAdd(String dest_reg, String src1, String src2, PrintStream s) {
        s.println("\t" + dest_reg + "\t<-\t" + src1 + " + " + src2);
    }

    static void emitIRDiv(String dest_reg, String src1, String src2, PrintStream s) {
        s.println("\t" + dest_reg + "\t<-\t" + src1 + " / " + src2);
    }

    static void emitIRMul(String dest_reg, String src1, String src2, PrintStream s) {
        s.println("\t" + dest_reg + "\t<-\t" + src1 + " * " + src2);
    }

    static void emitIRSub(String dest_reg, String src1, String src2, PrintStream s) {
        s.println("\t" + dest_reg + "\t<-\t" + src1 + " - " + src2);
    }

    static void emitIRLt(String dest_reg, String src1, String src2, PrintStream s) {
        s.println("\t" + dest_reg + "\t<-\t" + src1 + " < " + src2);
    }

    static void emitIRLeq(String dest_reg, String src1, String src2, PrintStream s) {
        s.println("\t" + dest_reg + "\t<-\t" + src1 + " <= " + src2);
    }

    static void emitIREq(String dest_reg, String src1, String src2, PrintStream s) {
        s.println("\t" + dest_reg + "\t<-\t" + src1 + " = " + src2);
    }

    static void emitIRJumpf(String src, String label, PrintStream s) {
        s.println(JUMPF + src + " " + label);
    }

    static void emitIRJumpt(String src, String label, PrintStream s) {
        s.println(JUMPT + src + " " + label);
    }

    static void emitIRJump(String label, PrintStream s) {
        s.println(JUMP + label);
    }

    static void emitIRCall(String callAddr, String retReg, List<String> paramRegs, PrintStream s) {
        if (retReg != null)
            s.print("\t( " + retReg + " )\t<-");
        s.print(CALL + " " + callAddr);
        if (paramRegs != null && paramRegs.size() > 0) {
            s.print(" ( ");
            for (String paramReg: paramRegs) {
                s.print(paramReg + " ");
            }
            s.print(")");
        }
        s.println();
    }

    static void emitIRMov(String dst, String src, PrintStream s) {
        s.println("\t" + dst + "\t<-\t" + src);
    }

    private static boolean ascii = false;

    /** Switch output mode to ASCII.
     * @param s the output stream
     * */
    static void asciiMode(PrintStream s) {
        if (!ascii) {
            s.print("\tDB\t\"");
            ascii = true;
        }
    }

    /** Switch output mode to BYTE
     * @param s the output stream
     * */
    static void byteMode(PrintStream s) {
        if (ascii) {
            s.println("\"");
            ascii = false;
        }
    }

    /** Emits a string constant.
     * @param str the string constant
     * @param s the output stream
     * */
    static void emitStringConstant(String str, PrintStream s) {
        ascii = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            switch (c) {
                case '\n':
                    asciiMode(s);
                    s.print("\\n");
                    break;
                case '\t':
                    asciiMode(s);
                    s.print("\\t");
                    break;
                case '\\':
                    byteMode(s);
                    s.println("\tDB\t" + (byte) '\\');
                    break;
                case '"':
                    asciiMode(s);
                    s.print("\"\"");
                    break;
                default:
                    if (c >= 0x20 && c <= 0x7f) {
                        asciiMode(s);
                        s.print(c);
                    } else {
                        byteMode(s);
                        s.println("\tDB\t" + (byte) c);
                    }
            }
        }

        byteMode(s);
        s.println("\tDS\t" + (4 - str.length() % 4));
    }
}
