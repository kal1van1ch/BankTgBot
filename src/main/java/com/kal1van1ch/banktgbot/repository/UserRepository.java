package com.kal1van1ch.banktgbot.repository;

import com.kal1van1ch.banktgbot.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select count(u) > 0 from User u where u.tgId = :tgId")
    boolean containsTgId(@Param("tgId") String tgId);
}
