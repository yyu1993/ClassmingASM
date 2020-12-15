import org.objectweb.asm.commons.LocalVariablesSorter;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeUnit;

import java.lang.instrument.*;


/*
 * This class is responsible for taking a seed class and parse it into a list of methods.
 * Methods will be counted and numbered so that random program can pick a method to mutate later.
 * Methods are objects holding method name, mutation information and labeled statement information.
 */
public class ClassParser {
    public ClassVisitor parsiveCV;

    public int methodCount;
    public int mutationCount;
    public Hashtable<String, Method> methodDictionary;
    public HashSet<String> seedInsnSet;
    public HashSet<String> totalLivecodeSet;
    public ArrayList<String> curLivecodeList;
    public double curCoverSeedVal;
    public Hashtable<String, InsnStmt> insnDict;

    public Hashtable<String, Hashtable<String, Label>> methodLabelDictionary;

    /**
     * Used to parse the seed class.
     */
    private class ParsiveMethodVisitor extends MethodVisitor {
        String methodName;
        InsnStmt curInsn;
        int insnCount;
        int labelCount;

        public ParsiveMethodVisitor(String methodName) {
            super(Opcodes.ASM9);
            this.methodName = methodName;
            Method m = new Method(methodName);
            methodDictionary.put(methodName, m);
            curInsn = null;
            insnCount = 0;
            labelCount = -1;
        }

        public void record(InsnStmt insn) {
            seedInsnSet.add(insn.identifier());
            insnDict.put(insn.identifier(), insn);
            insnCount += 1;
            methodDictionary.get(methodName).addInsn(insn);
        }

        public boolean checkDefUse(int opcode) {
            switch(opcode) {
                case Opcodes.ASTORE:
                case Opcodes.AASTORE:
                case Opcodes.BASTORE:
                case Opcodes.CASTORE:
                case Opcodes.DASTORE:
                case Opcodes.DSTORE:
                case Opcodes.FASTORE:
                case Opcodes.FSTORE:
                case Opcodes.IASTORE:
                case Opcodes.ISTORE:
                case Opcodes.LASTORE:
                case Opcodes.LSTORE:
                case Opcodes.SASTORE:
                case Opcodes.ALOAD:
                case Opcodes.AALOAD:
                case Opcodes.BALOAD:
                case Opcodes.CALOAD:
                case Opcodes.DALOAD:
                case Opcodes.DLOAD:
                case Opcodes.FALOAD:
                case Opcodes.FLOAD:
                case Opcodes.IALOAD:
                case Opcodes.ILOAD:
                case Opcodes.IINC:
                case Opcodes.LALOAD:
                case Opcodes.LLOAD:
                case Opcodes.SALOAD:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            InsnStmt is = new InsnStmt("IincInsn", var+" "+increment, methodName, insnCount, true, labelCount);
            record(is);
            super.visitIincInsn(var, increment);
        }

        @Override
        public void visitInsn(int opcode) {
            InsnStmt is = new InsnStmt("Insn", opcode+"", methodName, insnCount, checkDefUse(opcode), labelCount);
            record(is);
            super.visitInsn(opcode);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            InsnStmt is = new InsnStmt("InvokeDynamicInsn", name+" "+descriptor+" "+bootstrapMethodHandle+" "+ Arrays.toString(bootstrapMethodArguments), methodName, insnCount, false, labelCount);
            record(is);
            super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

//        @Override
//        public void visitJumpInsn(int opcode, Label label) {
//            InsnStmt is = new InsnStmt("JumpInsn", opcode+label.toString(), methodName, insnCount, false);
//            record(is);
//            super.visitJumpInsn(opcode, label);
//        }

        @Override
        public void visitLdcInsn(Object value) {
            InsnStmt is = new InsnStmt("LdcInsn", value.toString(), methodName, insnCount, false, labelCount);
            record(is);
            super.visitLdcInsn(value);
        }

        @Override
        public void visitLabel(Label label) {
            labelCount++;
            super.visitLabel(label);
        }

//        @Override
//        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
//            InsnStmt is = new InsnStmt("LookupSwitchInsn", dflt.toString()+" "+keys.toString()+" "+labels.toString(), methodName, insnCount, false);
//            record(is);
//            super.visitLookupSwitchInsn(dflt, keys, labels);
//        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            InsnStmt is = new InsnStmt("MethodInsn", opcode+" "+owner+" "+name+" "+descriptor+" "+isInterface, methodName, insnCount, checkDefUse(opcode), labelCount);
            record(is);
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimension) {
            InsnStmt is = new InsnStmt("MultiANewArrayInsn", descriptor+" "+numDimension, methodName, insnCount, false, labelCount);
            record(is);
            super.visitMultiANewArrayInsn(descriptor, numDimension);
        }

//        @Override
//        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
//            InsnStmt is = new InsnStmt("TableSwitchInsn", min+" "+max+" "+dflt.toString()+" "+labels.toString(), methodName, insnCount, false);
//            record(is);
//            super.visitTableSwitchInsn(min, max, dflt, labels);
//        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            InsnStmt is = new InsnStmt("TypeInsn", opcode+" "+type, methodName, insnCount, checkDefUse(opcode), labelCount);
            record(is);
            super.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            InsnStmt is = new InsnStmt("VarInsn", opcode+" "+var, methodName, insnCount, checkDefUse(opcode), labelCount);
            record(is);
            super.visitVarInsn(opcode, var);
        }

        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            methodDictionary.get(methodName).variableCount++;
            super.visitLocalVariable(name, descriptor, signature, start, end, index);
        }

