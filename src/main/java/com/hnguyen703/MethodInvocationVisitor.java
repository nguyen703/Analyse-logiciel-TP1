package com.hnguyen703;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class MethodInvocationVisitor extends ASTVisitor {
    private final Map<String, Set<String>> callGraph = new HashMap<>();
    private String currentMethod;

    public void setCurrentMethod(String methodName) {
        this.currentMethod = methodName;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        if (currentMethod != null) {
            String calleeName = node.getName().toString();
            String receiverType = "Unknown";
            if (node.getExpression() != null) {
                ITypeBinding binding = node.getExpression().resolveTypeBinding();
                if (binding != null) {
                    receiverType = binding.getQualifiedName();
                }
            }
            String qualifiedCallee = calleeName + " : " + receiverType;

            callGraph.computeIfAbsent(currentMethod, k -> new HashSet<>()).add(qualifiedCallee);
        }
        return super.visit(node);
    }

    public Map<String, Set<String>> getCallGraph() {
        return callGraph;
    }
}
