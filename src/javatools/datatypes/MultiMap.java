package javatools.datatypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga).

This class can be used to keep track of a non-functional, directed relation
*/
public class MultiMap<A, B> {
  private Map<A, Set<B>> relation;
  
  public MultiMap() {
    relation = new HashMap<A, Set<B>>();
  }
  
  public MultiMap(int initialSize) {
    relation = new HashMap<A, Set<B>>(initialSize);
  }
  
  public void put(A a, B b) {
    Set<B> bs = relation.get(a);
    
    if (bs == null) {
      bs = new HashSet<B>();
      relation.put(a, bs);
    }
    
    bs.add(b);
  }
  
  public Set<B> get(A a) {
    return relation.get(a);
  }
}