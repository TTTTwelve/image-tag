package com.tag.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jiaqiwu on 2019/6/25.
 */
public enum ImageTagErrorType {

    UnknownError(0),
    IOError(1),
    TokenError(2),
    ApiError(3)
    ;

    private final int value;

    private static Map<Integer, ImageTagErrorType> valuesMap;

    static {
        valuesMap = new HashMap<>();
        for (ImageTagErrorType t : values()) {
            ImageTagErrorType exist = valuesMap.put(t.getValue(), t);
            if (exist != null) {
                throw new IllegalStateException("value冲突: " + exist + " " + t);
            }
        }
        valuesMap = Collections.unmodifiableMap(valuesMap);
    }

    private ImageTagErrorType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ImageTagErrorType fromValue(Integer value) {
        return valuesMap.get(value);
    }
}
