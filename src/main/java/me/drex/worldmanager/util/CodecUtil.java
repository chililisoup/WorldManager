package me.drex.worldmanager.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import java.util.Optional;
import java.util.function.Supplier;

public class CodecUtil {
    // Optional when decoding (uses default value), but always encoded
    public static <A> MapCodec<A> optionalFieldOf(final Codec<A> codec, final String name, final Supplier<A> defaultValue, final boolean lenient) {
        return Codec.optionalField(name, codec, lenient).xmap(
            o -> o.orElse(defaultValue.get()),
            Optional::of
        );
    }
}
