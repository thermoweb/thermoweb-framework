package org.thermoweb.core.data;

public class Pair<L, R> {

    private L left;
    private R right;

    public Pair() {

    }

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return this.left;
    }

    public R getRight() {
        return this.right;
    }

    public void setLeft(L left) {
        this.left = left;
    }

    public void setRight(R right) {
        this.right = right;
    }

    public String toString() {
        return "(" + left.toString() + ", " + right.toString() + ")";
    }
}
