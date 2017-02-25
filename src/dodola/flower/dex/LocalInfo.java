package dodola.flower.dex;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by baidu on 2017/2/25.
 */
public class LocalInfo {
    private HashMap<Integer, String> infos = new HashMap<>(20);

    public String getName(int reg) {
        return infos.get(reg);
    }

    public void setName(int reg, String name) {
        infos.put(reg, name);
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        Set<Map.Entry<Integer, String>> entries = infos.entrySet();
        for (Map.Entry<Integer, String> entry : entries) {
            sb.append(" regsiter:" + entry.getKey() + ",name:" + entry.getValue());
        }

        return "LocalInfo{" +
                "infos=" + sb.toString() + '}';
    }
}
