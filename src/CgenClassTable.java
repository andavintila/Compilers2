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
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.Enumeration;


/** This class is used for representing the inheritance tree during code
    generation. You will need to fill in some of its methods and
    potentially extend it in other useful ways. */
class CgenClassTable extends SymbolTable {

    /** All classes in the program, represented as CgenNode */
    private Vector nds;
    private Hashtable<AbstractSymbol, Hashtable<method, Integer>> dispatchTable= new Hashtable<AbstractSymbol, Hashtable<method, Integer>>();
    //hastable util pentru retinerea offsetului metodelor; string reprezinta dispatchTable-ul concatenat cu clasa din care face parte metoda si numele metodei
    private Hashtable<String, Integer> methodOffset = new Hashtable<String, Integer>();
    //hashtable pentru a retine atributele si clasele din care fac parte
    private Hashtable<String, attr> attrHt = new Hashtable<String, attr>();

    /** This is the stream to which assembly instructions are output */
    private PrintStream str;

    private int stringclasstag;
    private int intclasstag;
    private int boolclasstag;
    //hashtabel pentru contorizarea label-urilor
    Hashtable<String, Integer> labelCount = new Hashtable<String, Integer>();
    private boolean basicM = false;
    // The following methods emit code for constants and global
    // declarations.

    /** Emits code to start the .data segment and to
     * declare the global names.
     * */
    private void codeGlobalData() {
	// The following global names must be defined first.
    	str.print("\t.data\n");
    	str.println("\tDS\t" + "4");
    }

    /** Emits code to start the .text segment and to
     * declare the global names.
     * */
    private void codeGlobalText() {
    	str.println("#### code section");
    	str.println("\t.code");
    }

    /** Emits code definitions for boolean constants. */
    private void codeBools(int classtag) {
		BoolConst.falsebool.codeIRDef(classtag, str);
		BoolConst.truebool.codeIRDef(classtag, str);
    }

    /** Emits code to reserve space for and initialize all of the
     * constants.  Class names should have been added to the string
     * table (in the supplied code, is is done during the construction
     * of the inheritance graph), and code for emitting string constants
     * as a side effect adds the string's length to the integer table.
     * The constants are emmitted by running through the stringtable and
     * inttable and producing code for each entry. */
    private void codeConstants() {
		// Add constants that are required by the code generator.
	    str.println("#### constants");
		AbstractTable.stringtable.addString("");
		AbstractTable.inttable.addString("0");
	
		AbstractTable.stringtable.codeStringTable(stringclasstag, str);
		AbstractTable.inttable.codeStringTable(intclasstag, str);
		codeBools(boolclasstag);
    }

    /** Creates data structures representing basic Cool classes (Object,
     * IO, Int, Bool, String).  Please note: as is this method does not
     * do anything useful; you will need to edit it to make if do what
     * you want.
     * */
    private void installBasicClasses() {
		AbstractSymbol filename 
		    = AbstractTable.stringtable.addString("<basic class>");
		
		// A few special class names are installed in the lookup table
		// but not the class list.  Thus, these classes exist, but are
		// not part of the inheritance hierarchy.  No_class serves as
		// the parent of Object and the other special classes.
		// SELF_TYPE is the self class; it cannot be redefined or
		// inherited.  prim_slot is a class known to the code generator.
	
		addId(TreeConstants.No_class,
		      new CgenNode(new class_(0,
					      TreeConstants.No_class,
					      TreeConstants.No_class,
					      new Features(0),
					      filename),
				   CgenNode.Basic, this));
	
		addId(TreeConstants.SELF_TYPE,
		      new CgenNode(new class_(0,
					      TreeConstants.SELF_TYPE,
					      TreeConstants.No_class,
					      new Features(0),
					      filename),
				   CgenNode.Basic, this));
		
		addId(TreeConstants.prim_slot,
		      new CgenNode(new class_(0,
					      TreeConstants.prim_slot,
					      TreeConstants.No_class,
					      new Features(0),
					      filename),
				   CgenNode.Basic, this));
	
		// The Object class has no parent class. Its methods are
		//        cool_abort() : Object    aborts the program
		//        type_name() : Str        returns a string representation 
		//                                 of class name
		//        copy() : SELF_TYPE       returns a copy of the object
	
		class_ Object_class = 
		    new class_(0, 
			       TreeConstants.Object_, 
			       TreeConstants.No_class,
			       new Features(0)
				   .appendElement(new method(0, 
						      TreeConstants.cool_abort, 
						      new Formals(0), 
						      TreeConstants.Object_, 
						      new no_expr(0)))
				   .appendElement(new method(0,
						      TreeConstants.type_name,
						      new Formals(0),
						      TreeConstants.Str,
						      new no_expr(0)))
				   .appendElement(new method(0,
						      TreeConstants.copy,
						      new Formals(0),
						      TreeConstants.SELF_TYPE,
						      new no_expr(0))),
			       filename);
	
		installClass(new CgenNode(Object_class, CgenNode.Basic, this));
		
		// The IO class inherits from Object. Its methods are
		//        out_string(Str) : SELF_TYPE  writes a string to the output
		//        out_int(Int) : SELF_TYPE      "    an int    "  "     "
		//        in_string() : Str            reads a string from the input
		//        in_int() : Int                "   an int     "  "     "
	
		class_ IO_class = 
		    new class_(0,
			       TreeConstants.IO,
			       TreeConstants.Object_,
			       new Features(0)
				   .appendElement(new method(0,
						      TreeConstants.out_string,
						      new Formals(0)
							  .appendElement(new formal(0,
									     TreeConstants.arg,
									     TreeConstants.Str)),
						      TreeConstants.SELF_TYPE,
						      new no_expr(0)))
				   .appendElement(new method(0,
						      TreeConstants.out_int,
						      new Formals(0)
							  .appendElement(new formal(0,
									     TreeConstants.arg,
									     TreeConstants.Int)),
						      TreeConstants.SELF_TYPE,
						      new no_expr(0)))
				   .appendElement(new method(0,
						      TreeConstants.in_string,
						      new Formals(0),
						      TreeConstants.Str,
						      new no_expr(0)))
				   .appendElement(new method(0,
						      TreeConstants.in_int,
						      new Formals(0),
						      TreeConstants.Int,
						      new no_expr(0))),
			       filename);
	
		installClass(new CgenNode(IO_class, CgenNode.Basic, this));
	
		// The Int class has no methods and only a single attribute, the
		// "val" for the integer.
	
		class_ Int_class = 
		    new class_(0,
			       TreeConstants.Int,
			       TreeConstants.Object_,
			       new Features(0)
				   .appendElement(new attr(0,
						    TreeConstants.val,
						    TreeConstants.prim_slot,
						    new no_expr(0))),
			       filename);
	
		installClass(new CgenNode(Int_class, CgenNode.Basic, this));
	
		// Bool also has only the "val" slot.
		class_ Bool_class = 
		    new class_(0,
			       TreeConstants.Bool,
			       TreeConstants.Object_,
			       new Features(0)
				   .appendElement(new attr(0,
						    TreeConstants.val,
						    TreeConstants.prim_slot,
						    new no_expr(0))),
			       filename);
	
		installClass(new CgenNode(Bool_class, CgenNode.Basic, this));
	
		// The class Str has a number of slots and operations:
		//       val                              the length of the string
		//       str_field                        the string itself
		//       length() : Int                   returns length of the string
		//       concat(arg: Str) : Str           performs string concatenation
		//       substr(arg: Int, arg2: Int): Str substring selection
	
		class_ Str_class =
		    new class_(0,
			       TreeConstants.Str,
			       TreeConstants.Object_,
			       new Features(0)
				   .appendElement(new attr(0,
						    TreeConstants.val,
						    TreeConstants.Int,
						    new no_expr(0)))
				   .appendElement(new attr(0,
						    TreeConstants.str_field,
						    TreeConstants.prim_slot,
						    new no_expr(0)))
				   .appendElement(new method(0,
						      TreeConstants.concat,
						      new Formals(0)
							  .appendElement(new formal(0,
									     TreeConstants.arg, 
									     TreeConstants.Str)),
						      TreeConstants.Str,
						      new no_expr(0)))
				   .appendElement(new method(0,
						      TreeConstants.length,
						      new Formals(0),
						      TreeConstants.Int,
						      new no_expr(0)))
				   .appendElement(new method(0,
						      TreeConstants.substr,
						      new Formals(0)
							  .appendElement(new formal(0,
									     TreeConstants.arg,
									     TreeConstants.Int))
							  .appendElement(new formal(0,
									     TreeConstants.arg2,
									     TreeConstants.Int)),
						      TreeConstants.Str,
						      new no_expr(0))),
			       filename);
	
		installClass(new CgenNode(Str_class, CgenNode.Basic, this));
    }
	
