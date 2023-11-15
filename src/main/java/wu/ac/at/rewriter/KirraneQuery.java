package wu.ac.at.rewriter;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.core.*;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.path.P_OneOrMore1;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.syntax.*;
import org.apache.jena.sparql.util.NodeFactoryExtra;

public class KirraneQuery implements RDFQuery{

	private enum ElementType { BGP, FILTER, MINUS, SUBQUERY, AGGREGATES, PROPERTYPATH};
	private Query query;
	private List<ElementType> elementTypes; 
	List<Quad> bgpList = null;
	List<Element> negationList = null;
	ElementFilter newFilter = null;
	ElementMinus newMinus = null;
	ArrayList<ElementMinus> minusFilters = null;
	ArrayList<ElementFilter> existsFilters = null;
	
	public KirraneQuery(String q) throws Exception{
		this.query = QueryFactory.create(q, Syntax.syntaxSPARQL_11);
		setQueryType();
	}
		
    public void addBGPFilter(List<RDFQuad> policies, boolean filter) throws Exception{    
    	addBGPFilter(policies, query, filter);
    }
    
    public void addBGPFilter(List<RDFQuad> policies, Query query, boolean filter) throws Exception{      	
		//update the query for each matched authorisation
		for(RDFQuad policy : policies){
			updateBGPQuery(policy, query, filter);
		}
    	//addOuterSelect(policies);
    } 
       
