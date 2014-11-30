package diff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * A simplified port of SequenceMatcher from python difflib
 * 
 * @author Sun, Huanliang (Jimmy)
 * @param <T>
 */
public class SequenceMatcher<T> {
	protected List<T> a = null;
	protected List<T> b = null;
	protected Map<T, List<Integer>> b2j = null;
	protected Map<T, Integer> fullbcount = new HashMap<T, Integer>();
	protected Set<T> bjunk = null;
	protected List<OpCode> opcodes = null;
	protected List<Match> matching_blocks = null;
	
	class Match {
		protected int i;
		protected int j;
		protected int size;
		Match(int i, int j, int size) {
			this.i = i;
			this.j = j;
			this.size = size;
		}
	}
	
	enum OpTag {
		Replace,
		Delete,
		Insert,
		Equal
	}

	public class OpCode {
		public OpTag tag;
		public int i1;
		public int i2;
		public int j1;
		public int j2;
		public OpCode(OpTag tag, int i1, int i2, int j1, int j2) {
			this.tag = tag;
			this.i1 = i1;
			this.i2 = i2;
			this.j1 = j1;
			this.j2 = j2;
		}
		public String toString() {
			String s = tag.toString();
			s += " a["+i1+":"+i2+"]=" + a.subList(i1, i2);
            s += " b["+j1+":"+j2+"]=" + b.subList(j1, j2);
			return s;
		}
	}

	public SequenceMatcher(List<T> a, List<T> b) {
		this.set_seqs(a, b);
	}
	
	public void set_seqs(List<T> a, List<T> b) {
		this.set_seq1(a);
		this.set_seq2(b);
	}
	
	public void set_seq1(List<T> a) {
		if (a == this.a) {
			return;
		}
		this.a = a;
		this.matching_blocks = null;
		this.opcodes = null;
	}

	public void set_seq2(List<T> b) {
		if (b == this.b) {
			return;
		}
		this.b = b;
		this.matching_blocks = null;
		this.opcodes = null;
		this.fullbcount = null;
		this.__chain_b();
	}
	
	protected void __chain_b() {
		this.b2j = new HashMap<T, List<Integer>>();
		for (int i=0; i<b.size(); i++) {
			T e = b.get(i);
			List<Integer> idxs = b2j.get(e);
			if (idxs == null) {
				idxs = new ArrayList<Integer>();
				b2j.put(e, idxs);
			}
			idxs.add(i);
		}

		// Purge junk elements
		this.bjunk = new HashSet<T>();
		//
	}
	
	protected boolean isbjunk(T e) {
		return this.bjunk.contains(e);
	}
	
	public Match find_longest_match(int alo, int ahi, int blo, int bhi) {
		int besti = alo;
		int bestj = blo;
		int bestsize = 0;
		
		Map<Integer, Integer> j2len = new HashMap<Integer, Integer>();
		for (int i=alo; i<ahi; i++) {
			List<Integer> idxs = b2j.get(a.get(i));
			if (idxs == null) {
				idxs = new ArrayList<Integer>();
			}
			Map<Integer, Integer> newj2len = new HashMap<Integer, Integer>();
			for (int j : idxs) {
				// a[i] matches b[j]
				if (j < blo) {
					continue;
				}
				if (j >= bhi) {
					break;
				}
				Integer t = j2len.get(j-1);
				if (t == null) {
					t = 0;
				}
				int k = t + 1;
				newj2len.put(j, k);
				if (k > bestsize) {
					besti = i-k+1;
					bestj = j-k+1;
					bestsize = k;
				}
			}
			j2len = newj2len;
		}
		
		while (besti > alo && bestj > blo &&
				!isbjunk(b.get(bestj-1)) &&
				a.get(besti-1) == b.get(bestj-1)) {
			besti = besti-1;
			bestj = bestj-1;
			bestsize = bestsize+1;
		}
	    while (besti+bestsize < ahi && bestj+bestsize < bhi &&
	    		!isbjunk(b.get(bestj+bestsize)) &&
	    		a.get(besti+bestsize) == b.get(bestj+bestsize))	{
	    	bestsize += 1;
	    }
	    
	    // 
	    
	    while (besti > alo && bestj > blo &&
	    		isbjunk(b.get(bestj-1)) &&
	    		a.get(besti-1) == b.get(bestj-1)) {
			besti = besti-1;
			bestj = bestj-1;
			bestsize = bestsize+1;
	    }
	    while (besti+bestsize < ahi && bestj+bestsize < bhi &&
	    		isbjunk(b.get(bestj+bestsize)) &&
	    		a.get(besti+bestsize) == b.get(bestj+bestsize))	{
	    	bestsize = bestsize + 1;
	    }
	    return new Match(besti, bestj, bestsize);
	}
	
