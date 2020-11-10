import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
    public ArrayList<HashSet<String>> livecodeSetList = new ArrayList<>();

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
//            System.out.println(insn);
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
            InsnStmt is = new InsnStmt("InvokeDynamicInsn", name+" "+descriptor+" "+bootstrapMethodHandle+" "+bootstrapMethodArguments, methodName, insnCount, false);
            record(is);
            super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            InsnStmt is = new InsnStmt("JumpInsn", opcode+label.toString(), methodName, insnCount, false);
            record(is);
            super.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitLdcInsn(Object value) {
            InsnStmt is = new InsnStmt("LdcInsn", value.toString(), methodName, insnCount, false);
            record(is);
            super.visitLdcInsn(value);
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            InsnStmt is = new InsnStmt("LookupSwitchInsn", dflt.toString()+" "+keys.toString()+" "+labels.toString(), methodName, insnCount, false);
            record(is);
            super.visitLookupSwitchInsn(dflt, keys, labels);
        }

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

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            InsnStmt is = new InsnStmt("TableSwitchInsn", min+" "+max+" "+dflt.toString()+" "+labels.toString(), methodName, insnCount, false);
            record(is);
            super.visitTableSwitchInsn(min, max, dflt, labels);
        }

//        @Override
//        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
//            InsnStmt is = new InsnStmt("TryCatchBlock", start+" "+end+" "+handler+" "+type, methodName, insnCount, false);
//            record(is);
//            super.visitTryCatchBlock(start, end, handler, type);
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

        public void visitEnd() {
            methodCount++;
        }
    }

    private class InstrumentalMethodVisitor extends MethodVisitor {
        String methodName;
        int insnCount;

        public InstrumentalMethodVisitor(String methodName, MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
            this.methodName = methodName;
            insnCount = 0;
        }

        public void instrument(String insn) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn(insn);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            insnCount += 1;
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
            InsnStmt is = new InsnStmt("InvokeDynamicInsn", name+" "+descriptor+" "+bootstrapMethodHandle+" "+bootstrapMethodArguments, methodName, insnCount, false);
            instrument(is.identifier());
            mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            InsnStmt is = new InsnStmt("JumpInsn", opcode+label.toString(), methodName, insnCount, false);
            instrument(is.identifier());
            mv.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitLdcInsn(Object value) {
            InsnStmt is = new InsnStmt("LdcInsn", value.toString(), methodName, insnCount, false);
            instrument(is.identifier());
            mv.visitLdcInsn(value);
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            InsnStmt is = new InsnStmt("LookupSwitchInsn", dflt.toString()+" "+keys.toString()+" "+labels.toString(), methodName, insnCount, false);
            instrument(is.identifier());
            mv.visitLookupSwitchInsn(dflt, keys, labels);
        }

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

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            InsnStmt is = new InsnStmt("TableSwitchInsn", min+" "+max+" "+dflt.toString()+" "+labels.toString(), methodName, insnCount, false);
            instrument(is.identifier());
            mv.visitTableSwitchInsn(min, max, dflt, labels);
        }

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
    }

    public void parseClass(InputStream in) throws IOException {
        ClassReader cr = new ClassReader(in);
        cr.accept(parsiveCV, 0);
    }

    public byte[] instrumentClass(InputStream in) throws IOException {
        ClassReader cr = new ClassReader(in);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        cr.accept(new InstrumentalClassVisitor(cw), 0);

        return cw.toByteArray();
    }
}