    // The following creates an inheritance graph from
    // a list of classes.  The graph is implemented as
    // a tree of `CgenNode', and class names are placed
    // in the base class symbol table.
    
    private void installClass(CgenNode nd) {
		AbstractSymbol name = nd.getName();
		if (probe(name) != null) return;
		nds.addElement(nd);
		addId(name, nd);
    }

    private void installClasses(Classes cs) {
        for (Enumeration e = cs.getElements(); e.hasMoreElements(); ) {
	    installClass(new CgenNode((Class_)e.nextElement(), 
				       CgenNode.NotBasic, this));
        }
    }

    private void buildInheritanceTree() {
	for (Enumeration e = nds.elements(); e.hasMoreElements(); ) {
	    setRelations((CgenNode)e.nextElement());
	}
    }

    private void setRelations(CgenNode nd) {
		CgenNode parent = (CgenNode)probe(nd.getParent());
		nd.setParentNd(parent);
		parent.addChild(nd);
    }

    /** Constructs a new class table and invokes the code generator */
    public CgenClassTable(Classes cls, PrintStream str) {
		nds = new Vector();
	
		this.str = str;
	
		stringclasstag = 3 /* Change to your String class tag here */;
		intclasstag =    1 /* Change to your Int class tag here */;
		boolclasstag =   2 /* Change to your Bool class tag here */;
	
		enterScope();
		if (Flags.cgen_debug) System.out.println("Building CgenClassTable");
		
		installBasicClasses();
		installClasses(cls);
		buildInheritanceTree();
	
		code();
	
		exitScope();
    }
    public String vr(int i){
    	return "VR"+i;
    }
    public String vi(int i){
    	return "VI"+i;
    }
    
    //afisarea claselor
    public void emitClassNameTab(){
    	str.println("#### class names by tag");
    	str.print(CgenSupport.CLASSNAMETAB + CgenSupport.LABEL);
    	str.println(CgenSupport.DLABEL + ((StringSymbol)AbstractTable.stringtable.lookup("Object")).IRRef());
    	str.println(CgenSupport.DLABEL + ((StringSymbol)AbstractTable.stringtable.lookup("Int")).IRRef());
    	str.println(CgenSupport.DLABEL + ((StringSymbol)AbstractTable.stringtable.lookup("Bool")).IRRef());
    	str.println(CgenSupport.DLABEL + ((StringSymbol)AbstractTable.stringtable.lookup("String")).IRRef());
    	str.println(CgenSupport.DLABEL + ((StringSymbol)AbstractTable.stringtable.lookup("IO")).IRRef());
    	
    	for (Enumeration e = nds.elements(); e.hasMoreElements(); ) {
    	    CgenNode cn = (CgenNode)e.nextElement();
    	    if (!cn.basic())
    	    	str.println(CgenSupport.DLABEL + ((StringSymbol)AbstractTable.stringtable.lookup(cn.getName().getString())).IRRef());
    	}
    }
    
    //afisarea prototipurilor
    public void emitClassObjTab(){
    	str.println("#### prototypes and init by tag");
    	str.print(CgenSupport.CLASSOBJTAB + CgenSupport.LABEL);
    	for (Enumeration e = nds.elements(); e.hasMoreElements(); ) {
    	    CgenNode cn = (CgenNode)e.nextElement();
    	    str.println(CgenSupport.DLABEL + cn.getName().getString()+ "_protObj");
    	    str.println(CgenSupport.DLABEL + cn.getName().getString()+ "_init");
    	}
    }
    
    //crearea si afisarea tabelelor cu metode
    //se apeleaza de 2 ori, o data pentru memorarea lor si folosirea la generarea de cod si apoi pentru afisarea in sectiunea .data
    public void emitDispatchTables(boolean print){
    	if (print) str.println("#### dispatch tables");
    	
    	methodOffset.put("Object\tDL\tObject.abort", 0);
    	methodOffset.put("Object\tDL\tObject.copy", 1);
    	methodOffset.put("Object\tDL\tObject.type_name", 2);
    	methodOffset.put("IO\tDL\tObject.abort", 0);
    	methodOffset.put("IO\tDL\tObject.copy", 1);
    	methodOffset.put("IO\tDL\tObject.type_name", 2);
    	methodOffset.put("String\tDL\tObject.abort", 0);
    	methodOffset.put("String\tDL\tObject.copy", 1);
    	methodOffset.put("String\tDL\tObject.type_name", 2);

    	methodOffset.put("Bool\tDL\tObject.abort", 0);
    	methodOffset.put("Bool\tDL\tObject.copy", 1);
    	methodOffset.put("Bool\tDL\tObject.type_name", 2);
    	
    	methodOffset.put("IO\tDL\tIO.out_string", 3);
    	methodOffset.put("IO\tDL\tIO.out_int", 4);
    	methodOffset.put("IO\tDL\tIO.in_string", 5);
    	methodOffset.put("IO\tDL\tIO.in_int", 6);
    	
    	methodOffset.put("String\tDL\tString.concat", 3);
    	methodOffset.put("String\tDL\tString.length", 4);
    	methodOffset.put("String\tDL\tString.substr", 5);
    	
    	//pentru toate clasele ce nu sunt basic
    	for (Enumeration e = nds.elements(); e.hasMoreElements(); ) {
    	    CgenNode cn = (CgenNode)e.nextElement();
    	    String name =  cn.getName().getString();
    	    //vector pentru retinerea label urilor si apoi inversarea lor
    	    Vector<String> v = null;
    	    
    	    if (!cn.basic()){
    	    	//arrayList folosit pentru retinerea metodelor duplicat din copil/parinte
    	    	ArrayList<String> methods = new ArrayList<String>();
    	    	if (print) str.print(name + CgenSupport.DISPTAB_SUFFIX + CgenSupport.LABEL);
    	    	v = new Vector<String>();
    	    	Enumeration ef = cn.getFeatures().getElements();
    	    	ArrayList al = Collections.list(ef);
    	    	//se adauga toate metodele in vector
    	    	for (int i = al.size()-1; i >=0 ; i--){
    	    		Object feature = al.get(i);
    	    		if(feature instanceof method){
    	    			method m = (method) feature;
    	    			methods.add(m.name.str);
    	    			v.add(CgenSupport.DLABEL + name + CgenSupport.METHOD_SEP + m.name.str);
    	    		}
    	    	}
    	    	
    	    	//parcurgere pe linia parintelui
    	    	CgenNode cnParent = cn.getParentNd();
	    		CgenNode parentAux = null;
    	    	while (cnParent!=null&&!cnParent.name.str.equals("Object")){
    	    		ef = cnParent.getFeatures().getElements();
    	    		al = Collections.list(ef);
    	    		for (int i = al.size()-1; i >=0 ; i--){
    		    		Object feature = al.get(i);
    		    		if(feature instanceof method){
    		    			method m = (method) feature;
    		    			//daca metoda nu a mai fost intalnita pana acum o adaug
    		    			if (!methods.contains(m.name.str)){
    		    				methods.add(m.name.str);
    		    				v.add(CgenSupport.DLABEL + cnParent.getName() + CgenSupport.METHOD_SEP + m.name.str);
    		    			}
    		    			else {
    		    				//daca a mai fost intalnita o scot si o adaug din nou pentru a-i schimba pozitia si astfel si offsetul
    		    				if (v.contains(CgenSupport.DLABEL + cn.getName() + CgenSupport.METHOD_SEP + m.name.str)){
    		    					v.remove(v.indexOf(CgenSupport.DLABEL + cn.getName() + CgenSupport.METHOD_SEP + m.name.str));
        		    				v.add(CgenSupport.DLABEL + cn.getName() + CgenSupport.METHOD_SEP + m.name.str);
    		    				}
    		    			}
    		    		}
    		    	}
    	    		cnParent = cnParent.getParentNd();
    	    	}
    	    	Collections.reverse(v);
    	    	int offset = 3;
    	    	
    	    	if (print){
	    	    	str.println("\tDL\tObject.abort");
	    	    	str.println("\tDL\tObject.copy");
	    	    	str.println("\tDL\tObject.type_name");
    	    	}
    	    	else{
    	    		methodOffset.put(cn.getName().str+"\tDL\tObject.abort", 0);    	    	
	    	    	methodOffset.put(cn.getName().str+"\tDL\tObject.copy", 1);    	    	
	    	    	methodOffset.put(cn.getName().str+"\tDL\tObject.type_name", 2);
    	    	}
    	    	for (int i = 0; i < v.size(); i++){
    	    		if (print){
    	    			str.println(v.get(i));
    	    		}
    	    		else{
    	    			//memorarea offsetului
    	    			methodOffset.put(cn.getName().str+v.get(i), offset);
    	    			offset++;
    	    		}
    	    	}
    	    }
    	}
    }
    
