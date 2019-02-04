/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.textminingoncommunities.control;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.clementlevallois.datamining.graph.CommunityInfo;
import net.clementlevallois.datamining.graph.GraphOperations;
import net.clementlevallois.datamining.io.Printer;
import net.clementlevallois.datamining.nlp.MinorityLanguagesRemover;
import net.clementlevallois.datamining.nlp.NGramsExtractor;
import net.clementlevallois.datamining.nlp.RelativeFreqComputer;
import net.clementlevallois.textminingoncommunities.db.ElasticSearchInitializer;
import net.clementlevallois.utils.Clock;

/**
 *
 * @author LEVALLOIS
 */
public class GetTopNGramsPerCommunity {

    ElasticSearchInitializer es;
    GraphOperations graphOps;
    boolean isLocal;

    public GetTopNGramsPerCommunity(ElasticSearchInitializer es, GraphOperations graphOps, boolean isLocal) {
        this.es = es;
        this.graphOps = graphOps;
        this.isLocal = isLocal;
    }

    public Map<Integer, Map<String, Multiset<String>>> findTopTermsPerCommunities(Map<Integer, Map<String, Multiset<String>>> descriptionsPerLanguagePerCommunities, int topTermsToPrintPerCommunity, Map<String, CommunityInfo> communitiesInfo) throws IOException {

        Map<Integer, Map<String, Multiset<String>>> nGramsPerLanguagePerCommunities = new HashMap();
        Map<String, Multiset<String>> nGramsPerLanguage;

        for (Map.Entry<Integer, Map<String, Multiset<String>>> oneCommunity : descriptionsPerLanguagePerCommunities.entrySet()) {

            nGramsPerLanguage = new HashMap();
            Clock clock = new Clock("extracting ngrams for community " + oneCommunity.getKey());
            for (Map.Entry<String, Multiset<String>> oneLanguage : oneCommunity.getValue().entrySet()) {
                if (oneLanguage.getKey().equals("unknown")) {
                    continue;
                }
                NGramsExtractor nGramsExtractor = new NGramsExtractor();
                Multiset<String> nGramsForStringsInAGivenLanguage = nGramsExtractor.extractNGramsForStringsInAGivenLanguage(oneLanguage.getValue(), oneLanguage.getKey(), isLocal, 3);
                nGramsPerLanguage.put(oneLanguage.getKey(), nGramsForStringsInAGivenLanguage);
            }
            clock.closeAndPrintClock();

            MinorityLanguagesRemover minorityLanguagesRemover = new MinorityLanguagesRemover();
            Map<String, Multiset<String>> minorityLanguagesRemoved = minorityLanguagesRemover.removeMinorityLanguages(nGramsPerLanguage, 0.15f);
            nGramsPerLanguagePerCommunities.put(oneCommunity.getKey(), minorityLanguagesRemoved);
        }

        Map<Integer, Map<String, Map<String, Double>>> computeRelativeFreqs = computeRelativeFreqs(nGramsPerLanguagePerCommunities);

        Printer p = new Printer();
        p.printTopTermsPerLanguagePerCommunity(nGramsPerLanguagePerCommunities, topTermsToPrintPerCommunity, communitiesInfo);
        p.printTopTermsPerLanguagePerCommunityInRelativeFreqs(computeRelativeFreqs, topTermsToPrintPerCommunity, communitiesInfo);

        return nGramsPerLanguagePerCommunities;

    }

    private Map<Integer, Map<String, Map<String, Double>>> computeRelativeFreqs(Map<Integer, Map<String, Multiset<String>>> nGramsPerLanguagePerCommunities) {

        // computing total freqs for all terms
        Multiset<String> totalFreqs = HashMultiset.create();

        for (Map.Entry<Integer, Map<String, Multiset<String>>> entry : nGramsPerLanguagePerCommunities.entrySet()) {
            Map<String, Multiset<String>> value = entry.getValue();
            for (Map.Entry<String, Multiset<String>> entry2 : value.entrySet()) {
                totalFreqs.addAll(entry2.getValue());
            }
        }

        // data structure to store relative frequencies
        Map<Integer, Map<String, Map<String, Double>>> output = new HashMap();

        // computing relative freqs
        for (Map.Entry<Integer, Map<String, Multiset<String>>> entry : nGramsPerLanguagePerCommunities.entrySet()) {
            int community = entry.getKey();
            Map<String, Multiset<String>> langsAndTheirTermsInAGivenCommunity = entry.getValue();
            Map<String, Map<String, Double>> mapLangTomMpTermToRelativeFreq = new HashMap();
            for (Map.Entry<String, Multiset<String>> entry2 : langsAndTheirTermsInAGivenCommunity.entrySet()) {
                String lang = entry2.getKey();
                Multiset<String> termsForAGivenLanguageInAGivenCommunity = entry2.getValue();
                Map<String, Double> mapTermToRelativeFreq = new HashMap();
                for (Multiset.Entry<String> termEntry : termsForAGivenLanguageInAGivenCommunity.entrySet()) {
                    if (termEntry.getCount() < 2) {
                        continue;
                    }
//                    if (termEntry.getElement().equals("m health july")) {
//                        System.out.println("stoop");
//                    }
                    double relativeFreq = RelativeFreqComputer.compute(termEntry.getCount(), totalFreqs.count(termEntry.getElement()));
                    mapTermToRelativeFreq.put(termEntry.getElement(), relativeFreq);
                }
                mapLangTomMpTermToRelativeFreq.put(lang, mapTermToRelativeFreq);
            }
            output.put(community, mapLangTomMpTermToRelativeFreq);
        }

        return output;

    }

}