	public void updateBGPQuery(RDFQuad policy, Query query, boolean filter) throws Exception{			
		List<Quad> optList = null;
    	List<Quad> filterList = null;
    	List<Integer> removeTripleList = null;
    	List<Quad> matchedList = null;
    	List<TriplePath> propertyPathList=null;  
    	List<Integer> removeNotExistsList = null;    	
    	boolean innerFilter = false;
    	boolean filterAll = false;
		//get all the SPARQL query elements
		List<Element> elementList = ((ElementGroup) query.getQueryPattern()).getElements();
		Quad newQuad = null;
		int findex = 0;
		ElementFilter newInnerFilter = null;
		
		//get the authorisation
		Quad authorisation = policy.getQuad();
		bgpList = null;
    	//loop through all query elements
		for (Element elem : elementList){
			innerFilter = false;
			if(elem instanceof ElementNamedGraph){
            	//store the graph for future reference
            	Node graph = ((ElementNamedGraph) elem).getGraphNameNode();
            	//get the namedGraph is a group
            	Element namedGraph = ((ElementNamedGraph) elem).getElement();
            	if(namedGraph instanceof ElementGroup){
	            	List<Element> list = ((ElementGroup) namedGraph).getElements();
	    			if(bgpList == null)
	    				bgpList = new ArrayList<Quad>();
	            	for(Element e :list){
	            		findex++;
	            		//Check Path, Filter or Minus
			            if(e instanceof ElementPathBlock){
			            	ElementPathBlock pathBlock = (ElementPathBlock) e;
			            	int cnt=0;
			            	//Loop through all the triples
			            	for(TriplePath path : pathBlock.getPattern().getList()){
			            		//Check triple or property path
			            		if(path.isTriple()){
				            		Triple triple = path.asTriple();
			            			//use for index for removing triples
				            		cnt++;
				            		Quad q = Quad.create(graph, triple.getSubject(), triple.getPredicate(), triple.getObject());
				            		bgpList.add(q);
				            		//if the graph pattern matches the authorisation graph pattern			            		
				            		if(authorisation.getSubject().isVariable() && authorisation.getPredicate().isVariable() 
				            				&& authorisation.getObject().isVariable() && authorisation.getGraph().isVariable())
				            			filterAll = true;
				            		else if (isExactMatch(q, authorisation)){
				            			filterAll = true;
				            		}
				            		else {
					            		if(isMatch(q, authorisation)){
					            			if(elementTypes != null){					            				
				            					if(matchedList == null)
				            						matchedList = new ArrayList<Quad>();
							            		Quad quad = new Quad(graph, triple);
							            		matchedList.add(quad);							
					            			}

					            		}
				            		}
			            		} else {
			            			if(filter){
			            				Path temp = path.getPath();
			            				Quad qpp = null;
			            				if(temp instanceof P_OneOrMore1){
			            					P_OneOrMore1 ptemp = (P_OneOrMore1) temp;
			            					String ttemp = ptemp.getSubPath().toString();
					            			qpp = Quad.create(graph, path.getSubject(), NodeFactoryExtra.parseNode(ttemp), path.getSubject());
			            				}
			            				else if(temp instanceof P_ZeroOrMore1){
			            					P_ZeroOrMore1 ptemp = (P_ZeroOrMore1) temp;
			            					String ttemp = ptemp.getSubPath().toString();
					            			qpp = Quad.create(graph, path.getSubject(), NodeFactoryExtra.parseNode(ttemp), path.getSubject());
			            				}
			            				
					            		if(isMatch(qpp, authorisation)){
					            			if(elementTypes != null){					            				
				            					if(propertyPathList == null)
				            						propertyPathList = new ArrayList<TriplePath>();
				            					propertyPathList.add(path);
					            			}
					            		}
			            			}
			            		}
			            	}
			            	//need to make the triples optional so remove them now
			            	if(removeTripleList != null)
				            	for(int index : removeTripleList )
				            		pathBlock.getPattern().getList().remove(index-1);
			            	
		                }
	            		else if(e instanceof ElementSubQuery){
	            			Query subQuery = ((ElementSubQuery) e).getQuery();
	            			List<Element> subList = ((ElementGroup) subQuery.getQueryPattern()).getElements();
            				addInnerNotFilter(subList, authorisation);
			            }
	            		else if(e instanceof ElementFilter){
	            			if(filter){
		            			if(e instanceof ElementPathBlock){
		            		    	ElementPathBlock pathBlock = (ElementPathBlock) e;
		            		    	filterList = addFilter(pathBlock, graph, authorisation, filterList);
		            		    	
		            			} else {
		            				ElementFilter fil = ((ElementFilter) e);
			            			Expr exp = fil.getExpr();
			            			if(exp instanceof E_NotExists){
			            				Element exists = ((E_NotExists) exp).getElement();
			            				List<Element> expList = ((ElementGroup) exists).getElements();
			            				addInnerNotFilter(expList, authorisation);	            				

			            			} else if(exp instanceof E_Exists){
			            				Element exists = ((E_Exists) exp).getElement();
			            				List<Element> expList = ((ElementGroup) exists).getElements();		            						            		
			            				addInnerNotFilter(expList, authorisation);
			            			}
		            			}
	            			}
			            }
	            		else if(e instanceof ElementMinus){	
	            			if(filter){
	            				
		            			Element minus = ((ElementMinus) e).getMinusElement();
		    	            	List<Element> minusList = ((ElementGroup) minus).getElements();

//SK Add a minus to the minus		    	            	
//		    	            	addInnerMinus(minusList, authorisation);
	            				
//SK Add a filter not exists to the minus
	            				addInnerNotFilter(minusList, authorisation);
	            				
//SK Add a filter to the minus	            				
//		    	            	List<Quad> matchedQuads = addInnerFilter(minusList, authorisation);
//		    					for(Quad q : matchedQuads){
//		    						addNotEqualsOrQuadFilter((ElementGroup) minus, authorisation, q);  
//		    					}

//SK remove the entire minus		    					
//		    					if(removeNotExistsList == null)
//		    						removeNotExistsList = new ArrayList<Integer>();
//		    					removeNotExistsList.add(findex-1);
	            			}
			            }
	            	}
        			if(propertyPathList != null){
        				for(TriplePath path : propertyPathList){
        					Path temp = path.getPath();
            				Quad qpp = null;
            				if(temp instanceof P_OneOrMore1){
            					P_OneOrMore1 ptemp = (P_OneOrMore1) temp;
            					String ttemp = ptemp.getSubPath().toString();
		            			qpp = Quad.create(graph, path.getSubject(), NodeFactoryExtra.parseNode(ttemp), path.getSubject());
            				}
            				else if(temp instanceof P_ZeroOrMore1){
            					P_ZeroOrMore1 ptemp = (P_ZeroOrMore1) temp;
            					String ttemp = ptemp.getSubPath().toString();
		            			qpp = Quad.create(graph, path.getSubject(), NodeFactoryExtra.parseNode(ttemp), path.getSubject());
            				}

		            		addNotExistsFilter(elem, path, qpp, authorisation);	
		            		if(newFilter != null) 
		            			((ElementGroup) namedGraph).addElement(newFilter);
		            		
		            		matchedList = null;
		            		newFilter = null;
        				}
	            	}	
        			else {
		            	if(filterAll){
		            		addNotExistsFilter(list, authorisation);
		            	} 
		            	else {
		            		if(filter){
	
		    	            	if(removeNotExistsList != null){
				            		for(Integer i : removeNotExistsList){
				            			int it = (int)i;
				            			list.remove(it);
				            		}
				            		removeNotExistsList = null;
		            			}
//SK add filter exists to the outer query			    	            	
		    	        		if(!filterAll){
		    	        			if(matchedList != null) {
		    	        	            //BGP
//SK use minus instead of not exists		    	        				
//		    	        				addMinus(elem, authorisation);	

//SK use exists instead of not minus		    	        				
		    	        	    		addNotExistsFilter(elem, authorisation);	

		    	        	    		if(newFilter != null) 
		    	        	    			((ElementGroup) namedGraph).addElement(newFilter);
//SK use minus instead of not exists
//		    	        	    		if(newMinus != null) 
//		    	        	    			((ElementGroup) namedGraph).addElement(newMinus);
		    	        	    		
		    	        	    		matchedList = null;
		    	        	    		newFilter = null;
		    	        	    		newMinus = null;
		    	        	    	}	
		    	        		}		    	            	        				        
				            }				
		            	}
        			}
            	}
	        }
		}
//SK add plain filters to the outer query		
//		if(!filterAll){
//			if(matchedList != null) {
//	            //BGP
//				for(Quad q : matchedList){
//					addNotEqualsOrQuadFilter((ElementGroup) query.getQueryPattern(), authorisation, q);  
//				}	    		
//	    		matchedList = null;
//	    		newFilter = null;
//	    	}	
//		}
	}

	
	private void setQueryType() throws Exception{
		//Determine the type of query we are dealing with
		String q = query.getQueryPattern().toString().toLowerCase();
		elementTypes = new ArrayList<ElementType>();
		if(q.contains("filter"))
			elementTypes.add(ElementType.FILTER);	
		if(q.contains("minus"))
			elementTypes.add(ElementType.MINUS);	
		if(q.contains("select"))
			elementTypes.add(ElementType.SUBQUERY);
	}
		
