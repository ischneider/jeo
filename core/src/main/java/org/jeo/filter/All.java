package org.jeo.filter;

/**
 * Identity filter that always returns true.
 */
public class All extends Filter {

    @Override
    public boolean apply(Object obj) {
        return true;
    }

    @Override
    public Object accept(FilterVisitor v, Object obj) {
        return v.visit(this, obj);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof All;
    }

    @Override
    public int hashCode() {
        return All.class.getName().hashCode();
    }

    @Override
    public String toString() {
        return "All";
    }
}
