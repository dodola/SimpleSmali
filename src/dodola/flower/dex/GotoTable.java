package dodola.flower.dex;

import org.jf.dexlib2.builder.BuilderExceptionHandler;
import org.jf.dexlib2.dexbacked.DexBackedExceptionHandler;
import org.jf.dexlib2.dexbacked.DexBackedTryBlock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

public class GotoTable {
    public LinkedHashMap<Long, HashSet<String>> targets = new LinkedHashMap<>();
    public LinkedHashMap<Long, HashSet<String>> referrers = new LinkedHashMap<>();
    public HashMap<BuilderExceptionHandler, String> handlers = new HashMap<>();

    public static class Counter {
        int counter;
    }

    public String nextTargetLabel(long offset, String labelPrefix, Counter counter) {
        Set<String> strings = targets.get(offset);
        if (strings != null) {
            for (String target : strings) {
                if (target.startsWith(labelPrefix)) {
                    return target;
                }
            }
        }
        return labelPrefix + counter.counter++;
    }

    public String addTarget(long from, long to, String labelPrefix, Counter counter) {
        String label = nextTargetLabel(to, labelPrefix, counter);
        HashSet<String> strings = targets.get(to);
        if (strings == null) {
            strings = new HashSet<>();
            targets.put(to, strings);
        }
        strings.add(label);
        HashSet<String> referrersSet = this.referrers.get(from);
        if (referrersSet == null) {
            referrersSet = new HashSet<>();
            referrers.put(from, referrersSet);
        }
        referrersSet.add(label);
        return label;
    }

    public String getHandlerLabel(DexBackedTryBlock tryBlock) {
        return handlers.get(tryBlock);
    }

    public String getReffererLaebl(long offset) {
        HashSet<String> strings = referrers.get(offset);
        if (strings == null) {
            return "";
        }
        return strings.iterator().next();
    }

    public void addHandler(BuilderExceptionHandler handler, String labelPrefix, Counter counter) {
        String label = addTarget(-1, handler.getHandlerCodeAddress(), labelPrefix, counter);
        handlers.put(handler, label);
    }

}