	private List<Quad> addFilter(ElementPathBlock pathBlock, Node g, Quad pattern, List<Quad> filterList) throws Exception {
    	for(TriplePath path : pathBlock.getPattern().getList()){
    		Triple t = null;
    		if(path.getPredicate() == null){    			
    			String p=path.getPath().toString();
    			p = p.substring(2, p.length()-3);
    			Node predicate = NodeFactory.createURI(p);
    			t = Triple.create(path.getSubject(), predicate, path.getObject());
    		}else    		
    			t = path.asTriple();
    		
    		//if the graph pattern matches the authorisation graph pattern
    		Quad q = Quad.create(g, t.getSubject(), t.getPredicate(), t.getObject());
    		if(isMatch(q, pattern)){
    			if(filterList == null)
    				filterList = new ArrayList<Quad>();
    			//add triple to BGP filter
    			filterList.add(q);
    		}
    	}
    	return filterList;
	}


	private boolean isMatch(Node q, Node p) throws Exception{
		if ((q.toString().equals(p.toString())) 
		|| (q.isVariable() && p.isURI())
		|| (q.isVariable() && p.isLiteral())
		|| (p.isVariable() && q.isURI())
		|| (p.isVariable() && q.isLiteral())
		|| (p.isVariable() && q.isVariable()))
			return true;
		else
			return false;
	}

	private boolean isExactMatch(Node q, Node p) throws Exception{
		if ((q.toString().equals(p.toString()))
				|| (p.isVariable() && q.isURI())
				|| (p.isVariable() && q.isLiteral())
		|| (p.isVariable() && q.isVariable()))
			return true;
		else
			return false;
	}

	private boolean isExactMatch(Quad q, Quad p) throws Exception{
		//check if the graph pattern matches the authorisation graph pattern
		if(isExactMatch(q.getGraph(), p.getGraph()) &&
				isExactMatch(q.getSubject(), p.getSubject()) &&
				isExactMatch(q.getPredicate(), p.getPredicate()) &&
				isExactMatch(q.getObject(), p.getObject()))
			return true;
		else
			return false;
	}

	private boolean isMatch(Quad q, Quad p) throws Exception{
		//check if the graph pattern matches the authorisation graph pattern
		if(isMatch(q.getGraph(), p.getGraph()) &&
					isMatch(q.getSubject(), p.getSubject()) &&
							isMatch(q.getPredicate(), p.getPredicate()) &&
									isMatch(q.getObject(), p.getObject()))
			return true;
		else
			return false;
	}
	
//  SK ISWC Update don't make them optional    	            		
//	private ElementGroup addOptional(List<Element> list, Triple t) throws Exception{
//		//Add triple to optional to group
//		Triple triple = Triple.create(t.getSubject(), t.getPredicate(), t.getObject());
//		ElementGroup optionalGroup = new ElementGroup();
//    	ElementOptional optional = new ElementOptional(optionalGroup);   	
//    	optionalGroup.addTriplePattern(triple);
//    	list.add(optional);
//    	return optionalGroup;
//	}
					
