package io.mpjsons.impl.serializer;

import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;
import java.lang.classfile.instruction.SwitchCase;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static java.lang.constant.ConstantDescs.*;

/**
 * Generates a hidden class implementing io.mpjsons.impl.serializer.FieldReaders for the given fields (see FieldReaders.scala).
 * Written in Java because the Scala 2.13 compiler cannot read the java.lang.classfile API.
 */
public final class FieldReadersGenerator {

    private FieldReadersGenerator() {
    }

    private static final AtomicLong COUNTER = new AtomicLong();

    private static final ClassDesc METHOD_HANDLE = ClassDesc.of(MethodHandle.class.getName());
    private static final ClassDesc METHOD_HANDLES = ClassDesc.of(MethodHandles.class.getName());
    private static final ClassDesc LOOKUP = ClassDesc.of(MethodHandles.Lookup.class.getName());
    private static final ClassDesc ILLEGAL_STATE = ClassDesc.of(IllegalStateException.class.getName());

    /** One method of FieldReaders: its name, value type and the return instruction for that type. */
    private record ReaderMethod(String name, Class<?> valueClass, ClassDesc valueDesc, Consumer<CodeBuilder> ret) {
    }

    private static final List<ReaderMethod> PRIMITIVE_METHODS = List.of(
            new ReaderMethod("getInt", int.class, CD_int, CodeBuilder::ireturn),
            new ReaderMethod("getLong", long.class, CD_long, CodeBuilder::lreturn),
            new ReaderMethod("getDouble", double.class, CD_double, CodeBuilder::dreturn),
            new ReaderMethod("getFloat", float.class, CD_float, CodeBuilder::freturn),
            new ReaderMethod("getBoolean", boolean.class, CD_boolean, CodeBuilder::ireturn),
            new ReaderMethod("getShort", short.class, CD_short, CodeBuilder::ireturn),
            new ReaderMethod("getByte", byte.class, CD_byte, CodeBuilder::ireturn),
            new ReaderMethod("getChar", char.class, CD_char, CodeBuilder::ireturn));

    /** Available for every field; a primitive value is boxed, like Field.get does. */
    private static final ReaderMethod OBJECT_METHOD = new ReaderMethod("getObject", Object.class, CD_Object, CodeBuilder::areturn);

    public static Object generate(Field[] fields, Class<?> readersInterface) throws ReflectiveOperationException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        // class data: for field i, element 2*i reads the field as its own type, element 2*i+1 reads it as Object
        List<Object> getters = new ArrayList<>(fields.length * 2);
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                throw new IllegalArgumentException("Static field " + field);
            }
            MethodHandle getter = lookup.unreflectGetter(field);
            Class<?> valueClass = field.getType().isPrimitive() ? field.getType() : Object.class;
            getters.add(getter.asType(MethodType.methodType(valueClass, Object.class)));
            getters.add(getter.asType(MethodType.methodType(Object.class, Object.class)));
        }

        ClassDesc thisClass = ClassDesc.of(readersInterface.getPackageName() + "." + readersInterface.getSimpleName() + "$Generated" + COUNTER.incrementAndGet());

        byte[] bytes = ClassFile.of().build(thisClass, cb -> {
            cb.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC);
            cb.withInterfaceSymbols(ClassDesc.of(readersInterface.getName()));

            for (int i = 0; i < getters.size(); i++) {
                cb.withField("h" + i, METHOD_HANDLE, ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC | ClassFile.ACC_FINAL);
            }

            cb.withMethodBody("<clinit>", MethodTypeDesc.of(CD_void), ClassFile.ACC_STATIC, code -> {
                for (int i = 0; i < getters.size(); i++) {
                    code.invokestatic(METHOD_HANDLES, "lookup", MethodTypeDesc.of(LOOKUP));
                    code.ldc(DEFAULT_NAME);
                    code.ldc(METHOD_HANDLE);
                    code.loadConstant(i);
                    code.invokestatic(METHOD_HANDLES, "classDataAt", MethodTypeDesc.of(CD_Object, LOOKUP, CD_String, CD_Class, CD_int));
                    code.checkcast(METHOD_HANDLE);
                    code.putstatic(thisClass, "h" + i, METHOD_HANDLE);
                }
                code.return_();
            });

            cb.withMethodBody("<init>", MethodTypeDesc.of(CD_void), ClassFile.ACC_PUBLIC, code -> {
                code.aload(0);
                code.invokespecial(CD_Object, "<init>", MethodTypeDesc.of(CD_void));
                code.return_();
            });

            for (ReaderMethod method : PRIMITIVE_METHODS) {
                List<Integer> handles = new ArrayList<>();
                List<Integer> indices = new ArrayList<>();
                for (int i = 0; i < fields.length; i++) {
                    if (fields[i].getType() == method.valueClass()) {
                        indices.add(i);
                        handles.add(2 * i);
                    }
                }
                addReaderMethod(cb, thisClass, method, indices, handles);
            }
            List<Integer> allIndices = new ArrayList<>();
            List<Integer> objectHandles = new ArrayList<>();
            for (int i = 0; i < fields.length; i++) {
                allIndices.add(i);
                objectHandles.add(2 * i + 1);
            }
            addReaderMethod(cb, thisClass, OBJECT_METHOD, allIndices, objectHandles);
        });

        Class<?> hiddenClass = lookup.defineHiddenClassWithClassData(bytes, List.copyOf(getters), true).lookupClass();
        return hiddenClass.getDeclaredConstructor().newInstance();
    }

    /** `name(Object target, int index)`: a switch on the index calling the constant getter `h<handle>` of that field. */
    private static void addReaderMethod(java.lang.classfile.ClassBuilder cb, ClassDesc thisClass, ReaderMethod method,
                                        List<Integer> indices, List<Integer> handles) {
        MethodTypeDesc getterType = MethodTypeDesc.of(method.valueDesc(), CD_Object);
        cb.withMethodBody(method.name(), MethodTypeDesc.of(method.valueDesc(), CD_Object, CD_int), ClassFile.ACC_PUBLIC, code -> {
            Label fail = code.newLabel();
            if (!indices.isEmpty()) {
                List<Label> labels = new ArrayList<>();
                List<SwitchCase> cases = new ArrayList<>();
                for (int index : indices) {
                    Label label = code.newLabel();
                    labels.add(label);
                    cases.add(SwitchCase.of(index, label));
                }
                code.iload(2);
                code.lookupswitch(fail, cases);
                for (int j = 0; j < indices.size(); j++) {
                    code.labelBinding(labels.get(j));
                    code.getstatic(thisClass, "h" + handles.get(j), METHOD_HANDLE);
                    code.aload(1);
                    code.invokevirtual(METHOD_HANDLE, "invokeExact", getterType);
                    method.ret().accept(code);
                }
            }
            code.labelBinding(fail);
            code.new_(ILLEGAL_STATE);
            code.dup();
            code.ldc("No " + method.name() + " field with given index");
            code.invokespecial(ILLEGAL_STATE, "<init>", MethodTypeDesc.of(CD_void, CD_String));
            code.athrow();
        });
    }
}
