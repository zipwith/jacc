// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc;

/** Defines the numeric codes that are used to represent Jacc tokens.
 */
interface JaccTokens {
    int ERROR      = -1;          // Bad token
    int ENDINPUT   = 0;           // end of input
    int MARK       = 1;           // %%
    int CODE       = 2;           // %{ code %}
    int IDENT      = 3;           // symbol
    int CHARLIT    = 4;           // character literal
    int STRLIT     = 5;           // string literal
    int INTLIT     = 6;           // integer literal
    int ACTION     = 7;           // { code }
    int TOKEN      = 8;           // %token
    int TYPE       = 9;           // %type
    int PREC       = 10;          // %prec
    int LEFT       = 11;          // %left
    int RIGHT      = 12;          // %right
    int NONASSOC   = 13;          // %nonassoc
    int START      = 14;          // %start
    int PACKAGE    = 15;          // %package
    int CLASS      = 16;          // %class
    int INTERFACE  = 17;          // %interface
    int EXTENDS    = 18;          // %extends
    int IMPLEMENTS = 19;          // %implements
    int SEMANTIC   = 20;          // %semantic
    int GETTOKEN   = 21;          // %get
    int NEXTTOKEN  = 22;          // %next
    int COLON      = ':';         // :
    int SEMI       = ';';         // ;
    int BAR        = '|';         // |
    int TOPEN      = '<';         // <
    int TCLOSE     = '>';         // >
    int BOPEN      = '[';         // [
    int BCLOSE     = ']';         // ]
    int DOT        = '.';         // .
}
