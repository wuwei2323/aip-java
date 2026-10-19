package cn.org.aip.common.utils;

import com.github.yitter.contract.IdGeneratorOptions;
import com.github.yitter.idgen.YitIdHelper;

public class IdGeneratorUtil {
    private static boolean initialized = false;

    private IdGeneratorUtil() {}
    
    public static synchronized void initIdGenerator(short workerId) {
        if (!initialized) {
            IdGeneratorOptions options = new IdGeneratorOptions(workerId);
            YitIdHelper.setIdGenerator(options);
            initialized = true;
        }
    }

    // 提供静态方法供外部调用生成ID
    public static long nextId() {
        if (!initialized) {
            throw new IllegalStateException("ID生成器尚未初始化，请先调用initIdGenerator方法");
        }
        return YitIdHelper.nextId();
    }
}