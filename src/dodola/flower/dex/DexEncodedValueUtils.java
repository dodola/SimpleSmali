package dodola.flower.dex;

import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.iface.value.*;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public final class DexEncodedValueUtils {
    public static boolean isDefaultValue(EncodedValue encodedValue) {
        switch (encodedValue.getValueType()) {
            case ValueType.BOOLEAN:
                return !((BooleanEncodedValue) encodedValue).getValue();
            case ValueType.BYTE:
                return ((ByteEncodedValue) encodedValue).getValue() == 0;
            case ValueType.CHAR:
                return ((CharEncodedValue) encodedValue).getValue() == 0;
            case ValueType.DOUBLE:
                return ((DoubleEncodedValue) encodedValue).getValue() == 0;
            case ValueType.FLOAT:
                return ((FloatEncodedValue) encodedValue).getValue() == 0;
            case ValueType.INT:
                return ((IntEncodedValue) encodedValue).getValue() == 0;
            case ValueType.LONG:
                return ((LongEncodedValue) encodedValue).getValue() == 0;
            case ValueType.NULL:
                return true;
            case ValueType.SHORT:
                return ((ShortEncodedValue) encodedValue).getValue() == 0;
        }
        return false;
    }

    public static Object getEncodeValue(EncodedValue encodedValue) {
        switch (encodedValue.getValueType()) {
            case ValueType.BOOLEAN:
                return ((BooleanEncodedValue) encodedValue).getValue();
            case ValueType.BYTE:
                return ((ByteEncodedValue) encodedValue).getValue();
            case ValueType.CHAR:
                return ((CharEncodedValue) encodedValue).getValue();
            case ValueType.DOUBLE:
                return ((DoubleEncodedValue) encodedValue).getValue();
            case ValueType.FLOAT:
                return ((FloatEncodedValue) encodedValue).getValue();
            case ValueType.INT:
                return ((IntEncodedValue) encodedValue).getValue();
            case ValueType.LONG:
                return ((LongEncodedValue) encodedValue).getValue();
            case ValueType.NULL:
                return "null";
            case ValueType.SHORT:
                return ((ShortEncodedValue) encodedValue).getValue();
            case ValueType.STRING:
                return ((StringEncodedValue) encodedValue).getValue();
            case ValueType.TYPE:
                return ((TypeEncodedValue) encodedValue).getValue();
            case ValueType.FIELD:
                return ((FieldEncodedValue) encodedValue).getValue();
            case ValueType.ENUM:
                return ((EnumEncodedValue) encodedValue).getValue();
            case ValueType.METHOD:
                return ((MethodEncodedValue) encodedValue).getValue().getName();
            case ValueType.ARRAY: {
                ArrayList<String> vals = new ArrayList<>();
                StringJoiner joiner = new StringJoiner(",");
                List<? extends EncodedValue> valueS = ((ArrayEncodedValue) encodedValue).getValue();
                for (EncodedValue encodedValue1 : valueS) {
                    joiner.add(String.valueOf(getEncodeValue(encodedValue1)));
                }
                return String.format("[%s]", joiner.toString());
            }
            case ValueType.ANNOTATION:
                return ((AnnotationEncodedValue) encodedValue).getType();//FIXME:
        }
       
        return "Unknown";

    }

    private DexEncodedValueUtils() {
    }
}
