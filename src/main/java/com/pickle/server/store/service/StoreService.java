package com.pickle.server.store.service;

import com.pickle.server.store.domain.Store;
import com.pickle.server.store.dto.StoreCoordDto;
import com.pickle.server.common.util.KeyValueService;
import com.pickle.server.dress.domain.DressCategory;
import com.pickle.server.dress.dto.DressBriefDto;
import com.pickle.server.store.dto.StoreDetailDto;
import com.pickle.server.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {
    private final StoreRepository storeRepository;
    private final KeyValueService keyValueService;

    /**
     * 현재 위치 기준으로 1km 이내 매장 리스트
     *
     * @param lat
     * @param lng
     * @return
     */
    public List<StoreCoordDto> getNearStores(Double lat, Double lng) {
        List<Store> allStores = storeRepository.findAll();
        List<StoreCoordDto> nearStores = new ArrayList<>();

        for (int i = 0; i < allStores.size(); i++) {
            Double sLat = allStores.get(i).getLatitude();
            Double SLng = allStores.get(i).getLongitude();

            // 매장과 사용자 현재 위치 사이의 거리 (meter)
            Double dist = distance(lat, lng, sLat, SLng);

            // 1km 내에 있는 매장
            if (dist <= 1000.0) {
                StoreCoordDto storeCoordDto = new StoreCoordDto(allStores.get(i), dist);
                nearStores.add(storeCoordDto);
            }
        }

        // 매장 거리순으로 정렬
        Collections.sort(nearStores, new Comparator<StoreCoordDto>() {
            @Override
            public int compare(StoreCoordDto s1, StoreCoordDto s2) {
                if(s1.getDist()-s2.getDist() < 0) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });

        return nearStores;
    }

    // 거리 미터 단위로 계산
    private static Double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));

        dist = Math.acos(dist);
        dist = (dist * 180 / Math.PI);
        dist = dist * 60 * 1.1515 * 1609.344;

        return (dist);
    }

    private static Double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }


    public StoreDetailDto findStoreDetailInfoByStoreId(Long storeId, String category){
        if(category != null && !DressCategory.findCategoryByName(category))
            throw new IllegalArgumentException("잘못된 카테고리 입니다.");

        Store store = storeRepository.findById(storeId).orElseThrow(
                ()->new RuntimeException("해당 id의 스토어를 찾을 수 없습니다.")
        );

        List<DressBriefDto> dressBriefDtoList;
        if(category == null){
            dressBriefDtoList = storeRepository.findDressDtoByStoreId(storeId);
        }
        else{
            dressBriefDtoList = storeRepository.findDressDtoByStoreIdAndCategory(storeId,category);
        }

        return new StoreDetailDto(store, dressBriefDtoList, keyValueService.makeUrlHead("stores"));
    }
}
