package com.github.sulir.runtimesave.starter;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.util.ArrayList;
import java.util.List;

public class LineJumpInstrumentation implements Opcodes {
    public static final String FLAG_CLASS = Type.getInternalName(StarterAgent.class);
    public static final String FLAG_FIELD = "JUMPED_TO_LINE";

    private final InsnList instructions;
    private final List<LocalVariableNode> variables;

    public LineJumpInstrumentation(MethodNode method) {
        this.instructions = method.instructions;
        this.variables = method.localVariables;
    }

    public void generate(int line) {
        LabelNode lineStart = findLineNode(line).start;
        LabelNode skipLabel = new LabelNode();
        InsnList prepended = new InsnList();
        prepended.add(generateSkipIfAlreadyJumped(skipLabel));

        for (LocalVariableNode variable : findVariablesAt(lineStart))
            prepended.add(generateInitializer(variable));

        prepended.add(new JumpInsnNode(GOTO, lineStart));
        prepended.add(skipLabel);
        instructions.insert(prepended);

        if (StarterAgent.DEBUG)
            printInstructions();
    }

    private InsnList generateSkipIfAlreadyJumped(LabelNode skipLabel) {
        InsnList list = new InsnList();
        list.add(new FieldInsnNode(GETSTATIC, FLAG_CLASS, FLAG_FIELD, Type.BOOLEAN_TYPE.getDescriptor()));
        list.add(new JumpInsnNode(IFNE, skipLabel));
        list.add(new InsnNode(ICONST_1));
        list.add(new FieldInsnNode(PUTSTATIC, FLAG_CLASS, FLAG_FIELD, Type.BOOLEAN_TYPE.getDescriptor()));
        return list;
    }

    private LineNumberNode findLineNode(int line) {
        for (AbstractInsnNode node : instructions) {
            if (node instanceof LineNumberNode lineNode && lineNode.line == line)
                    return lineNode;
        }

        throw new IllegalArgumentException("Line " + line + " not found");
    }

    private List<LocalVariableNode> findVariablesAt(AbstractInsnNode node) {
        int index = instructions.indexOf(node);
        List<LocalVariableNode> variablesAtNode = new ArrayList<>();

        for (LocalVariableNode variable : variables) {
            if (variable.name.equals("this"))
                continue;

            int start = instructions.indexOf(variable.start);
            int end = instructions.indexOf(variable.end);

            if (start <= index && index <= end)
                variablesAtNode.add(variable);
        }

        return variablesAtNode;
    }

    private InsnList generateInitializer(LocalVariableNode variable) {
        Type type = Type.getType(variable.desc);
        InsnList list = new InsnList();

        list.add(getZeroConstant(type));
        list.add(new VarInsnNode(type.getOpcode(ISTORE), variable.index));

        return list;
    }

    private AbstractInsnNode getZeroConstant(Type type) {
        return switch (type.getSort()) {
            case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> new InsnNode(ICONST_0);
            case Type.LONG -> new InsnNode(LCONST_0);
            case Type.FLOAT -> new InsnNode(FCONST_0);
            case Type.DOUBLE -> new InsnNode(DCONST_0);
            case Type.ARRAY, Type.OBJECT -> new InsnNode(ACONST_NULL);
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        };
    }

    private void printInstructions() {
        Printer printer = new Textifier();
        TraceMethodVisitor tracer = new TraceMethodVisitor(printer);
        instructions.forEach(instruction -> instruction.accept(tracer));
        printer.getText().forEach(System.err::print);
    }
}
