/*
 * Copyright 2010 Sean M. Foy
 * 
 *  This program is free software: you can redistribute it and/or modify
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class Greenspun {
	public static <T> int hashCode(T t, Func1<T, ? extends Map<String, ?>> f) {
		Map<?, ?> valueEquality = f.f(t);
		int result = 0;
		for (Map.Entry<?, ?> i : valueEquality.entrySet()) {
			result = result << 27 ^ result >> 5 ^ i.getValue().hashCode();
		}
		return result;
	}
	public static <T> String toString(T t, Func1<T, ? extends Map<String, ?>> f) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("(%s ", t.getClass().getName()));
		for (Map.Entry<?, ?> i : f.f(t).entrySet()) {
			sb.append(String.format(":%s %s ", i.getKey(), i.getValue()));
		}
		sb.delete(sb.length() - 1, sb.length());
		sb.append(")");
		return sb.toString();
	}
	public static <T> boolean equals(T a, Object b, Func1<T, ? extends Map<String, ?>> f) {
		if (a == b) return true;
		if (a == null ^ b == null) return false;
		if (a == null) return true;
		
		if (!a.getClass().isAssignableFrom(b.getClass())) return false;
		@SuppressWarnings("unchecked")
		T tb = (T)b;
		
		Map<String, ?> ma = f.f(a);
		Map<String, ?> mb = f.f(tb);
		for (Map.Entry<String, ?> kva : ma.entrySet()) {
			if (!kva.getValue().equals(mb.get(kva.getKey()))) return false;
		}
		return true;
	}
	public abstract static class ROIterator<T> implements Iterator<T> {
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	public static <T, U> Iterable<Pair<T, U>> zip(final Iterable<T> ts, final Iterable<U> us) {
		return new Iterable<Pair<T, U>>() {
			public Iterator<Pair<T, U>> iterator() {
				final Iterator<T> tsi = ts.iterator();
				final Iterator<U> usi = us.iterator();
				return new ROIterator<Pair<T, U>>() {
					public boolean hasNext() {
						return tsi.hasNext() && usi.hasNext();
					}
					public Pair<T, U> next() {
						return Pair.makePair(tsi.next(), usi.next());
					}
				};
			}
		};
	}
	public static <T, U> U reduce(Func2<U, T, U> f, U init, final Iterable<T> xs) {
		U a = init;
		for (T t : xs) {
			a = f.f(a, t);
		}
		return a;
	}
	public static <T> int count(Iterable<T> ts) {
		int i = 0;
		for (@SuppressWarnings("unused") T _ : ts) ++i;
		return i;
	}
	public static <T, U> Iterable<U> map(final Func1<T, U> f, final Iterable<T> xs) {
	    return new Iterable<U>() {
            public Iterator<U> iterator() {
                final Iterator<T> xsi = xs.iterator();
                return new ROIterator<U>() {
                    public boolean hasNext() {
                        return xsi.hasNext();
                    }
                    public U next() {
                        return f.f(xsi.next());
                    }
                };
            }
	    };
	}
	public interface Func0<T> {
		public T f();
	}
	public interface Func1<T, U> {
		public U f(T t);
	}
	public interface Func2<T, U, V> {
		public V f(T t, U u);
	}
	public static class Pair<T, U> {
		public T fst;
		public U snd;
		public Pair(T fst, U snd) {
			this.fst = fst;
			this.snd = snd;
		}
		public static <T, U> Pair<T, U> makePair(T fst, U snd) {
		    return new Pair<T, U>(fst, snd);
		}
		public Map<String, Object> getVE() {
			Map<String, Object> result = new HashMap<String, Object>();
			result.put("fst", fst);
			result.put("snd", snd);
			return result;
		}
		private Func1<Pair<T, U>, Map<String, Object>> fGetVE =
			new Func1<Pair<T, U>, Map<String, Object>>() {
				public Map<String, Object> f(Pair<T, U> p) {
					return p.getVE();
				}
			};
		public int hashCode() {
			return Greenspun.hashCode(this, fGetVE);
		}
		public boolean equals(Object o) {
			return Greenspun.equals(this, o, fGetVE);
		}
		public String toString() {
			return Greenspun.toString(this, fGetVE);
		}
	}
	
    public interface Disposable<T extends Throwable> {
        public void close() throws T;
    }
    public static <R> void dispose(boolean supress, R res) {
        if (!(res instanceof Disposable<?>)) return;
        Disposable<?> d = (Disposable<?>)res;
        try {
            d.close();
        }
        catch (Throwable e) {
            if (!supress) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException)e;
                }
                // :-(
                throw new RuntimeException(e);
            }
            // else let the outer exception propagate
        }
    }
    public static <R, T> T enhtry(R res, Func1<R, T> f) {
        boolean pass = false;
        try {
            T t = f.f(res);
            pass = true;
            return t;
        }
        finally {
            dispose(!pass, res);
        }
    }
    private static class EnhforBreakException extends RuntimeException {}
    public static void enhforbreak() {
        throw new EnhforBreakException();
    }
    public static <T, U> void enhfor(Iterable<T> ts, final Func1<T, Void> f) {
        enhtry(
            ts.iterator(),
            new Func1<Iterator<T>, Void>() {
                public Void f(Iterator<T> tsi) {
                    try {
                        while (tsi.hasNext()) {
                            f.f(tsi.next());
                        }
                    }
                    catch (EnhforBreakException brk) {
                        // ignore
                    }
                    return null;
                }
            });
    }
    public static <K, V> V setDefault(
            Map<K, V> dict,
            K key, V dflt) {
        if (!dict.containsKey(key)) {
            dict.put(key, dflt);
        }
        return dict.get(key);
    }
    public static <K, V> V setDefault(
            Map<K, V> dict,
            K key,
            Func0<V> makeDefault) {        
        if (!dict.containsKey(key)) {
            dict.put(key, makeDefault.f());
        }
        return dict.get(key);
    }

    public static void sleep(long millis) {
        long deadline = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(Math.max(0, deadline - System.currentTimeMillis()));
            }
            catch (InterruptedException e) {
                // try, try again
            }
        }
    }
    
    public static <T> Iterable<Set<T>> combinations(final int choose, final T... ts) {
        final int lastFirstElement = ts.length - choose;
        return new Iterable<Set<T>>() {
            public Iterator<Set<T>> iterator() {
                final int pastTheEnd = -1;
                final int [] cursor = new int[choose];
                for (int i = 0; i < choose; ++i) {
                    cursor[i] = i;
                }
                return new ROIterator<Set<T>>() {
                    public boolean hasNext() {
                        return cursor[0] != pastTheEnd;
                    }
                    public Set<T> next() {
                        Set<T> result = new HashSet<T>();
                        for (int i = 0; i < choose; ++i) {
                            result.add(ts[cursor[i]]);
                        }
                        advance();
                        return result;
                    }
                    private void advance() {
                        if (cursor[0] == lastFirstElement) {
                            cursor[0] = pastTheEnd;
                            return;
                        }
                        for (int i = choose - 1; i >= 0; --i) {
                            if (cursor[i] < ts.length - 1) {
                                ++cursor[i];
                                for (int j = i + 1; j < choose; ++j) {
                                    cursor[j] = cursor[j - 1] + 1;
                                }
                                break;
                            }
                        }
                    }
                };
            }
        };
    }
}
