package cop5556sp17;


import cop5556sp17.AST.Dec;

import java.util.HashMap;
import java.util.Stack;


public class SymbolTable {


    //TODO  add fields
    private int currentScope;
    private int nextScope;
    public static Stack<Integer> scopeStack = new Stack<>();
    public static HashMap<String, HashMap<Integer, Dec>> leblanccookTable = new HashMap<>();

    /**
     * to be called when block entered
     */
    public void enterScope() {
        //TODO:  IMPLEMENT THIS
        currentScope = nextScope++;
        scopeStack.push(currentScope);
    }


    /**
     * leaves scope
     */
    public void leaveScope() {
        //TODO:  IMPLEMENT THIS
        currentScope = scopeStack.pop();
    }

    public boolean insert(String ident, Dec dec) {
        //TODO:  IMPLEMENT THIS
        if (!leblanccookTable.containsKey(ident)) {
            leblanccookTable.put(ident, new HashMap<>());
        }
        if (leblanccookTable.get(ident).containsKey(currentScope)) {
            return false;
        }
        leblanccookTable.get(ident).put(currentScope, dec);
        return true;
    }

    public Dec lookup(String ident) {
        //TODO:  IMPLEMENT THIS
        if (leblanccookTable.containsKey(ident) && leblanccookTable.get(ident).containsKey(currentScope)) {
            return leblanccookTable.get(ident).get(currentScope);
        }
        return null;
    }

    public SymbolTable() {
        //TODO:  IMPLEMENT THIS
    }


    @Override
    public String toString() {
        //TODO:  IMPLEMENT THIS
        return "";
    }


}
