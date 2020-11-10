import org.objectweb.asm.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class TraceClassGenerator extends ClassVisitor{
    public TraceClassGenerator(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name,
                                     String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        return new TraceClassGenerator.LogMethodVisitor(name, mv);
    }

    private static class LogMethodVisitor extends MethodVisitor {
        String methodName;

        public LogMethodVisitor(String methodName, MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
            this.methodName = methodName;
        }

        @Override
        public void visitLabel(Label label) {
            super.visitLabel(label);
            System.out.println(methodName + "-" + label.toString());
            mv.visitLdcInsn(methodName + "-" + label.toString());
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }
    }

    public static byte[] generateClass(InputStream in) throws IOException {
        ClassReader cr = new ClassReader(in);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new TraceClassGenerator(cw), ClassReader.EXPAND_FRAMES);

        return cw.toByteArray();
    }
}
