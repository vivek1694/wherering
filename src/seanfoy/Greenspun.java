package seanfoy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
						return new Pair<T, U>(tsi.next(), usi.next());
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
		for (T _ : ts) ++i;
		return i;
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
}
