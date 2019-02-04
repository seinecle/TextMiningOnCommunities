/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.textminingoncommunities.control;

import net.clementlevallois.datamining.graph.CommunityInfo;
import LanguageDetection.LanguageDetector;
import com.google.common.collect.Multiset;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import net.clementlevallois.datamining.graph.GraphCommunityDetection;
import net.clementlevallois.datamining.graph.GraphExtractor;
import net.clementlevallois.datamining.graph.GraphOperations;
import net.clementlevallois.datamining.langdetect.LanguageResourcesInitializer;
import net.clementlevallois.textminingoncommunities.db.ElasticSearchInitializer;
import net.clementlevallois.textminingoncommunities.picocli.CommandLine;
import net.clementlevallois.textminingoncommunities.picocli.CommandLine.Command;
import net.clementlevallois.textminingoncommunities.picocli.CommandLine.Option;
import net.clementlevallois.utils.Clock;
import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.appearance.api.Function;
import org.gephi.appearance.api.Partition;
import org.gephi.appearance.api.PartitionFunction;
import org.gephi.appearance.plugin.PartitionElementColorTransformer;
import org.gephi.graph.api.Column;
import org.gephi.statistics.plugin.Modularity;
import org.openide.util.Lookup;

/**
 *
 * @author LEVALLOIS
 */
@Command(description = "Gets key terms from the communities in the field - and detect these communities if necessary",
        name = "text mining on communities of the field", mixinStandardHelpOptions = true, version = "1.0")

public class Controller implements Callable {

    @Option(names = {"-f", "--fileName"}, description = "network to analyze. Must be a gexf file in the same directory as the jar file. (default: the command will analyze the gexf file in the root folder)")
    String fileName;

    @Option(names = {"-o", "--oneCommunity"}, description = "add this option to test on just one community. (default: ${DEFAULT-VALUE})")
    boolean justOneCommunity = false;

    @Option(names = {"-nl", "--noLanguage"}, description = "add this option to deactivate language detection. All texts will be treated together instead of being grouped per language. (default: ${DEFAULT-VALUE})")
    boolean noLanguage = false;

    public static void main(String[] args) throws UnknownHostException, IOException {
        CommandLine.call(new Controller(), args);
    }

    @Override
    public Void call() throws Exception {
        GraphOperations graphOps;

        Clock overallClock = new Clock("overall clock");

        Clock clock = new Clock("initializing the elastich search connection");
        ElasticSearchInitializer es = new ElasticSearchInitializer();
        es.initElasticSearch();
        clock.closeAndPrintClock();

        Boolean isLocal;
        isLocal = System.getProperty("os.name").toLowerCase().contains("win");
        if (isLocal) {
            System.out.println("we are local on Windows");
        } else {
            System.out.println("we are remote on Linux");
        }

        if (fileName == null) {
            Optional<Path> findFirstGexfFile = Files.list(Paths.get(".")).filter(f -> f.toString().endsWith("gexf")).findFirst();
            if (findFirstGexfFile.isPresent()) {
                fileName = findFirstGexfFile.get().toString();
                System.out.println("gexf file that will be analyzed: " + fileName);
            } else {
                System.out.println("no gexf file found in the directory");
                System.exit(2);
            }
        }

        clock = new Clock("load gexf");
        graphOps = new GraphOperations();
        graphOps.loadGexf(fileName);
        clock.closeAndPrintClock();

        clock = new Clock("detect communities if not already present");
        Column column = graphOps.getGm().getNodeTable().getColumn(Modularity.MODULARITY_CLASS);
        if (column == null) {
            GraphCommunityDetection comDetect = new GraphCommunityDetection(graphOps);
            graphOps = comDetect.computeCommunities(0.8d);
        }
        clock.closeAndPrintClock();

        clock = new Clock("initializing the language detection");
        LanguageResourcesInitializer l = new LanguageResourcesInitializer();
        l.load(isLocal, Parameters.supportedLanguages);
        LanguageDetector ld = new LanguageDetector();
        clock.closeAndPrintClock();
        
        clock = new Clock("get info on communities");
        Column modColumn = graphOps.getGm().getNodeTable().getColumn(Modularity.MODULARITY_CLASS);
                AppearanceController appearanceController = Lookup.getDefault().lookup(AppearanceController.class);
        AppearanceModel appearanceModel = appearanceController.getModel();

        Function func = appearanceModel.getNodeFunction(graphOps.getGm().getGraph(), modColumn, PartitionElementColorTransformer.class);
        Partition partition = ((PartitionFunction) func).getPartition();

        System.out.println("found " + partition.size() + " communities");
        Map<String,CommunityInfo> communitiesInfo = new HashMap();

 
        int totalNodeCount = graphOps.getGm().getGraph().getNodeCount();
        for (Object communityName : partition.getValues()) {
            String communityNameString = String.valueOf(communityName);
            int countCommunityMembers = partition.count(communityName);

            float shareCommunity = (float) countCommunityMembers / (float) totalNodeCount * 100f;
            CommunityInfo c = new CommunityInfo();
            c.size = countCommunityMembers;
            c.share = shareCommunity;
            c.name = communityNameString;
            communitiesInfo.put(communityNameString,c);            
            
        }
        clock.closeAndPrintClock();
        

        clock = new Clock("extract a corpus per community and per language, based on a node's textual attribute");
        Map<Integer, Map<String, Multiset<String>>> descriptionsPerLanguagePerCommunities;
        GraphExtractor graphExtractor = new GraphExtractor(graphOps, ld, isLocal);
        descriptionsPerLanguagePerCommunities = graphExtractor.extractTextualAttributePerLanguageAndPerCommunities(justOneCommunity, "user description", noLanguage);
        clock.closeAndPrintClock();

        clock = new Clock("get top ngrams per language per community");
        GetTopNGramsPerCommunity nlper = new GetTopNGramsPerCommunity(es, graphOps, isLocal);
        nlper.findTopTermsPerCommunities(descriptionsPerLanguagePerCommunities, 10, communitiesInfo);
        clock.closeAndPrintClock();

        overallClock.closeAndPrintClock();

        return null;
    }

}