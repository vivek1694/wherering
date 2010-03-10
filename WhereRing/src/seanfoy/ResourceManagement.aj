/*
 * Copyright 2010 Sean M. Foy
 * 
 * This file is part of WhereRing.
 *
 *  WhereRing is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  WhereRing is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with WhereRing.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
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
