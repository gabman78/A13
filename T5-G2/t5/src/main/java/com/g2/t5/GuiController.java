/*
 *   Copyright (c) 2024 Stefano Marano https://github.com/StefanoMarano80017
 *   All rights reserved.

 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.g2.t5;
import com.g2.Interfaces.T23Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.LocaleResolver;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.g2.Components.GenericObjectComponent;
import com.g2.Components.PageBuilder;
import com.g2.Components.ServiceObjectComponent;
import com.g2.Components.VariableValidationLogicComponent;
import com.g2.Interfaces.ServiceManager;
import com.g2.Model.AchievementProgress;
import com.g2.Model.ClassUT;
import com.g2.Model.Game;
import com.g2.Model.ScalataGiocata;
import com.g2.Model.Statistic;
import com.g2.Model.StatisticProgress;
import com.g2.Model.User;
import com.g2.Service.AchievementService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@CrossOrigin
@Controller
public class GuiController {

    private final ServiceManager serviceManager;
    private final LocaleResolver localeResolver;

    @Autowired
    private AchievementService achievementService;

    @Autowired
    private T23Service t23Service;



    @Autowired
    public GuiController(RestTemplate restTemplate, LocaleResolver localeResolver) {
        this.serviceManager = new ServiceManager(restTemplate);
        this.localeResolver = localeResolver;
    }

    //Gestione lingua 
    @PostMapping("/changeLanguage")
    public ResponseEntity<Void> changeLanguage(@RequestParam("lang") String lang, 
                                                HttpServletRequest request, 
                                                HttpServletResponse response) {
        Cookie cookie = new Cookie("lang", lang);
        cookie.setMaxAge(3600); // Imposta la durata del cookie a 1 ora
        cookie.setPath("/"); // Imposta il percorso per il cookie
        response.addCookie(cookie); // Aggiungi il cookie alla risposta

        Locale locale = new Locale(lang);
        localeResolver.setLocale(request, response, locale);
        // Restituisce una risposta vuota con codice di stato 200 OK
        return ResponseEntity.ok().build(); 
    }

    @GetMapping("/main")
    public String GUIController(Model model, @CookieValue(name = "jwt", required = false) String jwt) {
        PageBuilder main = new PageBuilder(serviceManager, "main", model);
        main.SetAuth(jwt); //con questo metodo abilito l'autenticazione dell'utente
        return main.handlePageRequest();
    }

    @GetMapping("/profile")
    public String profilePagePersonal(Model model, @CookieValue(name = "jwt", required = false) String jwt)
    {
        byte[] decodedUserObj = Base64.getDecoder().decode(jwt.split("\\.")[1]);
        String decodedUserJson = new String(decodedUserObj, StandardCharsets.UTF_8);

        try {
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(decodedUserJson, Map.class);
            String userId = map.get("userId").toString();
            return profilePage(model, userId, jwt);
        }
        catch (Exception e) {
            System.out.println("(/profile) Error requesting profile: " + e.getMessage());
        }

        return "error";
    }
        //modifiche cami
    @PostMapping("/updateBiography")
    public ResponseEntity<String> updateBiography(
        @CookieValue(name = "jwt", required = false) String jwt,
        @RequestParam("biography") String biography) {
        try {
            // Decodifica il token JWT per ottenere l'ID utente
            byte[] decodedUserObj = Base64.getDecoder().decode(jwt.split("\\.")[1]);
            String decodedUserJson = new String(decodedUserObj, StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(decodedUserJson, Map.class);
            String userId = map.get("userId").toString();
            // Chiamata al metodo del T23Service
            Boolean updateSuccess = t23Service.updateBiography(userId, biography);
            if (updateSuccess) {
                return ResponseEntity.ok("Biography updated successfully!");
            } else {
                return ResponseEntity.status(400).body("Failed to update biography.");
            }
        } catch (Exception e) {
            System.out.println("Error updating biography: " + e.getMessage());
            return ResponseEntity.status(500).body("Internal server error.");
        }
    }

    @GetMapping("/getBiography")
    public ResponseEntity<Map<String, String>> getBiography(@CookieValue(name = "jwt", required = false) String jwt) {
        try {
            // Decodifica il token JWT per ottenere l'ID utente
            byte[] decodedUserObj = Base64.getDecoder().decode(jwt.split("\\.")[1]);
            String decodedUserJson = new String(decodedUserObj, StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(decodedUserJson, Map.class);
            String userId = map.get("userId").toString();

            // Recupera la biografia dal servizio
            String biography = t23Service.getBiography(userId);
            if (biography != null) {
                Map<String, String> response = new HashMap<>();
                response.put("biography", biography);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(404).body(null);
            }
        } catch (Exception e) {
            System.out.println("Error fetching biography: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }   
        }


    


    @GetMapping("/profile/{playerID}")
    public String profilePage(Model model,
                              @PathVariable(value="playerID") String playerID,
                              @CookieValue(name = "jwt", required = false) String jwt) {
        PageBuilder profile = new PageBuilder(serviceManager, "profile", model);
        profile.SetAuth(jwt);

        int userId = Integer.parseInt(playerID);

        List<AchievementProgress> achievementProgresses = achievementService.getProgressesByPlayer(userId);
        List<StatisticProgress> statisticProgresses = achievementService.getStatisticsByPlayer(userId);
        List<Statistic> allStatistics = achievementService.getStatistics();
        Map<String, Statistic> IdToStatistic = new HashMap<>();

        for (Statistic stat : allStatistics)
            IdToStatistic.put(stat.getID(), stat);

        GenericObjectComponent objAchievementProgresses = new GenericObjectComponent("achievementProgresses", achievementProgresses);
        GenericObjectComponent objStatisticProgresses = new GenericObjectComponent("statisticProgresses", statisticProgresses);
        GenericObjectComponent objIdToStatistic = new GenericObjectComponent("IdToStatistic", IdToStatistic);
        GenericObjectComponent objUserID = new GenericObjectComponent("userID", userId);

        profile.setObjectComponents(objAchievementProgresses);
        profile.setObjectComponents(objStatisticProgresses);
        profile.setObjectComponents(objIdToStatistic);
        profile.setObjectComponents(objUserID);

        return profile.handlePageRequest();
    }

    @GetMapping("/gamemode")
    public String gamemodePage(Model model,
            @CookieValue(name = "jwt", required = false) String jwt,
            @RequestParam(value = "mode", required = false) String mode) {
       
        if("Sfida".equals(mode) || "Allenamento".equals(mode)){
            PageBuilder gamemode = new PageBuilder(serviceManager, "gamemode", model);
            //controllo che sia stata fornita una modalità valida dall'utente
            VariableValidationLogicComponent Valida_classeUT = new VariableValidationLogicComponent(mode);
            Valida_classeUT.setCheckNull(); 
            List<String> list_mode = Arrays.asList("Sfida", "Allenamento");
            Valida_classeUT.setCheckAllowedValues(list_mode); //Se il request param non è in questa lista è un problema 
            ServiceObjectComponent lista_classi = new ServiceObjectComponent(serviceManager, "lista_classi", "T1", "getClasses");        
            gamemode.setObjectComponents(lista_classi);
            List<String> list_robot = new ArrayList<>();
            // Aggiungere elementi alla lista
            list_robot.add("Randoop");
            list_robot.add("EvoSuite");
            GenericObjectComponent lista_robot = new GenericObjectComponent("lista_robot", list_robot);
            gamemode.setObjectComponents(lista_robot);
            gamemode.SetAuth(jwt);
            return gamemode.handlePageRequest();
        }
        if("Scalata".equals(mode)){
            PageBuilder gamemode = new PageBuilder(serviceManager, "gamemode_scalata", model);
            gamemode.SetAuth(jwt);
            return gamemode.handlePageRequest();
        }
            return "main";
    }

    @GetMapping("/editor")
    public String editorPage(Model model,
            @CookieValue(name = "jwt", required = false) String jwt,
            @RequestParam(value = "ClassUT", required = false) String ClassUT) {

        PageBuilder editor = new PageBuilder(serviceManager, "editor", model);
        VariableValidationLogicComponent Valida_classeUT = new VariableValidationLogicComponent(ClassUT);
        Valida_classeUT.setCheckNull(); 
        @SuppressWarnings("unchecked")
        List<ClassUT> Lista_classi_UT = (List<com.g2.Model.ClassUT>) serviceManager.handleRequest("T1", "getClasses");      
        List<String>  Lista_classi_UT_nomi =  new ArrayList<>();
        for(ClassUT element : Lista_classi_UT){
            Lista_classi_UT_nomi.add(element.getName());
        }

        System.out.println(Lista_classi_UT_nomi);

        Valida_classeUT.setCheckAllowedValues(Lista_classi_UT_nomi); //Se il request param non è in questa lista è un problema 
        ServiceObjectComponent ClasseUT = new ServiceObjectComponent(serviceManager, "classeUT","T1", "getClassUnderTest", ClassUT);
        editor.setObjectComponents(ClasseUT);
        editor.SetAuth(jwt);
        editor.setLogicComponents(Valida_classeUT);
        //Se l'utente ha inserito un campo nullo o un valore non consentito vuol dire che non è passato da gamemode
        editor.setErrorPage( "NULL_VARIABLE",  "redirect:/main"); 
        editor.setErrorPage( "VALUE_NOT_ALLOWED",  "redirect:/main");
        return editor.handlePageRequest();
    }
    
    @GetMapping("/leaderboard")
    public String leaderboard(Model model, @CookieValue(name = "jwt", required = false) String jwt) {
        PageBuilder leaderboard = new PageBuilder(serviceManager, "leaderboard", model);
        ServiceObjectComponent lista_utenti = new ServiceObjectComponent(serviceManager, "listaPlayers",
                "T23", "GetUsers");
        leaderboard.setObjectComponents(lista_utenti);
        leaderboard.SetAuth(jwt);
        return leaderboard.handlePageRequest();
    }

    @GetMapping("/edit_profile")
    public String edit_profile(Model model, @CookieValue(name = "jwt", required = false) String jwt) {
        PageBuilder main = new PageBuilder(serviceManager, "Edit_Profile", model);

        
        User player_placeholder = new User((long) 1, "placeholder", "placeholder", "email", "password",
                true, "studies", "resetToke");

        GenericObjectComponent player = new GenericObjectComponent("player", player_placeholder);
        main.setObjectComponents(player);
        main.SetAuth(jwt);
        return main.handlePageRequest();
    }

    @GetMapping("/report")
    public String reportPage(Model model, @CookieValue(name = "jwt", required = false) String jwt) {
        Boolean Auth = (Boolean) serviceManager.handleRequest("T23", "GetAuthenticated", jwt);
        if (Auth) {
            return "report";
        }
        return "redirect:/login";
    }

    // TODO: Salvataggio della ScalataGiocata
    @PostMapping("/save-scalata")
    public ResponseEntity<String> saveScalata(@RequestParam("playerID") int playerID,
            @RequestParam("scalataName") String scalataName,
            HttpServletRequest request) {
        /*
         * Nella schermata /gamemode_scalata, il player non dovrà far altro che che selezionare una delle
         * "Scalate" presenti nella lista e dunque, le informazioni da elaborare saranno esclusivamente:
         * playerID
         * scalataName, dal quale è possibile risalire a tutte le informazioni relative quella specifica "Scalata"
         */

        /*
        * Verifica dell'autenticità del player controllando che l'header identificato dal: "X-UserID" sia lo stesso
        * associato all'utente identificato da "playerID"
         */
        if (!request.getHeader("X-UserID").equals(String.valueOf(playerID))) {

            System.out.println("(/save-scalata)[T5] Player non autorizzato.");
            return ResponseEntity.badRequest().body("Unauthorized");
        } else {

            // Player autorizzato.
            System.out.println("(/save-scalata)[T5] Player autorizzato.");

            // Recupero della data e dell'ora di inizio associata alla ScalataGiocata
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime currentHour = LocalTime.now();
            LocalDate currentDate = LocalDate.now();
            String fomattedHour = currentHour.format(formatter);
            System.out.println("(/save-scalata)[T5] Data ed ora di inizio recuperate con successo.");

            // Creazione di un oggetto scalataDataWriter
            ScalataDataWriter scalataDataWriter = new ScalataDataWriter();

            // Creazione di un oggetto ScalataGiocata
            ScalataGiocata scalataGiocata = new ScalataGiocata();

            // Inizializzazione dell'oggetto ScalataGiocata
            scalataGiocata.setPlayerID(playerID);
            scalataGiocata.setScalataName(scalataName);
            scalataGiocata.setCreationDate(currentDate);
            scalataGiocata.setCreationTime(fomattedHour);

            JSONObject ids = scalataDataWriter.saveScalata(scalataGiocata);
            System.out.println("(/save-scalata)[T5] Creazione dell'oggetto scalataDataWriter avvenuta con successo.");

            if (ids == null) {
                return ResponseEntity.badRequest().body("Bad Request");
            }

            return ResponseEntity.ok(ids.toString());
        }
    }

    @PostMapping("/save-data")
    public ResponseEntity<String> saveGame(@RequestParam("playerId") int playerId, 
                                            @RequestParam("robot") String robot,
                                            @RequestParam("classe") String classe, 
                                            @RequestParam("difficulty") String difficulty, 
                                            @RequestParam("gamemode") String gamemode,
                                            @RequestParam("username") String username, 
                                            @RequestParam("selectedScalata") Optional<Integer> selectedScalata, 
                                            HttpServletRequest request) {

        if (!request.getHeader("X-UserID").equals(String.valueOf(playerId))) {
            return ResponseEntity.badRequest().body("Unauthorized");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime oraCorrente = LocalTime.now();
        String oraFormattata = oraCorrente.format(formatter);

        GameDataWriter gameDataWriter = new GameDataWriter();
        // g.setGameId(gameDataWriter.getGameId());
        Game g = new Game(playerId, gamemode, "nome", difficulty, username);
        // g.setPlayerId(pl);
        // g.setPlayerClass(classe);
        // g.setRobot(robot);
        g.setData_creazione(LocalDate.now());
        g.setOra_creazione(oraFormattata);
        g.setClasse(classe);
        g.setUsername(username);
        // System.out.println(g.getUsername() + " " + g.getGameId());

        System.out.println("ECCO LO USERNAME : " + username);       //in realtà stampa l'indirizzo e-mail del player...

        // globalID = g.getGameId();
        JSONObject ids = gameDataWriter.saveGame(g, username, selectedScalata);
        if (ids == null) {
            return ResponseEntity.badRequest().body("Bad Request");
        }

        System.out.println("Checking achievements...");
        achievementService.updateProgressByPlayer(playerId);

        return ResponseEntity.ok(ids.toString());
    }

    @GetMapping("/leaderboardScalata")
    public String getLeaderboardScalata(Model model, @CookieValue(name = "jwt", required = false) String jwt) {
        Boolean Auth = (Boolean) serviceManager.handleRequest("T23", "GetAuthenticated", jwt);
        if (Auth) {
            return "leaderboardScalata";
        }
        return "redirect:/login";
    }

    @GetMapping("/editor_old")
    public String getEditorOld(Model model, @CookieValue(name = "jwt", required = false) String jwt) {
        PageBuilder main = new PageBuilder(serviceManager, "editor_old", model);
        main.SetAuth(jwt); //con questo metodo abilito l'autenticazione dell'utente
        return main.handlePageRequest();
    }



//  Metodo per aggiungere un amico
    @PostMapping("/addFriend")
    public ResponseEntity<String> addFriend(
        @CookieValue(name = "jwt", required = false) String jwt,
        @RequestParam Integer friendId) {
        try {
            Integer userId = extractUserIdFromJwt(jwt);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token non valido.");
            }

            String response = t23Service.addFriend(userId.toString(), friendId.toString());
            if (response == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore durante l'aggiunta dell'amico.");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore interno del server.");
        }
    }
    
    @GetMapping("/searchFriend")
    public ResponseEntity<Map<String, String>> searchFriend(
    @RequestParam String identifier,
    @CookieValue(name = "jwt", required = false) String jwt
    ) {
    try {
        // Verifica autenticazione
        Integer userId = extractUserIdFromJwt(jwt);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Cerca l'utente tramite T23Service
        Map<String, String> user = t23Service.searchFriend(identifier);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(user);
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    }
    
    @PostMapping("/removeFriend")
    public ResponseEntity<String> removeFriend(
        @CookieValue(name = "jwt", required = false) String jwt,
        @RequestParam Integer friendId) {
    try {
        // Verifica che il token JWT esista
        if (jwt == null || jwt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utente non autenticato.");
        }

        // Chiama il T23Service per rimuovere l'amico
        Boolean result = t23Service.removeFriend(jwt, friendId);

        if (result != null && result) {
            return ResponseEntity.ok("Amico rimosso con successo.");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore durante la rimozione dell'amico.");
        }
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore interno del server.");
    }
    }

    
    
    //cami (02/12)
    @PostMapping("/updateAvatar")
    public ResponseEntity<String> updateAvatar(
        @CookieValue(name = "jwt", required = false) String jwt,
        @RequestParam("avatar") String avatar) {
    try {
        // Verifica se il token JWT è presente
        if (jwt == null || jwt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        // Decodifica il token JWT per ottenere l'ID utente
        byte[] decodedUserObj = Base64.getDecoder().decode(jwt.split("\\.")[1]);
        String decodedUserJson = new String(decodedUserObj, StandardCharsets.UTF_8);

        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> map = mapper.readValue(decodedUserJson, Map.class);
        String userId = map.get("userId").toString();
        Integer userIdAsInteger = Integer.parseInt(userId);
        // Chiamata al metodo del T23Service per aggiornare l'avatar
        Boolean updateSuccess = (Boolean) t23Service.updateAvatar(userIdAsInteger, avatar);

        if (updateSuccess) {
            return ResponseEntity.ok("Avatar updated successfully!");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to update avatar.");
        }
    } catch (Exception e) {
        System.err.println("Error updating avatar: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error.");
    }
    }

    @GetMapping("/getAvatar")
    public ResponseEntity<String> getAvatar(
    @CookieValue(name = "jwt", required = false) String jwt) {
    try {
        // Verifica se il token JWT è presente
        if (jwt == null || jwt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        // Decodifica il token JWT per ottenere l'ID utente
        byte[] decodedUserObj = Base64.getDecoder().decode(jwt.split("\\.")[1]);
        String decodedUserJson = new String(decodedUserObj, StandardCharsets.UTF_8);

        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> map = mapper.readValue(decodedUserJson, Map.class);
        String userId = map.get("userId").toString();

        // Chiamata al servizio di T23 per recuperare l'avatar
        String avatar = t23Service.getAvatar(userId);

        // Restituisci il valore dell'avatar
        if (avatar != null) {
            return ResponseEntity.ok(avatar);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Avatar not found");
        }
    } catch (Exception e) {
        System.err.println("Error fetching avatar: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error.");
    }
    }



    @GetMapping("/getUserInfo")
    public ResponseEntity<Map<String, String>> getUserInfo(@CookieValue(name = "jwt", required = false) String jwt) {
    try {
        // Verifica che il token JWT esista
        if (jwt == null || jwt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); // Utente non autenticato
        }

        // Decodifica il token JWT per ottenere l'ID utente
        byte[] decodedUserObj = Base64.getDecoder().decode(jwt.split("\\.")[1]);
        String decodedUserJson = new String(decodedUserObj, StandardCharsets.UTF_8);

        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> map = mapper.readValue(decodedUserJson, Map.class);
        String userId = map.get("userId").toString();

        // Chiamata al servizio T23 per ottenere le informazioni utente
        Map<String, String> userInfo = t23Service.getUserInfo(userId);
        if (userInfo != null) {
            return ResponseEntity.ok(userInfo);
        } else {
            return ResponseEntity.status(404).body(null); // Informazioni utente non trovate
        }
    } catch (Exception e) {
        System.out.println("Errore durante il recupero delle informazioni utente: " + e.getMessage());
        return ResponseEntity.status(500).body(null); // Errore del server
    }
    }

    //Gabman 09/12 (Update user info)
    @PostMapping("/updateUserInfo")
    public ResponseEntity<String> updateUserInfo(
            @CookieValue(name = "jwt", required = false) String jwt,
            @RequestParam("name") String name,
            @RequestParam("surname") String surname,
            @RequestParam("nickname") String nickname) {
        try {
            // Decodifica il token JWT per ottenere l'ID utente
            byte[] decodedUserObj = Base64.getDecoder().decode(jwt.split("\\.")[1]);
            String decodedUserJson = new String(decodedUserObj, StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(decodedUserJson, Map.class);
            String userId = map.get("userId").toString();

            // Chiamata al metodo del T23Service
            Boolean updateSuccess = t23Service.updateUserInfo(userId, name, surname, nickname);
            if (updateSuccess) {
                return ResponseEntity.ok("User information updated successfully!");
            } else {
                return ResponseEntity.status(400).body("Failed to update user information.");
            }
        } catch (Exception e) {
            System.out.println("Error updating user information: " + e.getMessage());
            return ResponseEntity.status(500).body("Internal server error.");
        }
    }

    //GabMan 03/12 (Ottengo lista amici)
    @GetMapping("/getFriendlist")
    public ResponseEntity<List<Map<String, String>>> getFriendlist(
        @CookieValue(name = "jwt", required = false) String jwt) {
    try {
        // 1. Verifica il JWT
        if (jwt == null || jwt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); // Utente non autenticato
        }

        // 2. Decodifica del JWT
        Integer userId = extractUserIdFromJwt(jwt);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // JWT non valido
        }

        // 3. Chiamata al T23Service per ottenere la lista amici
        List<Map<String, String>> friendlist = t23Service.getFriendlist(userId.toString());
        if (friendlist == null || friendlist.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // Nessun amico trovato
        }

        return ResponseEntity.ok(friendlist); // Restituisci la lista amici
    } catch (Exception e) {
        // 4. Log degli errori per debugging
        System.err.println("Errore durante il recupero della lista amici: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
    }

/**
 * Metodo per estrarre l'ID utente dal JWT.
 */
    private Integer extractUserIdFromJwt(String jwt) {
    try {
        // Decodifica il JWT
        byte[] decodedUserObj = Base64.getDecoder().decode(jwt.split("\\.")[1]);
        String decodedUserJson = new String(decodedUserObj, StandardCharsets.UTF_8);

        // Converte il payload del JWT in una mappa
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> map = mapper.readValue(decodedUserJson, Map.class);

        // Restituisce l'ID utente
        return Integer.parseInt(map.get("userId").toString());
    } catch (Exception e) {
        System.err.println("Errore nella decodifica del JWT: " + e.getMessage());
        return null; // Ritorna null in caso di errore
    }
    }

    //GabMan 09/12 (Ottengo ID utente)
    @GetMapping("/getUserId")
    public ResponseEntity<Map<String, String>> getUserId(@CookieValue(name = "jwt", required = false) String jwt) {
    try {
        // Usa il metodo esistente per estrarre l'userId
        Integer userId = extractUserIdFromJwt(jwt);

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); // Utente non autenticato
        }

        // Restituisci l'userId come risposta JSON
        Map<String, String> response = new HashMap<>();
        response.put("userId", userId.toString());
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        System.err.println("Errore durante il recupero dell'userId: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // Errore generico
    }
    }


    @PostMapping("/updateProfilePicture")
    public ResponseEntity<String> updateProfilePicture(
        @RequestBody Map<String, String> payload,
        @CookieValue(name = "jwt", required = false) String jwt
    ) {
        try {
            if (jwt == null || jwt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utente non autenticato.");
            }

            Integer userId = extractUserIdFromJwt(jwt);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token JWT non valido.");
            }

            String base64Image = payload.get("profilePicture");
            if (base64Image == null || base64Image.isEmpty()) {
                return ResponseEntity.badRequest().body("Nessuna immagine fornita.");
            }

            boolean success = t23Service.updateProfilePicture(userId, base64Image);
            return success
                ? ResponseEntity.ok("Immagine caricata con successo!")
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore durante il caricamento dell'immagine.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Si è verificato un errore.");
        }
    }



   @GetMapping("/getImage")
    public ResponseEntity<Map<String, String>> getImage(@CookieValue(name = "jwt", required = false) String jwt) {
        try {
            if (jwt == null || jwt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Utente non autenticato."));
            }

            Integer userId = extractUserIdFromJwt(jwt);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token JWT non valido."));
            }

            String imageUrl = t23Service.getImage(userId);
            if (imageUrl != null) {
                return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Immagine non trovata."));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Errore nel recupero dell'immagine."));
        }
    }


}


















