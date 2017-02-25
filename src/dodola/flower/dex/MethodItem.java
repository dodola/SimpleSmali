package dodola.flower.dex;

import org.jf.util.IndentingWriter;

import java.io.IOException;

public abstract class MethodItem implements Comparable<MethodItem> {
    protected final int codeAddress;

    protected MethodItem(int codeAddress) {
        this.codeAddress = codeAddress;
    }

    public int getCodeAddress() {
        return codeAddress;
    }

    //return an arbitrary double that determines how this item will be sorted with others at the same address
    public abstract double getSortOrder();

    public int compareTo(MethodItem methodItem) {
        int result = ((Integer) codeAddress).compareTo(methodItem.codeAddress);

        if (result == 0){
            return ((Double)getSortOrder()).compareTo(methodItem.getSortOrder());
        }
        return result;
    }

    public abstract boolean writeTo(IndentingWriter writer) throws IOException;
}
