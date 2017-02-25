package dodola.flower.dex;

import org.jf.util.IndentingWriter;

import java.io.IOException;

public class LabelMethodItem extends MethodItem {
    private final String labelPrefix;
    private int labelSequence;

    public LabelMethodItem(int codeAddress, String labelPrefix) {
        super(codeAddress);
        this.labelPrefix = labelPrefix;
    }

    public double getSortOrder() {
        return 0;
    }

    public int compareTo(MethodItem methodItem) {
        int result = super.compareTo(methodItem);

        if (result == 0) {
            if (methodItem instanceof LabelMethodItem) {
                result = labelPrefix.compareTo(((LabelMethodItem) methodItem).labelPrefix);
            }
        }
        return result;
    }

    public int hashCode() {
        //force it to call equals when two labels are at the same address
        return getCodeAddress();
    }

    public boolean equals(Object o) {
        if (!(o instanceof LabelMethodItem)) {
            return false;
        }
        return this.compareTo((MethodItem) o) == 0;
    }

    public boolean writeTo(IndentingWriter writer) throws IOException {
        writer.write(':');
        writer.write(labelPrefix);
        writer.printUnsignedLongAsHex(labelSequence);
        return true;
    }

    public String getLabelPrefix() {
        return labelPrefix;
    }

    public int getLabelAddress() {
        return this.getCodeAddress();
    }

    public int getLabelSequence() {
        return labelSequence;
    }

    public void setLabelSequence(int labelSequence) {
        this.labelSequence = labelSequence;
    }
}
