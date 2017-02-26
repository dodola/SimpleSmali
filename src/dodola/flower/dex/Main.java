package dodola.flower.dex;

import com.google.common.base.Strings;
import org.jf.dexlib2.*;
import org.jf.dexlib2.builder.*;
import org.jf.dexlib2.builder.instruction.*;
import org.jf.dexlib2.dexbacked.*;
import org.jf.dexlib2.dexbacked.raw.CodeItem;
import org.jf.dexlib2.dexbacked.reference.DexBackedFieldReference;
import org.jf.dexlib2.dexbacked.reference.DexBackedMethodReference;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.immutable.debug.ImmutableStartLocal;
import org.jf.dexlib2.util.ReferenceUtil;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

public class Main {

    public static void main(String[] args) {
        try {
            PrintStream printStream = System.out;
            DexBackedDexFile dexBackedDexFile =
                    DexFileFactory.loadDexFile("/Users/baidu/Downloads/app-debug/classes.dex", Opcodes.getDefault());
            Set<? extends DexBackedClassDef> classes = dexBackedDexFile.getClasses();
            for (DexBackedClassDef aClass : classes) {
                Indentation indent = new Indentation(0);

                printStream.println(classToString(aClass));
                printStream.println("{");
                indent.increment();

                Iterable<? extends DexBackedField> fields = aClass.getFields();
                for (DexBackedField field : fields) {
                    field.getAnnotations().forEach(
                            (Consumer<Annotation>) annotation -> annotionToString(printStream, annotation, indent));
                    printStream.print(indent.toString());
                    printStream.println(String.format("%s %s %s", accessFlagsToString(field.getAccessFlags()),
                            typeToString(field.getType()), field.getName()));
                }

                Iterable<? extends DexBackedMethod> methods = aClass.getMethods();
                methods.forEach((Consumer<DexBackedMethod>) dexBackedMethod -> {

                    Set<? extends Annotation> annotations = dexBackedMethod.getAnnotations();

                    annotations.forEach(
                            (Consumer<Annotation>) annotation -> annotionToString(printStream, annotation, indent));

                    List<? extends Set<? extends DexBackedAnnotation>> parameterAnnotations =
                            dexBackedMethod.getParameterAnnotations();

                    parameterAnnotations
                            .forEach((Consumer<Set<? extends DexBackedAnnotation>>) dexBackedAnnotations -> {
                                dexBackedAnnotations.forEach(new Consumer<DexBackedAnnotation>() {
                                    @Override public void accept(DexBackedAnnotation dexBackedAnnotation) {
                                        annotionToString(printStream, dexBackedAnnotation, indent);
                                    }
                                });

                            });
                    printStream.print(indent.toString());
                    LocalInfo localInfo = buildLocalInfo(dexBackedMethod);
                    printStream.print(methodToString(dexBackedMethod, localInfo));
                    printStream.println(" {");

                    DexBackedMethodImplementation methodImplementation = dexBackedMethod.getImplementation();
                    if (methodImplementation != null) {
                        indent.increment();
                        //                        System.out.println(localInfo);

                        MutableMethodImplementation mutableMethodImplementation =
                                new MutableMethodImplementation(methodImplementation);
                        GotoTable gotoTable = buildGotoTable(mutableMethodImplementation);
                        List<BuilderInstruction> instructions = mutableMethodImplementation.getInstructions();

                        for (BuilderInstruction builderInstruction : instructions) {
                            long codeAddress = builderInstruction.getLocation().getCodeAddress();
                            HashSet<String> targetsLabels = gotoTable.targets.get(codeAddress);
                            if (targetsLabels != null) {
                                printStream.println();
                                printStream.println(
                                        String.format("%s:%s", indent.toString(), String.join(", ", targetsLabels)));
                            }
                            if (builderInstruction.getOpcode() != Opcode.NOP) {
                                printStream.print(indent.toString());
                                printStream.println(opcodeToString(builderInstruction, gotoTable, localInfo));

                            }
                        }
                        indent.decrement();
                    }
                    printStream.print(indent.toString());
                    printStream.println("}");
                    printStream.println();
                });
                indent.decrement();
                printStream.println("}");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String classToString(DexBackedClassDef aclass) {
        String extendStr = "";

        if (!"Ljava/lang/Object;".equals(aclass.getSuperclass())) {
            extendStr = "extends " + formatTypeDescriptor(aclass.getSuperclass());
        }

        String implmentsStr = "";

        List<String> interfaces = aclass.getInterfaces();
        if (interfaces.size() > 0) {
            implmentsStr = "implements " + String.join(",", interfaces);
        }
        String accessStr = accessFlagsToString(aclass.getAccessFlags());

        if (!AccessFlags.INTERFACE.isSet(aclass.getAccessFlags()) && !AccessFlags.ENUM.isSet(aclass.getAccessFlags())) {
            accessStr += " class";
        }

        return String.format("%s %s %s %s", accessStr, formatTypeDescriptor(aclass.getType()), extendStr, implmentsStr);
    }

    private static String fieldsToString(DexBackedClassDef aclass) {
        Iterable<? extends DexBackedField> fields = aclass.getFields();
        for (DexBackedField field : fields) {

        }
        return "";
    }

    private static String accessFlagsToString(int flag) {
        List<String> access = new ArrayList<String>();

        if ((flag & AccessFlags.PUBLIC.getValue()) != 0)
            access.add("public");

        if ((flag & AccessFlags.PRIVATE.getValue()) != 0)
            access.add("private");

        if ((flag & AccessFlags.PROTECTED.getValue()) != 0)
            access.add("protected");

        if ((flag & AccessFlags.STATIC.getValue()) != 0)
            access.add("static");

        if ((flag & AccessFlags.FINAL.getValue()) != 0)
            access.add("final");

        if ((flag & AccessFlags.SYNCHRONIZED.getValue()) != 0)
            access.add("synchronized");

        if ((flag & AccessFlags.VOLATILE.getValue()) != 0)
            access.add("volatile");

        if ((flag & AccessFlags.BRIDGE.getValue()) != 0)
            access.add("bridge");

        if ((flag & AccessFlags.TRANSIENT.getValue()) != 0)
            access.add("transient");

        if ((flag & AccessFlags.VARARGS.getValue()) != 0)
            access.add("varargs");

        if ((flag & AccessFlags.NATIVE.getValue()) != 0)
            access.add("native");

        if ((flag & AccessFlags.INTERFACE.getValue()) != 0)
            access.add("interface");
        else if ((flag & AccessFlags.ABSTRACT.getValue()) != 0)
            access.add("abstract");

        if ((flag & AccessFlags.STRICTFP.getValue()) != 0)
            access.add("strictfp");

        if ((flag & AccessFlags.SYNTHETIC.getValue()) != 0)
            access.add("synthetic");

        if ((flag & AccessFlags.ANNOTATION.getValue()) != 0)
            access.add("annotation");

        if ((flag & AccessFlags.ENUM.getValue()) != 0)
            access.add("enum");

        if ((flag & AccessFlags.CONSTRUCTOR.getValue()) != 0)
            access.add("constructor");

        if ((flag & AccessFlags.DECLARED_SYNCHRONIZED.getValue()) != 0)
            access.add("synchronized");

        return String.join(" ", access);
    }

    private static LocalInfo buildLocalInfo(DexBackedMethod dexBackedMethod) {

        LocalInfo info = new LocalInfo();

        int insSize = getInsSize(dexBackedMethod);

        List<? extends MethodParameter> parameters = dexBackedMethod.getParameters();

        int argReg = getRegisterSize(dexBackedMethod) - insSize;//reg = resiter - ins
        //                        System.out.println("registerCount:" + methodImplementation.getRegisterCount());

        boolean isStatic = AccessFlags.STATIC.isSet(dexBackedMethod.getAccessFlags());

        if (!isStatic) {
            info.setName(argReg, "this");
            argReg++;
        }

        for (MethodParameter p : parameters) {
            int reg = argReg;

            if ("D".equals(p.getType()) || "J".equals(p.getType())) {
                argReg += 2;
            } else {
                argReg += 1;
            }
            if (!Strings.isNullOrEmpty(p.getName())) {
                info.setName(reg, p.getName());
            }
            //            System.out.println("pararms:====" + p.getName() + ",paramRegister:" + reg);
        }
        DexBackedMethodImplementation implementation = dexBackedMethod.getImplementation();
        if (implementation != null) {
            Iterable<? extends DebugItem> debugItems = implementation.getDebugItems();
            for (DebugItem debugItem : debugItems) {
                if (debugItem.getDebugItemType() == DebugItemType.START_LOCAL) {
                    //                System.out.println(
                    //                        "debugItemStartLocal:====" + ((ImmutableStartLocal) debugItem).getName() + ",register:"
                    //                                + ((ImmutableStartLocal) debugItem).getRegister());
                    info.setName(((ImmutableStartLocal) debugItem).getRegister(),
                            ((ImmutableStartLocal) debugItem).getName());
                }
                if (debugItem.getDebugItemType() == DebugItemType.START_LOCAL_EXTENDED) {
                    //                System.out.println(
                    //                        "debugItemStartLocalExt:====" + ((ImmutableStartLocal) debugItem).getName() + ",register:"
                    //                                + ((ImmutableStartLocal) debugItem).getRegister());
                    info.setName(((ImmutableStartLocal) debugItem).getRegister(),
                            ((ImmutableStartLocal) debugItem).getName());
                }
            }
        }
        return info;
    }

    private static int getInsSize(DexBackedMethod methodImplementation) {
        //用非正常手段获取insSize,
        int insSize = -1;
        //反射获取codeOffset
        try {
            Field codeOffsetField = DexBackedMethod.class.getDeclaredField("codeOffset");
            codeOffsetField.setAccessible(true);
            int codeOffset = (int) codeOffsetField.get(methodImplementation);
            int registerSize = methodImplementation.dexFile.readUbyte(codeOffset + CodeItem.REGISTERS_OFFSET);
            insSize = methodImplementation.dexFile.readUbyte(codeOffset + CodeItem.INS_OFFSET);
            //            System.out.println("insSize:" + insSize + "," + registerSize);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return insSize;
    }

    private static int getRegisterSize(DexBackedMethod methodImplementation) {
        //用非正常手段获取insSize,
        int registerSize = -1;
        //反射获取codeOffset
        try {
            Field codeOffsetField = DexBackedMethod.class.getDeclaredField("codeOffset");
            codeOffsetField.setAccessible(true);
            int codeOffset = (int) codeOffsetField.get(methodImplementation);
            registerSize = methodImplementation.dexFile.readUbyte(codeOffset + CodeItem.REGISTERS_OFFSET);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return registerSize;
    }

    private static String methodToString(DexBackedMethod dexBackedMethod, LocalInfo info) {
        String returnType = typeToString(dexBackedMethod.getReturnType());

        int insSize = getInsSize(dexBackedMethod);

        List<? extends MethodParameter> parameters =
                dexBackedMethod.getParameters();//被坑一笔这里的paramterssize不是inssize,是从debuginfo里获取来的,就是说这货在非静态方法下将this去掉了

        int argReg = getRegisterSize(dexBackedMethod) - insSize;//reg = resiter - ins
        //                        System.out.println("registerCount:" + methodImplementation.getRegisterCount());

        boolean isStatic = AccessFlags.STATIC.isSet(dexBackedMethod.getAccessFlags());

        if (!isStatic) {
            argReg++;
        }
        List<String> paramtersStr = new ArrayList<>();

        for (MethodParameter p : parameters) {
            int reg = argReg;

            if ("D".equals(p.getType()) || "J".equals(p.getType())) {
                argReg += 2;
            } else {
                argReg += 1;
            }
            paramtersStr.add(String.format("%s %s", typeToString(p.getType()), formatRegisterName(reg, info)));

        }

        return String.format("%s %s %s (%s)", AccessFlags.formatAccessFlagsForMethod(dexBackedMethod.getAccessFlags()),
                returnType, dexBackedMethod.getName(), String.join(",", paramtersStr));

    }

    /**
     * [v0, v1, ..., vn, this(vn+1)
     *
     * @param index
     * @param info
     * @return
     */
    private static String formatRegisterName(int index, LocalInfo info) {
        //        // 第一版本 直接按照寄存器地址拼出名称不直观
        //        System.out.println("====index:" + index);
        //
        //        int accessFlags = methodImplementation.method.getAccessFlags();
        //
        //        boolean isStatic = AccessFlags.STATIC.isSet(accessFlags);
        //        List<? extends MethodParameter> parameters = methodImplementation.method.getParameters();
        //
        //        int localcount = methodImplementation.getRegisterCount() - parameters.size() - (isStatic ?
        //                0 :
        //                1);//此处需要注意,paramters不包含this,所以非静态方法需要多减一个
        //
        //        //寄存器个数-入参个数
        //        if (index < localcount) {
        //            return String.format("v%d", index);
        //        }
        //        if (!isStatic) {//不是静态
        //            if (index == localcount) {
        //                return "this";
        //            }
        //            index--;
        //        }
        //
        //        return String.format("a%d", (index - localcount));
        ////第二版 从debuginfo中获取
        String name = info.getName(index);
        if (Strings.isNullOrEmpty(name)) {
            return String.format("v%d", index);
        } else {
            return name;
        }
    }

    private static String opcodeToString(BuilderInstruction builderInstruction,
           /* DexBackedMethodImplementation methodImplementation,*/ GotoTable gotoTable, LocalInfo localInfo) {

        Opcode opcode = builderInstruction.getOpcode();
        switch (opcode) {
            case CONST:
            case CONST_4:
            case CONST_16:
            case CONST_HIGH16:
            case CONST_WIDE:
            case CONST_WIDE_16:
            case CONST_WIDE_32:
            case CONST_WIDE_HIGH16: {
                int registerIndex = 0;
                String value = "";
                switch (opcode.format) {
                    case Format11n: {
                        BuilderInstruction11n builderInstruction1 = (BuilderInstruction11n) builderInstruction;
                        registerIndex = builderInstruction1.getRegisterA();
                        value = String.valueOf(builderInstruction1.getWideLiteral());
                    }
                    break;
                    case Format21s: {
                        BuilderInstruction21s builderInstruction1 = (BuilderInstruction21s) builderInstruction;
                        registerIndex = builderInstruction1.getRegisterA();
                        value = String.valueOf(builderInstruction1.getWideLiteral());
                    }
                    break;
                    case Format31i: {
                        BuilderInstruction31i builderInstruction1 = (BuilderInstruction31i) builderInstruction;
                        registerIndex = builderInstruction1.getRegisterA();
                        value = String.valueOf(builderInstruction1.getWideLiteral());
                    }
                    break;
                    case Format21ih: {
                        BuilderInstruction21ih builderInstruction21ih = (BuilderInstruction21ih) builderInstruction;
                        registerIndex = builderInstruction21ih.getRegisterA();
                        value = String.valueOf(builderInstruction21ih.getWideLiteral());
                    }
                    break;
                    case Format51l: {
                        BuilderInstruction51l builderInstruction51l = (BuilderInstruction51l) builderInstruction;
                        registerIndex = builderInstruction51l.getRegisterA();
                        value = String.valueOf(builderInstruction51l.getWideLiteral());
                    }
                    break;
                    case Format21lh: {
                        BuilderInstruction21lh builderInstruction21lh = (BuilderInstruction21lh) builderInstruction;
                        registerIndex = builderInstruction21lh.getRegisterA();
                        value = String.valueOf(builderInstruction21lh.getWideLiteral());
                    }
                    break;
                }
                return String.format("%s = #%s", formatRegisterName(registerIndex, localInfo), value);
            }
            case MOVE:
            case MOVE_FROM16:
            case MOVE_16:
            case MOVE_WIDE:
            case MOVE_WIDE_FROM16:
            case MOVE_WIDE_16:
            case MOVE_OBJECT:
            case MOVE_OBJECT_FROM16:
            case MOVE_OBJECT_16: {
                String from = "";
                String to = "";
                switch (opcode.format) {
                    case Format12x: {
                        BuilderInstruction12x builderInstruction1 = (BuilderInstruction12x) builderInstruction;
                        to = formatRegisterName(builderInstruction1.getRegisterB(), localInfo);
                        from = formatRegisterName(builderInstruction1.getRegisterA(), localInfo);
                    }
                    break;
                    case Format22x: {
                        BuilderInstruction22x builderInstruction1 = (BuilderInstruction22x) builderInstruction;
                        to = formatRegisterName(builderInstruction1.getRegisterB(), localInfo);
                        from = formatRegisterName(builderInstruction1.getRegisterA(), localInfo);
                    }
                    break;
                    case Format32x: {
                        BuilderInstruction32x builderInstruction1 = (BuilderInstruction32x) builderInstruction;
                        to = formatRegisterName(builderInstruction1.getRegisterB(), localInfo);
                        from = formatRegisterName(builderInstruction1.getRegisterA(), localInfo);
                    }
                    break;
                }
                return String.format("%s = %s", to, from);

            }
            case MOVE_RESULT:
            case MOVE_RESULT_WIDE:
            case MOVE_RESULT_OBJECT:
            case MOVE_EXCEPTION: {
                BuilderInstruction11x builderInstruction11x = (BuilderInstruction11x) builderInstruction;
                return String.format("%s %s", opcode.name,
                        formatRegisterName(builderInstruction11x.getRegisterA(), localInfo));
            }

            case CONST_STRING:
            case CONST_STRING_JUMBO: {
                String params = "";
                String value = "";
                switch (opcode.format) {
                    case Format21c: {
                        BuilderInstruction21c builderInstruction21c = (BuilderInstruction21c) builderInstruction;
                        params = formatRegisterName(builderInstruction21c.getRegisterA(), localInfo);
                        value = ReferenceUtil.getReferenceString(builderInstruction21c.getReference());
                    }
                    break;
                    case Format31c: {
                        BuilderInstruction31c builderInstruction21c = (BuilderInstruction31c) builderInstruction;
                        params = formatRegisterName(builderInstruction21c.getRegisterA(), localInfo);
                        value = ReferenceUtil.getReferenceString(builderInstruction21c.getReference());
                    }
                    break;
                }
                return String.format("%s = %s", params, value);
            }
            case CONST_CLASS: {
                BuilderInstruction21c builderInstruction21c = (BuilderInstruction21c) builderInstruction;
                return String.format("%s = %s", formatRegisterName(builderInstruction21c.getRegisterA(), localInfo),
                        formatTypeDescriptor(ReferenceUtil.getReferenceString(builderInstruction21c.getReference())));
            }
            case CHECK_CAST: {
                BuilderInstruction21c builderInstruction21c = (BuilderInstruction21c) builderInstruction;
                return String.format("(%s)%s",
                        formatTypeDescriptor(ReferenceUtil.getReferenceString(builderInstruction21c.getReference())),
                        formatRegisterName(builderInstruction21c.getRegisterA(), localInfo));
            }
            case INSTANCE_OF: {//TODO:需要优化
                BuilderInstruction22c builderInstruction21c = (BuilderInstruction22c) builderInstruction;
                return String.format("instance-of v%s, v%s, %s", builderInstruction21c.getRegisterA(),
                        builderInstruction21c.getRegisterB(),
                        formatTypeDescriptor(ReferenceUtil.getReferenceString(builderInstruction21c.getReference())));

            }
            case NEW_INSTANCE: {
                BuilderInstruction21c builderInstruction21c = (BuilderInstruction21c) builderInstruction;
                return String.format("%s = new %s", formatRegisterName(builderInstruction21c.getRegisterA(), localInfo),
                        formatTypeDescriptor(ReferenceUtil.getReferenceString(builderInstruction21c.getReference())));
            }
            case NEW_ARRAY: {
                BuilderInstruction22c builderInstruction22c = (BuilderInstruction22c) builderInstruction;
                String referenceString = ReferenceUtil.getReferenceString(builderInstruction22c.getReference());

                return String.format("%s =new %s", formatRegisterName(builderInstruction22c.getRegisterA(), localInfo),
                        referenceString != null ?
                                referenceString.replace("[]", String.format("[%s]",
                                        formatRegisterName(builderInstruction22c.getRegisterB(), localInfo))) :
                                null);
            }
            case FILLED_NEW_ARRAY: {
                //FIXME:
                BuilderInstruction35c builderInstruction35c = (BuilderInstruction35c) builderInstruction;
                //                return String.format("filled-new-array %s, %s",String.join(",",))
                return "";
            }
            case FILLED_NEW_ARRAY_RANGE: {
                return "";
            }
            case ARRAY_LENGTH: {
                BuilderInstruction12x builderInstruction12x = (BuilderInstruction12x) builderInstruction;
                return String
                        .format("%s = %s.length", formatRegisterName(builderInstruction12x.getRegisterA(), localInfo),
                                formatRegisterName(builderInstruction12x.getRegisterB(), localInfo));
            }
            case INVOKE_VIRTUAL:
            case INVOKE_SUPER:
            case INVOKE_DIRECT:
            case INVOKE_STATIC:
            case INVOKE_INTERFACE: {
                BuilderInstruction35c builderInstruction35c = (BuilderInstruction35c) builderInstruction;
                DexBackedMethodReference resolvedMethod =
                        (DexBackedMethodReference) builderInstruction35c.getReference();
                String invokeMethod = resolvedMethod.getName();

                String invokeObject = "";
                boolean isStatic = (opcode == Opcode.INVOKE_STATIC);
                if (isStatic) {//静态
                    invokeObject = formatTypeDescriptor(resolvedMethod.getDefiningClass());
                } else {
                    invokeObject = formatRegisterName(builderInstruction35c.getRegisterC(), localInfo);
                }

                int[] registers = new int[5];
                registers[0] = builderInstruction35c.getRegisterC();
                registers[1] = builderInstruction35c.getRegisterD();
                registers[2] = builderInstruction35c.getRegisterE();
                registers[3] = builderInstruction35c.getRegisterF();
                registers[4] = builderInstruction35c.getRegisterG();
                List<String> parameterTypes = resolvedMethod.getParameterTypes();

                int argReg = 0;
                List<String> registerNames = new ArrayList<>();
                if (!isStatic) {
                    argReg++;
                }

                for (String p : parameterTypes) {
                    int reg = argReg;

                    if ("D".equals(p) || "J".equals(p)) {
                        argReg += 2;
                    } else {
                        argReg += 1;
                    }
                    registerNames.add(formatRegisterName(registers[reg], localInfo));
                }

                return String.format("%s.%s(%s)", invokeObject, invokeMethod, String.join(",", registerNames));
            }
            case SPUT:
            case SPUT_WIDE:
            case SPUT_OBJECT:
            case SPUT_BOOLEAN:
            case SPUT_CHAR:
            case SPUT_SHORT:
            case SPUT_BYTE: {
                BuilderInstruction21c builderInstruction21c = (BuilderInstruction21c) builderInstruction;
                DexBackedFieldReference fieldReference = (DexBackedFieldReference) builderInstruction21c.getReference();
                return String.format("%s = %s", getFiledName(fieldReference),
                        formatRegisterName(builderInstruction21c.getRegisterA(), localInfo));

            }

            case SGET:
            case SGET_WIDE:
            case SGET_OBJECT:
            case SGET_BOOLEAN:
            case SGET_CHAR:
            case SGET_SHORT:
            case SGET_BYTE: {
                BuilderInstruction21c builderInstruction21c = (BuilderInstruction21c) builderInstruction;
                DexBackedFieldReference fieldReference = (DexBackedFieldReference) builderInstruction21c.getReference();
                return String.format("%s = %s", formatRegisterName(builderInstruction21c.getRegisterA(), localInfo),
                        getFiledName(fieldReference));
            }
            case IPUT:
            case IPUT_WIDE:
            case IPUT_OBJECT:
            case IPUT_BOOLEAN:
            case IPUT_CHAR:
            case IPUT_SHORT:
            case IPUT_BYTE: {
                BuilderInstruction22c builderInstruction22c = (BuilderInstruction22c) builderInstruction;
                DexBackedFieldReference fieldReference = (DexBackedFieldReference) builderInstruction22c.getReference();
                return String.format("%s = %s", getFiledName(fieldReference),
                        formatRegisterName(builderInstruction22c.getRegisterA(), localInfo));
            }

            case IGET:
            case IGET_WIDE:
            case IGET_OBJECT:
            case IGET_BOOLEAN:
            case IGET_CHAR:
            case IGET_SHORT:
            case IGET_BYTE: {
                BuilderInstruction22c builderInstruction22c = (BuilderInstruction22c) builderInstruction;
                DexBackedFieldReference fieldReference = (DexBackedFieldReference) builderInstruction22c.getReference();
                return String.format("%s = %s", formatRegisterName(builderInstruction22c.getRegisterA(), localInfo),
                        getFiledName(fieldReference));
            }

            case APUT:
            case APUT_WIDE:
            case APUT_OBJECT:
            case APUT_BOOLEAN:
            case APUT_CHAR:
            case APUT_SHORT:
            case APUT_BYTE: {
                BuilderInstruction23x builderInstruction23x = (BuilderInstruction23x) builderInstruction;
                return String.format("%s[%s] = %s",
                        formatRegisterName(((BuilderInstruction23x) builderInstruction).getRegisterA(), localInfo),
                        formatRegisterName(builderInstruction23x.getRegisterC(), localInfo),
                        formatRegisterName(builderInstruction23x.getRegisterA(), localInfo));
            }

            case AGET:
            case AGET_WIDE:
            case AGET_OBJECT:
            case AGET_BOOLEAN:
            case AGET_CHAR:
            case AGET_SHORT:
            case AGET_BYTE: {
                BuilderInstruction23x builderInstruction23x = (BuilderInstruction23x) builderInstruction;
                return String.format("%s = %s[%s]",
                        formatRegisterName(((BuilderInstruction23x) builderInstruction).getRegisterA(), localInfo),
                        formatRegisterName(builderInstruction23x.getRegisterC(), localInfo),
                        formatRegisterName(builderInstruction23x.getRegisterA(), localInfo));
            }
            case ADD_INT:
            case ADD_LONG:
            case ADD_FLOAT:
            case ADD_DOUBLE: {
                BuilderInstruction23x builderInstruction23x = (BuilderInstruction23x) builderInstruction;
                return formatBinaryOperation(formatRegisterName(builderInstruction23x.getRegisterA(), localInfo),
                        formatRegisterName(builderInstruction23x.getRegisterB(), localInfo), "+",
                        formatRegisterName(builderInstruction23x.getRegisterC(), localInfo));
            }
            case SUB_INT:
            case SUB_LONG:
            case SUB_FLOAT:
            case SUB_DOUBLE: {
                BuilderInstruction23x builderInstruction23x = (BuilderInstruction23x) builderInstruction;
                return formatBinaryOperation(formatRegisterName(builderInstruction23x.getRegisterA(), localInfo),
                        formatRegisterName(builderInstruction23x.getRegisterB(), localInfo), "-",
                        formatRegisterName(builderInstruction23x.getRegisterC(), localInfo));
            }
            case MUL_INT:
            case MUL_LONG:
            case MUL_FLOAT:
            case MUL_DOUBLE: {
                BuilderInstruction23x builderInstruction23x = (BuilderInstruction23x) builderInstruction;
                return formatBinaryOperation(formatRegisterName(builderInstruction23x.getRegisterA(), localInfo),
                        formatRegisterName(builderInstruction23x.getRegisterB(), localInfo), "*",
                        formatRegisterName(builderInstruction23x.getRegisterC(), localInfo));
            }
            case DIV_INT:
            case DIV_LONG:
            case DIV_FLOAT:
            case DIV_DOUBLE: {
                BuilderInstruction23x builderInstruction23x = (BuilderInstruction23x) builderInstruction;
                return formatBinaryOperation(formatRegisterName(builderInstruction23x.getRegisterA(), localInfo),
                        formatRegisterName(builderInstruction23x.getRegisterB(), localInfo), "/",
                        formatRegisterName(builderInstruction23x.getRegisterC(), localInfo));
            }
            case REM_INT:
            case REM_LONG:
            case REM_FLOAT:
            case REM_DOUBLE: {
                BuilderInstruction23x builderInstruction23x = (BuilderInstruction23x) builderInstruction;
                return formatBinaryOperation(formatRegisterName(builderInstruction23x.getRegisterA(), localInfo),
                        formatRegisterName(builderInstruction23x.getRegisterB(), localInfo), "&",
                        formatRegisterName(builderInstruction23x.getRegisterC(), localInfo));
            }
            case AND_INT:
            case AND_LONG: {
                BuilderInstruction23x builderInstruction23x = (BuilderInstruction23x) builderInstruction;
                return formatBinaryOperation(formatRegisterName(builderInstruction23x.getRegisterA(), localInfo),
                        formatRegisterName(builderInstruction23x.getRegisterB(), localInfo), "&",
                        formatRegisterName(builderInstruction23x.getRegisterC(), localInfo));
            }
            case OR_INT:
            case OR_LONG: {
                BuilderInstruction23x builderInstruction23x = (BuilderInstruction23x) builderInstruction;
                return formatBinaryOperation(formatRegisterName(builderInstruction23x.getRegisterA(), localInfo),
                        formatRegisterName(builderInstruction23x.getRegisterB(), localInfo), "|",
                        formatRegisterName(builderInstruction23x.getRegisterC(), localInfo));
            }

            case XOR_INT:
            case XOR_LONG: {
                BuilderInstruction23x builderInstruction23x = (BuilderInstruction23x) builderInstruction;
                return formatBinaryOperation(formatRegisterName(builderInstruction23x.getRegisterA(), localInfo),
                        formatRegisterName(builderInstruction23x.getRegisterB(), localInfo), "^",
                        formatRegisterName(builderInstruction23x.getRegisterC(), localInfo));
            }
            case SHL_INT:
            case SHL_LONG: {
                BuilderInstruction23x builderInstruction23x = (BuilderInstruction23x) builderInstruction;
                return formatBinaryOperation(formatRegisterName(builderInstruction23x.getRegisterA(), localInfo),
                        formatRegisterName(builderInstruction23x.getRegisterB(), localInfo), "<<",
                        formatRegisterName(builderInstruction23x.getRegisterC(), localInfo));
            }
            case SHR_INT:
            case USHR_INT:
            case SHR_LONG:
            case USHR_LONG: {
                BuilderInstruction23x builderInstruction23x = (BuilderInstruction23x) builderInstruction;
                return formatBinaryOperation(formatRegisterName(builderInstruction23x.getRegisterA(), localInfo),
                        formatRegisterName(builderInstruction23x.getRegisterB(), localInfo), ">>",
                        formatRegisterName(builderInstruction23x.getRegisterC(), localInfo));
            }
            case IF_EQ: {
                BuilderInstruction22t builderInstruction22t = (BuilderInstruction22t) builderInstruction;
                return formatIfOperation(localInfo, gotoTable, builderInstruction22t, "==");
            }
            case IF_NE: {
                BuilderInstruction22t builderInstruction22t = (BuilderInstruction22t) builderInstruction;
                return formatIfOperation(localInfo, gotoTable, builderInstruction22t, "!=");

            }
            case IF_LT: {
                BuilderInstruction22t builderInstruction22t = (BuilderInstruction22t) builderInstruction;
                return formatIfOperation(localInfo, gotoTable, builderInstruction22t, "<");

            }
            case IF_GE: {
                BuilderInstruction22t builderInstruction22t = (BuilderInstruction22t) builderInstruction;
                return formatIfOperation(localInfo, gotoTable, builderInstruction22t, ">=");

            }
            case IF_GT: {
                BuilderInstruction22t builderInstruction22t = (BuilderInstruction22t) builderInstruction;
                return formatIfOperation(localInfo, gotoTable, builderInstruction22t, ">");
            }
            case IF_LE: {
                BuilderInstruction22t builderInstruction22t = (BuilderInstruction22t) builderInstruction;
                return formatIfOperation(localInfo, gotoTable, builderInstruction22t, "<=");
            }
            case IF_EQZ: {
                BuilderInstruction21t builderInstruction21t = (BuilderInstruction21t) builderInstruction;
                return formatIfzOperation(localInfo, gotoTable, builderInstruction21t, "==");
            }
            case IF_NEZ: {
                BuilderInstruction21t builderInstruction21t = (BuilderInstruction21t) builderInstruction;
                return formatIfzOperation(localInfo, gotoTable, builderInstruction21t, "!=");
            }
            case IF_LTZ: {
                BuilderInstruction21t builderInstruction21t = (BuilderInstruction21t) builderInstruction;
                return formatIfzOperation(localInfo, gotoTable, builderInstruction21t, "<");
            }
            case IF_GEZ: {
                BuilderInstruction21t builderInstruction21t = (BuilderInstruction21t) builderInstruction;
                return formatIfzOperation(localInfo, gotoTable, builderInstruction21t, ">=");
            }
            case IF_GTZ: {
                BuilderInstruction21t builderInstruction21t = (BuilderInstruction21t) builderInstruction;
                return formatIfzOperation(localInfo, gotoTable, builderInstruction21t, ">");
            }
            case IF_LEZ: {
                BuilderInstruction21t builderInstruction21t = (BuilderInstruction21t) builderInstruction;
                return formatIfzOperation(localInfo, gotoTable, builderInstruction21t, "<=");
            }
            case RSUB_INT: {
                BuilderInstruction22s builderInstruction22s = (BuilderInstruction22s) builderInstruction;
                return String
                        .format("%s = #%s - %s", formatRegisterName(builderInstruction22s.getRegisterA(), localInfo),
                                builderInstruction22s.getWideLiteral(),
                                formatRegisterName(builderInstruction22s.getRegisterB(), localInfo));
            }
            case RSUB_INT_LIT8: {
                BuilderInstruction22b builderInstruction22b = (BuilderInstruction22b) builderInstruction;
                return String
                        .format("%s = #%s - %s", formatRegisterName(builderInstruction22b.getRegisterA(), localInfo),
                                builderInstruction22b.getWideLiteral(),
                                formatRegisterName(builderInstruction22b.getRegisterB(), localInfo));
            }
            case ADD_INT_LIT8: {
                BuilderInstruction22b builderInstruction22b = (BuilderInstruction22b) builderInstruction;
                String operate = "+";
                return formatBinaryLiteralOperation22b(localInfo, builderInstruction22b, operate);

            }
            case ADD_INT_LIT16: {
                BuilderInstruction22s builderInstruction22s = (BuilderInstruction22s) builderInstruction;
                String operate = "+";
                return formatBinaryLiteralOperation22s(localInfo, builderInstruction22s, operate);
            }
            case MUL_INT_LIT16: {
                BuilderInstruction22s builderInstruction22s = (BuilderInstruction22s) builderInstruction;
                String operate = "*";
                return formatBinaryLiteralOperation22s(localInfo, builderInstruction22s, operate);
            }
            case MUL_INT_LIT8: {
                BuilderInstruction22b builderInstruction22b = (BuilderInstruction22b) builderInstruction;
                String operate = "*";
                return formatBinaryLiteralOperation22b(localInfo, builderInstruction22b, operate);
            }
            case DIV_INT_LIT8: {
                BuilderInstruction22b builderInstruction22b = (BuilderInstruction22b) builderInstruction;
                String operate = "/";
                return formatBinaryLiteralOperation22b(localInfo, builderInstruction22b, operate);
            }
            case DIV_INT_LIT16: {
                BuilderInstruction22s builderInstruction22s = (BuilderInstruction22s) builderInstruction;
                String operate = "/";
                return formatBinaryLiteralOperation22s(localInfo, builderInstruction22s, operate);
            }
            case REM_INT_LIT8: {
                BuilderInstruction22b builderInstruction22b = (BuilderInstruction22b) builderInstruction;
                String operate = "%";
                return formatBinaryLiteralOperation22b(localInfo, builderInstruction22b, operate);
            }
            case REM_INT_LIT16: {
                BuilderInstruction22s builderInstruction22s = (BuilderInstruction22s) builderInstruction;
                String operate = "%";
                return formatBinaryLiteralOperation22s(localInfo, builderInstruction22s, operate);
            }
            case AND_INT_LIT16: {
                BuilderInstruction22s builderInstruction22s = (BuilderInstruction22s) builderInstruction;
                String operate = "&";
                return formatBinaryLiteralOperation22s(localInfo, builderInstruction22s, operate);
            }
            case AND_INT_LIT8: {
                BuilderInstruction22b builderInstruction22b = (BuilderInstruction22b) builderInstruction;
                String operate = "&";
                return formatBinaryLiteralOperation22b(localInfo, builderInstruction22b, operate);
            }
            case OR_INT_LIT16: {
                BuilderInstruction22s builderInstruction22s = (BuilderInstruction22s) builderInstruction;
                String operate = "|";
                return formatBinaryLiteralOperation22s(localInfo, builderInstruction22s, operate);
            }
            case OR_INT_LIT8: {
                BuilderInstruction22b builderInstruction22b = (BuilderInstruction22b) builderInstruction;
                String operate = "|";
                return formatBinaryLiteralOperation22b(localInfo, builderInstruction22b, operate);
            }
            case XOR_INT_LIT16: {
                BuilderInstruction22s builderInstruction22s = (BuilderInstruction22s) builderInstruction;
                String operate = "^";
                return formatBinaryLiteralOperation22s(localInfo, builderInstruction22s, operate);
            }
            case XOR_INT_LIT8: {
                BuilderInstruction22b builderInstruction22b = (BuilderInstruction22b) builderInstruction;
                String operate = "^";
                return formatBinaryLiteralOperation22b(localInfo, builderInstruction22b, operate);
            }
            case SHL_INT_LIT8: {
                BuilderInstruction22b builderInstruction22b = (BuilderInstruction22b) builderInstruction;
                String operate = "<<";
                return formatBinaryLiteralOperation22b(localInfo, builderInstruction22b, operate);
            }
            case SHR_INT_LIT8: {
                BuilderInstruction22b builderInstruction22b = (BuilderInstruction22b) builderInstruction;
                String operate = ">>";
                return formatBinaryLiteralOperation22b(localInfo, builderInstruction22b, operate);
            }
            case USHR_INT_LIT8: {
                BuilderInstruction22b builderInstruction22b = (BuilderInstruction22b) builderInstruction;
                String operate = ">>";
                return formatBinaryLiteralOperation22b(localInfo, builderInstruction22b, operate);
            }
            case ADD_INT_2ADDR:
            case ADD_LONG_2ADDR:
            case ADD_FLOAT_2ADDR:
            case ADD_DOUBLE_2ADDR: {
                BuilderInstruction12x builderInstruction12x = (BuilderInstruction12x) builderInstruction;
                return formatBinary2AddrOperation(localInfo, builderInstruction12x, "+");
            }
            case SUB_INT_2ADDR:
            case SUB_LONG_2ADDR:
            case SUB_FLOAT_2ADDR:
            case SUB_DOUBLE_2ADDR: {
                BuilderInstruction12x builderInstruction12x = (BuilderInstruction12x) builderInstruction;
                return formatBinary2AddrOperation(localInfo, builderInstruction12x, "-");
            }
            case MUL_INT_2ADDR:
            case MUL_LONG_2ADDR:
            case MUL_FLOAT_2ADDR:
            case MUL_DOUBLE_2ADDR: {
                BuilderInstruction12x builderInstruction12x = (BuilderInstruction12x) builderInstruction;
                return formatBinary2AddrOperation(localInfo, builderInstruction12x, "*");
            }
            case DIV_INT_2ADDR:
            case DIV_LONG_2ADDR:
            case DIV_FLOAT_2ADDR:
            case DIV_DOUBLE_2ADDR: {
                BuilderInstruction12x builderInstruction12x = (BuilderInstruction12x) builderInstruction;
                return formatBinary2AddrOperation(localInfo, builderInstruction12x, "/");
            }
            case REM_INT_2ADDR:
            case REM_LONG_2ADDR:
            case REM_FLOAT_2ADDR:
            case REM_DOUBLE_2ADDR: {
                BuilderInstruction12x builderInstruction12x = (BuilderInstruction12x) builderInstruction;
                return formatBinary2AddrOperation(localInfo, builderInstruction12x, "&");
            }
            case AND_INT_2ADDR:
            case AND_LONG_2ADDR: {
                BuilderInstruction12x builderInstruction12x = (BuilderInstruction12x) builderInstruction;
                return formatBinary2AddrOperation(localInfo, builderInstruction12x, "&");
            }
            case OR_INT_2ADDR:
            case OR_LONG_2ADDR: {
                BuilderInstruction12x builderInstruction12x = (BuilderInstruction12x) builderInstruction;
                return formatBinary2AddrOperation(localInfo, builderInstruction12x, "|");
            }
            case XOR_INT_2ADDR:
            case XOR_LONG_2ADDR: {
                BuilderInstruction12x builderInstruction12x = (BuilderInstruction12x) builderInstruction;
                return formatBinary2AddrOperation(localInfo, builderInstruction12x, "^");
            }
            case SHL_INT_2ADDR:
            case SHL_LONG_2ADDR: {
                BuilderInstruction12x builderInstruction12x = (BuilderInstruction12x) builderInstruction;
                return formatBinary2AddrOperation(localInfo, builderInstruction12x, "<<");
            }
            case SHR_INT_2ADDR:
            case USHR_INT_2ADDR:
            case SHR_LONG_2ADDR:
            case USHR_LONG_2ADDR: {
                BuilderInstruction12x builderInstruction12x = (BuilderInstruction12x) builderInstruction;
                return formatBinary2AddrOperation(localInfo, builderInstruction12x, ">>");
            }
            case GOTO: {
                BuilderInstruction10t builderInstruction10t = (BuilderInstruction10t) builderInstruction;
                return String.format("%s %s", opcode.name,
                        gotoTable.getReffererLaebl(builderInstruction10t.getLocation().getCodeAddress()));
            }
            case GOTO_16: {
                BuilderInstruction20t builderInstruction20t = (BuilderInstruction20t) builderInstruction;
                return String.format("%s %s", opcode.name,
                        gotoTable.getReffererLaebl(builderInstruction20t.getLocation().getCodeAddress()));
            }
            case GOTO_32: {
                BuilderInstruction30t builderInstruction30t = (BuilderInstruction30t) builderInstruction;

                return String.format("%s %s", opcode.name,
                        gotoTable.getReffererLaebl(builderInstruction30t.getLocation().getCodeAddress()));
            }
            case PACKED_SWITCH: {
                BuilderInstruction31t builderInstruction31t = (BuilderInstruction31t) builderInstruction;
                BuilderPackedSwitchPayload builderPackedSwitchPayload =
                        (BuilderPackedSwitchPayload) builderInstruction31t.getTarget().getLocation().getInstruction();
                List<BuilderSwitchElement> switchElements = builderPackedSwitchPayload.getSwitchElements();
                return String.format("%s %s - first:%s - [%s]", opcode.name,
                        formatRegisterName(builderInstruction31t.getRegisterA(), localInfo),
                        switchElements.get(0).getKey(), String.join(",", gotoTable.referrers
                                .get((long) builderInstruction31t.getLocation().getCodeAddress())));//FIXME:顺序有问题
            }
            case SPARSE_SWITCH: {
                BuilderInstruction31t builderInstruction31t = (BuilderInstruction31t) builderInstruction;
                BuilderSparseSwitchPayload builderSparseSwitchPayload =
                        (BuilderSparseSwitchPayload) builderInstruction31t.getTarget().getLocation().getInstruction();
                List<BuilderSwitchElement> switchElements = builderSparseSwitchPayload.getSwitchElements();
                StringJoiner joiner = new StringJoiner(",");
                for (BuilderSwitchElement element : switchElements) {
                    joiner.add(String.valueOf(element.getKey()));
                }
                String keys = joiner.toString();
                String switchs = String.join(",",
                        gotoTable.referrers.get((long) builderInstruction31t.getLocation().getCodeAddress()));
                return String.format("%s %s - Keys:[%s] Targets:[%s]", opcode.name,
                        formatRegisterName(builderInstruction31t.getRegisterA(), localInfo), keys, switchs);

            }
            case RETURN_VOID: {
                return opcode.name;
            }
            //            return-void ,
            //            return-object ,
            //            return ,
            //            monitor-enter ,
            //            monitor-exit ,
            //            throw ,

            case RETURN:
            case RETURN_WIDE:
            case RETURN_OBJECT:
            case MONITOR_ENTER:
            case MONITOR_EXIT:
            case THROW: {
                BuilderInstruction11x builderInstruction11x = (BuilderInstruction11x) builderInstruction;
                return String.format("%s v%s", opcode.name, builderInstruction11x.getRegisterA());
            }
            case CMP_LONG:
            case CMPG_DOUBLE:
            case CMPG_FLOAT:
            case CMPL_DOUBLE:
            case CMPL_FLOAT: {
                BuilderInstruction23x builderInstruction23x = (BuilderInstruction23x) builderInstruction;
                return String.format("%s v%s, v%s, v%s", opcode.name, builderInstruction23x.getRegisterA(),
                        builderInstruction23x.getRegisterB(), builderInstruction23x.getRegisterC());
            }
            case INVOKE_VIRTUAL_RANGE:
            case INVOKE_SUPER_RANGE:
            case INVOKE_DIRECT_RANGE:
            case INVOKE_STATIC_RANGE:
            case INVOKE_INTERFACE_RANGE: {//FIXME:
                BuilderInstruction3rc builderInstruction3rc = (BuilderInstruction3rc) builderInstruction;
                return String.format("%s {%s..%s}, method@%s", opcode.name, builderInstruction3rc.getStartRegister(),
                        builderInstruction3rc.getRegisterCount(),
                        ((MethodReference) builderInstruction3rc.getReference()).getName());
            }
            case NEG_INT:
            case NOT_INT:
            case NEG_LONG:
            case NOT_LONG:
            case NEG_FLOAT:
            case NEG_DOUBLE:
            case INT_TO_LONG:
            case INT_TO_FLOAT:
            case INT_TO_DOUBLE:
            case LONG_TO_INT:
            case LONG_TO_FLOAT:
            case LONG_TO_DOUBLE:
            case FLOAT_TO_INT:
            case FLOAT_TO_LONG:
            case FLOAT_TO_DOUBLE:
            case DOUBLE_TO_INT:
            case DOUBLE_TO_LONG:
            case DOUBLE_TO_FLOAT:
            case INT_TO_BYTE:
            case INT_TO_CHAR:
            case INT_TO_SHORT: {
                BuilderInstruction12x builderInstruction12x = (BuilderInstruction12x) builderInstruction;
                return String.format("%s v%s v%s", opcode.name, builderInstruction12x.getRegisterA(),
                        builderInstruction12x.getRegisterB());
            }
            case FILL_ARRAY_DATA: {
                BuilderInstruction31t builderInstruction31t = (BuilderInstruction31t) builderInstruction;
                BuilderArrayPayload instruction =
                        (BuilderArrayPayload) builderInstruction31t.getTarget().getLocation().getInstruction();
                if (instruction != null) {
                    List<Number> arrayElements = instruction.getArrayElements();
                    StringJoiner joiner = new StringJoiner(",");
                    for (Number number : arrayElements) {
                        joiner.add(number.toString());
                    }

                    return String.format("%s v%s, [%s]", opcode.name, builderInstruction31t.getRegisterA(),
                            joiner.toString());
                }
                return "fill-array-data ERRRRORRR";
            }
        }

        return "";
    }

    private static String formatBinary2AddrOperation(LocalInfo methodImplementation,
            BuilderInstruction12x builderInstruction12x, String operate) {
        return String
                .format("%s = %s %s %s", formatRegisterName(builderInstruction12x.getRegisterA(), methodImplementation),
                        formatRegisterName(builderInstruction12x.getRegisterA(), methodImplementation), operate,
                        formatRegisterName(builderInstruction12x.getRegisterB(), methodImplementation));
    }

    private static String formatBinaryLiteralOperation22b(LocalInfo methodImplementation,
            BuilderInstruction22b builderInstruction22b, String operate) {
        return String.format("%s = %s %s #%s",
                formatRegisterName(builderInstruction22b.getRegisterA(), methodImplementation),
                formatRegisterName(builderInstruction22b.getRegisterB(), methodImplementation), operate,
                builderInstruction22b.getWideLiteral());
    }

    private static String formatBinaryLiteralOperation22s(LocalInfo methodImplementation,
            BuilderInstruction22s builderInstruction22s, String operate) {
        return String.format("%s = %s %s #%s",
                formatRegisterName(builderInstruction22s.getRegisterA(), methodImplementation),
                formatRegisterName(builderInstruction22s.getRegisterB(), methodImplementation), operate,
                builderInstruction22s.getWideLiteral());
    }

    private static String formatIfzOperation(LocalInfo methodImplementation, GotoTable gotoTable,
            BuilderInstruction21t builderInstruction21t, String operate) {
        return String.format("if (%s %s 0) :%s",
                formatRegisterName(builderInstruction21t.getRegisterA(), methodImplementation), operate,
                gotoTable.getReffererLaebl(builderInstruction21t.getCodeOffset()));
    }

    private static String formatIfOperation(LocalInfo methodImplementation, GotoTable gotoTable,
            BuilderInstruction22t builderInstruction22t, String operate) {
        return String.format("if (%s %s %s) :%s",
                formatRegisterName(builderInstruction22t.getRegisterA(), methodImplementation), operate,
                formatRegisterName(builderInstruction22t.getRegisterB(), methodImplementation),
                gotoTable.getReffererLaebl(builderInstruction22t.getCodeOffset()));
    }

    private static String formatBinaryOperation(String destination, String first, String operation, String second) {
        return String.format("%s = %s %s %s", destination, first, operation, second);
    }

    private static String getFiledName(DexBackedFieldReference fieldReference) {
        return "this." + fieldReference.getName();
    }

    private static String typeToString(String typeDescriptor) {
        if (Strings.isNullOrEmpty(typeDescriptor))
            return "";

        switch (typeDescriptor.charAt(0)) {
            case 'V':
                return "void";

            case 'Z':
                return "boolean";

            case 'B':
                return "byte";

            case 'S':
                return "short";

            case 'C':
                return "char";

            case 'I':
                return "int";

            case 'J':
                return "long";

            case 'F':
                return "float";

            case 'D':
                return "double";

            case 'L':
                return formatTypeDescriptor(typeDescriptor);

            case '[':
                return typeToString(typeDescriptor.substring(1)) + "[]";

            default:
                return "unknown";
        }
    }

    private static String formatTypeDescriptor(String typeDescriptor) {
        return typeDescriptor.replace('/', '.').substring(1, typeDescriptor.length() - 1);
    }

    private static void annotionToString(PrintStream printStream, Annotation annotation, Indentation indent) {
        List<String> attributes = new ArrayList<String>();
        Set<? extends AnnotationElement> elements = annotation.getElements();
        for (AnnotationElement element : elements) {
            attributes.add(String
                    .format("%s=%s", element.getName(), DexEncodedValueUtils.getEncodeValue(element.getValue())));
        }
        printStream.print(indent.toString());
        printStream.println(String.format("@%s(%s)", annotation.getType(), String.join(",", attributes)));
    }
    
    public static GotoTable buildGotoTable(MutableMethodImplementation methodImplementation) {
        GotoTable gotoTable = new GotoTable();

        GotoTable.Counter gotoCounter = new GotoTable.Counter();
        GotoTable.Counter ifCounter = new GotoTable.Counter();
        GotoTable.Counter switchCounter = new GotoTable.Counter();
        Iterable<? extends Instruction> instructions = methodImplementation.getInstructions();
        for (Instruction instruction : instructions) {
            Opcode opcode = instruction.getOpcode();
            int codeAddress = ((BuilderInstruction) instruction).getLocation().getCodeAddress();

            switch (opcode) {
                case GOTO: {
                    BuilderInstruction10t gotoInstruction = (BuilderInstruction10t) instruction;
                    gotoTable.addTarget(codeAddress, codeAddress + gotoInstruction.getCodeOffset(), "goto_",
                            gotoCounter);
                }
                break;
                case GOTO_16: {
                    BuilderInstruction20t gotoInstruction = (BuilderInstruction20t) instruction;
                    gotoTable.addTarget(codeAddress, codeAddress + gotoInstruction.getCodeOffset(), "goto_",
                            gotoCounter);
                }
                break;
                case GOTO_32: {
                    BuilderInstruction30t gotoInstruction = (BuilderInstruction30t) instruction;
                    gotoTable.addTarget(codeAddress, codeAddress + gotoInstruction.getCodeOffset(), "goto_",
                            gotoCounter);

                }
                break;
                case PACKED_SWITCH:
                case SPARSE_SWITCH: {
                    MethodLocation targetLocation = ((BuilderOffsetInstruction) instruction).getTarget().getLocation();
                    Instruction targetInstruction = targetLocation.getInstruction();

                    BuilderSwitchPayload switchInstruction = (BuilderSwitchPayload) targetInstruction;
                    List<? extends BuilderSwitchElement> switchElements = switchInstruction.getSwitchElements();
                    int finalOffset = codeAddress;
                    switchElements.forEach((Consumer<BuilderSwitchElement>) builderSwitchElement -> gotoTable
                            .addTarget(finalOffset, finalOffset + builderSwitchElement.getOffset(), "switch_",
                                    switchCounter));
                }
                break;
                case IF_EQ:
                case IF_NE:
                case IF_LT:
                case IF_GE:
                case IF_GT:
                case IF_LE: {
                    BuilderInstruction22t ifInstruction = (BuilderInstruction22t) instruction;
                    gotoTable.addTarget(codeAddress, codeAddress + ifInstruction.getCodeOffset(), "if_", ifCounter);
                }
                break;
                case IF_EQZ:
                case IF_NEZ:
                case IF_LTZ:
                case IF_GEZ:
                case IF_GTZ:
                case IF_LEZ: {
                    BuilderInstruction21t ifInstruction = (BuilderInstruction21t) instruction;
                    gotoTable.addTarget(codeAddress, codeAddress + ifInstruction.getCodeOffset(), "if_", ifCounter);
                }
                break;
            }
        }

        GotoTable.Counter catchCounter = new GotoTable.Counter();
        List<? extends BuilderTryBlock> tryBlocks = methodImplementation.getTryBlocks();
        for (BuilderTryBlock tryBlock : tryBlocks) {
            for (BuilderExceptionHandler handler : tryBlock.getExceptionHandlers()) {
                gotoTable.addHandler(handler, "catch_", catchCounter);
            }
        }

        return gotoTable;
    }
}
