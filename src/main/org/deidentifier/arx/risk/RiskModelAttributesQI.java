package org.deidentifier.arx.risk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deidentifier.arx.risk.RiskEstimateBuilder.ComputationInterruptedException;
import org.deidentifier.arx.risk.RiskEstimateBuilder.WrappedBoolean;
import org.deidentifier.arx.risk.RiskEstimateBuilder.WrappedInteger;

public abstract class RiskModelAttributesQI {

    public final class QuasiIdentifierRisk implements
            Comparable<QuasiIdentifierRisk> {

        /** Field */
        private final Set<String> identifier;
        /** Field */
        private double			  alphaDistinct;
        /** Field */
        private double			  alphaSeparation;

        /**
         * Creates a new instance
         * 
         * @param identifier
         */
        private QuasiIdentifierRisk(Set<String> identifier) {
            RiskProvider provider = getRiskProvider(identifier, stop);
            this.identifier = identifier;
            this.alphaDistinct = provider.getAlphaDistinct();
            this.alphaSeparation = provider.getAlphaSeparation();
        }

        @Override
        public int compareTo(QuasiIdentifierRisk other) {
            int cmp = Integer.compare(this.identifier.size(),
                                      other.identifier.size());
            if (cmp != 0) { return cmp; }
            cmp = Double.compare(this.alphaDistinct,
                                 other.alphaDistinct);
            if (cmp != 0) { return cmp; }
            return Double.compare(this.alphaSeparation,
                                  other.alphaSeparation);
        }

        public double getAlphaDistinct() {
            return alphaDistinct;
        }

        public double getAlphaSeparation() {
            return alphaSeparation;
        }

        /**
         * @return the identifier
         */
        public Set<String> getIdentifier() {
            return identifier;
        }
        
        public boolean isQI() {
        	return getAlphaSeparation() > 0.3;
        }
    }

    /**
     * Helper interface
     * 
     * @author Fabian Prasser
     */
    static interface RiskProvider {
        public abstract double getAlphaDistinct();

        public abstract double getAlphaSeparation();
    }

    /** Stop */
    private final WrappedBoolean        stop;
    /** Result */
    private final QuasiIdentifierRisk[] risks;
    /** Result */
    private final int                   numIdentifiers;
    /** Result */
    private final Set<String>           identifiers;

    /**
     * Creates a new instance
     * 
     * @param identifiers
     * @param stop
     */
    RiskModelAttributesQI(Set<String> identifiers,
                        WrappedBoolean stop,
                        WrappedInteger percentageDone) {
        this.stop = stop;
        this.numIdentifiers = identifiers.size();
        this.identifiers = identifiers;

        Set<Set<String>> quasiIdentifiers = new HashSet<Set<String>>();
        Set<Set<String>> candidates = new HashSet<Set<String>>();        

        for(String identifier : this.identifiers) {
        	Set<String> candidate = new HashSet<String>();
        	candidate.add(identifier);
        	candidates.add(candidate);
        }
        
        Map<Set<String>, QuasiIdentifierRisk> scores = new HashMap<Set<String>, QuasiIdentifierRisk>();
        int l = 1;
        
        while(!candidates.isEmpty()) {
            checkInterrupt();
        	
        	// Calculate quasiIdentifier measures
        	for (Set<String> candidate : candidates) {
        			QuasiIdentifierRisk cRisk = new QuasiIdentifierRisk(candidate);
        			if(cRisk.isQI()) {
        				scores.put(candidate, cRisk);
        				quasiIdentifiers.add(candidate);
        			}
        	}
        	l++;
        	candidates = getCandidateSet(candidates, l);
        	
        	// Filter out supersets of already found QIs
        	Iterator<Set<String>> iter = candidates.iterator();
        	while (iter.hasNext()) {
        		Set<String> candidate = iter.next();
        		for(Set<String> qi : quasiIdentifiers) {
        			if(candidate.containsAll(qi)) {
        				iter.remove();
        				break;
        			}
        		}
        	}
        }

        // Now create sorted array
        risks = new QuasiIdentifierRisk[scores.size()];
        int idx = 0;
        for (QuasiIdentifierRisk value : scores.values()) {
            risks[idx++] = value;
        }
        Arrays.sort(risks);
    }

    /**
     * Returns the quasi-identifiers, sorted by risk
     * 
     * @return
     */
    public QuasiIdentifierRisk[] getAttributeRisks() {
        return this.risks;
    }

    /**
     * Returns the number of identifiers
     * 
     * @return
     */
    public int getNumIdentifiers() {
        return this.numIdentifiers;
    }

    /**
     * Checks for interrupts
     */
    private void checkInterrupt() {
        if (stop.value) { throw new ComputationInterruptedException(); }
    }

    private Set<Set<String>> getCandidateSet(Set<Set<String>> previous, int length) {
    	Set<Set<String>> candidates = new HashSet<Set<String>>();
    	
    	for (Set<String> p : previous) {
    		for(String i : identifiers) {
        		Set<String> candidate = new HashSet<String>(p);
    			candidate.add(i);
    			if(candidate.size() == length) {
    				candidates.add(candidate);
    			}
    		}
    	}
    	return candidates;
    }
    
    /**
     * Returns the power set
     * 
     * @param originalSet
     * @return
     */
    private <T> Set<Set<T>> getPowerSet(Set<T> originalSet) {
        checkInterrupt();
        Set<Set<T>> sets = new HashSet<Set<T>>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<T>());
            return sets;
        }
        List<T> list = new ArrayList<T>(originalSet);
        T head = list.get(0);
        Set<T> rest = new HashSet<T>(list.subList(1, list.size()));
        for (Set<T> set : getPowerSet(rest)) {
            checkInterrupt();
            Set<T> newSet = new HashSet<T>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }

    /**
     * Implement this to provide risk estimates
     * 
     * @param attributes
     * @param stop
     * @return
     */
    protected abstract RiskProvider getRiskProvider(Set<String> attributes,
                                                    WrappedBoolean stop);
}