	class Indices {
		int alo;
		int ahi;
		int blo;
		int bhi;
		Indices(int alo, int ahi, int blo, int bhi) {
			this.alo = alo;
			this.ahi = ahi;
			this.blo = blo;
			this.bhi = bhi;
		}
	}

	public List<Match> get_matching_blocks() {
		if (this.matching_blocks != null) {
			return this.matching_blocks;
		}
		int la = a.size();
		int lb = b.size();
		
		Stack<Indices> queue = new Stack<Indices>();
		queue.push(new Indices(0, la, 0, lb));
		List<Match> matching_blocks = new ArrayList<Match>();
		while (! queue.isEmpty()) {
			Indices idx = queue.pop();
			Match x = this.find_longest_match(idx.alo, idx.ahi, idx.blo, idx.bhi);
			int i = x.i;
			int j = x.j;
			int k = x.size;
			if (k > 0) {
				matching_blocks.add(x);
				if (idx.alo < i && idx.blo < j) {
					queue.push(new Indices(idx.alo, i, idx.blo, j));
				}
				if (i+k < idx.ahi && j+k < idx.bhi) {
					queue.push(new Indices(i+k, idx.ahi, j+k, idx.bhi));
				}
			}
		}
		matching_blocks.sort(new Comparator<Match>() {
			@Override
			public int compare(SequenceMatcher<T>.Match arg0,
					SequenceMatcher<T>.Match arg1) {
				// TODO Auto-generated method stub
				return 0;
			}
		});

		Match m1 = new Match(0, 0, 0);
		List<Match> non_adjacent = new ArrayList<Match>();
		for (Match m2 : matching_blocks) {
			if (m1.i + m1.size == m2.i && m1.j + m1.size == m2.j) {
				m1.size += m2.size;
			} else {
				if (m1.size > 0) {
					non_adjacent.add(m1);
				}
				m1 = new Match(m2.i, m2.j, m2.size);
			}
		}
		if (m1.size > 0) {
			non_adjacent.add(m1);
		}
		non_adjacent.add(new Match(la, lb, 0));
		this.matching_blocks = non_adjacent;
		// map(Match._make, this.matching_blocks);
		return this.matching_blocks;
	}
	
	public List<OpCode> get_opcodes() {
		if (this.opcodes != null) {
			return this.opcodes;
		}
		int i = 0, j = 0;
		List<OpCode> answer = this.opcodes = new ArrayList<OpCode>();
		for (Match m : this.get_matching_blocks()) {
			OpTag tag = null;
			if (i < m.i && j < m.j) {
				tag = OpTag.Replace;
			} else if (i < m.i) {
				tag = OpTag.Delete;
			} else if (j < m.j) {
				tag = OpTag.Insert;
			}
			if (tag != null) {
				answer.add(new OpCode(tag, i, m.i, j, m.j));
			}
			i = m.i + m.size;
			j = m.j + m.size;
			if (m.size > 0) {
				answer.add(new OpCode(OpTag.Equal, m.i, i, m.j, j));
			}
		}
		return answer;
	}

	public static void main(String[] args) {
		String aa = "qabxcd";
		String bb = "abycdf";

		List<String> a = Arrays.asList(aa.split(""));
		List<String> b = Arrays.asList(bb.split(""));

		SequenceMatcher<String> s = new SequenceMatcher<String>(a, b);

		System.out.println("a = "+a);
		System.out.println("b = "+b);
		for (SequenceMatcher<?>.OpCode c : s.get_opcodes()) {
			System.out.println(c);
		}
	}
}