    //adaugare atribut classtag tuturor claselor
    int classtag = 5;
    public void addClasstag(CgenNode cn){
    	if (cn.classtag == -1)
    		cn.classtag = classtag;
    	else classtag--;
    	for (Enumeration e = cn.getChildren(); e.hasMoreElements();){
    		CgenNode cnc = (CgenNode) e.nextElement();
    		classtag++;
    		addClasstag(cnc);
    	}
    }
    
    //afisarea obiectelor prototipurilor
    public void emitPrototypeObjects(){
    	str.println("#### prototype objects");
    	//pentru fiecare clasa ce nu e basic
    	for (Enumeration e = nds.elements(); e.hasMoreElements(); ) {
    	    CgenNode cn = (CgenNode)e.nextElement();
    	    String name =  cn.getName().getString();
    	    String attributes = "";
    	    
    	    if (!cn.basic()){
    	    	str.print(name + CgenSupport.PROTOBJ_SUFFIX + CgenSupport.LABEL);
    	    	str.println(CgenSupport.DWORD + cn.classtag);
    	    	int objectSize = CgenSupport.DEFAULT_OBJFIELDS;
    	    	for (Enumeration ef = cn.getFeatures().getElements(); ef.hasMoreElements();){
    	    		Object feature = ef.nextElement();
    	    		if(feature instanceof attr){
    	    			attr a = (attr) feature;
    	    			if (a.type_decl.str.equals("String")){
    	    				attributes = attributes.concat(CgenSupport.DLABEL + ((StringSymbol)AbstractTable.stringtable.lookup("")).IRRef() + "\n");
    	    			}
    	    			else
    	    				attributes = attributes.concat(CgenSupport.DWORD + "0" + "\n");
    	    			objectSize++;
    	    			
    	    		}
    	    	}
    	    	//parcurgere pe linia parintelui
    	    	CgenNode cnParent = cn.getParentNd();
    	    	while (cnParent!=null){
    	    		for (Enumeration ef = cnParent.getFeatures().getElements(); ef.hasMoreElements();){
        	    		Object feature = ef.nextElement();
        	    		if(feature instanceof attr){
        	    			attr a = (attr) feature;
        	    			if (a.type_decl.str.equals("String"))
        	    				attributes = attributes.concat(CgenSupport.DLABEL + ((StringSymbol)AbstractTable.stringtable.lookup("")).IRRef() + "\n");
        	    			else
        	    				attributes = attributes.concat(CgenSupport.DWORD + "0\n");
        	    			objectSize++;
        	    		}
        	    	}
    	    		cnParent = cnParent.getParentNd();
    	    	}
    	    	str.println(CgenSupport.DWORD + objectSize);
    	    	str.println(CgenSupport.DLABEL + name + CgenSupport.DISPTAB_SUFFIX);
    	    	str.print(attributes);
    	    }
    	}
    }
    //afisarea functiei void_disp_handler
    public void emitDispatchHandler(){
    	str.println(".function \"Void dispatch handler\", 2, 0\nvoid_disp_handler:");
    	CgenSupport.emitIRLoadAddress("VR1", "VI0", str);
    	CgenSupport.emitIRLoadAddress("VR3", "VI1", str);
    	CgenSupport.emitIRLoadAddress("VR2", "Int_protObj", str);
    	List<String> ls1 = new ArrayList<String>();
    	ls1.add("VR2");
    	CgenSupport.emitIRCall("Object.copy", "VR2", ls1 ,str);
    	CgenSupport.emitIRStore("VR3", 3, "VR2", str);
    	List<String> ls2 = new ArrayList<String>();
    	ls2.add("VR1");
    	ls2.add("VR2");
    	CgenSupport.emitIRCall("_dispatch_abort", null, ls2, str);
    	str.println("\treturn");
    	str.println(".end\n");
    }
    //afisarea functiei void_case_handler
    public void emitCaseHandler(){
    	str.println(".function \"Void case handler\", 2, 0\nvoid_case_handler:");
    	CgenSupport.emitIRLoadAddress("VR1", "VI0", str);
    	CgenSupport.emitIRLoadAddress("VR3", "VI1", str);
    	CgenSupport.emitIRLoadAddress("VR2", "Int_protObj", str);
    	List<String> ls1 = new ArrayList<String>();
    	ls1.add("VR2");
    	CgenSupport.emitIRCall("Object.copy", "VR2", ls1 ,str);
    	CgenSupport.emitIRStore("VR3", 3, "VR2", str);
    	List<String> ls2 = new ArrayList<String>();
    	ls2.add("VR1");
    	ls2.add("VR2");
    	CgenSupport.emitIRCall("_case_abort2", null, ls2, str);
    	str.println("\treturn");
    	str.println(".end\n");
    }
    
    //adaugarea fiecarui atribut din fiecare clasa offsetul corespunzator
    //si popularea hashtable-ului attrHt pentru situatiile de atribute cu acelasi nume aflate in clase diferite
    public void addAttrOffset(int offset, CgenNode cn){
    	for (Enumeration ef = cn.getFeatures().getElements(); ef.hasMoreElements();){
    		Feature f = (Feature)ef.nextElement();
    		if (f instanceof attr){
    			attr a = (attr) f;
    			addId(a.name, a);
    			a.offset = offset;
    			attrHt.put(cn.getName() +"." + a.name, a);
    			offset++;
    		}
    	}
    	for (Enumeration ec = cn.getChildren(); ec.hasMoreElements();){
    		addAttrOffset(offset, (CgenNode)ec.nextElement());
    	}
    }
   
