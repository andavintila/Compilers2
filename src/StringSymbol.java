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

import java.io.PrintStream;

/** String table entry for string constants. */
class StringSymbol extends AbstractSymbol {
    /* Creates a new symbol.
     * 
     * @see AbstractSymbol
     * */
    public StringSymbol(String str, int len, int index) {
	super(str, len, index);
    }

    /** Generates code for the string constant definition.  This method
     * is incomplete; you get to finish it up in programming assignment
     * 5.
     * @param stringclasstag the class tag for string object
     * @param s the output stream
     *
     * */
    public void codeDef(int stringclasstag, PrintStream s) {
	IntSymbol lensym = (IntSymbol)AbstractTable.inttable.addInt(str.length());
	
	s.print(IRRef() + CgenSupport.LABEL); // label
	s.println(CgenSupport.DWORD + stringclasstag); // tag
	s.println(CgenSupport.DWORD + (CgenSupport.DEFAULT_OBJFIELDS +
				      CgenSupport.STRING_SLOTS +
				      (str.length() + 4) / 4)); // object size
	s.print(CgenSupport.DLABEL);

	/* Add code to reference the dispatch table for class String here */

	s.println(TreeConstants.Str + CgenSupport.DISPTAB_SUFFIX);		// dispatch table
	s.print(CgenSupport.DLABEL + lensym.IRRef()); s.println(""); // length
	CgenSupport.emitStringConstant(str, s); // ascii string
    }

    /** Returns a reference to this string constant.
     * @param s the output stream
     * */
    public String IRRef() {
	return CgenSupport.STRCONST_PREFIX + index;
    }

    /** Returns a copy of this symbol */
    public Object clone() {
	return new StringSymbol(str, str.length(), index);
    }
}

