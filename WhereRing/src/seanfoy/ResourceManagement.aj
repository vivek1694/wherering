package seanfoy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

/**
 * Automatically free resources
 * associated with the Iterators
 * produced by arbitrary Iterables
 * within the dcf of enhtry.
 */
public aspect ResourceManagement {
    pointcut tryiter() :
        within(seanfoy..*) &&
        execution(* seanfoy.Greenspun.enhtry(..)) &&
        args(Iterable+, ..);
    @SuppressWarnings("unchecked")
    pointcut makemess(Iterable ita, Iterable itb) :
        cflow(tryiter() && args(ita, ..)) &&
        call(Iterator+ Iterable+.iterator()) &&
        target(itb) &&
        if(ita == itb);
    Stack<List<Iterator<?>>> enhtryscopes =
        new Stack<List<Iterator<?>>>();
    before() : tryiter() {
        enhtryscopes.add(makeframe());
    }
    @SuppressWarnings("unchecked")
    after(Iterable ita, Iterable itb) returning (Iterator iter): makemess(ita, itb) {
        enhtryscopes.peek().add(iter);
    }
    after() : tryiter() {
        cleanup(enhtryscopes.pop());
    }
    List<Iterator<?>> makeframe() {
        return new ArrayList<Iterator<?>>(); 
    }
    void cleanup(List<Iterator<?>> res) {
        for (Iterator<?> garbage : res) {
            Greenspun.dispose(true, garbage);
        }
    }
}
