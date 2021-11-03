package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.Advertisement;
import com.t1908e.memeportalapi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Integer>, JpaSpecificationExecutor<Advertisement> {
    List<Advertisement> findAllByUserIdAndStatusOrderByCreatedAtDesc(long userId, int status);

    List<Advertisement> findAllByStatusOrderByUpdatedAtDesc(int status);
    @Query(value = "SELECT * FROM advertisement WHERE status = 2 and updated_at <= NOW() and updated_at >= (NOW() - INTERVAL 1 DAY) ORDER BY updated_at ASC", nativeQuery = true)
    List<Advertisement> findActiveAds();

    Page<Advertisement> findAll(Specification<Advertisement> spec, Pageable pageable);
}
