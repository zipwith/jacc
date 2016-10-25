// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc;

import jacc.grammar.Grammar;
import jacc.grammar.LookaheadMachine;
import jacc.grammar.LR0Machine;
import jacc.grammar.SLRMachine;
import jacc.grammar.LALRMachine;

/** Records settings for Jacc generated parsers.
 */
public class Settings {

    //- Type of generated machine ---------------------------------------------
    private int machineType = LALR1;

    /** Indicates that the grammar should be treated as LR(0).
     */
    public static final int LR0   = 0;

    /** Indicates that the grammar should be treated as SLR(1).
     */
    public static final int SLR1  = 1;

    /** Indicates that the grammar should be treated as LALR(1).
     */
    public static final int LALR1 = 2;

    /** Set the type of the machine to be generated.
     */
    public void setMachineType(int machineType) {
        this.machineType = machineType;
    }

    /** Return the type of the machine to be generated.
     */
    public int getMachineType() {
        return machineType;
    }

    /** Generate a machine of the appropriate type from a grammar.
     *  This localizes the connection between the symbolic constants for
     *  machine types specified above and the classes that are used to
     *  implement them.
     */
    public LookaheadMachine makeMachine(Grammar grammar) {
        if (machineType == LR0) {
            return new LR0Machine(grammar);
        } else if (machineType == SLR1) {
            return new SLRMachine(grammar);
        } else {
            return new LALRMachine(grammar);
        }
    }


    //- Name of the package for generated classes -----------------------------
    private String packageName;

    /** Set the name of the package in which the classes should be placed.
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /** Return the name of the package in which the classes should be placed.
     */
    public String getPackageName() {
        return packageName;
    }


    //- Name of the parser class ----------------------------------------------
    private String className;

    /** Set the name of the Parser class.
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /** Return the name of the Parser class.
     */
    public String getClassName() {
        return className;
    }


    //- Name of the tokens interface ------------------------------------------
    private String interfaceName;

    /** Set the name of the tokens interface.
     */
    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    /** Return the name of the tokens interface.
     */
    public String getInterfaceName() {
        return interfaceName;
    }


    //- Name of the parser's base class ---------------------------------------
    private String extendsName;

    /** Set the name of the base class for the parser.
     */
    public void setExtendsName(String extendsName) {
        this.extendsName = extendsName;
    }

    /** Return the name of the base class for the parser.
     */
    public String getExtendsName() {
        return extendsName;
    }


    //- The list of interfaces implemented by the parser ----------------------
    private String implementsNames;

    /** Set the text containining a list of the interfaces implemented by
     *  the Parser.
     */
    public void setImplementsNames(String implementsNames) {
        this.implementsNames = implementsNames;
    }

    /** Add the name of an interface that is implemented by the parser.
     */
    public void addImplementsNames(String implementsNames) {
        if (this.implementsNames!=null) {
            this.implementsNames += ", " + implementsNames;
        } else {
            this.implementsNames = implementsNames;
        }
    }

    /** Return text with a list of the interfaces implemented by the Parser.
     */
    public String getImplementsNames() {
        return implementsNames;
    }


    //- Name of the type used for semantic values -----------------------------
    private String typeName;

    /** Return the base type of semantic values.
     */
    public String getTypeName() {
        return typeName;
    }

    /** Set the base type for semantic values.
     */
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }


    //- The text that is used to retrieve the current token -------------------
    private String getToken;

    /** Return the string that will give the current token.
     */
    public String getGetToken() {
        return getToken;
    }

    /** Set the string that will give the current token.
     */
    public void setGetToken(String getToken) {
        this.getToken = getToken;
    }


    //- The text that is used to retrieve the next token ----------------------
    private String nextToken;

    /** Set the string that will give the next token.
     */
    public void setNextToken(String nextToken) {
        this.nextToken = nextToken;
    }

    /** Return the the string that will give the next token.
     */
    public String getNextToken() {
        return nextToken;
    }


    //- The text that is used to retrieve the value of the current token ------
    private String getSemantic;

    /** Set the string that will give the semantic value of the
     *  current token.
     */
    public void setGetSemantic(String getSemantic) {
        this.getSemantic = getSemantic;
    }

    /** Return the string that will give the semantic value of the
     *  current token.
     */
    public String getGetSemantic() {
        return getSemantic;
    }


    //- The text that precedes the parser class declaration -------------------
    private StringBuffer preTextBuffer = new StringBuffer();

    /** Add to the text that goes before the class declaration.
     */
    public void addPreText(String preText) {
        preTextBuffer.append(preText);
    }

    /** Return the text that goes before the class declaration, which
     *  is typically just imports.
     */
    public String getPreText() {
        return preTextBuffer.toString();
    }


    //- The text that appears at the end of the parser class ------------------
    private StringBuffer postTextBuffer = new StringBuffer();

    /** Add to the post text.
     */
    public void addPostText(String postText) {
        postTextBuffer.append(postText);
    }

    /** Return the post text, which is the text that appears in the tail
     *  end of the class definition after the generated code and before
     *  the closing brace.
     */
    public String getPostText() {
        return postTextBuffer.toString();
    }


    //- Defaults for unspecified settings -------------------------------------

    /** Set sensible default values if no other choices have been specified.
     */
    public void fillBlanks(String name) {
        if (getClassName()==null) {
            setClassName(name + "Parser");
        }
        if (getInterfaceName()==null) {
            setInterfaceName(name + "Tokens");
        }
        if (getTypeName()==null) {
            setTypeName("Object");
        }
        if (getInterfaceName()!=null) {
            addImplementsNames(getInterfaceName());
        }
        if (getGetSemantic()==null) {
            setGetSemantic("lexer.getSemantic()");
        }
        if (getGetToken()==null) {
            setGetToken("lexer.getToken()");
        }
        if (getNextToken()==null) {
            setNextToken("lexer.nextToken()");
        }
    }
}
