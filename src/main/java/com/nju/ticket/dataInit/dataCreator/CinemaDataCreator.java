package com.nju.ticket.dataInit.dataCreator;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nju.ticket.data.Repository.CinemaRepository;
import com.nju.ticket.data.entity.CinemaPo;
import com.nju.ticket.data.entity.MoviePo;
import com.nju.ticket.dataInit.MovieXmlData;
import com.nju.ticket.dataInit.ScreeningXmlData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by sbin on 2017/6/9.
 */
@Component
public class CinemaDataCreator {

    @Autowired
    CinemaRepository cinemaRepository;

    public boolean create(){
        cinemaRepository.deleteAll();
        XmlMapper mapper = new XmlMapper();

        List<MovieXmlData> meituanScreenData =
                XmlReader.getMovieData("movieData/meituan.xml");

        Set<CinemaPo> cinemaPoSet = meituanScreenData.stream()
                .map(data -> convertToPo(data))
                .collect(Collectors.toSet());
        System.out.println(cinemaPoSet);
        cinemaRepository.save(cinemaPoSet);
        return true;
    }

    private CinemaPo convertToPo(MovieXmlData xmlData){
        CinemaPo cinemaPo = new CinemaPo();
        cinemaPo.setName(xmlData.getCinema());

        String name = removeUnrelatedWord(xmlData.getCinema());
        String[] token = name.split("[（）]");

        cinemaPo.setShortName(token[0]);

        if(token.length==2){
            cinemaPo.setLocation(token[1]);
        }else{
            cinemaPo.setLocation("");
        }

        return cinemaPo;
    }

    private String removeUnrelatedWord(String name){

        String[] unrelatedWords =
                {"电影城","影城","电影院","影院","大剧院","剧院","大戏院","戏院"};

        String s = name;

        for(String word : unrelatedWords){
            s = s.replace(word,"");
        }

        return s;
    }

}