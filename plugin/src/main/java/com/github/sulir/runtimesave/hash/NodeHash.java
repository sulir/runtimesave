package com.github.sulir.runtimesave.hash;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class NodeHash {
    private static final Base64.Encoder base64Encoder = Base64.getEncoder().withoutPadding();
    private static final Base64.Decoder base64Decoder = Base64.getDecoder();

    private final byte[] bytes;
    private String string;

    public static NodeHash fromString(String base64String) {
        return new NodeHash(base64Decoder.decode(base64String));
    }

    public NodeHash(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public String toString() {
        if (string == null)
            string = base64Encoder.encodeToString(bytes);
        return string;
    }

    public String toShortString() {
        return toString().substring(0, 6);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof NodeHash hash && MessageDigest.isEqual(bytes, hash.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }
}