	private boolean addEqualsOrQuadFilter(ElementGroup group, Quad auth, Quad quad) throws Exception{
		
		Expr exprSub = null;
		Expr exprPred = null;
		Expr exprObj = null;
		Expr exprAnd = null;
		ElementFilter new_filter = null;
		
		//Add filters for each not equals relationship
		if(!auth.getSubject().isVariable() && quad.getSubject().isVariable())
			exprSub = new E_Equals(new ExprVar(quad.getSubject()), NodeValue.makeNode(auth.getSubject()));
		
		if(!auth.getPredicate().isVariable() && quad.getPredicate().isVariable())
			exprPred = new E_Equals(new ExprVar(quad.getPredicate()), NodeValue.makeNode(auth.getPredicate()));

		if(!auth.getObject().isVariable() && quad.getObject().isVariable())
			exprObj = new E_Equals(new ExprVar(quad.getObject()), NodeValue.makeNode(auth.getObject()));

		if(exprSub != null){
			if(exprPred !=null){
				exprAnd = new E_LogicalOr(exprSub, exprPred);				
				if(exprObj != null){
					exprAnd = new E_LogicalOr(exprAnd, exprObj);
				}
			}
			else if(exprObj !=null){
				exprAnd = new E_LogicalOr(exprSub, exprObj);						
			}

		} else if(exprPred !=null) {			
				if(exprObj != null){
					exprAnd = new E_LogicalOr(exprPred, exprObj);	
				}	
		} 
		
		if(exprAnd != null)
			new_filter = new ElementFilter(exprAnd);
		else if(exprSub != null)
			new_filter = new ElementFilter(exprSub);
		else if(exprPred != null)
			new_filter = new ElementFilter(exprPred);
		else if(exprObj != null)
			new_filter = new ElementFilter(exprObj);	

		if(new_filter != null){
			group.addElementFilter(new_filter);
			return true;
		}
		return false;
	}
	
	private boolean addNotEqualsOrQuadFilter(ElementGroup group, Quad auth, Quad quad) throws Exception{
		
		Expr exprSub = null;
		Expr exprPred = null;
		Expr exprObj = null;
		Expr exprAnd = null;
		Expr exprGraph = null;
		ElementFilter new_filter = null;
		

		//Add filters for each not equals relationship
		if(!auth.getSubject().isVariable() && quad.getSubject().isVariable())
			exprSub = new E_LogicalNot(new E_SameTerm(new ExprVar(quad.getSubject()), NodeValue.makeNode(auth.getSubject())));
		
		if(!auth.getPredicate().isVariable() && quad.getPredicate().isVariable())
			exprPred = new E_LogicalNot(new E_SameTerm(new ExprVar(quad.getPredicate()), NodeValue.makeNode(auth.getPredicate())));

		if(!auth.getObject().isVariable() && quad.getObject().isVariable())
			exprObj = new E_LogicalNot(new E_SameTerm(new ExprVar(quad.getObject()), NodeValue.makeNode(auth.getObject())));

		if(!auth.getGraph().isVariable() && quad.getGraph().isVariable())
			exprGraph = new E_LogicalNot(new E_SameTerm(new ExprVar(quad.getGraph()), NodeValue.makeNode(auth.getGraph())));

		if(exprSub != null){
			if(exprPred !=null){
				exprAnd = new E_LogicalOr(exprSub, exprPred);				
				if(exprObj != null){
					exprAnd = new E_LogicalOr(exprAnd, exprObj);
					if(exprGraph != null){
						exprAnd = new E_LogicalOr(exprAnd, exprGraph);
					}
				}
				else if(exprGraph != null){
					exprAnd = new E_LogicalOr(exprAnd, exprGraph);
				}
			}
			else if(exprObj !=null){
				exprAnd = new E_LogicalOr(exprSub, exprObj);
				if(exprGraph != null){
					exprAnd = new E_LogicalOr(exprAnd, exprGraph);
				}
			}
			else if(exprGraph != null){
				exprAnd = new E_LogicalOr(exprSub, exprGraph);
			}
			
		} else if(exprPred !=null) {			
			if(exprObj != null){
				exprAnd = new E_LogicalOr(exprPred, exprObj);	
				if(exprGraph != null){
					exprAnd = new E_LogicalOr(exprAnd, exprGraph);
				}
			}
			else if(exprGraph != null){
				exprAnd = new E_LogicalOr(exprPred, exprGraph);
			}				
		} 
		
		if(exprAnd != null)
			new_filter = new ElementFilter(exprAnd);
		else if(exprSub != null)
			new_filter = new ElementFilter(exprSub);
		else if(exprPred != null)
			new_filter = new ElementFilter(exprPred);
		else if(exprObj != null)
			new_filter = new ElementFilter(exprObj);	
		else if(exprGraph != null)
			new_filter = new ElementFilter(exprGraph);	
		
		if(new_filter != null){
			group.addElementFilter(new_filter);
			return true;
		}
		return false;
	}
		
