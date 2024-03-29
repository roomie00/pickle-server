package com.pickle.server.dress.service;

import com.pickle.server.common.error.NotFoundIdException;
import com.pickle.server.common.error.NotValidParamsException;
import com.pickle.server.common.util.KeyValueService;
import com.pickle.server.dress.domain.*;
import com.pickle.server.dress.dto.*;
import com.pickle.server.dress.repository.*;
import com.pickle.server.store.domain.Store;
import com.pickle.server.store.repository.StoreRepository;
import com.pickle.server.user.domain.User;
import com.pickle.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DressService {
    private final DressRepository dressRepository;
    private final UserRepository userRepository;
    private final DressLikeRepository dressLikeRepository;
    private final StoreRepository storeRepository;
    private final DressReservationRepository dressReservationRepository;
    private final DressOptionDetailRepository dressOptionDetailRepository;
    private final ReservedDressRepository reservedDressRepository;

    private final KeyValueService keyValueService;
    private final RecentViewRepository recentViewRepository;

    public DressDetailDto findDressDetailInfoByDressId(Long dressId, User user) {
        Dress dress = dressRepository.findById(dressId).orElseThrow(
                () -> new NotFoundIdException()
        );

        Optional<RecentView> recentView = recentViewRepository.findByUserIdAndStoreId(user.getId(), dress.getId());
        if (recentView.isPresent()) {
            recentView.get().setClick(recentView.get().getClick() + 1);
            recentViewRepository.save(recentView.get());
        } else {
            recentViewRepository.save(new RecentView(dress, user));
        }

        return new DressDetailDto(dress, keyValueService.makeUrlHead("dresses"),
                dressLikeRepository.existsByUserIdAndDressId(user.getId(), dressId));
    }

    public List<DressBriefDto> searchDress(String name, String sort, String category, Double latitude, Double longitude, User user) {
        if(!DressCategory.existsCategoryByName(category))
            throw new NotValidParamsException();

        if(!DressSortBy.existsSortConditionByName(sort))
            throw new NotValidParamsException();

        return dressRepository.findDressByCondition(name, sort, category, latitude, longitude, user.getId());
    }

    @Transactional(readOnly = true)
    public List<DressLikeDto> findDressLikeByUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundIdException());
        return dressRepository.findDressByUsers(userId);
    }

    @Transactional
    public UpdateDressLikeDto likesDress(UpdateDressLikeDto updateDressLikeDto,User user){
        Dress dress = dressRepository.findById(updateDressLikeDto.getDressId()).orElseThrow(NotFoundIdException::new);
        if(dressLikeRepository.findByUserAndDress(user,dress).isPresent()){
            dressLikeRepository.deleteDress(dress.getId(), user.getId());

            return new UpdateDressLikeDto(updateDressLikeDto.getDressId(), Boolean.FALSE);
        }
        else{
            DressLike dressLike = DressLike.builder().dress(dress).user(user).build();
            dressLikeRepository.save(dressLike);

            return new UpdateDressLikeDto(updateDressLikeDto.getDressId(), Boolean.TRUE);
        }
    }

    public DressReservationFormDto getDressReservationForm(Long storeId) {
        return new DressReservationFormDto(storeRepository.findById(storeId)
                .orElseThrow(()->new RuntimeException("해당 id의 스토어를 찾을 수 없습니다.")));
    }

    public void makeDressReservation(DressReservationDto dressReservationDto, User user) {
        Store store = storeRepository.findById(dressReservationDto.getStoreId())
                .orElseThrow(()->new RuntimeException("해당 id의 스토어를 찾을 수 없습니다."));

        List<ReservedDress> reservedDressList = new ArrayList<>();

        Dress reservedDress = dressRepository.findById(dressReservationDto.getDressId()).orElseThrow(()->new RuntimeException("해당 id의 드레스를 찾을 수 없습니다."));

        DressReservation dressReservation = new DressReservation(dressReservationDto, user, store, reservedDressList);
        dressReservationRepository.save(dressReservation);

        for(StockQuantityDto sqd : dressReservationDto.getReservedDressList()){
            DressOptionDetail option1 = dressOptionDetailRepository.findById(sqd.getStock1Id()).orElseThrow(() ->new RuntimeException("유효하지 않은 옵션"));
            DressOptionDetail option2 = dressOptionDetailRepository.findById(sqd.getStock2Id()).orElseThrow(() ->new RuntimeException("유효하지 않은 옵션"));
            ReservedDress reservedDressWithOption = new ReservedDress(option1, option2, reservedDress, sqd.getQuantity(), dressReservation);
            reservedDressList.add(reservedDressWithOption);
            reservedDressRepository.save(reservedDressWithOption);
        }
    }

    public List<DressOrderDto> getDressOrder(Long dressReservationId, Long userId){
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundIdException());
        return dressRepository.findReservationByUserAndReservationId(dressReservationId,userId);
    }
    public List<DressOrderListDto> getDressOrderList(String status,Long userId){
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundIdException());
        return dressRepository.findReservationListByStatusAndUser(status,userId);
    }

    public void cancelReservation(Long reservationId) {
        DressReservation dressReservation = dressReservationRepository.findById(reservationId).orElseThrow(
                () -> new NotFoundIdException()
        );
        dressReservation.setStatus(DressReservationStatus.Constants.canceledOrder);
        dressReservationRepository.save(dressReservation);
    }
}