    public method findMethod(CgenNode cn, AbstractSymbol methodName){
    	//localizarea metodei
    	method m = null;
    	if (cn.getFeatures()==null) return m;
		for (Enumeration em = cn.getFeatures().getElements(); em.hasMoreElements();){
			Feature ft = (Feature) em.nextElement();
			if (ft instanceof method){
				m = (method)ft;
				if (m.name.str.equals(methodName.str)){
					break;
				}
				else m = null;
			}
		}
		if (m == null && cn.getParentNd()!=null){
			return findMethod(cn.getParentNd(), methodName);
		}
		return m;    	
    }
    
    public boolean basicMethod(String str){
    	return str.equals("copy")||str.equals("abort")||str.equals("type_name")||str.equals("out_string")||str.equals("length")||str.equals("out_int")||str.equals("substr");
    }
    
    //metoda principala pentru emiterea codului in functie de tipul expresiei
    public void process(int reg, Expression exp){
    	if (exp instanceof string_const){
    		string_const s = (string_const) exp;
    		CgenSupport.emitIRLoadAddress(vr(reg), ((StringSymbol)s.token).IRRef(), str);
    	}
    	
    	if (exp instanceof int_const){
    		CgenSupport.emitIRLoadAddress(vr(reg), ((int_const)exp).token.str, str);
    	}
    	
    	if (exp instanceof bool_const){
    		if (((bool_const)exp).val.toString().equals("true"))
    			CgenSupport.emitIRLoadAddress(vr(reg), "1", str);
    		else CgenSupport.emitIRLoadAddress(vr(reg), "0", str);
     	}
    	if (exp instanceof new_){
    		str.println("\t## begin new");
    		new_ n = (new_) exp;
    		CgenSupport.emitIRLoadAddress(vr(reg), n.type_name.str+CgenSupport.PROTOBJ_SUFFIX, str);
    		ArrayList al = new ArrayList<String>();
    		al.add(vr(reg));
    		CgenSupport.emitIRCall("Object.copy", vr(reg) , al, str);
    		CgenSupport.emitIRCall(n.type_name.str+CgenSupport.CLASSINIT_SUFFIX, null , al, str);
    	}
    	if (exp instanceof isvoid){
    		str.println("\t## begin isvoid");
    		isvoid iv = (isvoid) exp;
    		int lc = 0;
    		if (!labelCount.containsKey("isvoid")){
    			labelCount.put("isvoid", 0);
    		}
    		else {
    			lc = labelCount.get("isvoid");
        		lc = lc + 1;
    			labelCount.put("isvoid", lc);
    		}
    		if (iv.e1.get_type().equals("Int")||iv.e1.get_type().equals("Bool")||iv.e1.get_type().equals("String")||iv.e1.get_type().equals("Object")||iv.e1.get_type().equals("IO"))
    			CgenSupport.emitIRLoadAddress(vr(reg), "0", str);
    		else{
    			process(reg, iv.e1);
    			CgenSupport.emitIRJumpf(vr(reg), "isvoid_true"+lc, str);
    			CgenSupport.emitIRLoadAddress(vr(reg), "0", str);
    			CgenSupport.emitIRJump("isvoid_end"+lc, str);
    			str.print("isvoid_true"+lc + CgenSupport.LABEL);
    			CgenSupport.emitIRLoadAddress(vr(reg), "1", str);
    			str.print("isvoid_end"+lc + CgenSupport.LABEL);
    		}
    	}
    	if (exp instanceof object){
    		object o = (object) exp;
    		Object sym = lookup(o.name);
    	
    		if (o.name.str.equals("self")) {
    			CgenSupport.emitIRLoadAddress(vr(reg), vr(0), str);
    		}
    		if (sym instanceof attr){
    			//localizarea atributului pentru a obtine offsetul si registrul in care se afla
    			attr a = (attr) sym;
    			CgenNode cn = (CgenNode) lookup(AbstractTable.idtable.addString("current_class"));
    			attr aux = a;
    			a = null;
    			while(a == null && cn != null){
    				a = attrHt.get(cn.getName() + "." + aux.name);
    				cn = cn.getParentNd();
    			}
    			CgenSupport.emitIRLoad(vr(reg), a.offset, vr(a.reg), str);
    		}
    		if (sym instanceof Integer){
    			//daca nu este atribut si este parametru de funtie am asociat un intreg
    			//in tabela de simboluri pentru a stii registrul in care s-a retinut
    			int i = (Integer) sym;
    			CgenSupport.emitIRLoadAddress(vr(reg), vr(i), str);
    		}
    	}	
    	if (exp instanceof plus){
    		str.println("\t## + operator");
    		plus pl = (plus) exp;
    		process(reg, pl.e1);
    		process(reg+1, pl.e2);
    		CgenSupport.emitIRAdd(vr(reg), vr(reg), vr(reg+1), str);
    	}
    	if (exp instanceof sub){
    		str.println("\t## - operator");
    		sub sb = (sub) exp;
    		process(reg, sb.e1);
    		process(reg+1, sb.e2);
    		CgenSupport.emitIRSub(vr(reg), vr(reg), vr(reg+1), str);
    	}
    	if (exp instanceof mul){
    		str.println("\t## * operator");
    		mul ml = (mul) exp;
    		process(reg, ml.e1);
    		process(reg+1, ml.e2);
    		CgenSupport.emitIRMul(vr(reg), vr(reg), vr(reg+1), str);
    	}
    	if (exp instanceof divide){
    		str.println("\t## / operator");
    		divide dv = (divide) exp;
    		process(reg, dv.e1);
    		process(reg+1, dv.e2);
    		CgenSupport.emitIRDiv(vr(reg), vr(reg), vr(reg+1), str);
    	}
    	if (exp instanceof leq){
    		str.println("\t## <= operator");
    		leq lq = (leq) exp;
    		process(reg, lq.e1);
    		process(reg+1, lq.e2);
    		CgenSupport.emitIRLeq(vr(reg), vr(reg), vr(reg+1), str);
    	}
    	if (exp instanceof eq){
    		str.println("\t## = operator");
    		
    		int lc = 0;
    		if (!labelCount.containsKey("slow")){
    			labelCount.put("slow", 0);
    		}
    		else{
    			lc = labelCount.get("slow");
        		lc = lc + 1;
    			labelCount.put("slow", lc);
    			
    		}
    		eq lq = (eq) exp;
    		if(!(lq.e1.get_type().str.equals("Int")|| 
    				lq.e1.get_type().str.equals("Bool")) &&
    				!(lq.e2.get_type().str.equals("Int")|| 
    	    				lq.e2.get_type().str.equals("Bool"))){
    			process(reg+1, lq.e1);
    			process(reg+2, lq.e2);
    			CgenSupport.emitIREq(vr(reg+3), vr(reg+1), vr(reg+2), str);
    			CgenSupport.emitIRJumpf(vr(reg+3), "_slow_eq"+lc, str);
    			CgenSupport.emitIRLoadAddress(vr(reg), 1+"", str);
    			CgenSupport.emitIRJump("_end_eq"+lc, str);
    			str.print("_slow_eq"+lc+CgenSupport.LABEL);
    			CgenSupport.emitIRLoadAddress(vr(reg+4), 1+"", str);
    			CgenSupport.emitIRLoadAddress(vr(reg+5), 0+"", str);
    			ArrayList<String> ls = new ArrayList<String>();
    			ls.add(vr(reg+4));
    			ls.add(vr(reg+5));
    			ls.add(vr(reg+1));
    			ls.add(vr(reg+2));
    			CgenSupport.emitIRCall("equality_test", vr(reg), ls, str);
    			str.print("_end_eq"+lc+CgenSupport.LABEL);
    		}
    		else {
    			process(reg, lq.e1);
    			process(reg+1, lq.e2);
    			
    			CgenSupport.emitIREq(vr(reg), vr(reg), vr(reg+1), str);
    		}
    	}
    	if (exp instanceof lt){
    		str.println("\t## < operator");
    		lt lq = (lt) exp;
    		process(reg, lq.e1);
    		process(reg+1, lq.e2);
    		CgenSupport.emitIRLt(vr(reg), vr(reg), vr(reg+1), str);
    	}
    	if (exp instanceof comp){
    		str.println("\t## not operator");
    		comp cp = (comp) exp;
    		process(reg, cp.e1);
    		CgenSupport.emitIRSub(vr(reg), "1", vr(reg), str);
    	}
    	if (exp instanceof neg){
    		str.println("\t## unary operator");
    		neg ng = (neg) exp;
    		process(reg, ng.e1);
    		CgenSupport.emitIRSub(vr(reg), "0", vr(reg), str);
    	}
    	if (exp instanceof cond){
    		str.println("\t## begin if-then-else");
    		cond cd = (cond) exp;
    		process(reg, cd.pred);
    		int lc = 0;
    		if (!labelCount.containsKey("if")){
    			labelCount.put("if", 0);
    		}
    		else{
    			lc = labelCount.get("if");
        		lc = lc + 1;
    			labelCount.put("if", lc);
    			
    		}
    		CgenSupport.emitIRJumpf(vr(reg), "ite_false"+lc, str);
    		process(reg, cd.then_exp);
    		//verificare boxing pentru ramura then
    		if ((cd.then_exp.get_type().str.equals("Bool")||cd.then_exp.get_type().str.equals("Int")) && !(cd.else_exp.get_type().str.equals("Bool")||cd.then_exp.get_type().str.equals("Int"))){
    			str.println("\t## boxing ");
				CgenSupport.emitIRLoadAddress(vr(reg+1), cd.then_exp.get_type()+CgenSupport.PROTOBJ_SUFFIX, str);
				ArrayList<String> ls1 = new ArrayList<String>();
				ls1.add(vr(reg+1));
				CgenSupport.emitIRCall("Object.copy", vr(reg+1), ls1, str);
		    	CgenSupport.emitIRStore(vr(reg), 3, vr(reg+1), str);
		    	CgenSupport.emitIRLoadAddress(vr(reg), vr(reg+1), str);
    		}
    		CgenSupport.emitIRJump("ite_end"+lc, str);
    		str.print("ite_false"+lc+CgenSupport.LABEL);
    		process(reg, cd.else_exp);
    		//verificare offset pentru ramura else
    		if ((cd.then_exp.get_type().str.equals("Object")||cd.then_exp.get_type().str.equals("SELF_TYPE")) && (cd.else_exp.get_type().str.equals("Int")|| cd.else_exp.get_type().str.equals("Bool"))){
    			str.println("\t## boxing ");
				CgenSupport.emitIRLoadAddress(vr(reg+1), cd.else_exp.get_type()+CgenSupport.PROTOBJ_SUFFIX, str);
				ArrayList<String> ls1 = new ArrayList<String>();
				ls1.add(vr(reg+1));
				CgenSupport.emitIRCall("Object.copy", vr(reg+1), ls1, str);
		    	CgenSupport.emitIRStore(vr(reg), 3, vr(reg+1), str);
		    	CgenSupport.emitIRLoadAddress(vr(reg), vr(reg+1), str);
    		}
    		str.print("ite_end"+lc+CgenSupport.LABEL);
    	}
    	if (exp instanceof assign){
    		str.println("\t## begin assign");
    		assign an = (assign) exp;
    		Object sym = lookup(an.name);
    		if (sym instanceof attr){
    			attr a = (attr) sym;
    			process(reg, an.expr);
    			CgenSupport.emitIRStore(vr(reg), a.offset, vr(a.reg), str);
    		}
    		else {
    			if (lookup(an.name)!=null){
    				int i = (Integer) lookup(an.name);
    				process(reg, an.expr);
    				CgenSupport.emitIRLoadAddress(vr(i), vr(reg), str);
    			}
    		}
    		//boxing la atribuire
    		if (an.get_type().str.equals("Object") && (an.expr.get_type().str.equals("Int")||an.expr.get_type().str.equals("Bool"))){
    			str.println("\t## boxing ");
				CgenSupport.emitIRLoadAddress(vr(reg+1), an.expr.get_type()+CgenSupport.PROTOBJ_SUFFIX, str);
				ArrayList<String> ls1 = new ArrayList<String>();
				ls1.add(vr(reg+1));
				CgenSupport.emitIRCall("Object.copy", vr(reg+1), ls1, str);
		    	CgenSupport.emitIRStore(vr(reg), 3, vr(reg+1), str);
		    	CgenSupport.emitIRLoadAddress(vr(reg), vr(reg+1), str);
    		}
    	}
    	if (exp instanceof block){
    		block bl = (block) exp;
    		for (Enumeration ebl = bl.body.getElements(); ebl.hasMoreElements();){
    			process(reg, (Expression)ebl.nextElement());
    		}
    	}
    	if (exp instanceof static_dispatch){
    		static_dispatch sd = (static_dispatch) exp;
    		str.println("\t## begin static dispatch");
    		System.out.println();
    		
    		int lc = 0;
    		if (!labelCount.containsKey("dispatch")){
    			labelCount.put("dispatch", 0);
    		}
    		else {
    			lc = labelCount.get("dispatch");
    			lc = lc+1;
    			labelCount.put("dispatch", lc);
    		}
    		
    		//procesare apelant
    		process(reg, sd.expr);	
    		CgenSupport.emitIRJumpt(vr(reg), "dispatch_notvoid"+lc, str);
    		
    		//cazul in care apelantul nu este void
    		CgenSupport.emitIRLoadAddress(vr(reg+1), "str_const0" , str);
    		CgenSupport.emitIRLoadAddress(vr(reg+2), sd.lineNumber + "" , str);
    		List<String> ls = new ArrayList<String>();
        	ls.add(vr(reg+1));
        	ls.add(vr(reg+2));
    		CgenSupport.emitIRCall("void_disp_handler", null, ls, str);
    		
    		str.print("dispatch_notvoid" + lc + CgenSupport.LABEL);
    		
    		//localizarea dispatchului metodei si a clasei din care face parte
    		method m = null;
    		CgenNode cn = null;
    		cn = (CgenNode) lookup(sd.type_name);
    		CgenNode parent =  cn;
    		CgenNode cnParent = cn;
    		while(parent !=  null){
    			for (Enumeration ef = parent.getFeatures().getElements(); ef.hasMoreElements();){
    				Feature f = (Feature) ef.nextElement();
    				if (f instanceof method){
    					method md = (method) f;
    					if (md.name.str.equals(sd.name.str)){
    						m = md;
    						break;
    					}
    				}
    			}
    			if (!parent.getName().str.equals("_no_class")) cnParent = parent;
    			parent = parent.getParentNd();
    			if(m!=null) break;
    		}
    		
    		if (cn != null){
	    		if (m != null){
		    		//adaugarea parametrilor
		    		int i = reg+1;
		    		ls = new ArrayList<String>();
		    		ls.add(vr(reg));
		    		//procesarea parametrilor
		    		for (Enumeration ed = sd.actual.getElements(), ef = m.formals.getElements(); ed.hasMoreElements(); ){
						Expression e = (Expression) ed.nextElement();
						formal f = (formal) ef.nextElement();
						process(i, e);
						//boxing
						if (m.name.str.equals("out_int") || f.type_decl.str.equals("Object")&&(e.get_type().str.equals("Int")||e.get_type().str.equals("Bool"))){
							str.println("\t## boxing ");
							CgenSupport.emitIRLoadAddress(vr(i+1), f.type_decl.str+CgenSupport.PROTOBJ_SUFFIX, str);
							ArrayList<String> ls1 = new ArrayList<String>();
							ls1.add(vr(i+1));
							CgenSupport.emitIRCall("Object.copy", vr(i+1), ls1, str);
					    	CgenSupport.emitIRStore(vr(i), 3, vr(i+1), str);
					    	CgenSupport.emitIRLoadAddress(vr(i), vr(i+1), str);
						}
						ls.add(vr(i));
						i++;
					}
		    		//preluarea offsetului metodei
		    		int offset = methodOffset.get(sd.type_name.str+CgenSupport.DLABEL+cnParent.getName().str+CgenSupport.METHOD_SEP+m.name.str);
		    		
		    		//incarcarea metodei
		    		CgenSupport.emitIRLoadAddress(vr(i),  sd.type_name.str+CgenSupport.DISPTAB_SUFFIX, str);
		    		CgenSupport.emitIRLoad(vr(i), offset, vr(i) , str);
		    		
		    		//apelul metodei
		    		CgenSupport.emitIRCall(vr(i), vr(reg), ls, str);
	    		}
    		}	
    	}
    	if (exp instanceof dispatch){
    		dispatch d = (dispatch) exp;
    		str.println("\t## begin dispatch");
    		
    		int lc = 0;
    		if (!labelCount.containsKey("dispatch")){
    			labelCount.put("dispatch", 0);
    		}
    		else {
    			lc = labelCount.get("dispatch");
    			lc = lc+1;
    			labelCount.put("dispatch", lc);
    		}
    		
    		//procesare apelant
    		process(reg, d.expr);
    		
    		method m = null;
    		CgenNode cn = null;
    		
    		//localizarea metodei
    		if (d.expr.get_type().str.equals(AbstractTable.idtable.addString("SELF_TYPE").str))
    			cn = (CgenNode) lookup(AbstractTable.idtable.addString("current_class"));
    		else cn = (CgenNode) lookup(d.expr.get_type());
    		
    		CgenNode parent =  cn;
    		CgenNode cnParent = cn;
    		while(parent !=  null){
    			for (Enumeration ef = parent.getFeatures().getElements(); ef.hasMoreElements();){
    				Feature f = (Feature) ef.nextElement();
    				if (f instanceof method){
    					method md = (method) f;
    					if (md.name.str.equals(d.name.str)){
    						m = md;
    						break;
    					}
    				}
    			}
    			if (!parent.getName().str.equals("_no_class")) cnParent = parent;
    			parent = parent.getParentNd();
    			if(m!=null) break;
    		}
    		//boxing
    		if (cn !=null && m !=null){
    			if ((d.expr.get_type().str.equals("Int")||d.expr.get_type().str.equals("Bool")) && cnParent.getName().str.equals("Object")){
    				str.println("\t## boxing ");
					CgenSupport.emitIRLoadAddress(vr(reg+1), d.expr.get_type().str+CgenSupport.PROTOBJ_SUFFIX, str);
					ArrayList<String> ls1 = new ArrayList<String>();
					ls1.add(vr(reg+1));
					CgenSupport.emitIRCall("Object.copy", vr(reg+1), ls1, str);
			    	CgenSupport.emitIRStore(vr(reg), 3, vr(reg+1), str);
			    	CgenSupport.emitIRLoadAddress(vr(reg), vr(reg+1), str);
    			}
    				
    		}
    		
    		CgenSupport.emitIRJumpt(vr(reg), "dispatch_notvoid"+lc, str);
    		//cazul in care apelantul nu este void
    		CgenSupport.emitIRLoadAddress(vr(reg+1), "str_const0" , str);
    		CgenSupport.emitIRLoadAddress(vr(reg+2), d.lineNumber + "" , str);
    		List<String> ls = new ArrayList<String>();
        	ls.add(vr(reg+1));
        	ls.add(vr(reg+2));
    		CgenSupport.emitIRCall("void_disp_handler", null, ls, str);
    		str.print("dispatch_notvoid" + lc + CgenSupport.LABEL);
    		
    		if (cn != null){
	    		if (m != null){
		    		//adaugarea parametrilor
		    		int i = reg+1;
		    		ls = new ArrayList<String>();
		    		ls.add(vr(reg));
		    		for (Enumeration ed = d.actual.getElements(), ef = m.formals.getElements(); ed.hasMoreElements(); ){
						Expression e = (Expression) ed.nextElement();
						formal f = (formal) ef.nextElement();
						process(i, e);
						if (basicMethod(m.name.str)&&cnParent.basic()&&(e.get_type().str.equals("Int")||e.get_type().str.equals("Bool")) || f.type_decl.str.equals("Object")&&(e.get_type().str.equals("Int")||e.get_type().str.equals("Bool"))){
							str.println("\t## boxing ");
							CgenSupport.emitIRLoadAddress(vr(i+1), e.get_type().str+CgenSupport.PROTOBJ_SUFFIX, str);
							ArrayList<String> ls1 = new ArrayList<String>();
							ls1.add(vr(i+1));
							CgenSupport.emitIRCall("Object.copy", vr(i+1), ls1, str);
					    	CgenSupport.emitIRStore(vr(i), 3, vr(i+1), str);
					    	CgenSupport.emitIRLoadAddress(vr(i), vr(i+1), str);
						}
						ls.add(vr(i));
						i++;
					}
		    	
		    		int offset = methodOffset.get(cn.getName().str+CgenSupport.DLABEL+cnParent.getName().str+CgenSupport.METHOD_SEP+m.name.str);
		    		
		    		//incarcarea metodei
		    		CgenSupport.emitIRLoad(vr(i), CgenSupport.DISPTABLE_OFFSET, vr(reg) , str);
		    		CgenSupport.emitIRLoad(vr(i), offset, vr(i) , str);
		    		
		    		//apelul metodei
		    		CgenSupport.emitIRCall(vr(i), vr(reg), ls, str);
		    		
		    		//unboxing
		    		if (cnParent.getName().str.equals("String") && m.name.str.equals("length")){
		    			str.println("\t## unboxing");
		    			CgenSupport.emitIRLoad(vr(i), 3, vr(reg) , str);
		    			CgenSupport.emitIRLoadAddress(vr(reg), vr(i), str);
		    		}
		    		if(cnParent.getName().str.equals("IO") && m.name.str.equals("in_int")){
		    			basicM = true;
		    		}
	    		}
    		}
    		
    	}
    	if (exp instanceof loop){
    		loop lp = (loop) exp;
    		int lc = 0;
    		if (!labelCount.containsKey("loop")){
    			labelCount.put("loop", 0);
    		}
    		else {
    			lc = labelCount.get("loop");
    			lc = lc+1;
    			labelCount.put("loop", lc);
    		}
    		str.print("loop_start"+lc+CgenSupport.LABEL);
    		process(reg, lp.pred);
    		CgenSupport.emitIRJumpf(vr(reg), "loop_end"+lc, str);
    		process(reg, lp.body);
    		CgenSupport.emitIRJump("loop_start"+lc, str);
    		str.print("loop_end"+lc+CgenSupport.LABEL);
    	}
    	if (exp instanceof let){
    		str.println("\t## begin let");
    		let lt = (let) exp;
    		
    		enterScope();
    		
    		//initializarea variabilei cu valorile default sau procesarea expresiei de initializare daca exista
    		if(lt.init.get_type()==null){
    			if (lt.type_decl.str.equals("String"))
    				CgenSupport.emitIRLoadAddress(vr(reg), ((StringSymbol)AbstractTable.stringtable.lookup("")).IRRef(), str);
    			else
    				CgenSupport.emitIRLoadAddress(vr(reg), "0", str);
    		}
    		else process(reg, lt.init);
    		
    		if(basicM && lt.type_decl.str.equals("Int")){
				str.println("\t## unboxing");
    			CgenSupport.emitIRLoad(vr(reg+1), 3, vr(reg) , str);
    			CgenSupport.emitIRLoadAddress(vr(reg), vr(reg+1), str);
    		}
			basicM = false;
			
			//adaugarea variabilei in tabela de simboluri impreuna cu registrul in care este memorata
    		addId(lt.identifier, reg);
    		process(reg+1, lt.body);
    		
			CgenSupport.emitIRLoadAddress(vr(reg), vr(reg+1), str);
		
    		exitScope();
    	}
    	if (exp instanceof typcase){
    		typcase tc = (typcase) exp;
    		int countCases = 0;
    		str.println("\t## begin typed case");
    		int lc = 0;
    		if (!labelCount.containsKey("case")){
    			labelCount.put("case", 0);
    		}
    		else {
    			lc = labelCount.get("case");
    			lc = lc+1;
    			labelCount.put("case", lc);
    		}
    		
    		process(reg, tc.expr);
    		
    		//cazul in care expresia nu este void
    		CgenSupport.emitIRJumpt(vr(reg), "case"+lc+"_notvoid", str);
    		CgenSupport.emitIRLoadAddress(vr(reg+1), "str_const0" , str);
    		CgenSupport.emitIRLoadAddress(vr(reg+2), tc.lineNumber + "" , str);
    		List<String> ls = new ArrayList<String>();
        	ls.add(vr(reg+1));
        	ls.add(vr(reg+2));
    		CgenSupport.emitIRCall("void_case_handler", null, ls, str);
    		
    		str.print("case"+lc+"_notvoid" + CgenSupport.LABEL);
    		CgenSupport.emitIRLoad(vr(reg+1), 0, vr(reg), str);
    		
    		//ordonarea branchurilor in functie de classtag
    		ArrayList<CgenNode> al = new ArrayList<CgenNode>();
    		for(Enumeration ec = tc.cases.getElements(); ec.hasMoreElements();){
    			branch c = (branch) ec.nextElement();
    			CgenNode cn = (CgenNode) lookup(c.type_decl);
    			al.add(cn);
    		}
    		Collections.sort(al);
    		
    		int classtagMax = classtag;
    		branch c = null;
    		//codul pentru branchuri
    		//daca e un singur branch
    		if (al.size() == 1){
    			CgenNode node = al.get(0);
    			
				str.print("case"+lc+"_tag"+node.classtag+CgenSupport.LABEL);
				CgenSupport.emitIREq(vr(reg+2), vr(reg+1), al.get(0).classtag +"", str);
				CgenSupport.emitIRJumpf(vr(reg+2), "case"+lc+"_error", str);
				CgenSupport.emitIRLt(vr(reg+2), vr(reg+1), node.classtag +"", str);
				CgenSupport.emitIRJumpt(vr(reg+2), "case"+lc+"_error", str);
				CgenSupport.emitIRLt(vr(reg+2), classtagMax+"", vr(reg+1), str);
				CgenSupport.emitIRJumpt(vr(reg+2), "case"+lc+"_error", str);
			
				for(Enumeration ec = tc.cases.getElements(); ec.hasMoreElements();){
	    			c = (branch) ec.nextElement();
	    			CgenNode cn = (CgenNode) lookup(c.type_decl);
	    			if (cn.name.str.equals(node.name.str))
	    				break;
	    		}
				if (c != null)
					process(reg+1, c.expr);
				//salt la sfarsitul case-ului
				CgenSupport.emitIRJump("case"+lc+"_end", str);
    		}
    		//daca sunt mai multe branchuri
    		else {
	    		//primul branch
	    		str.print("case"+lc+"_tag"+al.get(0).classtag+CgenSupport.LABEL);
				CgenSupport.emitIREq(vr(reg+2), vr(reg+1), al.get(0).classtag +"", str);
				if (al.size()>1){
					CgenSupport.emitIRJumpf(vr(reg+2), "case"+lc+"_tag"+al.get(1).classtag, str);
				}
				c = null;
				for(Enumeration ec = tc.cases.getElements(); ec.hasMoreElements();){
	    			c = (branch) ec.nextElement();
	    			CgenNode cn = (CgenNode) lookup(c.type_decl);
	    			if (cn.name.str.equals(al.get(0).name.str))
	    				break;
	    		}
				if (c != null){
					enterScope();
					addId(c.name, reg);
					process(reg+1, c.expr);
					exitScope();
				}
				//salt la sfarsitul case-ului
				CgenSupport.emitIRJump("case"+lc+"_end", str);
				
	    		for (int i = 1; i < al.size()-1; i++){
	    			CgenNode node = al.get(i);
	    			str.print("case"+lc+"_tag"+node.classtag+CgenSupport.LABEL);
	    			CgenSupport.emitIRLt(vr(reg+2), vr(reg+1), node.classtag +"", str);
	    		
	    			CgenSupport.emitIRJumpt(vr(reg+2), "case"+lc+"_tag"+al.get(i+1).classtag, str);
	    			CgenSupport.emitIRLt(vr(reg+2), classtagMax+"", vr(reg+1), str);
	    			CgenSupport.emitIRJumpt(vr(reg+2), "case"+lc+"_tag"+al.get(i+1).classtag, str);
	    		
	    			for(Enumeration ec = tc.cases.getElements(); ec.hasMoreElements();){
	        			c = (branch) ec.nextElement();
	        			CgenNode cn = (CgenNode) lookup(c.type_decl);
	        			if (cn.name.str.equals(node.name.str))
	        				break;
	        		}
	    			if (c != null)
	    				process(reg+1, c.expr);
	    			CgenSupport.emitIRJump("case"+lc+"_end", str);
	    		}
	    		//ultimul branch
	    		CgenNode node = al.get(al.size()-1);
				str.print("case"+lc+"_tag"+node.classtag+CgenSupport.LABEL);
				CgenSupport.emitIRLt(vr(reg+2), vr(reg+1), node.classtag +"", str);
			
				CgenSupport.emitIRJumpt(vr(reg+2), "case"+lc+"_error", str);
				CgenSupport.emitIRLt(vr(reg+2), classtagMax+"", vr(reg+1), str);
				CgenSupport.emitIRJumpt(vr(reg+2), "case"+lc+"_error", str);
			
				for(Enumeration ec = tc.cases.getElements(); ec.hasMoreElements();){
	    			c = (branch) ec.nextElement();
	    			CgenNode cn = (CgenNode) lookup(c.type_decl);
	    			if (cn.name.str.equals(node.name.str))
	    				break;
	    		}
				if (c != null)
					process(reg+1, c.expr);
				CgenSupport.emitIRJump("case"+lc+"_end", str);
				
    		}
    		//error
			str.print("case"+lc+"_error" + CgenSupport.LABEL);
			ls = new ArrayList<String>();
			ls.add(vr(reg));
			CgenSupport.emitIRCall("_case_abort", null, ls, str);
			
			//end
			str.print("case"+lc+"_end"+CgenSupport.LABEL);
			CgenSupport.emitIRLoadAddress(vr(reg), vr(reg+1), str);
    	}
    }
    
