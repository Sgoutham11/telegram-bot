package com.telegram.bot.repository;

import com.telegram.bot.entity.LoginSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;

public interface LoginSectionRepository extends JpaRepository<LoginSection, String> {

    @Query("select g.accessToken from LoginSection as g where g.userId=:userId and g.requestDate=:date")
    String getCurrentAccessToken(String userId, LocalDate date);

}
