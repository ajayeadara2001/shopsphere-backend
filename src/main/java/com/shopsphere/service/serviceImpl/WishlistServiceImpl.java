package com.shopsphere.service.serviceImpl;

import com.shopsphere.dto.WishlistRequestDto;
import com.shopsphere.dto.WishlistResponseDto;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.User;
import com.shopsphere.entity.Wishlist;
import com.shopsphere.exception.WishlistException;
import com.shopsphere.mapper.ProductMapper;
import com.shopsphere.mapper.UserMapper;
import com.shopsphere.mapper.WishlistMapper;
import com.shopsphere.repository.WishlistRepository;
import com.shopsphere.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {
    private final WishlistRepository wishlistRepository;
    private final WishlistMapper wishlistMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public WishlistResponseDto addToWishlist(WishlistRequestDto wishlistRequestDto) {
        User currentUser=userMapper.getCurrentUser();
        Product product=productMapper.getProduct(wishlistRequestDto.getProductId());
        if(wishlistRepository.existsByUserAndProduct(currentUser,product)){
            throw new WishlistException("Already Product is present in the wishlist");
        }
        Wishlist wishlist=new Wishlist();
        wishlist.setProduct(product);
        wishlist.setUser(currentUser);
        wishlist.setAddedAt(LocalDateTime.now());
        return wishlistMapper.mapWishlistToResponse(wishlistRepository.save(wishlist));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistResponseDto> getUserAllWishlists() {
        User currentUser=userMapper.getCurrentUser();
        List<Wishlist> wishlistLists=wishlistRepository.findByUser(currentUser);
        List<WishlistResponseDto> wishlistResponseDtoList=new ArrayList<>();
        for(Wishlist wishlist:wishlistLists){
            wishlistResponseDtoList.add(wishlistMapper.mapWishlistToResponse(wishlist));
        }
        return wishlistResponseDtoList;
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistResponseDto getWishlistById(Long wishlistId) {
        User currentUser=userMapper.getCurrentUser();
        Wishlist wishlist=wishlistRepository.findByWishlistIdAndUser(wishlistId,currentUser)
                .orElseThrow(()-> new WishlistException("Wishlist item not found for Id: "+wishlistId));
        return wishlistMapper.mapWishlistToResponse(wishlist);
    }

    @Override
    @Transactional
    public void deleteWishlist(Long wishlistId) {
        User currentUser=userMapper.getCurrentUser();
        Wishlist wishlist=wishlistRepository.findByWishlistIdAndUser(wishlistId,currentUser)
                .orElseThrow(()-> new WishlistException("Wishlist item not found for Id: "+wishlistId));
        wishlistRepository.delete(wishlist);
    }
}