    //generarea codului pentru metodele claselor
    public void emitClassMethod(CgenNode cn, method m){
    	int params = m.formals.getLength()+1;
    	String name = cn.getName() + CgenSupport.METHOD_SEP + m.name.str;
    	str.println(".function \"" + name + "\", " + params + ", 1");
    	str.print(name + CgenSupport.LABEL);
    	enterScope();
    	int i = 1;
    	CgenSupport.emitIRLoadAddress(vr(0), vi(0), str);
    	for (Enumeration ef = m.formals.getElements(); ef.hasMoreElements();){
    		formal f = (formal) ef.nextElement();
    		CgenSupport.emitIRLoadAddress(vr(i), vi(i), str);
    		addId(f.name, i);
    		i++;
    	}
    	process(i, m.expr);
    	//boxing in cazul valorilor returnate
    	if((m.expr.get_type().str.equals("Int")||m.expr.get_type().str.equals("Bool"))&&m.return_type.str.equals("Object")){
			str.println("\t## boxing ");
			CgenSupport.emitIRLoadAddress(vr(i+1), m.expr.get_type()+CgenSupport.PROTOBJ_SUFFIX, str);
			ArrayList<String> ls1 = new ArrayList<String>();
			ls1.add(vr(i+1));
			CgenSupport.emitIRCall("Object.copy", vr(i+1), ls1, str);
	    	CgenSupport.emitIRStore(vr(i), 3, vr(i+1), str);
	    	CgenSupport.emitIRLoadAddress(vr(i), vr(i+1), str);
		}
    	str.println("\treturn " + vr(i));
    	str.println(".end");
    	str.println();
    	exitScope();
    }
    
