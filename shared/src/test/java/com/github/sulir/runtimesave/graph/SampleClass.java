package com.github.sulir.runtimesave.graph;

public class SampleClass {
    private final int number;
    private final String text;
    private SampleClass reference;

    public SampleClass(int number, String text) {
        this.number = number;
        this.text = text;
    }

    public int getNumber() {
        return number;
    }

    public String getText() {
        return text;
    }

    public SampleClass getReference() {
        return reference;
    }

    public void setReference(SampleClass reference) {
        this.reference = reference;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SampleClass other))
            return false;

        return number == other.number && text.equals(other.text);
    }
}
