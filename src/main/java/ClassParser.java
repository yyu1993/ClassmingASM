import org.objectweb.asm.commons.LocalVariablesSorter;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;


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
    public double prevCoverSeed;
    public double curCoverSeed;

    public Hashtable<String, Hashtable<String, Label>> methodLabelDictionary;

    /**
     * Used to parse the seed class.
     */
    private class ParsiveMethodVisitor extends MethodVisitor {
        String methodName;
        InsnStmt curInsn;
        int insnCount;

        public ParsiveMethodVisitor(String methodName) {
            super(Opcodes.ASM9);
            this.methodName = methodName;
            Method m = new Method(methodName);
            methodDictionary.put(methodName, m);
            curInsn = null;
            insnCount = 0;
        }

        public void record(InsnStmt insn) {
            seedInsnSet.add(insn.identifier());
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
            InsnStmt is = new InsnStmt("IincInsn", var+" "+increment, methodName, insnCount, true);
            record(is);
            super.visitIincInsn(var, increment);
        }

        @Override
        public void visitInsn(int opcode) {
            InsnStmt is = new InsnStmt("Insn", opcode+"", methodName, insnCount, checkDefUse(opcode));
            record(is);
            super.visitInsn(opcode);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            InsnStmt is = new InsnStmt("InvokeDynamicInsn", name+" "+descriptor+" "+bootstrapMethodHandle+" "+ Arrays.toString(bootstrapMethodArguments), methodName, insnCount, false);
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
            InsnStmt is = new InsnStmt("LdcInsn", value.toString(), methodName, insnCount, false);
            record(is);
            super.visitLdcInsn(value);
        }

//        @Override
//        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
//            InsnStmt is = new InsnStmt("LookupSwitchInsn", dflt.toString()+" "+keys.toString()+" "+labels.toString(), methodName, insnCount, false);
//            record(is);
//            super.visitLookupSwitchInsn(dflt, keys, labels);
//        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            InsnStmt is = new InsnStmt("MethodInsn", opcode+" "+owner+" "+name+" "+descriptor+" "+isInterface, methodName, insnCount, checkDefUse(opcode));
            record(is);
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimension) {
            InsnStmt is = new InsnStmt("MultiANewArrayInsn", descriptor+" "+numDimension, methodName, insnCount, false);
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
            InsnStmt is = new InsnStmt("TypeInsn", opcode+" "+type, methodName, insnCount, checkDefUse(opcode));
            record(is);
            super.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            InsnStmt is = new InsnStmt("VarInsn", opcode+" "+var, methodName, insnCount, checkDefUse(opcode));
            record(is);
            super.visitVarInsn(opcode, var);
        }

        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            methodDictionary.get(methodName).variableCount++;
            super.visitLocalVariable(name, descriptor, signature, start, end, index);
        }

        public void visitEnd() {
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

            for (MutationStmt ms : methodDictionary.get(methodName).mutationList) {
                mv.visitIntInsn(Opcodes.BIPUSH, Config.LOOP_COUNT); // push int value to stack
                int loopVar = newLocal(Type.INT_TYPE);              // create new local variable
                mv.visitVarInsn(Opcodes.LSTORE, loopVar);           // store value on stack to this variable
                ms.loopVar = loopVar;
                varCount++;
            }

            if(mutationStmt.REMOVE < 0) {
                mv.visitIntInsn(Opcodes.BIPUSH, Config.LOOP_COUNT); // push int value to stack
                int loopVar = newLocal(Type.INT_TYPE);              // create new local variable
                mv.visitVarInsn(Opcodes.LSTORE, loopVar);           // store value on stack to this variable
                mutationStmt.loopVar = loopVar;
                varCount++;
            }
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

        public MutatingMethodVisitor(String methodName, MutationStmt ms, MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
            this.methodName = methodName;
            insnCount = 0;
            mutationCount = 0;
            mutationStmt = ms;
        }

        public void instrument(String insn) {
            if(seedInsnSet.contains(insn)) {
                insnCount += 1;
            }
        }

        public void mutate(String insn) {
            if(methodDictionary.get(methodName).mutationDictionary.contains(insn)) {
                for(MutationStmt ms : methodDictionary.get(methodName).mutationDictionary.get(insn)) {
                    if(ms.ID != mutationStmt.REMOVE) {
                        for(String tp : ms.TPS) {
                            if (ms.HI == Opcodes.GOTO) {
                                mv.visitIincInsn(ms.loopVar, -1);      // decrement loopcount
                                mv.visitVarInsn(Opcodes.ILOAD, ms.loopVar);     // load loopcount onto stack
                                Label l1 = new Label();
                                mv.visitJumpInsn(Opcodes.IFLE, l1);             // if loopcount greater than 0
                                mv.visitJumpInsn(Opcodes.GOTO, methodLabelDictionary.get(methodName).get(tp));
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            InsnStmt is = new InsnStmt("IincInsn", var+" "+increment, methodName, insnCount, true);
            insnCount++;
            if(methodDictionary.get(methodName).mutationDictionary.contains(is.identifier()) || mutationStmt.HP.equals(is.identifier())) {
                mutate(is.identifier());
            }
            mv.visitIincInsn(var, increment);
        }

        @Override
        public void visitInsn(int opcode) {
            InsnStmt is = new InsnStmt("Insn", opcode+"", methodName, insnCount, false);
            instrument(is.identifier());
            if(methodDictionary.get(methodName).mutationDictionary.contains(is.identifier()) || mutationStmt.HP.equals(is.identifier())) {
                mutate(is.identifier());
            }
            mv.visitInsn(opcode);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            InsnStmt is = new InsnStmt("InvokeDynamicInsn", name+" "+descriptor+" "+bootstrapMethodHandle+" "+ Arrays.toString(bootstrapMethodArguments), methodName, insnCount, false);
            instrument(is.identifier());
            if(methodDictionary.get(methodName).mutationDictionary.contains(is.identifier()) || mutationStmt.HP.equals(is.identifier())) {
                mutate(is.identifier());
            }
            mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

        @Override
        public void visitLdcInsn(Object value) {
            InsnStmt is = new InsnStmt("LdcInsn", value.toString(), methodName, insnCount, false);
            instrument(is.identifier());
            if(methodDictionary.get(methodName).mutationDictionary.contains(is.identifier()) || mutationStmt.HP.equals(is.identifier())) {
                mutate(is.identifier());
            }
            mv.visitLdcInsn(value);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            InsnStmt is = new InsnStmt("MethodInsn", opcode+" "+owner+" "+name+" "+descriptor+" "+isInterface, methodName, insnCount, false);
            instrument(is.identifier());
            if(methodDictionary.get(methodName).mutationDictionary.contains(is.identifier()) || mutationStmt.HP.equals(is.identifier())) {
                mutate(is.identifier());
            }
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimension) {
            InsnStmt is = new InsnStmt("MultiANewArrayInsn", descriptor+" "+numDimension, methodName, insnCount, false);
            instrument(is.identifier());
            if(methodDictionary.get(methodName).mutationDictionary.contains(is.identifier()) || mutationStmt.HP.equals(is.identifier())) {
                mutate(is.identifier());
            }
            mv.visitMultiANewArrayInsn(descriptor, numDimension);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            InsnStmt is = new InsnStmt("TypeInsn", opcode+" "+type, methodName, insnCount, false);
            instrument(is.identifier());
            if(methodDictionary.get(methodName).mutationDictionary.contains(is.identifier()) || mutationStmt.HP.equals(is.identifier())) {
                mutate(is.identifier());
            }
            mv.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            InsnStmt is = new InsnStmt("VarInsn", opcode+" "+var, methodName, insnCount, false);
            instrument(is.identifier());
            if(methodDictionary.get(methodName).mutationDictionary.contains(is.identifier()) || mutationStmt.HP.equals(is.identifier())) {
                mutate(is.identifier());
            }
            mv.visitVarInsn(opcode, var);
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
        prevCoverSeed = 0.0;
        curCoverSeed = 0.0;
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

        // instrument all labels
        ClassReader cr = new ClassReader(in);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        cr.accept(new LabelingClassVisitor(cw, ms), 0);

        System.out.println(cw.toByteArray().length);

        // instrument all loopcount variables
        cr = new ClassReader(cw.toByteArray());
        cw = new ClassWriter(cr, ClassReader.EXPAND_FRAMES);
        cr.accept(new VariableClassVisitor(cw, ms), ClassReader.EXPAND_FRAMES);

        System.out.println(cw.toByteArray().length);

        // instrument all mutations
        cr = new ClassReader(cw.toByteArray());
        cw = new ClassWriter(cr, ClassReader.EXPAND_FRAMES);
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

    /**
     * Method to get the livecode of a class. Uses instrumentClass.
     *
     * @param filePath      the file path class file is stored
     * @param className     the name of the class
     * @return              ArrayList of live instructions
     * @throws IOException  throws IOException
     */
    public ArrayList<String> getLivecode(String filePath, String className) throws IOException {
        InputStream run_in = new FileInputStream(filePath+className+Config.CLASS_EXT);
        FileUtils.writeByteArrayToFile(new File(Config.RUN_DIR+className+Config.CLASS_EXT), instrumentClass(run_in));
        run_in.close();

        ArrayList<String> executedInsn = new ArrayList<>();
        String runCmd = "java -cp ./run " + className;

        try {
            Process p = Runtime.getRuntime().exec(runCmd);

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
            p.waitFor();
            p.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return executedInsn;
    }

    public void generateMutant(MutationStmt ms) throws IOException {
        byte[] mutantBytecode = mutateClass(ms);
        FileUtils.writeByteArrayToFile(new File(Config.MUTANT_DIR+ms.CLASSNAME+Config.CLASS_EXT), mutantBytecode);
    }

    public void coverSeed() {
        double x = seedInsnSet.size();
        double y = new HashSet<>(curLivecodeList).size();

        prevCoverSeed = curCoverSeed;
        curCoverSeed = x/y;
        //return x/y;
    }
}