	private boolean addEqualsAndQuadFilter(ElementGroup group, Quad auth, Quad quad) throws Exception{
		
		Expr exprSub = null;
		Expr exprPred = null;
		Expr exprObj = null;
		Expr exprAnd = null;
		ElementFilter new_filter = null;
		
		//Add filters for each not equals relationship
		if(!auth.getSubject().isVariable() && quad.getSubject().isVariable())
			exprSub = new E_Equals(new ExprVar(quad.getSubject()), NodeValue.makeNode(auth.getSubject()));
		
		if(!auth.getPredicate().isVariable() && quad.getPredicate().isVariable())
			exprPred = new E_Equals(new ExprVar(quad.getPredicate()), NodeValue.makeNode(auth.getPredicate()));

		if(!auth.getObject().isVariable() && quad.getObject().isVariable())
			exprObj = new E_Equals(new ExprVar(quad.getObject()), NodeValue.makeNode(auth.getObject()));

		if(exprSub != null){
			if(exprPred !=null){
				exprAnd = new E_LogicalAnd(exprSub, exprPred);				
				if(exprObj != null){
					exprAnd = new E_LogicalAnd(exprAnd, exprObj);
				}
			}
			else if(exprObj !=null){
				exprAnd = new E_LogicalAnd(exprSub, exprObj);						
			}
		} else if(exprPred !=null) {			
				if(exprObj != null){
					exprAnd = new E_LogicalAnd(exprPred, exprObj);	
				}		
		} 
		
		if(exprAnd != null)
			new_filter = new ElementFilter(exprAnd);
		else if(exprSub != null)
			new_filter = new ElementFilter(exprSub);
		else if(exprPred != null)
			new_filter = new ElementFilter(exprPred);
		else if(exprObj != null)
			new_filter = new ElementFilter(exprObj);	

		if(new_filter != null){
			group.addElementFilter(new_filter);
			return true;
		}
		return false;
	}
		
	private boolean addNotEqualsAndQuadFilter(ElementGroup group, Quad auth, Quad quad) throws Exception{
		
		Expr exprSub = null;
		Expr exprPred = null;
		Expr exprObj = null;
		Expr exprAnd = null;
		ElementFilter new_filter = null;
		
		//Add filters for each not equals relationship
		if(!auth.getSubject().isVariable() && quad.getSubject().isVariable())
			exprSub = new E_LogicalNot(new E_SameTerm(new ExprVar(quad.getSubject()), NodeValue.makeNode(auth.getSubject())));
		
		if(!auth.getPredicate().isVariable() && quad.getPredicate().isVariable())
			exprPred = new E_LogicalNot(new E_SameTerm(new ExprVar(quad.getPredicate()), NodeValue.makeNode(auth.getPredicate())));

		if(!auth.getObject().isVariable() && quad.getObject().isVariable())
			exprObj = new E_LogicalNot(new E_SameTerm(new ExprVar(quad.getObject()), NodeValue.makeNode(auth.getObject())));

		if(exprSub != null){
			if(exprPred !=null){
				exprAnd = new E_LogicalAnd(exprSub, exprPred);				
				if(exprObj != null){
					exprAnd = new E_LogicalAnd(exprAnd, exprObj);
				}
			}
			else if(exprObj !=null){
				exprAnd = new E_LogicalAnd(exprSub, exprObj);						
			}
		} else if(exprPred !=null) {			
				if(exprObj != null){
					exprAnd = new E_LogicalAnd(exprPred, exprObj);	
				}	
		} 
		
		if(exprAnd != null)
			new_filter = new ElementFilter(exprAnd);
		else if(exprSub != null)
			new_filter = new ElementFilter(exprSub);
		else if(exprPred != null)
			new_filter = new ElementFilter(exprPred);
		else if(exprObj != null)
			new_filter = new ElementFilter(exprObj);	

		if(new_filter != null){
			group.addElementFilter(new_filter);
			return true;
		}
		return false;
	}
	
	private void addNotExistsFilter(List<Element> list, Quad authorisation) throws Exception{
		
		ElementGroup group = new ElementGroup();
		Quad q = new Quad(NodeFactory.createVariable("g"), NodeFactory.createVariable("s"), NodeFactory.createVariable("p"), NodeFactory.createVariable("o"));		
		group.addTriplePattern(q.asTriple());
    	ElementNamedGraph graph = new ElementNamedGraph(authorisation.getGraph(), group);
    	ElementGroup graphGroup = new ElementGroup();
    	graphGroup.addElement(graph);
    	Expr exp = new E_NotExists(graphGroup);
    	ElementFilter new_filter = new ElementFilter(exp);   
		addEqualsAndQuadFilter(group, authorisation, q);    		
    	list.add(new_filter);
	}
	
