package com.example.studymatzip.services;

import com.example.studymatzip.entities.PlaceEntity;
import com.example.studymatzip.mappers.PlaceMapper;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;


@Service
public class PlaceService {
    private final PlaceMapper placeMapper;

    @Autowired
    public PlaceService(PlaceMapper placeMapper) {
        this.placeMapper = placeMapper;
    }

    public PlaceEntity getPlace(int index){
        return this.placeMapper.selectPlaceByIndex(index);
    } //썸네일을 index번호로 읽어드림

    public PlaceEntity[] getPlaces(double minLat, double minLng, double maxLat, double maxLng){
        return this.placeMapper.selectPlacesInRangeNoThumbnail(minLat,minLng,maxLat,maxLng);
    } //requestParam이랑 변수 순서안맞추면 틀림


    public boolean putPlace(PlaceEntity place) {
        place.setRegisterAt(new Date())
                .setDeleted(false);
        return this.placeMapper.insertPlace(place) > 0;
    }
}