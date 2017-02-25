package dodola.flower.dex;

import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.iface.value.*;

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
                return true;
            case ValueType.SHORT:
                return ((ShortEncodedValue) encodedValue).getValue();
        }
        return false;
    }

    private DexEncodedValueUtils() {
    }
}