    //generare codului pentru functiile de initializare ale claselor
    public void emitClassInit(){
    	//pentru fiecare clasa diferita de cele basic
    	for (Enumeration e = nds.elements(); e.hasMoreElements(); ) {
    	    CgenNode cn = (CgenNode)e.nextElement();
    	    String name =  cn.getName().getString();
    	    int reg = 1;
    	    ArrayList<Object> vr = new ArrayList<Object>();
    	    enterScope();
    	    addId(AbstractTable.idtable.addString("current_class"), cn);
    	    if (!cn.basic()){    	    	
    	    	//adaugare atribute pentru a le putea folosi inainte de a fi declarate
    	    	for (Enumeration ef = cn.getFeatures().getElements(); ef.hasMoreElements();){
    	    		Object feature = ef.nextElement();
    	    		if(feature instanceof attr){
    	    			attr a = (attr) feature;
    	    			addId(a.name, a);
    	    		}
    	    	}
    	    	
    	    	str.println(".function \"" + name + " init code\", 1, 0");
    	    	str.print(name + CgenSupport.CLASSINIT_SUFFIX + CgenSupport.LABEL);
    	    	CgenSupport.emitIRLoadAddress("VR0", "VI0" , str);
    	    	List<String> ls =  new ArrayList<String>();
    	    	ls.add("VR0");
    	    	CgenSupport.emitIRCall(cn.getParent().str.concat(CgenSupport.CLASSINIT_SUFFIX), null, ls, str);
    	    	
    	    	for (Enumeration ef = cn.getFeatures().getElements(); ef.hasMoreElements();){
    	    		Object feature = ef.nextElement();
    	    		if(feature instanceof attr){
    	    			attr a = (attr) feature;
    	    			//variabila Int neinitializata
    	    			if (a.init.toString().startsWith("no_expr")&&a.type_decl.str.equals("Int")){
    	    					CgenSupport.emitIRLoadAddress(vr(reg), "0", str);	
    	    					CgenSupport.emitIRStore(vr(reg), a.offset, "VR0", str);
    	    			}
    	    			//variabila initializata
    	    			if (!a.init.toString().startsWith("no_expr")){
    	    				str.println("\t## begin assign");
    	    				process(reg, a.init);
    	    				if (a.type_decl.str.equals("Object") && (a.init.get_type().str.equals("Int")||a.init.get_type().str.equals("Bool"))){
    	    	    			str.println("\t## boxing ");
    	    					CgenSupport.emitIRLoadAddress(vr(reg+1), a.init.get_type()+CgenSupport.PROTOBJ_SUFFIX, str);
    	    					ArrayList<String> ls1 = new ArrayList<String>();
    	    					ls1.add(vr(reg+1));
    	    					CgenSupport.emitIRCall("Object.copy", vr(reg+1), ls1, str);
    	    			    	CgenSupport.emitIRStore(vr(reg), 3, vr(reg+1), str);
    	    			    	CgenSupport.emitIRStore(vr(reg+1), a.offset, "VR0", str);
    	    	    		}
    	    				else CgenSupport.emitIRStore(vr(reg), a.offset, "VR0", str);
    	    			}
    	    		}
    	    		
    	    	}
    	    	str.println("\treturn");
    	    	str.println(".end\n");
    	    	//apelarea metodei de generare de cod pentru celelalte metode ale clasei
    	    	for (Enumeration ef = cn.getFeatures().getElements(); ef.hasMoreElements();){
    	    		Object feature = ef.nextElement();
    	    		if(feature instanceof method){
    	    			method m = (method) feature;
    	    			emitClassMethod(cn, m);
    	    		}
    	    		
    	    	}
    	    }
    	    
    	    exitScope();
    	}
    	
    }
    //metoda ce apeleaza toate metodele pentru generarea segmentului de cod
    public void emitObjectInit(){
    	emitDispatchHandler();
    	emitCaseHandler();
    	addAttrOffset(CgenSupport.DEFAULT_OBJFIELDS, root());
    	root().classtag = 0;
    	for (Enumeration e = root().getChildren(); e.hasMoreElements();){
    		CgenNode cn = (CgenNode) e.nextElement();
    		if (cn.getName().str.equals("Int"))
    			cn.classtag = 1;
    		if (cn.getName().str.equals("Bool"))
    			cn.classtag = 2;
    		if (cn.getName().str.equals("String"))
    			cn.classtag = 3;
    		if (cn.getName().str.equals("IO"))
    			cn.classtag = 4;
    	}
    	addClasstag(root());
		emitDispatchTables(false);
    	emitClassInit();
    }
    /** This method is the meat of the code generator.  It is to be
        filled in programming assignment 5 */
    public void code() {
    	//constanta necesara in generarea de cod
    	
		AbstractTable.stringtable.addString("");
	    if (Flags.cgen_debug) System.out.println("coding global text");
	    codeGlobalText();
	    
	    emitObjectInit();
	
	    //apelare metode pentru generarea segmentului .data
		if (Flags.cgen_debug) System.out.println("coding global data");
		codeGlobalData();
	
		if (Flags.cgen_debug) System.out.println("coding constants");
		codeConstants();
		
		emitClassNameTab();
		emitClassObjTab();
		emitDispatchTables(true);
		emitPrototypeObjects();
    
    }

    /** Gets the root of the inheritance tree */
    public CgenNode root() {
    	return (CgenNode)probe(TreeConstants.Object_);
    }
}
			  
    
