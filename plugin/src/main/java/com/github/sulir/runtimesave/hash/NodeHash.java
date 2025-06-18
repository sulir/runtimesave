package com.github.sulir.runtimesave.hash;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class NodeHash {
    private static final Base64.Encoder base64 = Base64.getEncoder().withoutPadding();

    private final byte[] digest;
    private String string;

    public NodeHash(byte[] digest) {
        this.digest = digest;
    }

    @Override
    public String toString() {
        if (string == null)
            string = base64.encodeToString(digest);
        return string;
    }

    public String toShortString() {
        return toString().substring(0, 6);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof NodeHash hash && MessageDigest.isEqual(digest, hash.digest);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(digest);
    }
}
