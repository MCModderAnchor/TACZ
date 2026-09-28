package com.tacz.guns.resource.serialize;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.tacz.guns.resource.pojo.data.attachment.Modifier;

import java.io.IOException;

/**
 * 在 Modifier 反序列化时预编译内联函数，覆盖数据包加载与网络同步。
 */
public class ModifierTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (type.getRawType() != Modifier.class) {
            return null;
        }
        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, value);
            }

            @Override
            public T read(JsonReader in) throws IOException {
                T value = delegate.read(in);
                if (value instanceof Modifier modifier) {
                    modifier.compileFunction();
                }
                return value;
            }
        };
    }
}
