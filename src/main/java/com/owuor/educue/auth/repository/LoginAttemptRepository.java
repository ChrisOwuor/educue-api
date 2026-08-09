package com.owuor.educue.auth.repository;
import com.owuor.educue.auth.entity.LoginAttempt;import org.springframework.data.jpa.repository.JpaRepository;
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt,String>{}