	private void addNotExistsFilter(Element elem, Quad auth) throws Exception{

    	Node graph = ((ElementNamedGraph) elem).getGraphNameNode();
    	//get the namedGraph is a group
    	Element namedGraph = ((ElementNamedGraph) elem).getElement();
    	if(namedGraph instanceof ElementGroup){    		
			ElementGroup group = new ElementGroup();
			List<Element> list = ((ElementGroup) namedGraph).getElements();
			
	    	List<Quad> matchedQuads = getQuads(list, graph, auth);
			if(matchedQuads != null){

				//Do not add FILTER EXISTS, FILTER NOT EXISTS or MINUS
				//for (Element e : list)
				//	group.addElement(e);
				group.addElement((Element)list.get(0));
		
				//Quad q = new Quad(NodeFactory.createVariable("g"), NodeFactory.createVariable("s"), NodeFactory.createVariable("p"), NodeFactory.createVariable("o"));		
		    	//group.addTriplePattern(q.asTriple());
		
		    	ElementNamedGraph newNamedGraph = null;
		    	if(graph.isVariable())
		    		newNamedGraph = new ElementNamedGraph(auth.getGraph(), group);
		    	else
		    		newNamedGraph = new ElementNamedGraph(graph, group);
		   		
		    	ElementGroup graphGroup = new ElementGroup();
		    	graphGroup.addElement(newNamedGraph);
		    	Expr exp = new E_NotExists(graphGroup);
		    	newFilter = new ElementFilter(exp); 
		    			    	
				for (Quad q : matchedQuads){
					addEqualsAndQuadFilter(group, auth, q);    
				}
			}
    	}
	}

	private void addMinus(Element elem, Quad auth) throws Exception{

    	Node graph = ((ElementNamedGraph) elem).getGraphNameNode();
    	//get the namedGraph is a group
    	Element namedGraph = ((ElementNamedGraph) elem).getElement();
    	if(namedGraph instanceof ElementGroup){    		
			ElementGroup group = new ElementGroup();
			List<Element> list = ((ElementGroup) namedGraph).getElements();
			
	    	List<Quad> matchedQuads = getQuads(list, graph, auth);
			if(matchedQuads != null){
				for (Element e : list)
					group.addElement(e);
		
				//Quad q = new Quad(NodeFactory.createVariable("g"), NodeFactory.createVariable("s"), NodeFactory.createVariable("p"), NodeFactory.createVariable("o"));		
		    	//group.addTriplePattern(q.asTriple());
		
		    	ElementNamedGraph newNamedGraph = null;
		    	if(graph.isVariable())
		    		newNamedGraph = new ElementNamedGraph(auth.getGraph(), group);
		    	else
		    		newNamedGraph = new ElementNamedGraph(graph, group);
		   		
		    	ElementGroup graphGroup = new ElementGroup();
		    	graphGroup.addElement(newNamedGraph);

		    	newMinus = new ElementMinus(graphGroup); 
		    			    	
				for (Quad q : matchedQuads){
					addEqualsAndQuadFilter(group, auth, q);    
				}
			}
    	}
	}

	private void addNotExistsFilter(Element elem, TriplePath path, Quad q, Quad auth) throws Exception{

    	Node graph = ((ElementNamedGraph) elem).getGraphNameNode();
    	//get the namedGraph is a group
    	Element namedGraph = ((ElementNamedGraph) elem).getElement();
    	if(namedGraph instanceof ElementGroup){    		
			ElementGroup group = new ElementGroup();
			ElementPathBlock block = new ElementPathBlock();
	    	block.addTriplePath(path);
			group.addElement(block);
				
	    	ElementNamedGraph newNamedGraph = null;
	    	if(graph.isVariable()){
				if(auth.getGraph().isVariable()){
					graph = NodeFactory.createVariable("temp");
		    		newNamedGraph = new ElementNamedGraph(graph, group);
				} else {
					newNamedGraph = new ElementNamedGraph(auth.getGraph(), group);
				}
	    	} else {
	    		newNamedGraph = new ElementNamedGraph(graph, group);
	    	}
			//graph = NodeFactory.createVariable("temp");
    		//newNamedGraph = new ElementNamedGraph(graph, group);
    		
	    	ElementGroup graphGroup = new ElementGroup();
	    	graphGroup.addElement(newNamedGraph);
	    	Expr exp = new E_NotExists(graphGroup);
	    	newFilter = new ElementFilter(exp); 
	    			    	
			addEqualsAndQuadFilter(group, auth, q);    
    	}
	}
	
