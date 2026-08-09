package com.owuor.educue.users.repository;

import com.owuor.educue.users.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);
    List<User> findByRoleName(String roleName);

    @Query("select s.user from Student s where upper(s.admissionNumber) = upper(:admissionNumber)")
    Optional<User> findByStudentAdmissionNumber(String admissionNumber);

    @Query("""
            select distinct u from User u
            join fetch u.role r
            left join fetch r.permissions
            left join fetch u.department
            where lower(u.email) = lower(:email)
            """)
    Optional<User> findForAuthenticationByEmail(String email);

    @Query("""
            select distinct s.user from Student s
            join fetch s.user.role r
            left join fetch r.permissions
            left join fetch s.user.department
            where upper(s.admissionNumber) = upper(:admissionNumber)
            """)
    Optional<User> findForAuthenticationByStudentAdmissionNumber(String admissionNumber);

    @Query("""
            select distinct u from User u
            join fetch u.role r
            left join fetch r.permissions
            left join fetch u.department
            where u.id = :id
            """)
    Optional<User> findForAuthenticationById(Long id);

    @EntityGraph(attributePaths = {
            "role",
            "department"
    })
    @Query("""
            select user
            from User user
            where user.id = :userId
            """)
    Optional<User> findProfileById(
            @Param("userId") Long userId
    );

}
