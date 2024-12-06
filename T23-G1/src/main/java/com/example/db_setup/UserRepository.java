package com.example.db_setup;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Metodi di ricerca esistenti
    User findByEmail(String email);
    User findByName(String name);
    User findByResetToken(String resetToken);
    User findByID(Integer ID);

    List<User> findAll();

    // MODIFICA 18/06/2024
    User findByNickname(String nickname);

    User findByisRegisteredWithFacebook(boolean isRegisteredWithFacebook);
    User findByisRegisteredWithGoogle(boolean isRegisteredWithGoogle);

    // INIZIO MODIFICHE cami
    List<User> findBySurname(String surname); // Ricerca utenti per cognome
    List<User> findByBiographyContaining(String keyword); // Ricerca utenti con una parola chiave nella biografia
    // FINE MODIFICHE cami

    // INIZIO MODIFICA avatar cami
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.avatar = :avatar WHERE u.ID = :userId")
    void updateAvatar(@Param("userId") Integer userId, @Param("avatar") String avatar);
    @Query("SELECT u.avatar FROM User u WHERE u.ID = :userId")
    String findAvatarByUserId(@Param("userId") Integer userId);

    // FINE MODIFICA avatar



 
}