	private void addEqualsFilter(List<Element> list, Node auth, Node query) throws Exception{
		Expr expr = null;
	
		if(!auth.isVariable() && query.isVariable())
			expr = new E_Equals(new ExprVar(query), NodeValue.makeNode(auth));
		if(auth.isVariable() && !query.isVariable())
			expr = new E_Equals(new ExprVar(auth), NodeValue.makeNode(query));

	
		if(expr != null){
			ElementFilter new_filter = new ElementFilter(expr);   
			list.add(new_filter);
		}
	}

	private void addNotFilter(List<Element> list, Quad auth) throws Exception{

		List<Quad> matchedList = getQuads(list, auth);
		if(matchedList != null){			
			ElementTriplesBlock block = new ElementTriplesBlock(); // Make a BGP
			Quad q = new Quad(NodeFactory.createVariable("g"), NodeFactory.createVariable("s"), NodeFactory.createVariable("p"), NodeFactory.createVariable("o"));			
			block.addTriple(q.asTriple());                         // Add our pattern match
			
			ElementGroup group = new ElementGroup();
				
			ElementNamedGraph graph = new ElementNamedGraph(q.getGraph(), group);	
	    	ElementGroup graphGroup = new ElementGroup();
	    	graphGroup.addElement(graph);
			group.addElement(block);
	   	
	    	Expr exp = new E_NotExists(graphGroup);
	    	ElementFilter newFilter = new ElementFilter(exp);   	
			addEqualsAndQuadFilter(group, auth, q);  
		  		
		    if(existsFilters == null)
		    	existsFilters = new ArrayList<ElementFilter>();
		    existsFilters.add(newFilter);
		}
  		
	}
	
	private void addInnerNotFilter(List<Element> list, Quad auth) throws Exception{
		
    	for(Element e :list){			
    		if(e instanceof ElementNamedGraph){
    			
				Node graph = ((ElementNamedGraph) e).getGraphNameNode();
				Element namedGraph = ((ElementNamedGraph) e).getElement();
				
		    	if(namedGraph instanceof ElementGroup){    		
					ElementGroup group = new ElementGroup();
					List<Element> list2 = ((ElementGroup) namedGraph).getElements();					
			    	List<Quad> matchedQuads = getQuads(list2, graph, auth);
			    	
					if(matchedQuads != null){
						for (Element e2 : list2)
							group.addElement(e2);
								
				    	ElementNamedGraph newNamedGraph = null;
				    	if(graph.isVariable())
				    		newNamedGraph = new ElementNamedGraph(auth.getGraph(), group);
				    	else
				    		newNamedGraph = new ElementNamedGraph(graph, group);
				   		
				    	ElementGroup graphGroup = new ElementGroup();
				    	graphGroup.addElement(newNamedGraph);
				    	Expr exp = new E_NotExists(graphGroup);
				    	ElementFilter newInnerFilter = new ElementFilter(exp); 
				    			    	
						for (Quad q : matchedQuads){
							addEqualsAndQuadFilter(group, auth, q);    
						}
						
						list2.add(newInnerFilter);						
					}
				}
    		}
    	} 		
	}

	private void addInnerMinus(List<Element> list, Quad auth) throws Exception{
		
    	for(Element e :list){			
    		if(e instanceof ElementNamedGraph){
    			
				Node graph = ((ElementNamedGraph) e).getGraphNameNode();
				Element namedGraph = ((ElementNamedGraph) e).getElement();
				
		    	if(namedGraph instanceof ElementGroup){    		
					ElementGroup group = new ElementGroup();
					List<Element> list2 = ((ElementGroup) namedGraph).getElements();					
			    	List<Quad> matchedQuads = getQuads(list2, graph, auth);
			    	
					if(matchedQuads != null){
						for (Element e2 : list2)
							group.addElement(e2);
								
				    	ElementNamedGraph newNamedGraph = null;
				    	if(graph.isVariable())
				    		newNamedGraph = new ElementNamedGraph(auth.getGraph(), group);
				    	else
				    		newNamedGraph = new ElementNamedGraph(graph, group);
				   		
				    	ElementGroup graphGroup = new ElementGroup();
				    	graphGroup.addElement(newNamedGraph);
				    	
				    	ElementMinus newInnerFilter = new ElementMinus(newNamedGraph); 
				    			    	
						for (Quad q : matchedQuads){
							addEqualsAndQuadFilter(group, auth, q);    
						}
						
						list2.add(newInnerFilter);						
					}
				}
    		}
    	} 		
	}

