package io.mpjsons.impl.deserializer;

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
 * Generates a hidden class implementing io.mpjsons.impl.deserializer.FieldWriters for the given fields (see FieldWriters.scala).
 * Written in Java because the Scala 2.13 compiler cannot read the java.lang.classfile API.
 */
public final class FieldWritersGenerator {

    private FieldWritersGenerator() {
    }

    private static final AtomicLong COUNTER = new AtomicLong();

    private static final ClassDesc METHOD_HANDLE = ClassDesc.of(MethodHandle.class.getName());
    private static final ClassDesc METHOD_HANDLES = ClassDesc.of(MethodHandles.class.getName());
    private static final ClassDesc LOOKUP = ClassDesc.of(MethodHandles.Lookup.class.getName());
    private static final ClassDesc ILLEGAL_STATE = ClassDesc.of(IllegalStateException.class.getName());

    /** One method of FieldWriters: its name, value type and how the value is loaded from local slot 3. */
    private record WriterMethod(String name, Class<?> valueClass, ClassDesc valueDesc, Consumer<CodeBuilder> load) {
    }

    private static final List<WriterMethod> METHODS = List.of(
            new WriterMethod("setInt", int.class, CD_int, code -> code.iload(3)),
            new WriterMethod("setLong", long.class, CD_long, code -> code.lload(3)),
            new WriterMethod("setDouble", double.class, CD_double, code -> code.dload(3)),
            new WriterMethod("setFloat", float.class, CD_float, code -> code.fload(3)),
            new WriterMethod("setBoolean", boolean.class, CD_boolean, code -> code.iload(3)),
            new WriterMethod("setShort", short.class, CD_short, code -> code.iload(3)),
            new WriterMethod("setByte", byte.class, CD_byte, code -> code.iload(3)),
            new WriterMethod("setChar", char.class, CD_char, code -> code.iload(3)),
            new WriterMethod("setObject", Object.class, CD_Object, code -> code.aload(3)));

    private static WriterMethod methodFor(Field field) {
        for (WriterMethod method : METHODS) {
            if (method.valueClass() == field.getType()) {
                return method;
            }
        }
        return METHODS.get(METHODS.size() - 1);
    }

    public static Object generate(Field[] fields, Class<?> writersInterface) throws ReflectiveOperationException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        List<Object> setters = new ArrayList<>(fields.length);
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                throw new IllegalArgumentException("Static field " + field);
            }
            MethodType type = MethodType.methodType(void.class, Object.class, methodFor(field).valueClass());
            setters.add(lookup.unreflectSetter(field).asType(type));
        }

        ClassDesc thisClass = ClassDesc.of(writersInterface.getPackageName() + "." + writersInterface.getSimpleName() + "$Generated" + COUNTER.incrementAndGet());

        byte[] bytes = ClassFile.of().build(thisClass, cb -> {
            cb.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC);
            cb.withInterfaceSymbols(ClassDesc.of(writersInterface.getName()));

            for (int i = 0; i < fields.length; i++) {
                cb.withField("h" + i, METHOD_HANDLE, ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC | ClassFile.ACC_FINAL);
            }

            cb.withMethodBody("<clinit>", MethodTypeDesc.of(CD_void), ClassFile.ACC_STATIC, code -> {
                for (int i = 0; i < fields.length; i++) {
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

            for (WriterMethod method : METHODS) {
                List<Integer> indices = new ArrayList<>();
                for (int i = 0; i < fields.length; i++) {
                    if (methodFor(fields[i]) == method) {
                        indices.add(i);
                    }
                }
                MethodTypeDesc setterType = MethodTypeDesc.of(CD_void, CD_Object, method.valueDesc());
                cb.withMethodBody(method.name(), MethodTypeDesc.of(CD_void, CD_Object, CD_int, method.valueDesc()), ClassFile.ACC_PUBLIC, code -> {
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
                            code.getstatic(thisClass, "h" + indices.get(j), METHOD_HANDLE);
                            code.aload(1);
                            method.load().accept(code);
                            code.invokevirtual(METHOD_HANDLE, "invokeExact", setterType);
                            code.return_();
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
        });

        Class<?> hiddenClass = lookup.defineHiddenClassWithClassData(bytes, List.copyOf(setters), true).lookupClass();
        return hiddenClass.getDeclaredConstructor().newInstance();
    }
}
