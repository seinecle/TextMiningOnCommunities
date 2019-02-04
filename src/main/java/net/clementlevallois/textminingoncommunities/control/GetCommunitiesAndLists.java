/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.textminingoncommunities.control;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author LEVALLOIS
 */
public class GetCommunitiesAndLists {

    public Map<String, List<String>> getComAndLists(String fileName) throws IOException {
        Map<String, List<String>> outputMap = new HashMap();

        Path pathFile = Paths.get(fileName);

        List<String> allLines = Files.readAllLines(pathFile);
        String[] fields;
        String clusterId;
        List<String> listIds;
        for (String line : allLines) {
            fields = line.split(",");
            clusterId = fields[0];
            if (outputMap.containsKey(clusterId)) {
                listIds = outputMap.get(clusterId);
            } else {
                listIds = new ArrayList();
            }
            List<String> tempListElements = new LinkedList<String>(Arrays.asList(fields));
            tempListElements.remove(0);
            listIds.addAll(tempListElements);
            outputMap.put(clusterId, listIds);
        }

        return outputMap;
    }

}