	private List<Quad> addInnerFilter(List<Element> list, Quad auth) throws Exception{
		List<Quad> matchedQuads = null;
    	for(Element e :list){			
    		if(e instanceof ElementNamedGraph){
    			
				Node graph = ((ElementNamedGraph) e).getGraphNameNode();
				Element namedGraph = ((ElementNamedGraph) e).getElement();
				
		    	if(namedGraph instanceof ElementGroup){    		
					List<Element> list2 = ((ElementGroup) namedGraph).getElements();					
			    	matchedQuads = getQuads(list2, graph, auth);			    	
				}
    		}
    	}
    	return matchedQuads;
	}

	private ElementFilter updateInnerNotFilter(List<Element> list, Quad auth) throws Exception{
		
		ElementFilter newInnerFilter = null;
    	for(Element e :list){			
    		if(e instanceof ElementNamedGraph){
    			
				Node graph = ((ElementNamedGraph) e).getGraphNameNode();
				Element namedGraph = ((ElementNamedGraph) e).getElement();
				
		    	if(namedGraph instanceof ElementGroup){    		
					ElementGroup group = new ElementGroup();
					List<Element> list2 = ((ElementGroup) namedGraph).getElements();					
			    	List<Quad> matchedQuads = getQuads(list2, graph, auth);
			    	
					if(matchedQuads != null){
						for (Element e2 : list2)
							group.addElement(e2);
								
				    	ElementNamedGraph newNamedGraph = null;
				    	if(graph.isVariable())
				    		newNamedGraph = new ElementNamedGraph(auth.getGraph(), group);
				    	else
				    		newNamedGraph = new ElementNamedGraph(graph, group);
				   		
				    	ElementGroup graphGroup = new ElementGroup();
				    	graphGroup.addElement(newNamedGraph);
				    	Expr exp = new E_NotExists(graphGroup);
				    	newInnerFilter = new ElementFilter(exp); 
				    			    	
						for (Quad q : matchedQuads){
							addEqualsAndQuadFilter(group, auth, q);    
						}	
						
					}
				}
    		}
    	}  	
    	return newInnerFilter;
	}
	
	private List<Quad> getQuads(List<Element> list, Quad auth) throws Exception{
		
		List<Quad> matchedList = null;
    	
    	//get the namedGraph is a group
		for(Element e : list)
		{
			if(e instanceof ElementNamedGraph){
            	//get the namedGraph is a group
				Node graph = ((ElementNamedGraph) e).getGraphNameNode();
            	Element namedGraph = ((ElementNamedGraph) e).getElement();
            	
            	if(namedGraph instanceof ElementGroup){
	            	List<Element> list2 = ((ElementGroup) namedGraph).getElements();	            	
	            	for(Element e2 :list2){			
	            		if(e2 instanceof ElementPathBlock){
	            			ElementPathBlock pathBlock = (ElementPathBlock) e2;
	            			//Loop through all the triples
	            			for(TriplePath path : pathBlock.getPattern().getList()){
			            		//Check triple or property path
			            		if(path.isTriple()){
				            		Triple triple = path.asTriple();
				            		Quad q = Quad.create(graph, triple.getSubject(), triple.getPredicate(), triple.getObject());
				            		//if the graph pattern matches the authorisation graph pattern			            		
					            	if(isMatch(q, auth)){		
					            		if(matchedList== null)
					            			matchedList = new ArrayList<Quad>();
					            		matchedList.add(q);
					            	}
			            		}
	            			}
	            		}
	            	}
            	}		            		
			}
		}

		return matchedList;
	}
	
	private List<Quad> getQuads(List<Element> list, Node graph, Quad auth) throws Exception{
		
		List<Quad> matchedList = null;
    	
    	//get the namedGraph is a group            	
    	for(Element e :list){			
    		if(e instanceof ElementPathBlock){
    			ElementPathBlock pathBlock = (ElementPathBlock) e;
    			//Loop through all the triples
    			for(TriplePath path : pathBlock.getPattern().getList()){
            		//Check triple or property path
            		if(path.isTriple()){
	            		Triple triple = path.asTriple();
	            		Quad q = Quad.create(graph, triple.getSubject(), triple.getPredicate(), triple.getObject());
	            		//if the graph pattern matches the authorisation graph pattern			            		
		            	if(isMatch(q, auth)){		
		            		if(matchedList== null)
		            			matchedList = new ArrayList<Quad>();
		            		matchedList.add(q);
		            	}
            		}
    			}
    		}
    	}

		return matchedList;
	}
	
	@Override 
	public String toString(){
		try{
			String q = query.toString().replace("\t", " ");
			return q;
		} catch(Exception e){}
		return "";
	}
}
