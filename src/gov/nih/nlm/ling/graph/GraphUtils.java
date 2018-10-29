package gov.nih.nlm.ling.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.AbstractBaseGraph;

/**
 * Contains generic static utility methods for graphs created
 * with JGraph.
 * 
 * @author Halil Kilicoglu
 *
 */
public class GraphUtils {

	/**
	 * Returns all roots of a multi-graph.
	 * 
	 * @param <V> 	the generic node type
	 * @param <E> 	the generic edge type
	 * @param graph	a graph
	 * 
	 * @return  a list of all its roots
	 */
	public static <V,E> List<V> getRoots(DirectedGraph<V,E> graph) {
		List<V> result = new ArrayList<>();
		Set<V> vertices = graph.vertexSet();
		for (V v : vertices) {
			if (graph.inDegreeOf(v) == 0) {
				result.add(v);
			}
		}
		return result;
	}

	/**
	 * 
	 * @param <V> 	the generic node type
	 * @param <E> 	the generic edge type
	 * @param graph	a graph
	 * @param node  a node in the graph
	 * @return  true if <var>node</var> is a leaf of the <var>graph</var>
	 */
	public static <V,E> boolean isLeaf(AbstractBaseGraph<V,E> graph, V node) {
		if (graph.outDegreeOf(node) == 0) return true;
		return false;
	}

	/**
	 * Attempts to pretty-print a graph
	 * 
	 * @param <V> 	the generic node type
	 * @param <E> 	the generic edge type
	 * @param graph	a graph
	 * @return  a readable representation of the graph
	 */
	public static <V,E> String prettyPrint(DirectedGraph<V,E> graph) {
		Collection<V> rootNodes = getRoots(graph);
		if (rootNodes.isEmpty()) {
			return "no root";
		}
		StringBuilder sb = new StringBuilder();
		Set<V> used = new HashSet<>();
		for (V root : rootNodes) {
			sb.append("-> ").append(root).append(" (root)\n");
			recToString(graph,root, sb, 1, used);
		}
		Set<V> nodes = new HashSet<V>(graph.vertexSet());
		nodes.removeAll(used);
		while (!nodes.isEmpty()) {
			V node = nodes.iterator().next();
			sb.append(node).append("\n");
			recToString(graph,node, sb, 1, used);
			nodes.removeAll(used);
		}
		return sb.toString();
	}

	private static <V,E> void recToString(DirectedGraph<V,E> graph, 
			V curr, StringBuilder sb, int offset, Set<V> used) {
		used.add(curr);
		List<E> edges = new ArrayList<>(graph.outgoingEdgesOf(curr));
		for (E edge : edges) {
			V target = graph.getEdgeTarget(edge);
			sb.append(space(2 * offset)).append("-> ").append(target).
			append(" (").append(edge.toString()).append(")\n");
			if (!used.contains(target)) { // recurse
				recToString(graph,target, sb, offset + 1, used);
			}
		}
	}

	private static String space(int width) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < width; i++) {
			b.append(" ");
		}
		return b.toString();
	}

	/**
	 * Finds all descendants of a node in the graph.
	 * 																			
	 * @param <V> 	the generic node type
	 * @param <E> 	the generic edge type
	 * @param graph	a graph
	 * @param node  a node in the graph
	 * @param descendants  the set of all nodes descending from <var>node</var>
	 */
	public static <V,E> void getAllDescendants(DirectedGraph<V,E> graph, V node, Set<V> descendants) {
		if (graph.containsVertex(node) == false) return;
		descendants.add(node);
		if (graph.outDegreeOf(node) == 0) {
			return;
		} 
		Set<V> successors = getSuccessors(graph,node);
		for (V s: successors) {
			if (descendants.contains(s)) continue;
			getAllDescendants(graph,s,descendants);
		};
	}

	/**
	 * Same as {@code #getAllDescendants(DirectedGraph, Object)}, except it considers 
	 * a list of nodes as input.
	 * 
	 * @param <V> 	the generic node type
	 * @param <E> 	the generic edge type
	 * @param graph	a graph
	 * @param nodes a list of nodes
	 * @param descendants  all descendants of the <var>nodes</var> in the <var>graph</var>
	 */
	public static <V,E> void getAllDescendants(DirectedGraph<V,E> graph, List<V> nodes, Set<V> descendants) {
		for (V v1: nodes) {
			if (graph.containsVertex(v1) == false) continue;
			getAllDescendants(graph,v1,descendants);
		}
	}

	/**
	 * Finds all successors of a node in the graph. Successors are all nodes
	 * with an incoming edge from the given node.
	 * 
	 * @param <V> 	the generic node type
	 * @param <E> 	the generic edge type
	 * @param graph	a graph
	 * @param node  a node in the graph
	 * @return  a set of all successors
	 */
	public static <V,E> Set<V> getSuccessors(DirectedGraph<V,E> graph, V node) {
		Set<V> out = new HashSet<>();
		if (graph.containsVertex(node) == false) return out;
		if (graph.outDegreeOf(node) == 0) {
			return out;
		} 
		Set<E> edges = graph.outgoingEdgesOf(node);
		for (E e: edges) {
			if (nonDescendant(e)) continue;
			V v1 = graph.getEdgeTarget(e);
			out.add(v1);
		}
		return out;
	}

	/**
	 * Finds all predecessors of a node in the graph. Precedessors are all nodes
	 * with an outgoing edge to the given node.
	 * 
	 * @param <V> 	the generic node type
	 * @param <E> 	the generic edge type
	 * @param graph	a graph
	 * @param node  a node in the graph
	 * @return  a set of all predecessors
	 */
	public static <V,E> Set<V> getPrecedessors(DirectedGraph<V,E> graph, V node) {
		Set<V> out = new HashSet<>();
		if (graph.containsVertex(node) == false) return out;
		if (graph.inDegreeOf(node) == 0) {
			return out;
		} 
		Set<E> edges = graph.incomingEdgesOf(node);
		for (E e: edges) {
			if (nonDescendant(e)) continue;
			V v1 = graph.getEdgeSource(e);
			out.add(v1);
		}
		return out;
	}

	private static <E> boolean nonDescendant(E e) {
		return (e instanceof DummyEdge && ((DummyEdge)e).betweenSentences());
	}
}
