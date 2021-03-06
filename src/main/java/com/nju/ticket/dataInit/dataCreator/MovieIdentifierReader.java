package com.nju.ticket.dataInit.dataCreator;

import com.nju.ticket.data.Repository.MovieRepository;
import com.nju.ticket.dataInit.properties.InitProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by sbin on 2017/6/9.
 */
@Component
public class MovieIdentifierReader {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    InitProperties initProperties;



    public Set<String> getIdentifierSet(){
        Set<String> identifierSet = new HashSet<>(movieRepository.findIdentifier());
        addSynonymsIdentifier(identifierSet);
        return identifierSet;
    }

    public String getIdentifier(String name){

        name = name.replaceAll("[\\pP‘’“”]", "");
        Map<String,String> synonymsMap = initProperties.getSynonyms();
        for(Map.Entry<String,String> entry:synonymsMap.entrySet()){
            if(name.equals(entry.getValue())){
                return entry.getKey();
            }
        }
        return name;
    }

    private void addSynonymsIdentifier(Set<String> identifierSet){
        initProperties.getSynonyms().forEach( (k,v) -> {
            if(identifierSet.contains(k)){
                identifierSet.add(v);
            }
        });
    }

}
