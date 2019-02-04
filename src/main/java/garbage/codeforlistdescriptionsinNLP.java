//        Set<String> allUserListIds = new HashSet();
//        for (Map.Entry<String, List<String>> entry : communitiesAndListIds.entrySet()) {
//            allUserListIds.addAll(entry.getValue());
//        }



//        if (textSources == TextSources.UserList | textSources == TextSources.Both) {
//            Map<String, UserListImpl> multiGetOnUserListIds = enricher.es.multiGetOnUserListIds(allUserListIds.toArray(new String[allUserListIds.size()]));
//
//            Set<Long> listOwners = new HashSet();
//
//            for (Map.Entry<String, List<String>> entry : communitiesAndListIds.entrySet()) {
//                List<String> listDescriptionsForOneCommunity = new ArrayList();
//                Set<String> setOfListIds = new HashSet(entry.getValue());
//                for (String listId : setOfListIds) {
//                    if (multiGetOnUserListIds.get(listId) != null && multiGetOnUserListIds.get(listId).getListName() != null) {
//                        UserListImpl userListImpl = multiGetOnUserListIds.get(listId);
//                        String listDescription = userListImpl.getListName();
//                        // only one list to be added per list owner. To avoid list names with very similar names due to same list owner creating copycat lists
//                        if (listOwners.add(userListImpl.getOwnerId())) {
//                            listDescription = ShortTextCleaner.clean(listDescription);
//                            listDescriptionsForOneCommunity.add(listDescription);
//                        }
//                    }
//                }
//                comsAndTheirListDescriptions.put(entry.getKey(), listDescriptionsForOneCommunity);
//            }
//        }
//



//            if (textSources == TextSources.UserList | textSources == TextSources.Both) {
//                //loop through the list descriptions of a given community
//                List<String> listDescriptionsForOneCommunity = comsAndTheirListDescriptions.get(communityNameString);
//                for (String descr : listDescriptionsForOneCommunity) {
////                    if (communityNameInteger == 5){
////                        System.out.println("descr list: "+ descr);
////                    }
//                    String langSource;
//                    if (langDetection) {
//                        LanguageDetector lde = new LanguageDetector();
//                        langSource = lde.detect(descr);
//                        if (langSource == null) {
//                            langSource = "en";
//                        }
//                    } else {
//                        langSource = "all languages";
//                    }
//                    Multiset<String> previousValue = userAndListDescriptionsForOneCommunity.get(langSource);
//                    if (previousValue == null) {
//                        previousValue = HashMultiset.create();
//                    }
//                    previousValue.add(descr);
//                    userAndListDescriptionsForOneCommunity.put(langSource, previousValue);
//
//                    //finished looping through the descriptions for one community. We put the list descriptions (per language) in a map <communityName,<lang,userDescriptions>>
//                    descriptionsPerCommunities.put(communityNameInteger, userAndListDescriptionsForOneCommunity);
//                }
//            }

