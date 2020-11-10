import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;


/*
 * This class is responsible for taking a seed class and parse it into a list of methods.
 * Methods will be counted and numbered so that random program can pick a method to mutate later.
 * Methods are objects holding method name, mutation information and labeled statement information.
 */
public class ClassGenerator {
    public ClassVisitor cv;
    public Hashtable<String, Method> methodDictionary;

    class GenerativeMethodVisitor extends MethodVisitor {
        String methodName;

        public GenerativeMethodVisitor(String methodName) {
            super(Opcodes.ASM9);
            this.methodName = methodName;
        }

        @Override
        public void visitLabel(Label label) {
            if(methodDictionary.get(methodName).mutationDictionary.contains(label.toString())) {
                // inject mutation
            }
            super.visitLabel(label);
        }
    }

    public ClassGenerator(ArrayList<Method> methodList) {
        methodDictionary = new Hashtable<>();
        for(int i=0;i<methodList.size();i++) {
            methodDictionary.put(methodList.get(i).methodName, methodList.get(i));
        }

        this.cv = new ClassVisitor(Opcodes.ASM9) {
            /**
             * Called when a class is visited. This is the method called first
             */
            @Override
            public void visit(int version, int access, String name,
                              String signature, String superName, String[] interfaces) {
                System.out.println("Visiting class: "+name);
                System.out.println("Class Major Version: "+version);
                System.out.println("Super class: "+superName);
                super.visit(version, access, name, signature, superName, interfaces);
            }

            /**
             *Invoked when a class level annotation is encountered
             */
            @Override
            public AnnotationVisitor visitAnnotation(String desc,
                                                     boolean visible) {
                System.out.println("Annotation: "+desc);
                return super.visitAnnotation(desc, visible);
            }

            /**
             * When a class attribute is encountered
             */
            @Override
            public void visitAttribute(Attribute attr) {
                System.out.println("Class Attribute: "+attr.type);
                super.visitAttribute(attr);
            }

            /**
             * When a field is encountered
             */
            @Override
            public FieldVisitor visitField(int access, String name,
                                           String desc, String signature, Object value) {
                System.out.println("Field: "+name+" "+desc+" value:"+value);
                return super.visitField(access, name, desc, signature, value);
            }


            @Override
            public void visitEnd() {
                System.out.println("Method ends here");
                super.visitEnd();
            }

            /**
             * When a method is encountered
             */
            @Override
            public MethodVisitor visitMethod(int access, String name,
                                             String desc, String signature, String[] exceptions) {
                return new GenerativeMethodVisitor(name);
            }
        };
    }

    public void readClass(InputStream in) throws IOException {
        ClassReader cr = new ClassReader(in);
        cr.accept(this.cv, 0);
    }
}
