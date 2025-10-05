package com.hnguyen703.analyzer.visitors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class MethodInvocationVisitor extends ASTVisitor {
    private final Map<String, Set<String>> callGraph = new HashMap<>();
    private String currentMethod;

    public void setCurrentMethod(String methodName) {
        this.currentMethod = methodName;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        String calleeName;

        if (node.getExpression() != null) {
            calleeName = node.getExpression().toString() + "." + node.getName().toString();
        } else {
            calleeName = node.getName().toString();
        }

        callGraph.computeIfAbsent(currentMethod, k -> new HashSet<>()).add(calleeName);
        return super.visit(node);
    }


    public Map<String, Set<String>> getCallGraph() {
        return callGraph;
    }
}