        public void visitEnd() {
            methodDictionary.get(methodName).labelCount = (labelCount+1);
            methodCount++;
        }
    }

    /**
     * Used to instrument a class.
     */
    private class InstrumentalMethodVisitor extends MethodVisitor {
        String methodName;
        int insnCount;

        public InstrumentalMethodVisitor(String methodName, MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
            this.methodName = methodName;
            insnCount = 0;
        }

        public void instrument(String insn) {
            if(seedInsnSet.contains(insn)) {
                mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                mv.visitLdcInsn(insn);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
                insnCount += 1;
            }
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            InsnStmt is = new InsnStmt("IincInsn", var+" "+increment, methodName, insnCount, true);
            instrument(is.identifier());
            mv.visitIincInsn(var, increment);
        }

        @Override
        public void visitInsn(int opcode) {
            InsnStmt is = new InsnStmt("Insn", opcode+"", methodName, insnCount, false);
            instrument(is.identifier());
            mv.visitInsn(opcode);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            InsnStmt is = new InsnStmt("InvokeDynamicInsn", name+" "+descriptor+" "+bootstrapMethodHandle+" "+ Arrays.toString(bootstrapMethodArguments), methodName, insnCount, false);
            instrument(is.identifier());
            mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

//        @Override
//        public void visitJumpInsn(int opcode, Label label) {
//            InsnStmt is = new InsnStmt("JumpInsn", opcode+label.toString(), methodName, insnCount, false);
//            instrument(is.identifier());
//            mv.visitJumpInsn(opcode, label);
//        }

        @Override
        public void visitLdcInsn(Object value) {
            InsnStmt is = new InsnStmt("LdcInsn", value.toString(), methodName, insnCount, false);
            instrument(is.identifier());
            mv.visitLdcInsn(value);
        }

//        @Override
//        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
//            InsnStmt is = new InsnStmt("LookupSwitchInsn", dflt.toString()+" "+keys.toString()+" "+labels.toString(), methodName, insnCount, false);
//            instrument(is.identifier());
//            mv.visitLookupSwitchInsn(dflt, keys, labels);
//        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            InsnStmt is = new InsnStmt("MethodInsn", opcode+" "+owner+" "+name+" "+descriptor+" "+isInterface, methodName, insnCount, false);
            instrument(is.identifier());
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimension) {
            InsnStmt is = new InsnStmt("MultiANewArrayInsn", descriptor+" "+numDimension, methodName, insnCount, false);
            instrument(is.identifier());
            mv.visitMultiANewArrayInsn(descriptor, numDimension);
        }

//        @Override
//        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
//            InsnStmt is = new InsnStmt("TableSwitchInsn", min+" "+max+" "+dflt.toString()+" "+labels.toString(), methodName, insnCount, false);
//            instrument(is.identifier());
//            mv.visitTableSwitchInsn(min, max, dflt, labels);
//        }

//        @Override
//        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
//            InsnStmt is = new InsnStmt("TryCatchBlock", start+" "+end+" "+handler+" "+type, methodName, insnCount, false);
//            instrument(is.identifier());
//            mv.visitTryCatchBlock(start, end, handler, type);
//        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            InsnStmt is = new InsnStmt("TypeInsn", opcode+" "+type, methodName, insnCount, false);
            instrument(is.identifier());
            mv.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            InsnStmt is = new InsnStmt("VarInsn", opcode+" "+var, methodName, insnCount, false);
            instrument(is.identifier());
            mv.visitVarInsn(opcode, var);
        }
    }
    private class InstrumentalClassVisitor extends ClassVisitor {
        public InstrumentalClassVisitor(ClassVisitor cv) {
            super(Opcodes.ASM9, cv);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name,
                                         String desc, String signature, String[] exceptions) {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            return new ClassParser.InstrumentalMethodVisitor(name, mv);
        }
    }

    /**
     * Used to add label to a class.
     */
    private class LabelingMethodVisitor extends MethodVisitor {
        MutationStmt mutationStmt;
        String methodName;
        int insnCount;
        int mutationCount;
        Hashtable<String, Label> labelDictionary;

        public LabelingMethodVisitor(String methodName, MutationStmt ms, MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
            this.methodName = methodName;
            insnCount = 0;
            mutationCount = 0;
            labelDictionary = new Hashtable<>();
            mutationStmt = ms;
        }

        public void instrument(String insn) {
            if(seedInsnSet.contains(insn)) {
                insnCount += 1;
            }
        }

        public void instrumentLabel(InsnStmt is) {
            Label l = new Label();
            mv.visitLabel(l);
            labelDictionary.put(is.identifier(), l);
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            InsnStmt is = new InsnStmt("IincInsn", var+" "+increment, methodName, insnCount, true);
            instrument(is.identifier());
            if(methodDictionary.get(methodName).tpSet.contains(is.identifier()) || mutationStmt.TPS.contains(is.identifier())) {
                instrumentLabel(is);
            }
            mv.visitIincInsn(var, increment);
        }

        @Override
        public void visitInsn(int opcode) {
            InsnStmt is = new InsnStmt("Insn", opcode+"", methodName, insnCount, false);
            instrument(is.identifier());
            if(methodDictionary.get(methodName).tpSet.contains(is.identifier())) {
                instrumentLabel(is);
            }
            mv.visitInsn(opcode);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            InsnStmt is = new InsnStmt("InvokeDynamicInsn", name+" "+descriptor+" "+bootstrapMethodHandle+" "+ Arrays.toString(bootstrapMethodArguments), methodName, insnCount, false);
            instrument(is.identifier());
            if(methodDictionary.get(methodName).tpSet.contains(is.identifier())) {
                instrumentLabel(is);
            }
            mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

        @Override
        public void visitLdcInsn(Object value) {
            InsnStmt is = new InsnStmt("LdcInsn", value.toString(), methodName, insnCount, false);
            instrument(is.identifier());
            if(methodDictionary.get(methodName).tpSet.contains(is.identifier())) {
                instrumentLabel(is);
            }
            mv.visitLdcInsn(value);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            InsnStmt is = new InsnStmt("MethodInsn", opcode+" "+owner+" "+name+" "+descriptor+" "+isInterface, methodName, insnCount, false);
            instrument(is.identifier());
            if(methodDictionary.get(methodName).tpSet.contains(is.identifier())) {
                instrumentLabel(is);
            }
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimension) {
            InsnStmt is = new InsnStmt("MultiANewArrayInsn", descriptor+" "+numDimension, methodName, insnCount, false);
            instrument(is.identifier());
            if(methodDictionary.get(methodName).tpSet.contains(is.identifier())) {
                instrumentLabel(is);
            }
            mv.visitMultiANewArrayInsn(descriptor, numDimension);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            InsnStmt is = new InsnStmt("TypeInsn", opcode+" "+type, methodName, insnCount, false);
            instrument(is.identifier());
            if(methodDictionary.get(methodName).tpSet.contains(is.identifier())) {
                instrumentLabel(is);
            }
            mv.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            InsnStmt is = new InsnStmt("VarInsn", opcode+" "+var, methodName, insnCount, false);
            instrument(is.identifier());
            if(methodDictionary.get(methodName).tpSet.contains(is.identifier())) {
                instrumentLabel(is);
            }
            mv.visitVarInsn(opcode, var);
        }

        @Override
        public void visitEnd(){
            methodLabelDictionary.put(methodName, labelDictionary);
        }
    }
    private class LabelingClassVisitor extends ClassVisitor {
        public MutationStmt mutationStmt;
        public LabelingClassVisitor(ClassVisitor cv, MutationStmt ms) {
            super(Opcodes.ASM9, cv);
            mutationStmt = ms;
            methodLabelDictionary = new Hashtable<>();
        }

        @Override
        public MethodVisitor visitMethod(int access, String name,
                                         String desc, String signature, String[] exceptions) {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            if(methodDictionary.get(name).mutationCount > 1 || mutationStmt.METHOD.equals(name)) {
                return new ClassParser.LabelingMethodVisitor(name, mutationStmt, mv);
            } else {
                return mv;
            }
        }
    }

    /**
     * Used to add loopcount variable to a method.
     */
    private class VariableMethodAdapter extends LocalVariablesSorter {
        MutationStmt mutationStmt;
        String methodName;
        int varCount;

        public VariableMethodAdapter(int access, String desc, MethodVisitor mv, String methodName, MutationStmt ms) {
            super(Opcodes.ASM9, access, desc, mv);
            this.methodName = methodName;
            varCount = 0;
            mutationStmt = ms;
        }

        @Override
        public void visitCode() {
            super.visitCode();

            for(int i=0;i<methodDictionary.get(methodName).mutationCount;i++) {
                mv.visitIntInsn(Opcodes.BIPUSH, Config.LOOP_COUNT); // push int value to stack
                int loopVar = newLocal(Type.INT_TYPE);              // create new local variable
                mv.visitVarInsn(Opcodes.ISTORE, loopVar);           // store value on stack to this variable
                varCount++;
            }

//            for (MutationStmt ms : methodDictionary.get(methodName).mutationList) {
//                mv.visitIntInsn(Opcodes.BIPUSH, Config.LOOP_COUNT); // push int value to stack
//                int loopVar = newLocal(Type.INT_TYPE);              // create new local variable
//                mv.visitVarInsn(Opcodes.ISTORE, loopVar);           // store value on stack to this variable
//                ms.loopVar = loopVar;
//                varCount++;
//            }
//
//            if(mutationStmt.REMOVE < 0) {
//                mv.visitIntInsn(Opcodes.BIPUSH, Config.LOOP_COUNT); // push int value to stack
//                int loopVar = newLocal(Type.INT_TYPE);              // create new local variable
//                mv.visitVarInsn(Opcodes.ISTORE, loopVar);           // store value on stack to this variable
//                mutationStmt.loopVar = loopVar;
//                varCount++;
//            }
        }

        @Override
        public void visitEnd() {
            methodDictionary.get(methodName).variableCount = varCount;
            super.visitEnd();
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals){
            super.visitMaxs(maxStack + 4*varCount, maxLocals);
        }
    }
    private class VariableClassVisitor extends ClassVisitor {
        public MutationStmt mutationStmt;
        public VariableClassVisitor(ClassVisitor cv, MutationStmt ms) {
            super(Opcodes.ASM9, cv);
            mutationStmt = ms;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name,
                                         String desc, String signature, String[] exceptions) {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            if(methodDictionary.get(name).mutationCount > 1 || mutationStmt.METHOD.equals(name)) {
                return new ClassParser.VariableMethodAdapter(access, desc, mv, name, mutationStmt);
            } else {
                return mv;
            }
        }
    }

    /**
     * Used to add mutation to a method.
     */
    private class MutatingMethodVisitor extends MethodVisitor {
        MutationStmt mutationStmt;
        String methodName;
        int insnCount;
        int mutationCount;
        Label[] labelList;
        int labelCount;
        int[] loopVar;
        int loopVarCount;
        boolean skidLabelFlag;

        public MutatingMethodVisitor(String methodName, MutationStmt ms, MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
            this.methodName = methodName;
            insnCount = 0;
            mutationCount = 0;
            mutationStmt = ms;
            labelList = new Label[methodDictionary.get(methodName).labelCount];
            labelCount = 0;
            loopVar = new int[methodDictionary.get(methodName).mutationCount];
            loopVarCount = 0;
            skidLabelFlag = false;
        }

        public void instrument(String insn) {
            if(seedInsnSet.contains(insn)) {
                insnCount += 1;
            }
        }

//        public void instrumentLabel(String insn) {
//            if(methodDictionary.get(methodName).tpSet.contains(insn) || mutationStmt.TPS.contains(insn)) {
//                mv.visitLabel(labelDict.get(insn));
//            }
//        }

        public void mutate() {
            if (mutationStmt.HP==labelCount && mutationStmt.REMOVE < 0) {
                mv.visitIincInsn(loopVar[mutationCount], -1);      // decrement loopcount
                mv.visitVarInsn(Opcodes.ILOAD, loopVar[mutationCount]);     // load loopcount onto stack
                Label l1 = new Label();
                mv.visitJumpInsn(Opcodes.IFLE, l1);             // if loopcount greater than 0
                if (mutationStmt.HI == Opcodes.GOTO) {
                    int idx = insnDict.get(mutationStmt.TPS.get(0)).labelIdx;
                    if(idx < labelList.length && idx >= 0) {
                        mv.visitJumpInsn(Opcodes.GOTO, labelList[idx]);
                    }
                } else if (mutationStmt.HI == Opcodes.ATHROW) {
                    mv.visitInsn(Opcodes.ATHROW);
                } else if (mutationStmt.HI == Opcodes.RETURN) {
                    mv.visitInsn(Opcodes.RETURN);
                } else if (mutationStmt.HI == Opcodes.LOOKUPSWITCH) {
                    int[] keys = new int[mutationStmt.TPS.size()];
                    for (int i = 0; i < keys.length; i++) {
                        keys[i] = i;
                    }
                    Label[] labels = new Label[mutationStmt.TPS.size()];
                    int idx = 0;
                    for (String tp : mutationStmt.labelDict.keySet()) {
                        labels[idx] = mutationStmt.getLabel(tp);
                        idx += 1;
                    }
                    mv.visitLookupSwitchInsn(labels[0], keys, labels);
                } else if (mutationStmt.HI == Opcodes.TABLESWITCH) {
                    Label[] labels = new Label[mutationStmt.TPS.size()];
                    int idx = 0;
                    for (String tp : mutationStmt.labelDict.keySet()) {
                        labels[idx] = mutationStmt.getLabel(tp);
                        idx += 1;
                    }
                    mv.visitTableSwitchInsn(0, 100, labels[0], labels);
                }
                skidLabelFlag = true;
                mv.visitLabel(l1);
                skidLabelFlag = false;
                mutationCount++;
            }

            if(methodDictionary.get(methodName).mutationDictionary.containsKey(labelCount)) {
                for (int ii = methodDictionary.get(methodName).mutationDictionary.get(labelCount).size() - 1; ii > -1; ii--) {
                    MutationStmt ms = methodDictionary.get(methodName).mutationDictionary.get(labelCount).get(ii);
                    if (ms.ID != mutationStmt.REMOVE) {
                        mv.visitIincInsn(loopVar[mutationCount], -1);      // decrement loopcount
                        mv.visitVarInsn(Opcodes.ILOAD, loopVar[mutationCount]);     // load loopcount onto stack
                        Label l1 = new Label();
                        mv.visitJumpInsn(Opcodes.IFLE, l1);             // if loopcount greater than 0
                        if (ms.HI == Opcodes.GOTO) {
                            mv.visitJumpInsn(Opcodes.GOTO, labelList[insnDict.get(ms.TPS.get(0)).labelIdx]);
                        } else if (ms.HI == Opcodes.ATHROW) {
                            mv.visitInsn(Opcodes.ATHROW);
                        } else if (ms.HI == Opcodes.RETURN) {
                            mv.visitInsn(Opcodes.RETURN);
                        } else if (ms.HI == Opcodes.LOOKUPSWITCH) {
                            int[] keys = new int[ms.TPS.size()];
                            for (int i = 0; i < keys.length; i++) {
                                keys[i] = i;
                            }
                            Label[] labels = new Label[ms.TPS.size()];
                            int idx = 0;
                            for (String tp : ms.labelDict.keySet()) {
                                labels[idx] = ms.getLabel(tp);
                                idx += 1;
                            }
                            mv.visitLookupSwitchInsn(labels[0], keys, labels);
                        } else if (ms.HI == Opcodes.TABLESWITCH) {
                            Label[] labels = new Label[ms.TPS.size()];
                            int idx = 0;
                            for (String tp : ms.labelDict.keySet()) {
                                labels[idx] = ms.getLabel(tp);
                                idx += 1;
                            }
                            mv.visitTableSwitchInsn(0, 100, labels[0], labels);
                        }
                        skidLabelFlag = true;
                        mv.visitLabel(l1);
                        skidLabelFlag = false;
                        mutationCount++;
                    }
                }
            }
        }

        @Override
        public void visitCode() {
            for (int i=0;i<methodDictionary.get(methodName).labelCount;i++){
                labelList[i] = new Label();
            }
            mv.visitCode();
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            InsnStmt is = new InsnStmt("IincInsn", var+" "+increment, methodName, insnCount, true);
            instrument(is.identifier());
            //instrumentLabel(is.identifier());
//            if(methodDictionary.get(methodName).mutationDictionary.containsKey(is.identifier()) || mutationStmt.HP.equals(is.identifier())) {
//                mutate(is.identifier());
//            }
            mv.visitIincInsn(var, increment);
        }

        @Override
        public void visitInsn(int opcode) {
            InsnStmt is = new InsnStmt("Insn", opcode+"", methodName, insnCount, false);
            instrument(is.identifier());
            //instrumentLabel(is.identifier());
//            if(methodDictionary.get(methodName).mutationDictionary.containsKey(is.identifier()) || mutationStmt.HP.equals(is.identifier())) {
//                mutate(is.identifier());
//            }
            mv.visitInsn(opcode);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            InsnStmt is = new InsnStmt("InvokeDynamicInsn", name+" "+descriptor+" "+bootstrapMethodHandle+" "+ Arrays.toString(bootstrapMethodArguments), methodName, insnCount, false);
            instrument(is.identifier());
            //instrumentLabel(is.identifier());
//            if(methodDictionary.get(methodName).mutationDictionary.containsKey(is.identifier()) || mutationStmt.HP.equals(is.identifier())) {
//                mutate(is.identifier());
//            }
            mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

        @Override
        public void visitLabel(Label label) {
            mv.visitLabel(label);
            if(skidLabelFlag) {
                skidLabelFlag = false;
            } else {
                if (methodDictionary.get(methodName).mutationDictionary.containsKey(labelCount) || mutationStmt.HP == labelCount) {
                    mutate();
                }

                if (labelCount >= 0 && labelCount < labelList.length) {
                    mv.visitLabel(labelList[labelCount]);
                }
                labelCount++;
            }
        }

        @Override
        public void visitLdcInsn(Object value) {
            InsnStmt is = new InsnStmt("LdcInsn", value.toString(), methodName, insnCount, false);
            instrument(is.identifier());
            //instrumentLabel(is.identifier());
//            if(methodDictionary.get(methodName).mutationDictionary.containsKey(is.identifier()) || mutationStmt.HP.equals(is.identifier())) {
//                mutate(is.identifier());
//            }
            mv.visitLdcInsn(value);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            InsnStmt is = new InsnStmt("MethodInsn", opcode+" "+owner+" "+name+" "+descriptor+" "+isInterface, methodName, insnCount, false);
            instrument(is.identifier());
            //instrumentLabel(is.identifier());
//            if(methodDictionary.get(methodName).mutationDictionary.containsKey(is.identifier()) || mutationStmt.HP.equals(is.identifier())) {
//                mutate(is.identifier());
//            }
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimension) {
            InsnStmt is = new InsnStmt("MultiANewArrayInsn", descriptor+" "+numDimension, methodName, insnCount, false);
            instrument(is.identifier());
            //instrumentLabel(is.identifier());
//            if(methodDictionary.get(methodName).mutationDictionary.containsKey(is.identifier()) || mutationStmt.HP.equals(is.identifier())) {
//                mutate(is.identifier());
//            }
            mv.visitMultiANewArrayInsn(descriptor, numDimension);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            InsnStmt is = new InsnStmt("TypeInsn", opcode+" "+type, methodName, insnCount, false);
            instrument(is.identifier());
            //instrumentLabel(is.identifier());
//            if(methodDictionary.get(methodName).mutationDictionary.containsKey(is.identifier()) || mutationStmt.HP.equals(is.identifier())) {
//                mutate(is.identifier());
//            }
            mv.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            InsnStmt is = new InsnStmt("VarInsn", opcode+" "+var, methodName, insnCount, false);
            instrument(is.identifier());
            if (opcode == Opcodes.ISTORE && loopVarCount < loopVar.length) {
                loopVar[loopVarCount] = var;
            }
            loopVarCount++;
            //instrumentLabel(is.identifier());
//            if(methodDictionary.get(methodName).mutationDictionary.containsKey(is.identifier()) || mutationStmt.HP.equals(is.identifier())) {
//                mutate(is.identifier());
//            }
            mv.visitVarInsn(opcode, var);
        }

        @Override
        public void visitEnd() {
            mv.visitEnd();
        }
    }
    private class MutatingClassVisitor extends ClassVisitor {
        public MutationStmt mutationStmt;
        public MutatingClassVisitor(ClassVisitor cv, MutationStmt ms) {
            super(Opcodes.ASM9, cv);
            mutationStmt = ms;
            methodLabelDictionary = new Hashtable<>();
        }

        @Override
        public MethodVisitor visitMethod(int access, String name,
                                         String desc, String signature, String[] exceptions) {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            if(methodDictionary.get(name).mutationCount > 1 || mutationStmt.METHOD.equals(name)) {
                return new ClassParser.MutatingMethodVisitor(name, mutationStmt, mv);
            } else {
                return mv;
            }
        }
    }

    public ClassParser() {
        mutationCount = 0;
        methodDictionary = new Hashtable<>();
        parsiveCV = new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name,
                                             String desc, String signature, String[] exceptions) {
                return new ParsiveMethodVisitor(name);
            }
        };
        seedInsnSet = new HashSet<>();
        totalLivecodeSet = new HashSet<>();
        curLivecodeList = new ArrayList<>();
        insnDict = new Hashtable<>();
        curCoverSeedVal = 0.0;
    }

    /**
     * Method to parse the seed class.
     *
     * @param in            the InputStream of a class file
     * @throws IOException  throws IOException
     */
    public void parseClass(InputStream in) throws IOException {
        ClassReader cr = new ClassReader(in);
        cr.accept(parsiveCV, 0);
    }

    /**
     * Method to generate a mutant bytecode.
     *
     * @param ms            the mutationStmt to apply
     * @return              byte[] representing the mutant class
     * @throws IOException  throws IOException
     */
    public byte[] mutateClass(MutationStmt ms) throws IOException {
        // get seed class
        InputStream in = new FileInputStream(Config.SEED_DIR+Config.SEED_CLASS+Config.CLASS_EXT);

        // instrument all loopcount variables
        ClassReader cr = new ClassReader(in);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        cr.accept(new VariableClassVisitor(cw, ms), ClassReader.EXPAND_FRAMES);

        // instrument all mutations
        cr = new ClassReader(cw.toByteArray());
        cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        cr.accept(new MutatingClassVisitor(cw, ms), ClassReader.EXPAND_FRAMES);

        return cw.toByteArray();
    }

    /**
     * Method to run instrumentation on a class.
     *
     * @param in    the InputStream of a class file
     * @return      byte[] representing the instrumented class
     * @throws IOException  throws IOException
     */
    public byte[] instrumentClass(InputStream in) throws IOException {
        ClassReader cr = new ClassReader(in);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        cr.accept(new InstrumentalClassVisitor(cw), 0);

        return cw.toByteArray();
    }

    public void getLivecodeAgent(String filePath, String className) throws IOException {
        InputStream run_in = new FileInputStream(filePath+className+Config.CLASS_EXT);
        FileUtils.writeByteArrayToFile(new File(Config.SEED_CLASS+Config.CLASS_EXT), instrumentClass(run_in));
        run_in.close();

        Socket socket = new Socket("localhost", Config.SERVER_PORT);
        OutputStream output = socket.getOutputStream();
        PrintWriter writer = new PrintWriter(output, true);
        String msg = "loadClass ";
        msg += System.getProperty("user.dir") + " ";
        msg += Config.SEED_CLASS + " ";
        msg += System.getProperty("user.dir")+"\\"+Config.SEED_CLASS+Config.CLASS_EXT;

        writer.println(msg);
    }

    /**
     * Method to get the livecode of a class. Uses instrumentClass.
     *
     * @param filePath      the file path class file is stored
     * @param className     the name of the class
     * @return              ArrayList of live instructions
     * @throws IOException  throws IOException
     */
    public ArrayList<String> getLivecode(String filePath, String className) throws IOException{
        InputStream run_in = new FileInputStream(filePath+className+Config.CLASS_EXT);
        FileUtils.writeByteArrayToFile(new File(Config.SEED_CLASS+Config.CLASS_EXT), instrumentClass(run_in));
        run_in.close();

        ArrayList<String> executedInsn = new ArrayList<>();
        String runCmd = "java -cp " + Config.JAR_FILE + " " + Config.SEED_CLASS_MAIN;

        // update jar file
        String jarCmd = "jar uf " + Config.JAR_FILE + " " + Config.SEED_CLASS+Config.CLASS_EXT;

        try {
            Process jarP = Runtime.getRuntime().exec(jarCmd);
            if(!jarP.waitFor(5, TimeUnit.SECONDS)) {
                jarP.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Process p = Runtime.getRuntime().exec(runCmd);
            if(!p.waitFor(5, TimeUnit.SECONDS)) {
                p.destroy();
            }

            final InputStream stdout = p.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

            String line;
            try {
                while ((line = br.readLine()) != null) {
                    if(line.contains(Config.INSN_ID)){
                        executedInsn.add(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    br.close();
                    stdout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return executedInsn;
    }

    public void generateMutant(MutationStmt ms) throws IOException {
        byte[] mutantBytecode = mutateClass(ms);
        FileUtils.writeByteArrayToFile(new File(Config.MUTANT_DIR+ms.CLASSNAME+Config.CLASS_EXT), mutantBytecode);
    }

    public double coverSeed(ArrayList<String> livecode) {
        double x = seedInsnSet.size();
        double y = 0.0;
        for(String insn : livecode) {
            if(seedInsnSet.contains(insn)) {
                y += 1.0;
            }
        }
        return y/x;
    }

    public double accValue(double gCov, double fCov) {
        return Math.min(1.0, Math.exp(Config.BETA * (fCov - gCov)));
    }

    public String selectMutant(MutationStmt ms) throws IOException {
        System.out.println("==========="+totalLivecodeSet.size());
        // get livecode set of ms
        ArrayList<String> msLivecode = getLivecode(Config.MUTANT_DIR, ms.CLASSNAME);
        System.out.println("==========="+msLivecode.size());
//        for (String insn : msLivecode) {
//            System.out.println(insn);
//        }
        // calculate coverage of new mutant
        double covVal = coverSeed(msLivecode);

        if(covVal > 0.0) {
            // get a random value from 0.0 and 1.0
            double randVal = new Random(System.currentTimeMillis()).nextDouble();
            double accVal = accValue(covVal, curCoverSeedVal);

            if(accVal > randVal) {
                totalLivecodeSet.addAll(msLivecode);
                curLivecodeList = msLivecode;
                curCoverSeedVal = covVal;
                mutationCount += 1;
                methodDictionary.get(ms.METHOD).addMutation(ms);
                return Config.ACC;
            } else {
                return Config.REJ;
            }
        } else {
            return Config.NONLIVE;
        }
    }
}
